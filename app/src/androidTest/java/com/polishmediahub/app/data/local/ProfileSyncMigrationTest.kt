package com.polishmediahub.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileSyncMigrationTest {

    @Test
    fun profileAndWatchedDataSurviveDatabaseReopen() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dbFile = context.getDatabasePath("profile_sync_migration_test.db")
        dbFile.delete()

        val db = Room.databaseBuilder(
            context,
            MediaDatabase::class.java,
            dbFile.name
        )
            .addMigrations(*MediaDatabase.MIGRATIONS)
            .allowMainThreadQueries()
            .build()

        runBlocking {
            val profile = ProfileEntity(id = "p1", name = "Test Profile")
            val watched = WatchedEntity(
                profileId = "p1",
                id = "m1",
                positionMs = 60_000L,
                durationMs = 1_200_000L
            )
            db.profileDao().upsert(profile)
            db.historyDao().upsert(watched)
        }

        db.close()

        val reopened = Room.databaseBuilder(
            context,
            MediaDatabase::class.java,
            dbFile.name
        )
            .addMigrations(*MediaDatabase.MIGRATIONS)
            .allowMainThreadQueries()
            .build()

        runBlocking {
            val profiles = reopened.profileDao().getById("p1")
            assertEquals("Test Profile", profiles?.name)

            val history = reopened.historyDao().getById("p1", "m1")
            assertEquals(60_000L, history?.positionMs)
            assertTrue(history?.durationMs == 1_200_000L)
        }

        reopened.close()
    }
}
