package com.ajesh.syncspend

import com.ajesh.syncspend.ui.components.CountUp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CountUpTest {
    private val final = "12,345.67"

    @Test fun startsAtZeroWithNoLeadingZerosAndEndsOnTheExactFigure() {
        assertEquals("0.00", CountUp.frameText(final, 0f).trim())
        assertEquals(final, CountUp.frameText(final, CountUp.TOTAL_MS))
        assertEquals(final, CountUp.frameText(final, CountUp.TOTAL_MS + 500f))
    }

    @Test fun smallAmountsKeepTheirWholeFigureVisible() {
        assertEquals("0.00", CountUp.frameText("0.00", 0f))
        assertEquals("5.50", CountUp.frameText("5.50", CountUp.TOTAL_MS))
    }

    @Test fun eachPlaceTakesLongerThanTheOneToItsLeft() {
        val durations = (0 until 7).map { CountUp.durationMs(it, 7) }
        assertEquals(CountUp.LEFT_MS, durations.first(), 0f)
        assertEquals(CountUp.RIGHT_MS, durations.last(), 0f)
        assertEquals(durations.sorted(), durations)
        assertEquals(durations.distinct(), durations)
    }

    @Test fun leftmostDigitLocksInBeforeTheRightmostOne() {
        val leftFinal = final.first()
        val rightFinal = final.last()
        var sawRightStillRolling = false
        var t = CountUp.LEFT_MS
        while (t < CountUp.RIGHT_MS - 100f) {
            val frame = CountUp.frame(final, t)
            assertEquals("leftmost digit must be settled once its time is up (t=$t)", leftFinal, frame.first().char)
            if (frame.last().char != rightFinal) sawRightStillRolling = true
            t += 25f
        }
        assertTrue("the last digit should still be rolling after the first has settled", sawRightStillRolling)
    }

    @Test fun easingIsMonotoneAndDecelerating() {
        val samples = (0..20).map { CountUp.easeOut(it / 20f) }
        assertEquals(0f, samples.first(), 0f)
        assertEquals(1f, samples.last(), 0f)
        assertEquals(samples.sorted(), samples)
        val gaps = samples.zipWithNext { a, b -> b - a }
        assertTrue("each step should be no bigger than the last (ease-out)", gaps.zipWithNext().all { (a, b) -> b <= a + 1e-6f })
    }

    @Test fun commasAppearOnlyOnceTheDigitBeforeThemDoes() {
        // Early on the value is far below 10,000, so the ten-thousands digit and its comma stay hidden.
        val early = CountUp.frame(final, 5f)
        assertEquals(false, early[0].visible) // '1' (ten-thousands)
        assertEquals(false, early[2].visible) // ','
        assertEquals(true, early[early.size - 3].visible) // '.'
    }
}
