package com.ajesh.syncspend

import com.ajesh.syncspend.csv.CategoryIconGuesser
import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.domain.model.DefaultCategories
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultCategoriesTest {
    private fun cat(name: String, type: FlowType, archived: Boolean = false) =
        CategoryEntity(name = name, iconKey = "tag", type = type, sortOrder = 0, archived = archived)

    @Test fun shipsTheElevenStarterCategories() {
        val all = DefaultCategories.all
        assertEquals(11, all.size)
        assertEquals(8, all.count { it.type == FlowType.EXPENSE })
        assertEquals(listOf("Salary", "Freelance", "Gift"), all.filter { it.type == FlowType.INCOME }.map { it.name })
        assertEquals(all.map { it.name to it.type }.distinct().size, all.size)
    }

    @Test fun everyDefaultHasARealPresetIconMatchingTheImportGuess() {
        DefaultCategories.all.forEach {
            assertTrue("${it.name} icon ${it.iconKey} is not a preset icon", it.iconKey in SyncSpendIcons.pickerKeys)
            assertEquals("import guess for ${it.name}", it.iconKey, CategoryIconGuesser.guess(it.name))
        }
    }

    @Test fun anEmptyInstallGetsAllOfThem() {
        assertEquals(DefaultCategories.all, DefaultCategories.missingFrom(emptyList()))
    }

    @Test fun existingCategoriesAreNotDuplicatedCaseInsensitively() {
        val existing = listOf(cat("food", FlowType.EXPENSE), cat("SALARY", FlowType.INCOME), cat("Gift ", FlowType.INCOME))
        val missing = DefaultCategories.missingFrom(existing).map { it.name }
        assertEquals(8, missing.size)
        assertTrue("Food" !in missing && "Salary" !in missing && "Gift" !in missing)
    }

    @Test fun sameNameInTheOtherFlowDoesNotCount() {
        val missing = DefaultCategories.missingFrom(listOf(cat("Food", FlowType.INCOME))).map { it.name }
        assertTrue("Food" in missing)
    }

    @Test fun aDeletedDefaultThatWasArchivedIsNotBroughtBack() {
        val missing = DefaultCategories.missingFrom(listOf(cat("Groceries", FlowType.EXPENSE, archived = true))).map { it.name }
        assertTrue("Groceries" !in missing)
    }
}
