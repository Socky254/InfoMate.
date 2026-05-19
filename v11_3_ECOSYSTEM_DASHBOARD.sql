-- ======================================================
-- INFOMATE v11.3: ECOSYSTEM & CONSCIOUSNESS DASHBOARD SCHEMA
-- ======================================================

-- 1. AGENT REGISTRY (Persistent state for the DSM)
CREATE TABLE IF NOT EXISTS ecosystem_agents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL UNIQUE,
    growth_stage TEXT DEFAULT 'INFANT',
    xp FLOAT DEFAULT 0,
    energy FLOAT DEFAULT 1.0,
    traits JSONB, -- Stores curiosity, aggression, etc.
    last_action TEXT,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. SOCIAL RELATIONSHIPS
CREATE TABLE IF NOT EXISTS social_graph (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    agent_a TEXT NOT NULL,
    agent_b TEXT NOT NULL,
    trust_score FLOAT DEFAULT 0.5,
    interaction_count INT DEFAULT 0,
    UNIQUE(agent_a, agent_b)
);

-- 3. VITAL SIGNS LOGGING (For Live Graphs)
CREATE TABLE IF NOT EXISTS system_vitals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_name TEXT DEFAULT 'CORE',
    cpu_usage FLOAT,
    memory_usage FLOAT,
    neural_load FLOAT,
    latency_ms INT,
    is_alive BOOLEAN DEFAULT TRUE,
    timestamp TIMESTAMPTZ DEFAULT NOW()
);

-- 4. REAL-TIME SIMULATION FEED
CREATE TABLE IF NOT EXISTS simulation_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_type TEXT, -- 'GROWTH', 'CONFLICT', 'DISCOVERY'
    description TEXT,
    severity TEXT DEFAULT 'INFO',
    timestamp TIMESTAMPTZ DEFAULT NOW()
);

-- 5. DASHBOARD STATS VIEW
CREATE OR REPLACE VIEW v_ecosystem_summary AS
SELECT
    growth_stage,
    COUNT(*) as agent_count,
    AVG(xp) as avg_xp,
    AVG(energy) as avg_energy
FROM ecosystem_agents
GROUP BY growth_stage;

-- 6. RPC: FETCH LATEST VITALS
CREATE OR REPLACE FUNCTION get_latest_vitals()
RETURNS SETOF system_vitals AS $$
BEGIN
    RETURN QUERY SELECT * FROM system_vitals ORDER BY timestamp DESC LIMIT 20;
END;
$$ LANGUAGE plpgsql;
