package com.ajesh.syncspend.ui.transactions

import com.ajesh.syncspend.domain.analytics.AnalyticsEngine
import com.ajesh.syncspend.domain.analytics.TipsEngine
import com.ajesh.syncspend.domain.model.labelFor
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.ScopePeriod
import com.ajesh.syncspend.domain.model.StatsSummary
import com.ajesh.syncspend.domain.model.TipTone
import com.ajesh.syncspend.util.CurrencyFormatter
import com.ajesh.syncspend.util.DateUtils
import java.time.DayOfWeek
import java.time.LocalDate
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

/** A category that moved against the previous period; [good] means the move is welcome (spending down / income up). */
data class MoverUi(val name: String, val iconKey: String, val deltaFormatted: String, val up: Boolean, val good: Boolean)

data class TopEntryUi(val title: String, val subtitle: String, val amount: String)

/**
 * The compact Savings card on Stats — period-scoped *movement* (this range's contributions minus
 * withdrawals), never the all-time cumulative Savings Balance shown on the Transfer screen; the two
 * are never the same number and [periodLabel] makes that explicit. Flow-independent (shown the same
 * whether Expense or Income is selected), kept structurally separate from [StatsUi.tiles]/[StatsUi.categoryBars].
 */
data class SavingsSummaryUi(
    val periodLabel: String,
    val contributionsFormatted: String,
    val withdrawalsFormatted: String,
    val netFormatted: String,
    val netPositive: Boolean,
)

/** The current-month forecast: fractions are of the largest of so-far / projected / last month. */
data class PaceUi(
    val title: String,
    val caption: String,
    val soFarFraction: Float,
    val projectedFraction: Float,
    val lastMonthFraction: Float?,
)

data class StatsUi(
    val scope: ScopePeriod,
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
    val movers: List<MoverUi>,
    val topEntries: List<TopEntryUi>,
    val pace: PaceUi?,
    val findings: List<String>,
    val showComparison: Boolean,
    val currentLabel: String,
    val currentValue: String,
    val previousLabel: String,
    val previousValue: String,
    val savingsSummary: SavingsSummaryUi,
)

/** Turns the computed [StatsSummary] into the exact strings/tiles/bars the Stats tab shows. */
fun buildStatsUi(
    s: StatsSummary,
    scope: ScopePeriod,
    cur: String,
    subsMonthlyTotal: Double,
    subsCount: Int,
    transferTotals: AnalyticsEngine.TransferTotals,
    today: LocalDate = LocalDate.now(),
): StatsUi {
    // Savings reuses the "income" wording/direction throughout this builder (more saved is good, same as
    // more earned) rather than a bespoke Savings-specific dashboard — a deliberate scope call for this
    // round, not an oversight. A dedicated Savings breakdown (balance, contributions vs. savings-funded
    // expenses) is a reasonable follow-up once this shape is proven out.
    val income = s.flow != FlowType.EXPENSE
    // scopeLabel's own default `now` would read the real wall clock, which is fine in production but
    // would make a fixed-`today` test's "Last N months" label flip on whatever day it happens to run —
    // threading `today` through keeps it exactly as deterministic as every other date-aware builder here.
    val thisMonth = YearMonth.from(today)
    val previousScope = AnalyticsEngine.previousScope(scope)
    val label = AnalyticsEngine.scopeLabel(scope, thisMonth)
    val prevLabel = previousScope?.let { AnalyticsEngine.scopeLabel(it, thisMonth) } ?: "-"
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
                if (!income && s.netSavingsRatePercent != null) "You kept ${s.netSavingsRatePercent}%" else "Net " + money(abs(s.otherFlowTotal - s.total)) + if (income) " spent" else " left",
                StatTone.POSITIVE,
            ),
        )
        add(StatTileUi("Daily average", money(s.dailyAverage), "Across ${maxOf(s.activeDays, 1)} active days", StatTone.NEUTRAL))
        val busiest = s.busiestDay
        add(
            StatTileUi(
                if (income) "Biggest income day" else "Busiest day",
                busiest?.let { DateUtils.shortDate(it.date) } ?: "-",
                busiest?.let { money(it.total) + " in ${it.entryCount} ${if (it.entryCount == 1) "entry" else "entries"}" } ?: "Nothing logged yet",
                StatTone.NEUTRAL,
            ),
        )
        add(
            if (income) StatTileUi("Income days", s.activeDays.toString(), "Days money came in", StatTone.NEUTRAL)
            else StatTileUi("No-spend days", s.noSpendDays.toString(), "Days nothing was spent", StatTone.POSITIVE),
        )
    }

    val findings = buildList {
        add(
            if (top == null) "Nothing logged in this period yet."
            else "${top.name} takes $topShare% of your ${if (income) "income" else "spending"}: ${top.count} entries totalling ${money(top.totalAbs)}.",
        )
        add(
            s.biggestEntry?.let { "Largest single entry was ${s.labelFor(it)} at ${money(it.amount)} on ${DateUtils.shortDate(it.date)}." }
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
        "You ${if (income) "earn" else "spend"} the most on ${it.day.getDisplayName(TextStyle.FULL, Locale.US)}s, about ${money(it.average)} each."
    } ?: ""

    val categoryNames = s.categories.associate { it.categoryId to it.name }
    val movers = s.movers.map {
        MoverUi(
            name = it.name,
            iconKey = it.iconKey,
            deltaFormatted = (if (it.delta > 0) "+" else "−") + money(abs(it.delta)),
            up = it.delta > 0,
            good = if (income) it.delta > 0 else it.delta < 0,
        )
    }
    val topEntries = s.topEntries.map {
        TopEntryUi(
            // The category is the title; the entry's note (when it has one) leads the date line.
            title = categoryNames[it.categoryId] ?: "Deleted category",
            subtitle = (if (it.description.isBlank()) "" else it.description + " · ") + DateUtils.shortDate(it.date),
            amount = money(abs(it.amount)),
        )
    }
    val pace = s.projection?.let { p ->
        val scale = maxOf(p.projected, p.soFar, p.previousMonthTotal).takeIf { it > 0 } ?: 1.0
        PaceUi(
            title = "On pace for ${money(p.projected)} this month",
            caption = "${money(p.soFar)} so far · day ${p.dayOfMonth} of ${p.daysInMonth}" +
                if (p.previousMonthTotal > 0) " · last month ${money(p.previousMonthTotal)}" else "",
            soFarFraction = (p.soFar / scale).toFloat(),
            projectedFraction = (p.projected / scale).toFloat(),
            lastMonthFraction = if (p.previousMonthTotal > 0) (p.previousMonthTotal / scale).toFloat() else null,
        )
    }

    return StatsUi(
        scope = scope,
        kicker = (if (income) "Top income source · " else "Top spending category · ") + label,
        topName = top?.name ?: "-",
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
        movers = movers,
        topEntries = topEntries,
        pace = pace,
        findings = findings,
        showComparison = s.previousRange != null,
        currentLabel = label,
        currentValue = money(s.total),
        previousLabel = prevLabel,
        previousValue = money(s.previousTotal),
        savingsSummary = SavingsSummaryUi(
            periodLabel = label,
            contributionsFormatted = money(transferTotals.contributions),
            withdrawalsFormatted = money(transferTotals.withdrawals),
            netFormatted = money(kotlin.math.abs(transferTotals.net)),
            netPositive = transferTotals.net >= 0,
        ),
    )
}
