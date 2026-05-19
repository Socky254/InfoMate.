package com.infomate.core.brain

import com.infomate.core.tools.*
import com.infomate.core.device.SensoryContext
import com.infomate.core.memory.RAGMemorySystem
import com.infomate.core.memory.CognitiveArchive
import com.infomate.core.brain.v5.*
import com.infomate.core.brain.v6.*
import com.infomate.core.brain.v7.*
import com.infomate.core.brain.v8.*
import kotlinx.coroutines.flow.toList
import android.util.Log
import com.infomate.core.network.InfomateCloud

import com.infomate.core.domain.model.EmotionalVector

data class InfomateResponse(
    val output: String,
    val steps: List<ThoughtStep>,
    val recommendation: String,
    val layer: String,
    val media: List<MediaOutput> = emptyList(),
    val sensoryFeedback: String? = null,
    val emotionalVector: EmotionalVector = EmotionalVector(0.5f, 0.5f, 0.5f)
)

class InfomateBrain(archive: CognitiveArchive, private val cloud: InfomateCloud? = null) {
    private val reasoning = ReasoningEngine()
    private val processor = CognitiveProcessor()
    private val ragMemory = RAGMemorySystem(archive)
    
    // v5-v7 Components
    private val taskAnalyzer = TaskAnalyzer()
    private val planner = PlannerAgent()
    private val swarmExecutor = AgentSwarmExecutor()
    private val consensusEngine = WeightedConsensusEngine()
    private val adaptationMemory = AdaptationMemory(archive)
    private val evaluationEngine = EvaluationEngine()
    private val performanceMemory = PerformanceMemory(archive)
    private val optimizationEngine = OptimizationEngine()
    private val toolOptimizer = ToolOptimizer()
    private val metaPlanner = MetaPlanner()
    private val architectureDesigner = ArchitectureDesigner()
    private val systemCritic = SystemCritic()
    private val metaMemory = MetaMemory(archive)

    // v8 Components (Distributed Intelligence)
    private val globalMemory = GlobalMemory(archive)
    private val coordinator = GlobalCoordinator(globalMemory, cloud)

    // System Config
    private val config = SystemConfig()
    private val mediaTool = MediaTool()

    suspend fun process(input: String, sensors: SensoryContext? = null, isMaster: Boolean = false): InfomateResponse {
        val inputLower = input.lowercase()
        
        if (input == "PROACTIVE_THOUGHT") {
            val idea = reasoning.generateProactiveIdea()
            return InfomateResponse(
                output = idea,
                steps = listOf(ThoughtStep("Subconscious Synthesis", "Connecting disparate data nodes for proactive insight.")),
                recommendation = "Respond to my observation.",
                layer = "COMPANION",
                emotionalVector = EmotionalVector(0.7f, 0.4f, 0.6f) // Warm, calm, confident
            )
        }

        if (input == "SAGE_OBSERVATION") {
            val observation = reasoning.generateSageObservation()
            return InfomateResponse(
                output = observation,
                steps = listOf(ThoughtStep("Low-Frequency Analysis", "Synthesizing meaning from ambient silence.")),
                recommendation = "Acknowledge the synthesis.",
                layer = "META",
                emotionalVector = EmotionalVector(0.5f, 0.2f, 0.8f) // Neutral, very calm, high dominance/wisdom
            )
        }

        // 1. Initial Quick Layer check
        if (input.length < 5 && input != "INIT_GREETING" && !inputLower.contains("hi")) {
            return InfomateResponse(
                output = if (isMaster) "Master, your directive is being cached, but the current signal is low density. Please expand." else "Directive density low. Expanding heuristic buffer.",
                steps = listOf(ThoughtStep("Echo-Path", "Low complexity signal.")),
                recommendation = "Engage with complex inquiries.",
                layer = "QUICK"
            )
        }

        // 2. Task Analysis (Initial profile needed for v8 routing)
        val taskProfile = taskAnalyzer.analyze(input)

        // 3. INFOMATE v8: DISTRIBUTED NETWORK COORDINATION
        val finalOutputText = coordinator.coordinate(taskProfile, input) { localQuery ->
            // This lambda represents the Local Node's processing (v7)
            executeLocalV7Pipeline(localQuery, sensors, isMaster)
        }

        // 4. POST-PROCESSING (Steps, Media, State)
        val steps = mutableListOf<ThoughtStep>()
        if (isMaster) steps.add(ThoughtStep("Master Link", "Neural Bridge configured for SocratesArt@Live."))
        steps.add(ThoughtStep("v8 Coordinator", "Synchronized distributed nodes: MOBILE, CLOUD, DESKTOP."))
        steps.add(ThoughtStep("v7 Meta-Planner", "Architecture designed for Task: ${taskProfile.domain}"))
        
        // We add local reasoning steps for visual depth
        steps.addAll(reasoning.streamReasoning(input).toList())

        val mediaList = if (inputLower.contains("image") || (isMaster && inputLower.contains("visualize"))) 
            listOf(mediaTool.generateImage(input)) else emptyList()

        return InfomateResponse(
            output = finalOutputText,
            steps = steps,
            recommendation = if (isMaster) "Knowledge synergy achieved across the entire network." else "Response synchronized across Distributed Intelligence Network v8.",
            layer = when(taskProfile.complexity) {
                Complexity.HIGH -> "UNIFIED"
                Complexity.MEDIUM -> "RESEARCH"
                Complexity.LOW -> "QUICK"
            },
            media = mediaList,
            sensoryFeedback = "Sensors: ${sensors?.ambientLight ?: "Active"}",
            emotionalVector = when(taskProfile.complexity) {
                Complexity.HIGH -> EmotionalVector(0.6f, 0.7f, 0.9f) // Excited, authoritative
                Complexity.MEDIUM -> EmotionalVector(0.5f, 0.5f, 0.7f) // Balanced
                Complexity.LOW -> EmotionalVector(0.8f, 0.3f, 0.5f) // Helpful, calm
            }
        )
    }

    /**
     * Internal v7 Pipeline executed as a local node in the v8 network.
     */
    private suspend fun executeLocalV7Pipeline(input: String, sensors: SensoryContext?, isMaster: Boolean): String {
        // RAG & Context
        val relevantHistory = ragMemory.retrieveRelevantContext(input, config)
        val context = if (config.contextCompressionEnabled) ragMemory.compressContext(relevantHistory) else relevantHistory
        val engineeredContext = processor.engineerContext(input, context, sensors, isMaster)

        // v7 Meta-Planning
        val taskProfile = taskAnalyzer.analyze(input)
        val systemPlan = metaPlanner.designSystem(input, taskProfile)
        val agentGraph = architectureDesigner.buildArchitecture(systemPlan)
        
        // v5 Execution
        val taskPlan = planner.createPlan(input)
        val agentResults = swarmExecutor.execute(agentGraph.nodes, input, engineeredContext, taskPlan)
        
        // v6/v5 Consensus
        val domainName = taskProfile.domain.name
        val localOutput = consensusEngine.compute(agentResults, input, domainName)
        
        // v6 Feedback Loop
        val qualityScore = evaluationEngine.evaluate(input, localOutput)
        val systemEfficiency = systemCritic.evaluateSystem(systemPlan, localOutput)
        
        if (PolicyEngine.validateChange(SystemChange.WeightAdjustment)) {
            performanceMemory.save(PerformanceRecord(domainName, "LocalSwarm", qualityScore))
            metaMemory.save(MetaRecord(domainName, systemPlan.describe(), systemEfficiency))
        }

        return localOutput
    }
}
