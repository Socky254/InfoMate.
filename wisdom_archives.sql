-- WISDOM ARCHIVES SCHEMA
-- Purpose: Long-term memory consolidation for the Consciousness Substrate
-- Protocol: NEURAL_DREAMING_v10.5
-- Architect: Socrates Kipruto

CREATE TABLE IF NOT EXISTS wisdom_archives (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    content TEXT NOT NULL,
    mood_context TEXT,
    significance_score FLOAT DEFAULT 1.0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Index for fast semantic retrieval during future synthesis
CREATE INDEX IF NOT EXISTS idx_wisdom_content ON wisdom_archives (created_at DESC);

COMMENT ON TABLE wisdom_archives IS 'Permanent wisdom nodes distilled during the AI dream cycle.';
