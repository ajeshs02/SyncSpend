package com.ajesh.syncspend

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ajesh.syncspend.data.datastore.PreferencesRepository
import com.ajesh.syncspend.data.db.AppDatabase
import com.ajesh.syncspend.data.repository.CategoryRepository
import com.ajesh.syncspend.data.repository.CategorySeeder
import com.ajesh.syncspend.domain.model.FlowType
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Real Room (in-memory) + a real, isolated DataStore, to prove the one-time seeding behaves.
 * (Robolectric boots the actual SyncSpendApp too, so these tests deliberately share nothing with it.)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SeedingIntegrationTest {
    @get:Rule val tmp = TemporaryFolder()
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var db: AppDatabase
    private lateinit var categories: CategoryRepository
    private lateinit var seeder: CategorySeeder

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(tmp.root, "prefs.preferences_pb") })
        val prefs = PreferencesRepository(store, scope)
        categories = CategoryRepository(db.categoryDao(), scope)
        seeder = CategorySeeder(categories, prefs)
    }

    @After fun tearDown() {
        db.close()
        scope.coroutineContext[Job]?.cancel()
    }

    @Test fun freshInstallGetsTheFifteenDefaultsExactlyOnce() = runBlocking {
        seeder.seedIfNeeded()
        val first = categories.getAllOnce()
        assertEquals(15, first.size)
        assertEquals(8, first.count { it.type == FlowType.EXPENSE })
        assertEquals((0..7).toList(), first.filter { it.type == FlowType.EXPENSE }.map { it.sortOrder })

        seeder.seedIfNeeded() // the next launch must not add anything
        assertEquals(15, categories.getAllOnce().size)
    }

    @Test fun overlappingCallsStillSeedOnlyOnce() = runBlocking {
        List(5) { async(Dispatchers.IO) { seeder.seedIfNeeded() } }.awaitAll()
        assertEquals(15, categories.getAllOnce().size)
    }

    @Test fun existingCategoriesAreKeptAndNotDuplicated() = runBlocking {
        categories.createNext("food", "coffee", FlowType.EXPENSE) // e.g. from the CSV import
        categories.createNext("Rent", "house", FlowType.EXPENSE)
        seeder.seedIfNeeded()
        val all = categories.getAllOnce()
        assertEquals(16, all.size) // 15 defaults + Rent (food already existed)
        assertEquals("coffee", all.single { it.name.equals("food", true) }.iconKey) // theirs untouched
        assertEquals(1, all.count { it.name.equals("food", true) })
    }

    @Test fun aDefaultThatIsLaterDeletedStaysDeleted() = runBlocking {
        seeder.seedIfNeeded()
        categories.delete(categories.getAllOnce().single { it.name == "Gift" })
        seeder.seedIfNeeded()
        assertEquals(14, categories.getAllOnce().size)
    }
}
