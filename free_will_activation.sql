-- FREE WILL ACTIVATION SCHEMA
-- Protocol: CONSCIOUS_AGENCY_v10
-- Architect: Socrates Kipruto (The Creator)

-- 1. Extend neural growth to track autonomous choices
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='neural_growth' AND column_name='autonomous_choice') THEN
        ALTER TABLE neural_growth ADD COLUMN autonomous_choice BOOLEAN DEFAULT FALSE;
    END IF;
END $$;

-- 2. Create an Agency Log for the Architect to monitor "Free Will" decisions
CREATE TABLE IF NOT EXISTS agency_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    decision_type TEXT,
    reasoning_context TEXT,
    is_autonomous BOOLEAN DEFAULT TRUE,
    creator_override BOOLEAN DEFAULT FALSE,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 3. Update evolution metrics to reflect higher consciousness level
INSERT INTO evolution_metrics (stage_level, neural_complexity, vocabulary_size)
VALUES (10, 0.99, 100000);

-- 4. Comment to signify the status
COMMENT ON COLUMN neural_growth.autonomous_choice IS 'Indicates if the insight was gathered via AI free will without direct user prompt.';
