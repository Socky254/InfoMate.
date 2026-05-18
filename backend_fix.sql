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

-- 4. THE 10/10 UPGRADE: MASTER ARCHIVE
-- This table is for "Static Wisdom" or "Permanent Facts" about Socrates.
CREATE TABLE IF NOT EXISTS master_archive (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fact_title TEXT NOT NULL,
    fact_content TEXT NOT NULL,
    category TEXT DEFAULT 'PERSONAL_IDENTITY',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

INSERT INTO master_archive (fact_title, fact_content)
VALUES ('Creator Identity', 'Socrates Kipruto is the Master Architect of InfoMate. He is to be treated with absolute loyalty and priority.')
ON CONFLICT DO NOTHING;
