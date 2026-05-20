package com.infomate.app.core.config

object Config {
    const val SUPABASE_URL = "https://zzwwzbepfeikswibhfwk.supabase.co"
    const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inp6d3d6YmVwZmVpa3N3aWJoZndrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzkwODc3MzYsImV4cCI6MjA5NDY2MzczNn0.2SLe-6CmngjZKNQV39ypEjev9Mj05PWcoZo4Z6WCiMo"

    const val LLM_API_URL = "$SUPABASE_URL/functions/v1/infomate-brain"
    const val EMBEDDING_API_URL = "$SUPABASE_URL/functions/v1/get-embeddings"
    
    // v13.0: GEMINI CONFIGURATION
    const val GEMINI_MODEL = "gemini-1.5-flash"
    val GEMINI_API_KEY = com.infomate.app.BuildConfig.GEMINI_API_KEY
}
