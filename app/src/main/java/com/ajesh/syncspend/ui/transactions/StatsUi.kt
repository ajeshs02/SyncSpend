package com.ajesh.syncspend.ui.transactions

import com.ajesh.syncspend.domain.analytics.TipsEngine
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.StatsRange
import com.ajesh.syncspend.domain.model.StatsSummary
import com.ajesh.syncspend.domain.model.TipTone
import com.ajesh.syncspend.util.CurrencyFormatter
import com.ajesh.syncspend.util.DateUtils
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

enum class StatTone { NEUTRAL, POSITIVE, NEGATIVE }

data class StatTileUi(val label: String, val value: String, val note: String, val tone: StatTone)

/** One bar of a chart: [fraction] is 0..1 of the tallest bar; [highlighted] bars use the accent colour. */
data class BarUi(val label: String, val fraction: Float, val highlighted: Boolean)

data class CategoryBarUi(val name: String, val iconKey: String, val totalFormatted: String, val sharePercent: Int)

data class TipUi(val title: String, val body: String, val tone: TipTone)

data class StatsUi(
    val range: StatsRange,
    val kicker: String,
    val topName: String,
    val topIconKey: String,
    val topShareLine: String,
    val topTotal: String,
    val tiles: List<StatTileUi>,
    val monthlyBars: List<BarUi>,
    /** Where the average sits, as a fraction of the tallest bar (null when there's no data). */
    val monthlyAverageFraction: Float?,
    val monthlyCaption: String,
    val categoryBars: List<CategoryBarUi>,
    val weekdayBars: List<BarUi>,
    val weekdayCaption: String,
    val tips: List<TipUi>,
    val findings: List<String>,
    val showComparison: Boolean,
    val currentLabel: String,
    val currentValue: String,
    val previousLabel: String,
    val previousValue: String,
)

/** Turns the computed [StatsSummary] into the exact strings/tiles/bars the Stats tab shows. */
fun buildStatsUi(
    s: StatsSummary,
    kind: StatsRange,
    cur: String,
    subsMonthlyTotal: Double,
    subsCount: Int,
): StatsUi {
    val income = s.flow == FlowType.INCOME
    val label = kind.describe(s.range)
    val prevLabel = s.previousRange?.let { kind.describe(it) } ?: "—"
    fun money(v: Double) = cur + CurrencyFormatter.amount(v)

    val top = s.topCategory
    val topShare = if (top == null || s.total == 0.0) 0 else top.sharePercent
    val trend = s.trendPercent
    val trendGood = trend != null && (if (income) trend >= 0 else trend <= 0)

    val tiles = buildList {
        add(StatTileUi((if (income) "Income · " else "Spent · ") + label, money(s.total), "${s.entryCount} entries", StatTone.NEUTRAL))
        if (trend != null) {
            add(
                StatTileUi(
                    "vs $prevLabel",
                    (if (trend <= 0) "↓ " else "↑ ") + abs(trend) + "%",
                    money(abs(s.total - s.previousTotal)) + " difference",
                    if (trendGood) StatTone.POSITIVE else StatTone.NEGATIVE,
                ),
            )
        } else {
            add(StatTileUi("Average per entry", money(s.averagePerEntry), "Across ${s.entryCount} entries", StatTone.NEUTRAL))
        }
        add(
            StatTileUi(
                if (income) "Spending logged" else "Income logged",
                money(s.otherFlowTotal),
                if (!income && s.savingsRatePercent != null) "You kept ${s.savingsRatePercent}%" else "Net " + money(abs(s.otherFlowTotal - s.total)) + if (income) " spent" else " left",
                StatTone.POSITIVE,
            ),
        )
        add(StatTileUi("Daily average", money(s.dailyAverage), "Across ${maxOf(s.activeDays, 1)} active days", StatTone.NEUTRAL))
    }

    val findings = buildList {
        add(
            if (top == null) "Nothing logged in this period yet."
            else "${top.name} takes $topShare% of your ${if (income) "income" else "spending"} — ${top.count} entries totalling ${money(top.totalAbs)}.",
        )
        add(
            s.biggestEntry?.let { "Largest single entry was ${it.description} at ${money(it.amount)} on ${DateUtils.shortDate(it.date)}." }
                ?: "Nothing logged in this period yet.",
        )
        add(
            if (trend != null) "${if (income) "Income" else "Spending"} is ${if (trend <= 0) "down" else "up"} ${abs(trend)}% against $prevLabel."
            else "${s.entryCount} entries across ${maxOf(s.activeDays, 1)} days, averaging ${money(s.dailyAverage)} a day.",
        )
        add("Subscriptions run ${money(subsMonthlyTotal)} a month across $subsCount ${if (subsCount == 1) "service" else "services"}.")
    }

    // Six-month bars: months inside the selected range are highlighted, the rest dimmed.
    val firstInRange = YearMonth.from(s.range.start)
    val lastInRange = YearMonth.from(s.range.end)
    val maxMonth = s.monthly.maxOfOrNull { it.total } ?: 0.0
    val monthlyBars = s.monthly.map {
        BarUi(
            label = it.month.month.getDisplayName(TextStyle.SHORT, Locale.US),
            fraction = if (maxMonth > 0) (it.total / maxMonth).toFloat() else 0f,
            highlighted = !it.month.isBefore(firstInRange) && !it.month.isAfter(lastInRange),
        )
    }
    val nonEmpty = s.monthly.filter { it.total > 0 }
    val monthAverage = if (nonEmpty.isEmpty()) 0.0 else nonEmpty.sumOf { it.total } / nonEmpty.size
    val monthlyCaption = if (nonEmpty.isEmpty()) {
        "Nothing logged in the last six months."
    } else {
        val high = nonEmpty.maxBy { it.total }
        val low = nonEmpty.minBy { it.total }
        val name = { m: YearMonth -> m.month.getDisplayName(TextStyle.SHORT, Locale.US) }
        if (nonEmpty.size == 1) "Only ${name(high.month)} has entries so far."
        else "Highest ${name(high.month)} ${money(high.total)} · Lowest ${name(low.month)} ${money(low.total)} · Average ${money(monthAverage)}"
    }

    val maxWeekday = s.weekday.maxOfOrNull { it.average } ?: 0.0
    val peak = s.weekday.maxByOrNull { it.average }?.takeIf { it.average > 0 }
    val weekdayBars = s.weekday.map {
        BarUi(
            label = it.day.getDisplayName(TextStyle.NARROW, Locale.US),
            fraction = if (maxWeekday > 0) (it.average / maxWeekday).toFloat() else 0f,
            highlighted = it == peak,
        )
    }
    val weekdayCaption = peak?.let {
        "You ${if (income) "earn" else "spend"} the most on ${it.day.getDisplayName(TextStyle.FULL, Locale.US)}s — about ${money(it.average)} each."
    } ?: ""

    return StatsUi(
        range = kind,
        kicker = (if (income) "Top income source · " else "Top spending category · ") + label,
        topName = top?.name ?: "—",
        topIconKey = top?.iconKey ?: "receipt",
        topShareLine = "$topShare% of $label",
        topTotal = money(top?.totalAbs ?: 0.0),
        tiles = tiles,
        monthlyBars = monthlyBars,
        monthlyAverageFraction = if (maxMonth > 0 && monthAverage > 0) (monthAverage / maxMonth).toFloat() else null,
        monthlyCaption = monthlyCaption,
        categoryBars = s.categories.take(5).map { CategoryBarUi(it.name, it.iconKey, money(it.totalAbs), it.sharePercent) },
        weekdayBars = weekdayBars,
        weekdayCaption = weekdayCaption,
        tips = TipsEngine.build(s, subsMonthlyTotal, ::money).map { TipUi(it.title, it.body, it.tone) },
        findings = findings,
        showComparison = s.previousRange != null,
        currentLabel = label,
        currentValue = money(s.total),
        previousLabel = prevLabel,
        previousValue = money(s.previousTotal),
    )
}
