package com.ajesh.syncspend.ui.transactions

import com.ajesh.syncspend.domain.analytics.AnalyticsEngine
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.StatsSummary
import com.ajesh.syncspend.util.CurrencyFormatter
import com.ajesh.syncspend.util.DateUtils
import kotlin.math.abs

enum class StatTone { NEUTRAL, POSITIVE, NEGATIVE }

data class StatTileUi(val label: String, val value: String, val note: String, val tone: StatTone)

data class StatsUi(
    val kicker: String,
    val topName: String,
    val topIconKey: String,
    val topShareLine: String,
    val topTotal: String,
    val tiles: List<StatTileUi>,
    val findings: List<String>,
    val currentLabel: String,
    val currentValue: String,
    val previousLabel: String,
    val previousValue: String,
)

/** Turns the computed [StatsSummary] into the exact strings/tiles the design shows. */
fun buildStatsUi(
    s: StatsSummary,
    cur: String,
    subsMonthlyTotal: Double,
    subsCount: Int,
): StatsUi {
    val income = s.flow == FlowType.INCOME
    val scopeLabel = AnalyticsEngine.scopeLabel(s.scope)
    val prevLabel = AnalyticsEngine.previousScope(s.scope)?.let { AnalyticsEngine.scopeLabel(it) } ?: "—"
    fun money(v: Double) = cur + CurrencyFormatter.amount(v)

    val top = s.topCategory
    val topShare = if (top == null || s.total == 0.0) 0 else top.sharePercent
    val trend = s.trendPercent
    val trendGood = trend != null && (if (income) trend >= 0 else trend <= 0)

    val tiles = buildList {
        add(StatTileUi((if (income) "Income · " else "Spent · ") + scopeLabel, money(s.total), "${s.entryCount} entries", StatTone.NEUTRAL))
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
                "Net " + money(abs(s.otherFlowTotal - s.total)) + if (income) " spent" else " left",
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

    return StatsUi(
        kicker = (if (income) "Top income source · " else "Top spending category · ") + scopeLabel,
        topName = top?.name ?: "—",
        topIconKey = top?.iconKey ?: "receipt",
        topShareLine = "$topShare% of $scopeLabel",
        topTotal = money(top?.totalAbs ?: 0.0),
        tiles = tiles,
        findings = findings,
        currentLabel = scopeLabel,
        currentValue = money(s.total),
        previousLabel = prevLabel,
        previousValue = money(s.previousTotal),
    )
}
