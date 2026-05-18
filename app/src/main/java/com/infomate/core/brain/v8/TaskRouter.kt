package com.infomate.core.brain.v8

import com.infomate.core.brain.v5.Complexity
import com.infomate.core.brain.v5.TaskProfile
import java.util.UUID

class TaskRouter {

    fun split(profile: TaskProfile, query: String): List<Job> {
        val jobs = mutableListOf<Job>()
        
        when (profile.complexity) {
            Complexity.LOW -> {
                jobs.add(Job(UUID.randomUUID().toString(), NodeType.MOBILE, query))
            }
            Complexity.MEDIUM -> {
                jobs.add(Job(UUID.randomUUID().toString(), NodeType.MOBILE, query))
                jobs.add(Job(UUID.randomUUID().toString(), NodeType.CLOUD, "[DEEP_SCAN] $query"))
            }
            Complexity.HIGH -> {
                jobs.add(Job(UUID.randomUUID().toString(), NodeType.MOBILE, query))
                jobs.add(Job(UUID.randomUUID().toString(), NodeType.CLOUD, "[MULTI_VECTOR_ANALYSIS] $query"))
                jobs.add(Job(UUID.randomUUID().toString(), NodeType.DESKTOP, "[QUANTUM_SIMULATION] $query"))
            }
        }
        
        return jobs
    }
}
