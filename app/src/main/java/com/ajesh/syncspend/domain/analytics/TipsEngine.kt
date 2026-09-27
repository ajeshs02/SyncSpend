package com.ajesh.syncspend.domain.analytics

import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.StatsSummary
import com.ajesh.syncspend.domain.model.labelFor
import com.ajesh.syncspend.domain.model.Tip
import com.ajesh.syncspend.domain.model.TipTone
import java.time.DayOfWeek
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Rule-based tips for the Stats tab: each rule looks at the numbers already in
 * a [StatsSummary] and, when it fires, says what it noticed and what to do
 * about it. Deterministic and offline — no model, no network. The most useful
 * few are kept (worth-a-look first, then encouragement, then FYIs).
 */
object TipsEngine {
    private const val MAX_TIPS = 4

    /**
     * @param subscriptionsMonthly what active subscriptions cost per month
     * @param money formats an amount with the user's currency symbol
     */
    fun build(s: StatsSummary, subscriptionsMonthly: Double, money: (Double) -> String): List<Tip> {
        if (s.entryCount == 0) {
            return listOf(
                Tip(TipTone.INFO, "Nothing to analyse yet", "Log a few entries in this period and tips will show up here."),
            )
        }
        val tips = if (s.flow == FlowType.EXPENSE) expenseTips(s, subscriptionsMonthly, money) else incomeTips(s, money)
        return tips.sortedBy { it.tone.ordinal }.take(MAX_TIPS)
    }

    private fun expenseTips(s: StatsSummary, subsMonthly: Double, money: (Double) -> String): List<Tip> = buildList {
        val trend = s.trendPercent
        if (trend != null && trend >= 15) {
            val driver = s.movers.firstOrNull { it.delta > 0 }
            add(
                Tip(
                    TipTone.WATCH,
                    "Spending is up $trend%",
                    "You spent ${money(s.total - s.previousTotal)} more than the previous period." +
                        (driver?.let { " Biggest driver: ${it.name} (+${money(it.delta)})." } ?: ""),
                ),
            )
        } else if (trend != null && trend <= -10) {
            add(Tip(TipTone.GOOD, "Spending is down ${abs(trend)}%", "${money(s.previousTotal - s.total)} less than the previous period. Keep it up."))
        }

        val top = s.topCategory
        if (top != null && top.sharePercent >= 40 && s.categories.size > 1) {
            add(
                Tip(
                    TipTone.WATCH,
                    "${top.name} is ${top.sharePercent}% of spending",
                    "It's the one place a small change matters most: trimming it by 10% would free up about ${money(top.totalAbs * 0.1)}.",
                ),
            )
        }

        val rate = s.savingsRatePercent
        if (rate != null) {
            val income = s.otherFlowTotal
            when {
                rate < 0 -> add(Tip(TipTone.WATCH, "Spending is above income", "You spent ${money(s.total - income)} more than you earned in this period."))
                rate < 10 -> {
                    val gap = income * 0.2 - (income - s.total)
                    add(Tip(TipTone.WATCH, "You kept only $rate% of your income", "A common rule of thumb is to save about 20%. Cutting ${money(gap)} would get you there."))
                }
                rate >= 30 -> add(Tip(TipTone.GOOD, "You kept $rate% of your income", "That's a strong savings rate. Consider moving some of it somewhere it can grow."))
            }
        }

        s.projection?.let { p ->
            if (p.previousMonthTotal > 0 && p.projected > p.previousMonthTotal * 1.1) {
                val daysLeft = p.daysInMonth - p.dayOfMonth
                val body = if (daysLeft > 0 && p.soFar < p.previousMonthTotal) {
                    "That's ${money(p.projected - p.previousMonthTotal)} above last month. Keeping to about ${money((p.previousMonthTotal - p.soFar) / daysLeft)} a day would match it."
                } else {
                    "That's ${money(p.projected - p.previousMonthTotal)} above last month."
                }
                add(Tip(TipTone.WATCH, "On pace for ${money(p.projected)} this month", body))
            } else if (p.previousMonthTotal > 0 && p.projected < p.previousMonthTotal * 0.9) {
                add(Tip(TipTone.GOOD, "On track to spend less than last month", "At this pace you'll finish around ${money(p.projected)} against ${money(p.previousMonthTotal)} last month."))
            }
        }

        val weekend = s.weekday.filter { it.day == DayOfWeek.SATURDAY || it.day == DayOfWeek.SUNDAY }.sumOf { it.total }
        val weekendShare = if (s.total > 0) (weekend / s.total * 100).roundToInt() else 0
        if (s.entryCount >= 6 && weekendShare >= 45) {
            add(Tip(TipTone.INFO, "Weekends drive $weekendShare% of spending", "Two days a week take nearly half. A rough plan for Saturday and Sunday goes a long way."))
        }

        val months = (ChronoUnit.DAYS.between(s.range.start, s.range.end) + 1) / 30.4
        val monthlySpend = if (months > 0) s.total / months.coerceAtLeast(1.0) else s.total
        if (subsMonthly > 0 && monthlySpend > 0 && subsMonthly / monthlySpend >= 0.15) {
            add(Tip(TipTone.INFO, "Subscriptions are ${(subsMonthly / monthlySpend * 100).roundToInt()}% of monthly spend", "${money(subsMonthly)} a month. Worth a look for anything you rarely use."))
        }

        s.smallPurchases?.let { sp ->
            if (sp.sharePercent >= 15) {
                add(Tip(TipTone.INFO, "Small purchases add up", "${sp.count} entries under ${money(sp.threshold)} each total ${money(sp.total)}, which is ${sp.sharePercent}% of spending."))
            }
        }

        val days = ChronoUnit.DAYS.between(s.range.start, s.range.end) + 1
        if (s.noSpendDays >= 3 && days <= 100) {
            add(Tip(TipTone.GOOD, "${s.noSpendDays} no-spend days", "Each one is a day your money stayed put."))
        }

        s.biggestEntry?.let { big ->
            val share = if (s.total > 0) abs(big.amount) / s.total * 100 else 0.0
            if (s.entryCount >= 3 && share >= 35) {
                add(Tip(TipTone.INFO, "One purchase was ${share.roundToInt()}% of spending", "“${s.labelFor(big)}” at ${money(abs(big.amount))}. Big one-offs are worth planning for."))
            }
        }
    }

    private fun incomeTips(s: StatsSummary, money: (Double) -> String): List<Tip> = buildList {
        val trend = s.trendPercent
        if (trend != null && trend >= 10) {
            add(Tip(TipTone.GOOD, "Income is up $trend%", "${money(s.total - s.previousTotal)} more than the previous period."))
        } else if (trend != null && trend <= -15) {
            add(Tip(TipTone.WATCH, "Income is down ${abs(trend)}%", "${money(s.previousTotal - s.total)} less than the previous period. Worth checking what changed."))
        }
        val top = s.topCategory
        if (top != null && top.sharePercent >= 80 && s.categories.size >= 1) {
            add(Tip(TipTone.INFO, "${top.name} is ${top.sharePercent}% of income", "Relying on one source is risky. Even a small second stream adds a cushion."))
        }
        if (s.categories.size >= 3) {
            add(Tip(TipTone.GOOD, "${s.categories.size} income sources", "A spread of sources makes your income steadier."))
        }
    }
}
