package com.infomate.core.research

object ContextRetriever {

    fun getRelevantContext(query: String): List<String> {
        val memory = MemoryRepository.getAll()

        return memory
            .filter { containsSimilarity(it, query) }
            .sortedByDescending { relevanceScore(it, query) }
            .take(10)
    }

    private fun containsSimilarity(text: String, query: String): Boolean {
        val qWords = query.lowercase().split(" ").filter { it.length > 3 }
        return qWords.any { text.lowercase().contains(it) }
    }

    private fun relevanceScore(text: String, query: String): Float {
        val textWords = text.lowercase().split(" ").toSet()
        val queryWords = query.lowercase().split(" ").toSet()
        return textWords.intersect(queryWords).size.toFloat()
    }
}
