package com.ankith.reelaudio.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Converters {
    @TypeConverter fun fromStatus(s: ClipStatus): String = s.name
    @TypeConverter fun toStatus(s: String): ClipStatus = ClipStatus.valueOf(s)
    @TypeConverter fun fromTranscript(s: TranscriptStatus): String = s.name
    @TypeConverter fun toTranscript(s: String): TranscriptStatus = TranscriptStatus.valueOf(s)
    @TypeConverter fun fromLinkStatus(s: LinkStatus): String = s.name
    @TypeConverter fun toLinkStatus(s: String): LinkStatus = LinkStatus.valueOf(s)
}

@Database(entities = [Clip::class, Group::class, ClipGroup::class, SavedLink::class, LinkGroup::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clips(): ClipDao
    abstract fun groups(): GroupDao
    abstract fun links(): LinkDao

    companion object {
        /** 1 -> 2: speech-to-text fields. Keeps every saved clip. */
        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE clips ADD COLUMN transcript TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE clips ADD COLUMN transcriptLang TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE clips ADD COLUMN transcriptStatus TEXT NOT NULL DEFAULT 'NONE'")
            }
        }

        /** 2 -> 3: saved links (websites, tools, skills). */
        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `links` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`url` TEXT NOT NULL, `title` TEXT NOT NULL, `siteName` TEXT NOT NULL, " +
                        "`description` TEXT NOT NULL, `content` TEXT NOT NULL, `imagePath` TEXT NOT NULL, " +
                        "`sourceClipId` INTEGER, `status` TEXT NOT NULL, `error` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_links_url` ON `links` (`url`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_links_sourceClipId` ON `links` (`sourceClipId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `link_groups` (`linkId` INTEGER NOT NULL, `groupId` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`linkId`, `groupId`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_link_groups_groupId` ON `link_groups` (`groupId`)")
            }
        }

        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "reel-audio.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build().also { instance = it }
        }
    }
}
