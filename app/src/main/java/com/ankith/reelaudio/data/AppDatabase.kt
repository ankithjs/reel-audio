package com.ankith.reelaudio.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter fun fromStatus(s: ClipStatus): String = s.name
    @TypeConverter fun toStatus(s: String): ClipStatus = ClipStatus.valueOf(s)
}

@Database(entities = [Clip::class, Group::class, ClipGroup::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clips(): ClipDao
    abstract fun groups(): GroupDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "reel-audio.db")
                .build().also { instance = it }
        }
    }
}
