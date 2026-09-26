package com.ajesh.syncspend

import com.ajesh.syncspend.ui.components.NAV_ADD_SLOT
import com.ajesh.syncspend.ui.components.NavAddCellWidth
import com.ajesh.syncspend.ui.components.NavCellWidth
import com.ajesh.syncspend.ui.components.navSlotFor
import com.ajesh.syncspend.ui.components.navSlotOffset
import com.ajesh.syncspend.ui.components.navSlotWidth
import org.junit.Assert.assertEquals
import org.junit.Test

/** The sliding pill must land exactly on the icon it highlights — every slot, including those right of the wide Add cell. */
class NavGeometryTest {
    @Test fun routesMapToSlots() {
        assertEquals(0, navSlotFor("home"))
        assertEquals(1, navSlotFor("transactions"))
        assertEquals(NAV_ADD_SLOT, navSlotFor("add_entry"))
        assertEquals(3, navSlotFor("categories"))
        assertEquals(4, navSlotFor("settings"))
        assertEquals(-1, navSlotFor("subs_reminders/{listMode}"))
        assertEquals(-1, navSlotFor(null))
    }

    @Test fun offsetsAreTheSumOfThePrecedingCellWidths() {
        val cell = NavCellWidth.value
        val add = NavAddCellWidth.value
        assertEquals(0f, navSlotOffset(0).value, 0f)
        assertEquals(cell, navSlotOffset(1).value, 0f)
        assertEquals(2 * cell, navSlotOffset(2).value, 0f)
        assertEquals(2 * cell + add, navSlotOffset(3).value, 0f) // Categories: after Home, Transactions AND Add
        assertEquals(3 * cell + add, navSlotOffset(4).value, 0f) // Settings
    }

    @Test fun everySlotIsDistinctAndTheRowFillsExactlyTheCellWidths() {
        val offsets = (0..4).map { navSlotOffset(it).value }
        assertEquals(offsets.distinct(), offsets)
        val total = (0..4).sumOf { navSlotWidth(it).value.toDouble() }
        assertEquals(total, (navSlotOffset(4) + navSlotWidth(4)).value.toDouble(), 1e-6)
        assertEquals(NavAddCellWidth.value, navSlotWidth(NAV_ADD_SLOT).value, 0f)
    }
}
