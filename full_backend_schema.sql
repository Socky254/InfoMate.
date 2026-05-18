-- INFOMATE v9: COMPREHENSIVE BACKEND SCHEMA
-- This script ensures all data (Chat, Media, Voice, and Neural Memory) is persisted.

-- Enable Vector Extension (for AI memory)
CREATE EXTENSION IF NOT EXISTS vector;

-- 1. CHAT MESSAGES TABLE
-- Stores the entire conversation history including media links
CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES auth.users(id), -- Optional: links to authenticated user
    content TEXT NOT NULL,
    sender TEXT NOT NULL CHECK (sender IN ('OPERATOR', 'INFOMATE', 'SYSTEM')),
    message_type TEXT NOT NULL DEFAULT 'TEXT' CHECK (message_type IN ('TEXT', 'IMAGE', 'VIDEO')),
    media_url TEXT, -- Link to storage for images/videos
    timestamp TIMESTAMPTZ DEFAULT NOW(),
    metadata JSONB DEFAULT '{}' -- For storing things like "voice_used" or "location"
);

-- 2. AI MEMORY TABLE (Vector Search)
-- Stores "Neural Logs" for RAG (Retrieval Augmented Generation)
CREATE TABLE IF NOT EXISTS memory (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    content TEXT NOT NULL,
    embedding VECTOR(768), -- Gemini Standard
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. COGNITIVE STEPS TABLE
-- Persists the "Thinking" process if needed for future analysis
CREATE TABLE IF NOT EXISTS cognitive_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    message_id UUID REFERENCES messages(id) ON DELETE CASCADE,
    step_title TEXT,
    step_content TEXT,
    duration_ms INT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. USER SETTINGS TABLE
-- Persists user preferences like Voice Choice (Male/Female)
CREATE TABLE IF NOT EXISTS user_preferences (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id),
    voice_gender TEXT DEFAULT 'FEMALE' CHECK (voice_gender IN ('MALE', 'FEMALE')),
    theme_preference TEXT DEFAULT 'OBSIDIAN',
    neural_link_enabled BOOLEAN DEFAULT TRUE,
    last_updated TIMESTAMPTZ DEFAULT NOW()
);

-- 5. VECTOR SEARCH FUNCTION
CREATE OR REPLACE FUNCTION match_vectors (
  query_embedding VECTOR(768),
  match_threshold FLOAT,
  match_count INT
)
RETURNS TABLE (
  id UUID,
  content TEXT,
  similarity FLOAT
)
LANGUAGE plpgsql
AS $$
BEGIN
  RETURN QUERY
  SELECT
    memory.id,
    memory.content,
    1 - (memory.embedding <=> query_embedding) AS similarity
  FROM memory
  WHERE 1 - (memory.embedding <=> query_embedding) > match_threshold
  ORDER BY similarity DESC
  LIMIT match_count;
END;
$$;

-- 6. INDEXING FOR PERFORMANCE
CREATE INDEX IF NOT EXISTS idx_messages_timestamp ON messages(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_memory_vector ON memory USING ivfflat (embedding vector_cosine_ops);

-- 7. SECURITY (Row Level Security)
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_preferences ENABLE ROW LEVEL SECURITY;

-- Allow users to see only their own data (assuming Supabase Auth)
CREATE POLICY "Users can manage their own messages" ON messages FOR ALL USING (auth.uid() = user_id);
CREATE POLICY "Users can manage their own prefs" ON user_preferences FOR ALL USING (auth.uid() = user_id);
