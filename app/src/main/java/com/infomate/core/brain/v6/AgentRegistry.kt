package com.infomate.core.brain.v6

import java.util.concurrent.ConcurrentHashMap

object AgentRegistry {
    private val weights = ConcurrentHashMap<String, Float>()
    private val domainWeights = ConcurrentHashMap<String, MutableMap<String, Float>>()

    fun updateWeight(domain: String, agent: String, score: Float) {
        val currentDomainWeights = domainWeights.getOrPut(domain) { ConcurrentHashMap() }
        val currentWeight = currentDomainWeights[agent] ?: 1.0f
        // Smooth update: 90% old weight, 10% new performance
        currentDomainWeights[agent] = (currentWeight * 0.9f) + (score * 0.1f)
    }

    fun getWeight(domain: String, agent: String): Float {
        return domainWeights[domain]?.get(agent) ?: 1.0f
    }
    
    fun getAllWeightsForDomain(domain: String): Map<String, Float> {
        return domainWeights[domain] ?: emptyMap()
    }
}
