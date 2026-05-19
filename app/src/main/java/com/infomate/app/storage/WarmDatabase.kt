package com.infomate.app.storage

import android.content.Context
import androidx.room.*

@Entity(tableName = "agent_snapshots")
data class AgentSnapshot(
    @PrimaryKey val name: String,
    val growthIndex: Float,
    val xp: Float,
    val memoryCount: Int,
    val socialScore: Float,
    val stability: Float,
    val entropy: Float,
    val energy: Float, // Added back
    val stage: String,
    val traitsJson: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "world_snapshots")
data class WorldSnapshot(
    @PrimaryKey val id: Int = 1,
    val resources: Float,
    val currentStage: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface WarmDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAgent(snapshot: AgentSnapshot)

    @Query("SELECT * FROM agent_snapshots")
    suspend fun getAllAgents(): List<AgentSnapshot>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWorld(snapshot: WorldSnapshot)

    @Query("SELECT * FROM world_snapshots WHERE id = 1")
    suspend fun getWorld(): WorldSnapshot?
}

@Database(entities = [AgentSnapshot::class, WorldSnapshot::class], version = 2, exportSchema = false)
abstract class WarmDatabase : RoomDatabase() {
    abstract fun warmDao(): WarmDao

    companion object {
        @Volatile
        private var INSTANCE: WarmDatabase? = null

        fun getDatabase(context: Context): WarmDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WarmDatabase::class.java,
                    "warm_memory_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
