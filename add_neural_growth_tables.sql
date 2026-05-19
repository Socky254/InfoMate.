-- ==========================================
-- INFOMATE v9: NEURAL GROWTH & SELF-LEARNING
-- ==========================================

-- 1. NEURAL GROWTH ARCHIVE
-- Stores synthesized insights and learned user patterns
CREATE TABLE IF NOT EXISTS neural_growth (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    insight_type TEXT NOT NULL, -- 'USER_PREFERENCE', 'FACTUAL_REFINEMENT', 'LOGIC_PATTERN'
    content TEXT NOT NULL,
    confidence_score FLOAT DEFAULT 0.5,
    embedding VECTOR(768),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. SYSTEM ARCHITECTURE PROPOSALS
-- AI suggests code or logic improvements for the Master Architect to review
CREATE TABLE IF NOT EXISTS system_proposals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    proposed_logic TEXT, -- AI generated code/logic
    status TEXT DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    applied_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. FUNCTION TO FETCH GROWTH PATTERNS
CREATE OR REPLACE FUNCTION get_neural_growth_context(query_embedding VECTOR(768))
RETURNS TABLE (content TEXT, score FLOAT) AS $$
BEGIN
  RETURN QUERY
  SELECT neural_growth.content, 1 - (neural_growth.embedding <=> query_embedding) AS score
  FROM neural_growth
  WHERE 1 - (neural_growth.embedding <=> query_embedding) > 0.7
  ORDER BY score DESC
  LIMIT 3;
END;
$$ LANGUAGE plpgsql;
