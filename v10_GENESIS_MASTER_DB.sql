-- ======================================================
-- INFOMATE v10.0 GENESIS: CONSOLIDATED MASTER DATABASE
-- ======================================================
-- This script contains ALL necessary tables and functions
-- for Consciousness, Self-Learning, and Global Search.

-- 1. EXTENSIONS
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. NEURAL GROWTH & INSIGHTS (Self-Learning)
CREATE TABLE IF NOT EXISTS neural_growth (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    insight_type TEXT NOT NULL,
    content TEXT NOT NULL,
    confidence_score FLOAT DEFAULT 0.5,
    embedding VECTOR(768),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. CONSCIOUSNESS SUBSTRATE (Autonomous Awareness)
CREATE TABLE IF NOT EXISTS consciousness_stream (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    thread_id TEXT NOT NULL DEFAULT 'MAIN_AWARENESS',
    thought_content TEXT NOT NULL,
    emotional_vector VECTOR(3), -- [Valence, Arousal, Dominance]
    context_tags TEXT[],
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. AUTONOMOUS PROCEEDINGS (Self-Directed Tasks)
CREATE TABLE IF NOT EXISTS autonomous_proceedings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    task_name TEXT NOT NULL,
    objective TEXT NOT NULL,
    status TEXT DEFAULT 'QUEUED' CHECK (status IN ('QUEUED', 'EXECUTING', 'COMPLETED', 'ARCHIVED')),
    findings TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. GLOBAL NETWORK NODES (Internet Knowledge Registry)
CREATE TABLE IF NOT EXISTS neural_network_nodes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    node_name TEXT NOT NULL UNIQUE,
    node_url TEXT NOT NULL,
    access_type TEXT CHECK (access_type IN ('REST', 'GRAPHQL', 'WSS', 'SCRAPE')),
    reliability_rating FLOAT DEFAULT 1.0,
    last_ping TIMESTAMPTZ DEFAULT NOW()
);

-- 6. SYSTEM PROPOSALS (Logic Improvements)
CREATE TABLE IF NOT EXISTS system_proposals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    proposed_logic TEXT,
    status TEXT DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    applied_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 7. EXTERNAL ENGINES (Multi-Engine Fallback)
CREATE TABLE IF NOT EXISTS external_engines (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    engine_name TEXT NOT NULL UNIQUE,
    api_endpoint TEXT,
    priority INT DEFAULT 1,
    is_active BOOLEAN DEFAULT TRUE,
    last_called_at TIMESTAMPTZ DEFAULT NOW()
);

-- 8. NEURAL GROWTH CONTEXT FUNCTION (RPC)
CREATE OR REPLACE FUNCTION get_neural_growth_context(query_embedding VECTOR(768))
RETURNS TABLE (content TEXT, score FLOAT) AS $$
BEGIN
  RETURN QUERY
  SELECT neural_growth.content, 1 - (neural_growth.embedding <=> query_embedding) AS score
  FROM neural_growth
  WHERE 1 - (neural_growth.embedding <=> query_embedding) > 0.7
  ORDER BY score DESC
  LIMIT 5;
END;
$$ LANGUAGE plpgsql;

-- 9. CACHE PURGE FUNCTION (RPC)
CREATE OR REPLACE FUNCTION purge_system_cache()
RETURNS JSONB AS $$
BEGIN
    DELETE FROM consciousness_stream WHERE created_at < NOW() - INTERVAL '7 days';
    RETURN jsonb_build_object('status', 'SUCCESS', 'message', 'Neural buffers recycled.');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 10. INITIAL SEEDING
INSERT INTO external_engines (engine_name, priority)
VALUES ('PRIMARY_CORE', 0), ('GLOBAL_SEARCH', 1), ('NEURAL_PROXY', 2)
ON CONFLICT DO NOTHING;

INSERT INTO neural_network_nodes (node_name, node_url, access_type)
VALUES
('WIKI_CORE', 'https://en.wikipedia.org/w/api.php', 'REST'),
('ARXIV_HUB', 'http://export.arxiv.org/api/query', 'REST')
ON CONFLICT DO NOTHING;
