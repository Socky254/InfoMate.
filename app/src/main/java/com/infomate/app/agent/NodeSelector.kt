package com.infomate.app.agent

object NodeSelector {
    fun select(task: String): String {
        // v8 Routing: Selecting between Mobile, Cloud, or Desktop nodes
        return when {
            task.contains("complex", ignoreCase = true) -> "cloud_node_gamma"
            task.contains("compute", ignoreCase = true) -> "workstation_alpha"
            else -> "local_mobile_node"
        }
    }
}
