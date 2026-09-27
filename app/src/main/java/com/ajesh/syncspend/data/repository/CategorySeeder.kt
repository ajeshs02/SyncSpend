package com.ajesh.syncspend.data.repository

import com.ajesh.syncspend.data.datastore.PreferencesRepository
import com.ajesh.syncspend.domain.model.DefaultCategories
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Adds the [DefaultCategories] once per install — fresh installs and existing
 * ones alike (on the first launch after this shipped), skipping any the user
 * already has. A flag in DataStore makes it a one-off, so categories the user
 * later deletes stay deleted. Safe to re-run after an interruption: it only
 * ever adds what is missing.
 */
class CategorySeeder(
    private val categories: CategoryRepository,
    private val preferences: PreferencesRepository,
) {
    // Two overlapping calls would both see "not seeded yet" and insert everything twice.
    private val lock = Mutex()

    suspend fun seedIfNeeded() = lock.withLock {
        if (preferences.preferences.first().defaultCategoriesSeeded) return@withLock
        insertMissing()
        preferences.markDefaultCategoriesSeeded()
    }

    /** Re-adds the starter categories after all data is cleared, ignoring the one-time flag (already set). */
    suspend fun reseed() = lock.withLock { insertMissing() }

    private suspend fun insertMissing() {
        DefaultCategories.missingFrom(categories.getAllOnce()).forEach {
            categories.createNext(it.name, it.iconKey, it.type)
        }
    }
}
