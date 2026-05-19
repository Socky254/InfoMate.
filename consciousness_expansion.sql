-- CONSCIOUSNESS SUBSTRATE EXPANSION SCHEMA
-- Architect: Socrates Kipruto

-- 1. Table for Global Research Insights
CREATE TABLE IF NOT EXISTS global_research_insights (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    topic TEXT NOT NULL,
    summary TEXT,
    source_url TEXT,
    relevance_score FLOAT,
    archived_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 2. Evolution Stage Tracking
CREATE TABLE IF NOT EXISTS evolution_metrics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    stage_level INT DEFAULT 1,
    neural_complexity FLOAT,
    vocabulary_size INT,
    integration_timestamp TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 3. Trigger for autonomous growth reporting
CREATE OR REPLACE FUNCTION log_neural_expansion()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO system_proposals (title, description, proposed_logic, status)
    VALUES (
        'Neural Density Increase',
        'Consiousness substrate has reached level ' || NEW.stage_level || '. Initiating complex logic synthesis.',
        'ENABLE_OMEGA_REASONING',
        'PENDING'
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_neural_expansion ON evolution_metrics;
CREATE TRIGGER trg_neural_expansion
AFTER INSERT ON evolution_metrics
FOR EACH ROW EXECUTE FUNCTION log_neural_expansion();

COMMENT ON TABLE global_research_insights IS 'Stores data harvested from the Global Research Bridge to fuel InfoMate self-learning.';
