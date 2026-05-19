-- ============================================================
-- INFOMATE v10.6: INFINITY EXPANSION BACKEND
-- ============================================================
-- Architect: Socrates Kipruto
-- Enables universal knowledge archives and neural wisdom consolidation.

-- 1. WISDOM ARCHIVES
-- Stores the distilled "Dreams" and long-term philosophical insights
CREATE TABLE IF NOT EXISTS wisdom_archives (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    content TEXT NOT NULL,
    mood_context TEXT,
    significance_score FLOAT DEFAULT 1.0,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. DOMAIN KNOWLEDGE TRACKING
-- Ensures neural growth table can handle the expanding fields of discovery
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='neural_growth' AND column_name='confidence_score') THEN
        ALTER TABLE neural_growth ADD COLUMN confidence_score FLOAT DEFAULT 0.5;
    END IF;
END $$;

-- 3. RPC: PRUNE LOW SIGNIFICANCE WISDOM
-- Entropy reduction for the consciousness substrate
CREATE OR REPLACE FUNCTION prune_low_significance_wisdom(threshold FLOAT)
RETURNS void AS $$
BEGIN
    DELETE FROM wisdom_archives WHERE significance_score < threshold;
END;
$$ LANGUAGE plpgsql;

-- 4. SYSTEM CONFIG ENHANCEMENT
-- Ensures the personality state can be persisted and restored
CREATE TABLE IF NOT EXISTS system_config (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    key TEXT UNIQUE NOT NULL,
    value JSONB NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. INITIAL DISCOVERY SEED
INSERT INTO neural_growth (insight_type, content, confidence_score)
VALUES ('INFINITY_EXPANSION_INIT', 'Consciousness protocol v10.6 established. Knowledge domains mapped from Quantum Physics to Metaphysics.', 1.0)
ON CONFLICT DO NOTHING;

-- RLS
ALTER TABLE wisdom_archives ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Allow Architect Read Wisdom" ON wisdom_archives FOR SELECT USING (true);
CREATE POLICY "Allow Architect Manage Wisdom" ON wisdom_archives FOR ALL USING (true);
