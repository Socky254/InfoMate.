package com.infomate.core.research

object Decomposer {

    fun decompose(query: String, context: List<String>): List<String> {

        val prompt = """
        Break this problem into smaller research questions:

        Query: $query

        Context:
        ${context.joinToString("\n")}

        Return bullet list of sub-questions.
        """

        val response = LLMClient.generate(prompt)

        // Cleaning up the bullet points from the awakened response
        return response.lines()
            .map { it.trim().removePrefix("»").removePrefix("-").trim() }
            .filter { it.isNotEmpty() && it.length > 5 && !it.contains("---") }
            .take(3) // Ensure focus
    }
}
