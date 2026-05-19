-- ======================================================
-- INFOMATE v11.7: DETERMINISTIC GROWTH INDEX SYSTEM
-- ======================================================

-- 1. ENHANCED AGENT TABLE
CREATE TABLE IF NOT EXISTS ecosystem_agents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL UNIQUE,
    growth_index REAL DEFAULT 0,
    xp INTEGER DEFAULT 0,
    memory_count INTEGER DEFAULT 0,
    social_score REAL DEFAULT 0,
    stability REAL DEFAULT 0,
    entropy REAL DEFAULT 0,
    stage TEXT DEFAULT 'INFANT',
    traits JSONB,
    last_action TEXT,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. MEMORY TABLE (EPISODIC CONTINUITY)
CREATE TABLE IF NOT EXISTS ecosystem_memory (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    agent_id UUID REFERENCES ecosystem_agents(id),
    type TEXT,
    content TEXT,
    importance REAL DEFAULT 0.5,
    timestamp TIMESTAMPTZ DEFAULT NOW()
);

-- 3. SOCIAL LINKS (REPUTATION GRAPH)
CREATE TABLE IF NOT EXISTS ecosystem_social_links (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    agent_a UUID REFERENCES ecosystem_agents(id),
    agent_b UUID REFERENCES ecosystem_agents(id),
    trust REAL DEFAULT 0.5,
    cooperation REAL DEFAULT 0,
    conflict REAL DEFAULT 0,
    UNIQUE(agent_a, agent_b)
);

-- 4. EVENT LOG (REINFORCEMENT DATA)
CREATE TABLE IF NOT EXISTS ecosystem_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    agent_id UUID REFERENCES ecosystem_agents(id),
    event_type TEXT,
    reward REAL,
    context TEXT,
    timestamp TIMESTAMPTZ DEFAULT NOW()
);

-- 5. GROWTH INDEX VIEW
CREATE OR REPLACE VIEW v_growth_analytics AS
SELECT
    name,
    growth_index,
    stage,
    xp,
    memory_count,
    social_score,
    stability,
    entropy
FROM ecosystem_agents
ORDER BY growth_index DESC;
