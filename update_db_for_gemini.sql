-- Gemini embeddings use 768 dimensions instead of 1536 (OpenAI)
-- We need to recreate the memory table and the search function to match

-- 1. Drop old table and function if they exist
DROP FUNCTION IF EXISTS match_vectors(vector, float, int);
DROP TABLE IF EXISTS memory;

-- 2. Create the Memory Table with 768 dimensions
CREATE TABLE memory (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  content TEXT NOT NULL,
  metadata JSONB DEFAULT '{}',
  embedding VECTOR(768), -- Gemini standard
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. Create the Search Function for 768 dimensions
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

-- 4. Enable security and indexing
ALTER TABLE memory ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Allow Public Access" ON memory FOR ALL USING (true);
CREATE INDEX ON memory USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
