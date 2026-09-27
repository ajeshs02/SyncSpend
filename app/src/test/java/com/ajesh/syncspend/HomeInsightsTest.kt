package com.ajesh.syncspend

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasText
import com.ajesh.syncspend.domain.analytics.AnalyticsEngine
import com.ajesh.syncspend.domain.model.ScopePeriod
import com.ajesh.syncspend.ui.home.HomeInsights
import com.ajesh.syncspend.ui.home.RotatingInsight
import java.time.LocalDate
import java.time.YearMonth
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeInsightsTest {
    @get:Rule val rule = createComposeRule()

    @Test fun threeAveragesInWholeRupees() {
        // 10,700 over 9 entries in 27 days.
        assertEquals(
            listOf("Averaging ₹1,189 per entry", "Averaging ₹396 per day", "Averaging ₹2,774 per week"),
            HomeInsights.build(10_700.0, 9, 27, "₹"),
        )
    }

    @Test fun nothingToAverageMeansNoLine() {
        assertEquals(emptyList<String>(), HomeInsights.build(0.0, 0, 27, "₹"))
        assertEquals(emptyList<String>(), HomeInsights.build(100.0, 1, 0, "₹"))
    }

    @Test fun theNextLineIsAlwaysADifferentOne() {
        val random = Random(7)
        repeat(300) {
            val current = random.nextInt(3)
            val next = HomeInsights.nextIndex(current, 3, random)
            assertNotEquals(current, next)
            assertTrue(next in 0..2)
        }
        assertEquals(0, HomeInsights.nextIndex(0, 1))
    }

    @Test fun everyLineGetsItsTurn() {
        val random = Random(1)
        val seen = mutableSetOf<Int>()
        var current = 0
        repeat(60) { current = HomeInsights.nextIndex(current, 3, random); seen += current }
        assertEquals(setOf(0, 1, 2), seen)
    }

    @Test fun daysCountedSoFarInTheCurrentPeriodAndInFullForAFinishedOne() {
        val today = LocalDate.of(2026, 9, 27)
        assertEquals(27, AnalyticsEngine.daysInScope(ScopePeriod.Month(YearMonth.of(2026, 9)), today, null))
        assertEquals(31, AnalyticsEngine.daysInScope(ScopePeriod.Month(YearMonth.of(2026, 8)), today, null))
        assertEquals(270, AnalyticsEngine.daysInScope(ScopePeriod.Year(2026), today, null)) // Jan 1 .. Sep 27
        assertEquals(365, AnalyticsEngine.daysInScope(ScopePeriod.Year(2025), today, null))
        assertEquals(31 + 31 + 27, AnalyticsEngine.daysInScope(ScopePeriod.LastMonths(3, YearMonth.of(2026, 9)), today, null)) // Jul 1 .. Sep 27
        assertEquals(10, AnalyticsEngine.daysInScope(ScopePeriod.AllTime, today, LocalDate.of(2026, 9, 18)))
        assertEquals(1, AnalyticsEngine.daysInScope(ScopePeriod.AllTime, today, null))
        assertEquals(1, AnalyticsEngine.daysInScope(ScopePeriod.Month(YearMonth.of(2026, 10)), today, null)) // a period that hasn't started
    }

    @Test fun theHeroLineChangesByItselfEveryFewSeconds() {
        val lines = listOf("Averaging ₹1 per entry", "Averaging ₹2 per day", "Averaging ₹3 per week")
        rule.mainClock.autoAdvance = false
        rule.setContent { RotatingInsight(lines) }
        rule.mainClock.advanceTimeByFrame()
        fun visible() = lines.single { l -> rule.onAllNodes(hasText(l)).fetchSemanticsNodes().isNotEmpty() }
        val first = visible()
        rule.mainClock.advanceTimeBy(HomeInsights.ROTATE_MILLIS + 1_000)
        val second = visible()
        assertNotEquals(first, second)
        rule.mainClock.advanceTimeBy(HomeInsights.ROTATE_MILLIS)
        assertNotEquals(second, visible())
    }
}
