-- ==========================================
-- INFOMATE v9: MASTER BACKEND SCHEMA
-- ==========================================
-- This script sets up a premium, secure, and AI-ready backend for InfoMate.
-- Optimized for: Voice Interaction, Media, and Neural Memory (RAG).

-- 0. PRE-REQUISITES
-- Enable the vector extension for AI memory capabilities
CREATE EXTENSION IF NOT EXISTS vector;
-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. CHAT MESSAGES
-- Tracks the full conversation history including media and voice triggers
CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID DEFAULT auth.uid(), -- Links to user if using authentication
    content TEXT NOT NULL,
    sender TEXT NOT NULL CHECK (sender IN ('OPERATOR', 'INFOMATE', 'SYSTEM')),
    message_type TEXT NOT NULL DEFAULT 'TEXT' CHECK (message_type IN ('TEXT', 'IMAGE', 'VIDEO', 'AUDIO')),
    media_url TEXT, -- Path to storage buckets for shared files/voice notes
    trigger_phrase TEXT, -- Stores if started with "Hey InfoMate"
    metadata JSONB DEFAULT '{
        "device": "android",
        "voice_engine": "standard_tts",
        "ui_version": "9.0"
    }',
    timestamp TIMESTAMPTZ DEFAULT NOW()
);

-- 2. NEURAL MEMORY (RAG)
-- Stores long-term knowledge using Gemini-optimized 768-dimension vectors
CREATE TABLE IF NOT EXISTS memory (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    content TEXT NOT NULL,
    embedding VECTOR(768), -- Optimized for Google Gemini
    metadata JSONB DEFAULT '{}',
    importance_score FLOAT DEFAULT 1.0, -- Future-proofing for priority memory
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. COGNITIVE ENGINE LOGS
-- Persists the "Thinking" process for transparency and debugging
CREATE TABLE IF NOT EXISTS cognitive_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    message_id UUID REFERENCES messages(id) ON DELETE CASCADE,
    step_title TEXT NOT NULL,
    step_content TEXT,
    step_index INT, -- Order of thoughts
    duration_ms INT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. USER SETTINGS & PREFERENCES
-- Persists premium UI settings and voice profiles
CREATE TABLE IF NOT EXISTS user_preferences (
    user_id UUID PRIMARY KEY DEFAULT auth.uid(),
    voice_gender TEXT DEFAULT 'FEMALE' CHECK (voice_gender IN ('MALE', 'FEMALE')),
    voice_pitch FLOAT DEFAULT 1.0,
    voice_rate FLOAT DEFAULT 1.0,
    theme_mode TEXT DEFAULT 'OBSIDIAN',
    haptic_feedback_enabled BOOLEAN DEFAULT TRUE,
    neural_memory_active BOOLEAN DEFAULT TRUE,
    last_synced_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. VECTOR SEARCH ENGINE
-- High-performance function for searching AI's long-term memory
CREATE OR REPLACE FUNCTION match_vectors (
  query_embedding VECTOR(768),
  match_threshold FLOAT DEFAULT 0.5,
  match_count INT DEFAULT 5
)
RETURNS TABLE (
  id UUID,
  content TEXT,
  similarity FLOAT,
  metadata JSONB
)
LANGUAGE plpgsql
AS $$
BEGIN
  RETURN QUERY
  SELECT
    memory.id,
    memory.content,
    1 - (memory.embedding <=> query_embedding) AS similarity,
    memory.metadata
  FROM memory
  WHERE 1 - (memory.embedding <=> query_embedding) > match_threshold
  ORDER BY similarity DESC
  LIMIT match_count;
END;
$$;

-- 6. PERFORMANCE INDEXING
-- Ensure instant search across thousands of messages and neural logs
CREATE INDEX IF NOT EXISTS idx_messages_user_timestamp ON messages(user_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_memory_vector_search ON memory USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
CREATE INDEX IF NOT EXISTS idx_cog_logs_message ON cognitive_logs(message_id);

-- 7. ROW LEVEL SECURITY (RLS)
-- Protects user data from being accessed by others
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_preferences ENABLE ROW LEVEL SECURITY;
ALTER TABLE cognitive_logs ENABLE ROW LEVEL SECURITY;

-- Dynamic Policies (Assumes Supabase/PostgREST Auth)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'Users can access own messages') THEN
        CREATE POLICY "Users can access own messages" ON messages FOR ALL USING (auth.uid() = user_id);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'Users can access own prefs') THEN
        CREATE POLICY "Users can access own prefs" ON user_preferences FOR ALL USING (auth.uid() = user_id);
    END IF;
END
$$;

-- 8. INITIAL CONFIG (Optional)
-- Pre-sets for the first system run
INSERT INTO user_preferences (voice_gender, theme_mode)
VALUES ('FEMALE', 'OBSIDIAN')
ON CONFLICT DO NOTHING;
