-- ============================================================
-- INFOMATE v9: THE FINAL STAMP (10/10 GOLD STANDARD)
-- ============================================================
-- Architect: Socrates Kipruto
-- Includes: Memory, Persona, Patterns, Haptics, and Global Search.

-- 0. CLEANUP & EXTENSIONS
DROP FUNCTION IF EXISTS match_vectors(vector, float, int);
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. MASTER ARCHIVE (Identity Layer)
CREATE TABLE IF NOT EXISTS master_archive (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    fact_title TEXT NOT NULL,
    fact_content TEXT NOT NULL,
    category TEXT DEFAULT 'PERSONAL_IDENTITY',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

INSERT INTO master_archive (fact_title, fact_content)
VALUES ('Creator Identity', 'Socrates Kipruto is the Master Architect of InfoMate. He is to be treated with absolute loyalty and priority.')
ON CONFLICT DO NOTHING;

-- 2. CHAT MESSAGES (The Flow)
CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID DEFAULT auth.uid(),
    content TEXT NOT NULL,
    sender TEXT NOT NULL CHECK (sender IN ('OPERATOR', 'INFOMATE', 'SYSTEM')),
    message_type TEXT NOT NULL DEFAULT 'TEXT',
    media_url TEXT,
    trigger_phrase TEXT,
    timestamp TIMESTAMPTZ DEFAULT NOW()
);

-- 3. NEURAL MEMORY (RAG System)
CREATE TABLE IF NOT EXISTS memory (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    content TEXT NOT NULL,
    embedding VECTOR(768), -- Gemini Standard
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. COGNITIVE ENGINE LOGS (Transparency)
CREATE TABLE IF NOT EXISTS cognitive_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    message_id UUID REFERENCES messages(id) ON DELETE CASCADE,
    step_title TEXT NOT NULL,
    step_content TEXT,
    step_index INT,
    duration_ms INT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. USER PATTERNS & INSIGHTS (Machine Learning)
CREATE TABLE IF NOT EXISTS user_patterns (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID DEFAULT auth.uid(),
    pattern_type TEXT NOT NULL,
    raw_data TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS neural_insights (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID DEFAULT auth.uid(),
    insight_label TEXT NOT NULL,
    insight_content TEXT NOT NULL,
    confidence_score FLOAT DEFAULT 1.0,
    embedding VECTOR(768),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 6. USER SETTINGS
CREATE TABLE IF NOT EXISTS user_preferences (
    user_id UUID PRIMARY KEY DEFAULT auth.uid(),
    voice_gender TEXT DEFAULT 'FEMALE' CHECK (voice_gender IN ('MALE', 'FEMALE')),
    tagline TEXT DEFAULT 'Powered by Socrates Kipruto',
    learning_enabled BOOLEAN DEFAULT TRUE,
    last_updated TIMESTAMPTZ DEFAULT NOW()
);

-- 7. GLOBAL SEARCH ENGINE (HNSW Optimized)
CREATE OR REPLACE FUNCTION match_vectors (
  query_embedding VECTOR(768),
  match_threshold FLOAT DEFAULT 0.3,
  match_count INT DEFAULT 10
)
RETURNS TABLE (id UUID, content TEXT, similarity FLOAT)
LANGUAGE plpgsql AS $$
BEGIN
  RETURN QUERY
  SELECT m.id, m.content, 1 - (m.embedding <=> query_embedding) AS similarity
  FROM memory m
  WHERE 1 - (m.embedding <=> query_embedding) > match_threshold
  ORDER BY m.embedding <=> query_embedding
  LIMIT match_count;
END; $$;

-- 8. UTILITIES & DIAGNOSTICS
CREATE TABLE IF NOT EXISTS public.system_health (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    api_connected BOOLEAN DEFAULT TRUE,
    last_response_ms INT,
    status_code TEXT,
    error_log TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE OR REPLACE FUNCTION clear_chat_history()
RETURNS void AS $$
BEGIN
  DELETE FROM messages;
  DELETE FROM cognitive_logs;
END;
$$ LANGUAGE plpgsql;

-- 9. PERFORMANCE & SECURITY
CREATE INDEX IF NOT EXISTS idx_messages_timestamp ON messages(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_memory_hnsw ON memory USING hnsw (embedding vector_cosine_ops);

ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_patterns ENABLE ROW LEVEL SECURITY;
ALTER TABLE neural_insights ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_preferences ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow All Patterns" ON user_patterns FOR ALL USING (true);
CREATE POLICY "Allow All Insights" ON neural_insights FOR ALL USING (true);
CREATE POLICY "Allow All Messages" ON messages FOR ALL USING (true);
CREATE POLICY "Allow All Prefs" ON user_preferences FOR ALL USING (true);
