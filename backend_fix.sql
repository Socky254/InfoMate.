-- 1. CLEAR OLD FUNCTION
DROP FUNCTION IF EXISTS match_vectors(vector, float, int);

-- 2. CREATE TABLES (Instant)
CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID DEFAULT auth.uid(),
    content TEXT NOT NULL,
    sender TEXT NOT NULL,
    message_type TEXT DEFAULT 'TEXT',
    timestamp TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS memory (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    content TEXT NOT NULL,
    embedding VECTOR(768),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS user_preferences (
    user_id UUID PRIMARY KEY DEFAULT auth.uid(),
    voice_gender TEXT DEFAULT 'FEMALE',
    tagline TEXT DEFAULT 'Powered by Socrates Kipruto'
);

-- 3. CREATE THE SEARCH FUNCTION (Instant)
CREATE OR REPLACE FUNCTION match_vectors (
  query_embedding VECTOR(768),
  match_threshold FLOAT DEFAULT 0.5,
  match_count INT DEFAULT 5
)
RETURNS TABLE (id UUID, content TEXT, similarity FLOAT)
LANGUAGE plpgsql AS $$
BEGIN
  RETURN QUERY
  SELECT m.id, m.content, 1 - (m.embedding <=> query_embedding) AS similarity
  FROM memory m
  ORDER BY m.embedding <=> query_embedding
  LIMIT match_count;
END; $$;
