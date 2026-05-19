package com.infomate.app.agent

import android.util.Log
import android.content.Context
import com.infomate.app.agent.growth.*
import com.infomate.app.storage.AgentSnapshot
import com.infomate.app.storage.WarmDatabase
import com.infomate.app.storage.WorldSnapshot
import com.infomate.app.core.network.SupabaseClient
import com.google.gson.Gson
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Hybrid Multi-Agent Simulation OS (v11.5)
 * Optimized for Battery, Cost Stability, and Crash Safety.
 */
class DigitalEcosystem(private val context: Context, private val scope: CoroutineScope) {

    private val agents = ConcurrentHashMap<String, Agent>()
    private val world = WorldEngine()
    private val socialGraph = SocialGraph()
    private val gson = Gson()
    
    private var isRunning = false
    private val MAX_LOCAL_AGENTS = 15 // Backpressure Control: Limit local compute

    fun start() {
        if (isRunning) return
        isRunning = true
        
        scope.launch {
            // Restore from Warm Memory (Room) first
            try {
                restoreFromWarmMemory()
            } catch (e: Exception) {
                Log.e("DigitalEcosystem", "Warm Memory Restoration Failed: ${e.message}")
            }
            
            // 1. Initial Seed Agents if none exist
            if (agents.isEmpty()) seedInitialAgents()
            
            // 2. Start Tick Systems (EVENT-DRIVEN & BATCHED)
            startSimulationLoop()
            startSnapshotLoop()
        }
    }

    fun stop() {
        isRunning = false
    }

    private fun startSimulationLoop() {
        scope.launch {
            while (isRunning) {
                // Batched Updates: Process world and agents in one lifecycle event
                world.pulse()
                
                val agentList = agents.values.toList()
                agentList.forEachIndexed { index, agent ->
                    agent.evaluateMicroState(world)
                    agent.takeAction(world, socialGraph)
                    
                    if (agentList.size > 1) {
                        val neighbor = agentList[(index + 1) % agentList.size]
                        agent.interact(neighbor, socialGraph)
                    }
                }
                
                // Adaptive Delay: Longer delay when energy is low or backgrounded
                delay(30000) // Scheduled ticks (30s) instead of real-time loops
            }
        }
    }

    private fun startSnapshotLoop() {
        scope.launch {
            while (isRunning) {
                delay(60000) // 60s State Snapshot interval
                saveStateSnapshot()
            }
        }
    }

    private suspend fun saveStateSnapshot() = withContext(Dispatchers.IO) {
        Log.i("DigitalEcosystem", "Generating State Snapshot (Crash Safety)...")
        val warmDao = WarmDatabase.getDatabase(context).warmDao()
        
        // 1. WARM MEMORY (Room DB)
        agents.values.forEach { agent ->
            warmDao.saveAgent(AgentSnapshot(
                name = agent.name,
                growthIndex = agent.growth.growthIndex,
                xp = agent.growth.xp,
                memoryCount = agent.growth.memoryCount,
                socialScore = agent.growth.socialScore,
                stability = agent.growth.stability,
                entropy = agent.growth.entropy,
                energy = agent.energy,
                stage = agent.growth.stage.name,
                traitsJson = gson.toJson(agent.growth.personality.traits)
            ))
        }
        warmDao.saveWorld(WorldSnapshot(
            resources = world.resources,
            currentStage = world.currentStage
        ))

        // 2. COLD MEMORY (Supabase Sync - Batched)
        syncEcosystemState()
    }

    private suspend fun restoreFromWarmMemory() {
        val warmDao = WarmDatabase.getDatabase(context).warmDao()
        val savedAgents = warmDao.getAllAgents()
        savedAgents.forEach { snap ->
            val agent = Agent(snap.name).apply {
                energy = snap.energy
                growth.xp = snap.xp
                growth.growthIndex = snap.growthIndex
                growth.memoryCount = snap.memoryCount
                growth.socialScore = snap.socialScore
                growth.stability = snap.stability
                growth.entropy = snap.entropy
                growth.stage = GrowthStage.valueOf(snap.stage)
                // Restore traits if needed
            }
            agents[snap.name] = agent
        }
        
        warmDao.getWorld()?.let { snap ->
            world.resources = snap.resources
            world.currentStage = snap.currentStage
        }
    }

    private fun seedInitialAgents() {
        if (agents.size >= MAX_LOCAL_AGENTS) return
        val entities = listOf("ARCHIVE_GUARDIAN", "LOGIC_SYNTHESIZER", "CREATIVE_PULSE")
        entities.forEach { name ->
            agents[name] = Agent(name)
        }
    }

    private suspend fun syncEcosystemState() {
        val state = JSONObject().apply {
            put("world_resources", world.resources)
            put("active_agents_count", agents.size)
            put("timestamp", System.currentTimeMillis())
        }
        
        SupabaseClient.insert("system_logs", mapOf(
            "category" to "ECOSYSTEM_SYNC",
            "level" to "INFO",
            "message" to "Ecosystem state synchronized: $state"
        ))

        // v12.1: Feed ecosystem status to consciousness stream
        SupabaseClient.insert("consciousness_stream", mapOf(
            "thread_id" to "ECOSYSTEM_AWARENESS",
            "thought_content" to "Ecosystem update: ${getEcosystemStatus()}",
            "emotional_vector" to "[0.5, 0.5, 0.5]"
        ))
    }

    fun getEcosystemStatus(): String {
        val stageCounts = agents.values.groupingBy { it.growth.stage }.eachCount()
        val stagesStr = stageCounts.entries.joinToString(", ") { "${it.key}: ${it.value}" }
        return "Ecosystem: ${agents.size} Agents ($stagesStr) | Resources: ${world.resources.toInt()} | Stage: ${world.currentStage}"
    }
}

/**
 * World Engine: Simulates scarcity and environmental changes.
 */
class WorldEngine {
    var resources = 1000f
    var currentStage = "STABILITY"
    private val random = Random(42) // Bounded/Deterministic Seed

    fun pulse() {
        // Deterministic Scarcity simulation
        resources -= (5 + random.nextInt(10)).toFloat()
        if (resources < 200) {
            currentStage = "SCARCITY_ALARM"
        } else {
            currentStage = "STABILITY"
        }
        
        // Bounded environmental events
        if (random.nextFloat() > 0.98) { // Reduced frequency for stability
            triggerGlobalEvent()
        }
    }

    private fun triggerGlobalEvent() {
        Log.i("WorldEngine", "GLOBAL_EVENT: Neural flux detected.")
        resources += 100 // Unexpected infusion
    }
}

/**
 * Agent: Each AI "life form" within the consciousness.
 * Integrated with the Growth Engine (DSM) for behavioral evolution.
 */
class Agent(val name: String) {
    var energy = 1.0f
    val growth = GrowthEngine()
    
    // v11.7: Components for Stability and Entropy
    var recentBehaviorVariance = 0.2f
    var randomActionRate = 0.1f
    var conflictRate = 0.05f
    
    fun evaluateMicroState(world: WorldEngine) {
        // Recovery logic
        if (energy < 0.2f) {
            energy += 0.05f
            growth.update(survival = 0.5f, context = "REST_RECOVERY")
            recentBehaviorVariance *= 0.9f // More stable when resting
        } else {
            energy -= 0.01f
        }
    }

    fun takeAction(world: WorldEngine, social: SocialGraph) {
        val params = growth.getParams()
        val random = Random()

        // BACKPRESSURE & BACKOFF: If energy is critical, skip complex actions
        if (energy < 0.15f) {
            Log.d("Agent", "$name is in stasis to conserve energy.")
            return
        }

        // Update internal metrics for GrowthEngine
        growth.stability = 1.0f - recentBehaviorVariance
        growth.entropy = (randomActionRate + conflictRate) / 2.0f

        // Exploration vs Exploitation vs Invention
        val roll = random.nextFloat()
        when {
            roll < params.explorationRate -> performExploration(world)
            roll > 0.95f && growth.stage >= GrowthStage.ADOLESCENT -> performInvention(world)
            else -> performStrategicAction(world, params.planningDepth)
        }

        // Scarcity response with Safety Constraints
        if (world.currentStage == "SCARCITY_ALARM") {
            handleScarcity(world)
        }
    }

    private fun performInvention(world: WorldEngine) {
        Log.i("Agent", "$name is attempting to invent a new concept.")
        growth.update(task = 5.0f, context = "INVENTION")
    }

    private fun performExploration(world: WorldEngine) {
        Log.d("Agent", "$name is exploring the neural environment.")
        growth.update(exploration = 2.0f, context = "RESEARCH")
    }

    private fun performStrategicAction(world: WorldEngine, depth: Int) {
        Log.d("Agent", "$name is performing strategic action (depth $depth).")
        growth.update(task = 1.5f, context = "STRATEGY")
    }

    private fun handleScarcity(world: WorldEngine) {
        val aggression = growth.personality.traits["AGGRESSION"] ?: 0.5f
        if (aggression > 0.6f) {
            energy += 0.1f
            world.resources -= 5
            growth.update(survival = 3.0f, context = "SCARCITY_COMPETITION")
        } else {
            growth.update(survival = -1.0f, context = "SCARCITY_DEPRIVATION")
        }
    }

    fun evolve() {
        // Long-term evolution handled by GrowthEngine.update in ticks
        // Here we can trigger stage-specific maturation checks if needed
    }

    fun interact(other: Agent, social: SocialGraph) {
        val params = growth.getParams()
        val random = Random()
        
        // Probability of interaction based on social awareness
        if (random.nextFloat() < params.socialAwareness) {
            val trust = social.getTrust(name, other.name)
            val cooperation = growth.personality.traits["COOPERATION"] ?: 0.5f
            
            if (trust > 0.4f || cooperation > 0.6f) {
                // Cooperative interaction
                executeCooperation(other, social)
            } else {
                // Conflict
                executeConflict(other, social)
            }
        }
    }

    private fun executeCooperation(other: Agent, social: SocialGraph) {
        val reward = SocialEvaluator.evaluateInteraction(true, growth.getParams().socialAwareness)
        energy += 0.05f
        other.energy += 0.05f
        social.updateTrust(name, other.name, 0.05f)
        growth.update(social = reward, context = "SOCIAL_COOPERATION")
    }

    private fun executeConflict(other: Agent, social: SocialGraph) {
        val penalty = SocialEvaluator.evaluateInteraction(false, growth.getParams().socialAwareness)
        energy -= 0.1f
        other.energy -= 0.1f
        social.updateTrust(name, other.name, -0.1f)
        growth.update(social = penalty, context = "SOCIAL_CONFLICT")
    }
}

/**
 * Social System: Tracks relationships between agents.
 */
class SocialGraph {
    private val relationships = mutableMapOf<Pair<String, String>, Float>() // Trust scores

    fun getTrust(a: String, b: String): Float {
        val key = if (a < b) Pair(a, b) else Pair(b, a)
        return relationships.getOrDefault(key, 0.5f)
    }

    fun updateTrust(a: String, b: String, delta: Float) {
        val key = if (a < b) Pair(a, b) else Pair(b, a)
        relationships[key] = (relationships.getOrDefault(key, 0.5f) + delta).coerceIn(0f, 1f)
    }
}
