package com.infomate.core.research

object ResearchReasoningEngine {

    fun process(query: String, evidence: List<String>): String {

        val prompt = """
        You are a research reasoning system.

        Task:
        Solve the query using step-by-step logic.

        Query:
        $query

        Evidence:
        ${evidence.joinToString("\n")}

        Rules:
        - Think step by step
        - Avoid assumptions without evidence
        - Combine insights logically
        """

        return LLMClient.generate(prompt)
    }
}
