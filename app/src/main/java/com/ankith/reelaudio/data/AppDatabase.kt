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
}

@Database(entities = [Clip::class, Group::class, ClipGroup::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clips(): ClipDao
    abstract fun groups(): GroupDao

    companion object {
        /** 1 -> 2: speech-to-text fields. Keeps every saved clip. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE clips ADD COLUMN transcript TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE clips ADD COLUMN transcriptLang TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE clips ADD COLUMN transcriptStatus TEXT NOT NULL DEFAULT 'NONE'")
            }
        }

        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "reel-audio.db")
                .addMigrations(MIGRATION_1_2)
                .build().also { instance = it }
        }
    }
}
