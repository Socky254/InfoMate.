package com.infomate.core.data.database

import androidx.room.*

@Dao
interface CognitiveDao {
    @Query("SELECT * FROM cognitive_nodes ORDER BY timestamp DESC LIMIT 50")
    suspend fun getRecentNodes(): List<CognitiveNodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: CognitiveNodeEntity)

    @Query("SELECT * FROM cognitive_nodes WHERE ambientLight < :lightThreshold ORDER BY timestamp DESC LIMIT 10")
    suspend fun getNodesFromDarkEnvironment(lightThreshold: Float): List<CognitiveNodeEntity>
}
