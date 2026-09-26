package com.ajesh.syncspend

import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.data.db.entity.TransactionEntity
import com.ajesh.syncspend.domain.analytics.AnalyticsEngine
import com.ajesh.syncspend.domain.analytics.TipsEngine
import com.ajesh.syncspend.domain.model.DateRange
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.TipTone
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TipsEngineTest {
    private val today = LocalDate.of(2026, 9, 20)
    private val sept = DateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))
    private val aug = DateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31))
    private val cats = listOf(
        CategoryEntity(1, "Food", "utensils", FlowType.EXPENSE, 0),
        CategoryEntity(2, "Travel", "plane", FlowType.EXPENSE, 1),
        CategoryEntity(3, "Salary", "bank", FlowType.INCOME, 0),
    )
    private var id = 1L
    private fun tx(amount: Double, date: LocalDate, cat: Long = 1, note: String = "x") =
        TransactionEntity(id++, amount, note, cat, date, id)

    private fun money(v: Double) = "₹" + v.toLong()
    private fun tips(all: List<TransactionEntity>, flow: FlowType = FlowType.EXPENSE, subs: Double = 0.0) =
        TipsEngine.build(AnalyticsEngine.stats(all, cats, sept, aug, flow, today), subs, ::money)

    @Test fun emptyPeriodGivesAnOnboardingHint() {
        val t = tips(emptyList())
        assertEquals(1, t.size)
        assertEquals(TipTone.INFO, t.single().tone)
    }

    @Test fun spendingUpNamesTheBiggestDriver() {
        val all = listOf(
            tx(-1000.0, LocalDate.of(2026, 8, 10), cat = 1),
            tx(-1500.0, LocalDate.of(2026, 9, 10), cat = 2), // Travel jumps
            tx(-1000.0, LocalDate.of(2026, 9, 11), cat = 1),
        )
        val t = tips(all)
        val up = t.first { it.title.startsWith("Spending is up") }
        assertEquals(TipTone.WATCH, up.tone)
        assertTrue(up.body.contains("Travel"))
    }

    @Test fun spendingDownIsEncouraged() {
        val all = listOf(tx(-2000.0, LocalDate.of(2026, 8, 10)), tx(-1000.0, LocalDate.of(2026, 9, 10)))
        val t = tips(all)
        assertTrue(t.any { it.tone == TipTone.GOOD && it.title.startsWith("Spending is down") })
    }

    @Test fun concentratedCategoryIsFlagged() {
        val all = listOf(
            tx(-900.0, LocalDate.of(2026, 9, 2), cat = 1),
            tx(-100.0, LocalDate.of(2026, 9, 3), cat = 2),
        )
        val t = tips(all)
        val tip = t.first { it.title.contains("Food is 90%") }
        assertTrue(tip.body.contains("₹90")) // 10% of 900
    }

    @Test fun lowSavingsRateSuggestsWhatToCut() {
        val all = listOf(
            tx(10000.0, LocalDate.of(2026, 9, 1), cat = 3),
            tx(-9500.0, LocalDate.of(2026, 9, 5), cat = 1),
            tx(-200.0, LocalDate.of(2026, 9, 6), cat = 2),
        )
        val tip = tips(all).first { it.title.startsWith("You kept only") }
        assertEquals(TipTone.WATCH, tip.tone)
        assertTrue(tip.body.contains("₹1700")) // target 20% of 10000 = 2000, already keeping 300
    }

    @Test fun strongSavingsRateIsPraised() {
        val all = listOf(tx(10000.0, LocalDate.of(2026, 9, 1), cat = 3), tx(-2000.0, LocalDate.of(2026, 9, 5)), tx(-500.0, LocalDate.of(2026, 9, 9), cat = 2))
        assertTrue(tips(all).any { it.tone == TipTone.GOOD && it.title.contains("kept 75%") })
    }

    @Test fun neverMoreThanFourTipsAndWatchFirst() {
        val all = buildList {
            add(tx(50000.0, LocalDate.of(2026, 9, 1), cat = 3))
            add(tx(-500.0, LocalDate.of(2026, 8, 5)))
            repeat(12) { add(tx(-20.0, LocalDate.of(2026, 9, 2 + it), cat = 2)) }
            add(tx(-30000.0, LocalDate.of(2026, 9, 5), cat = 1, note = "Laptop"))
        }
        val t = tips(all, subs = 9000.0)
        assertTrue(t.size <= 4)
        assertEquals(t.sortedBy { it.tone.ordinal }, t)
    }

    @Test fun incomeSideTips() {
        val all = listOf(tx(9000.0, LocalDate.of(2026, 9, 1), cat = 3), tx(500.0, LocalDate.of(2026, 8, 1), cat = 3))
        val t = tips(all, flow = FlowType.INCOME)
        assertTrue(t.any { it.title.startsWith("Income is up") })
        assertTrue(t.any { it.title.contains("of income") })
    }
}
