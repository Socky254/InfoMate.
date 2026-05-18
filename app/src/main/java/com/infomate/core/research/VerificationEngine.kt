package com.infomate.core.research

object VerificationEngine {

    fun verify(response: String): String {

        val prompt = """
        Review the following answer for errors, contradictions, or weak logic:

        $response

        Return improved corrected version.
        """

        return LLMClient.generate(prompt)
    }
}
