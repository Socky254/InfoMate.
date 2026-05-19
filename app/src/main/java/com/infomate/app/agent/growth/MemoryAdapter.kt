package com.infomate.app.agent.growth

class MemoryAdapter(private var stage: GrowthStage) {
    private val memoryLog = mutableListOf<String>()

    fun updateStage(newStage: GrowthStage) {
        this.stage = newStage
    }

    fun store(event: String) {
        val params = StageManager.getParameters(stage)
        memoryLog.add(0, "[${System.currentTimeMillis()}] $event")
        
        // Prune memory based on stage-specific span
        if (memoryLog.size > params.memorySpan) {
            val toRemove = memoryLog.size - params.memorySpan
            repeat(toRemove) {
                memoryLog.removeAt(memoryLog.size - 1)
            }
        }
    }

    fun getMemory(): List<String> = memoryLog
}
