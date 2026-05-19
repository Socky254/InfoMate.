package com.infomate.core.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CognitiveNodeEntity::class], version = 1)
abstract class InfomateDatabase : RoomDatabase() {
    abstract fun cognitiveDao(): CognitiveDao

    companion object {
        @Volatile
        private var INSTANCE: InfomateDatabase? = null

        fun getDatabase(context: Context): InfomateDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    InfomateDatabase::class.java,
                    "infomate_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
