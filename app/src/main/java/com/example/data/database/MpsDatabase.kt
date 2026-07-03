package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.MpsDao
import com.example.data.model.*

@Database(
    entities = [
        PlayerEntity::class,
        TeamEntity::class,
        MatchEntity::class,
        PlayerMatchPerformanceEntity::class,
        PointSystemConfigEntity::class,
        AdminConfigEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MpsDatabase : RoomDatabase() {
    abstract fun mpsDao(): MpsDao

    companion object {
        @Volatile
        private var INSTANCE: MpsDatabase? = null

        fun getDatabase(context: Context): MpsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MpsDatabase::class.java,
                    "mps_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
