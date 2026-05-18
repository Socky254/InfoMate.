package com.infomate.core.research

import com.infomate.core.memory.CognitiveArchive
import com.infomate.core.tools.MetacognitiveCore
import com.infomate.core.tools.ToolRouter

/**
 * Internal bridges to connect the Research Engine with INFOMATE's existing core.
 */
object LLMClient {
    private val metacognitiveCore = MetacognitiveCore()
    
    fun generate(prompt: String): String {
        // Bridges the research request to our High-Frequency local core
        return metacognitiveCore.awaken(prompt)
    }
}

object MemoryRepository {
    private var archive: CognitiveArchive? = null
    
    fun initialize(cognitiveArchive: CognitiveArchive) {
        archive = cognitiveArchive
    }

    fun getAll(): List<String> {
        return archive?.getRecentTopicsDetailed()?.map { it.concept } ?: emptyList()
    }

    fun search(query: String): String {
        val matches = getAll().filter { it.contains(query, ignoreCase = true) }
        return if (matches.isNotEmpty()) "Found relevant memory nodes: ${matches.joinToString(", ")}" else "No direct memory matches."
    }
}

object InternalToolRouter {
    private val router = ToolRouter()
    
    fun tryExecute(query: String): String {
        // Maps research questions to tool routes
        router.route("RESEARCH_PROBE", query)
        return "Tool Probe synchronized for vector: $query"
    }
}
