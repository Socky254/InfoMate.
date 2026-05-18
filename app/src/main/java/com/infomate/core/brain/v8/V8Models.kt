package com.infomate.core.brain.v8

data class NodeInfo(
    val id: String,
    val type: NodeType,
    val status: NodeStatus = NodeStatus.ONLINE,
    val latency: Int = 0
)

enum class NodeType { MOBILE, CLOUD, DESKTOP }
enum class NodeStatus { ONLINE, OFFLINE, BUSY }

data class Job(
    val id: String,
    val targetNodeType: NodeType,
    val description: String,
    val contextKey: String = ""
)

data class NodeResult(
    val jobId: String,
    val nodeId: String,
    val output: String,
    val confidence: Float
)
