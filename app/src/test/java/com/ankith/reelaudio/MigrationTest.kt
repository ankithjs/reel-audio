package com.ankith.reelaudio

import android.app.Application
import androidx.room.Room
import com.ankith.reelaudio.data.AppDatabase
import com.ankith.reelaudio.data.Clip
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Rebuilds a version 1 database (the first release) and upgrades it the way a phone would,
 * so Room's own schema check proves no saved clip is lost.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MigrationTest {
    @Test fun upgradesFromFirstReleaseKeepingClips() = runBlocking {
        val ctx = RuntimeEnvironment.getApplication()
        val name = "migration-test.db"
        ctx.deleteDatabase(name)

        // Create today's schema, then strip it back to what version 1 had.
        Room.databaseBuilder(ctx, AppDatabase::class.java, name).allowMainThreadQueries().build().apply {
            clips().insert(Clip(sourceUrl = "https://www.instagram.com/reel/keep/", title = "Keep me"))
            openHelper.writableDatabase.apply {
                execSQL("DROP TABLE links")
                execSQL("DROP TABLE link_groups")
                execSQL("ALTER TABLE clips DROP COLUMN transcript")
                execSQL("ALTER TABLE clips DROP COLUMN transcriptLang")
                execSQL("ALTER TABLE clips DROP COLUMN transcriptStatus")
                version = 1
            }
            close()
        }

        // Opening runs both migrations and fails if the result doesn't match the entities.
        val db = Room.databaseBuilder(ctx, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()
        assertEquals("Keep me", db.clips().byUrl("https://www.instagram.com/reel/keep/")?.title)
        assertEquals(0, db.links().allOnce().size)
        db.close()
    }
}
