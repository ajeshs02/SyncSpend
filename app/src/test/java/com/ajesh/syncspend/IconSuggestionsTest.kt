package com.ajesh.syncspend

import com.ajesh.syncspend.csv.CategoryIconGuesser
import com.ajesh.syncspend.ui.icons.IconKind
import com.ajesh.syncspend.ui.icons.IconSuggestions
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IconSuggestionsTest {
    @Test fun aReminderStartsWithABellAndAClock() {
        assertEquals(listOf("bell", "clock", "cal", "wallet", "receipt"), IconSuggestions.suggest(IconKind.REMINDER, ""))
    }

    @Test fun aSubscriptionStartsWithRepeatAndCard() {
        assertEquals(listOf("repeat", "card", "film", "music", "wifi"), IconSuggestions.suggest(IconKind.SUBSCRIPTION, ""))
    }

    @Test fun theTypedNameComesFirst() {
        assertEquals(listOf("film", "repeat", "card", "music", "wifi"), IconSuggestions.suggest(IconKind.SUBSCRIPTION, "Netflix"))
        assertEquals(listOf("house", "bell", "clock", "cal", "wallet"), IconSuggestions.suggest(IconKind.REMINDER, "Rent due"))
        assertEquals("dumbbell", IconSuggestions.suggest(IconKind.SUBSCRIPTION, "Cult gym").first())
    }

    @Test fun aCategoryOnlySuggestsWhatTheNameMatches() {
        assertEquals(emptyList<String>(), IconSuggestions.suggest(IconKind.CATEGORY, ""))
        assertEquals(emptyList<String>(), IconSuggestions.suggest(IconKind.CATEGORY, "zzzz"))
        assertEquals("coffee", IconSuggestions.suggest(IconKind.CATEGORY, "Coffee runs").first())
    }

    @Test fun neverMoreThanFiveDistinctRealIcons() {
        listOf("", "Netflix", "Rent", "Gym membership", "Electricity bill", "Petrol", "x").forEach { name ->
            IconKind.entries.forEach { kind ->
                val out = IconSuggestions.suggest(kind, name)
                assertTrue("$kind/$name: $out", out.size <= IconSuggestions.LIMIT)
                assertEquals(out.distinct(), out)
                assertTrue("$kind/$name has an icon that isn't in the picker: $out", out.all { it in SyncSpendIcons.pickerKeys })
            }
        }
    }

    @Test fun theImportGuesserStillPicksOneIconPerCategory() {
        assertEquals("utensils", CategoryIconGuesser.guess("Food"))
        assertEquals("tag", CategoryIconGuesser.guess("Something unknown"))
        assertEquals("fuel", CategoryIconGuesser.guess("Transportation / Fuel"))
    }
}
