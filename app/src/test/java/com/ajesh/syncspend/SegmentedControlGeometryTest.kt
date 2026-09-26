package com.ajesh.syncspend

import com.ajesh.syncspend.ui.components.closeness
import org.junit.Assert.assertEquals
import org.junit.Test

/** A segment's label/icon colour follows the sliding pill: 1 with the pill on it, 0 a full segment away. */
class SegmentedControlGeometryTest {
    @Test fun fullyHighlightedOnlyWhenThePillIsExactlyOverIt() {
        assertEquals(1f, closeness(2f, 2), 0f)
        assertEquals(0f, closeness(2f, 1), 0f)
        assertEquals(0f, closeness(2f, 0), 0f)
    }

    @Test fun halfwayBetweenTwoSegmentsSplitsTheHighlightEvenly() {
        assertEquals(0.5f, closeness(0.5f, 0), 1e-6f)
        assertEquals(0.5f, closeness(0.5f, 1), 1e-6f)
        assertEquals(0f, closeness(0.5f, 2), 0f)
    }

    @Test fun neverLeavesZeroToOne() {
        listOf(-3f, -0.5f, 0f, 1.25f, 9f).forEach { pos ->
            (0..3).forEach { i -> assertEquals(true, closeness(pos, i) in 0f..1f) }
        }
    }
}
