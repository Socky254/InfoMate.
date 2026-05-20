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

@Entity(tableName = "research_cache")
data class ResearchCache(
    @PrimaryKey val query: String,
    val findings: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "memory_table")
data class MemoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val input: String,
    val response: String,
    val importanceScore: Float = 0.5f,
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun cacheResearch(research: ResearchCache)

    @Query("SELECT * FROM research_cache WHERE `query` = :query LIMIT 1")
    suspend fun getCachedResearch(query: String): ResearchCache?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMemory(entry: MemoryEntry)

    @Query("SELECT * FROM memory_table ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMemories(limit: Int): List<MemoryEntry>
}

@Database(entities = [AgentSnapshot::class, WorldSnapshot::class, ResearchCache::class, MemoryEntry::class], version = 4, exportSchema = false)
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
