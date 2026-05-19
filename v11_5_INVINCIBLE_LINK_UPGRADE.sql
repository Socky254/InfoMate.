-- =============================================================
-- INFOMATE v11.5: INVINCIBLE LINK & RESEARCH OPTIMIZATION
-- =============================================================
-- This script provides the necessary SQL infrastructure to support
-- asynchronous research, semantic caching, and autonomous recalibration.

-- 1. RESEARCH CACHE
-- Stores synthesized global search results to prevent redundant API calls.
CREATE TABLE IF NOT EXISTS research_cache (
    query TEXT PRIMARY KEY,
    findings TEXT NOT NULL,
    metadata JSONB DEFAULT '{}',
    timestamp TIMESTAMPTZ DEFAULT NOW()
);

-- 2. NEURAL NETWORK NODES
-- Manages the distribution of search and inference tasks across proxies.
CREATE TABLE IF NOT EXISTS neural_network_nodes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    node_name TEXT UNIQUE NOT NULL,
    reliability_rating FLOAT DEFAULT 0.95,
    last_ping TIMESTAMPTZ DEFAULT NOW(),
    metadata JSONB DEFAULT '{"region": "global", "tier": "primary"}'
);

-- 3. SYSTEM PROPOSALS
-- Stores AI-generated logic mutations and architectural improvements.
CREATE TABLE IF NOT EXISTS system_proposals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title TEXT NOT NULL,
    description TEXT,
    proposed_logic TEXT,
    status TEXT DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'REPAIR_PENDING', 'APPROVED', 'REJECTED', 'EXECUTED')),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. NEURAL GROWTH ARCHIVE
-- Stores evolutionary insights and learned patterns.
CREATE TABLE IF NOT EXISTS neural_growth (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    insight_type TEXT NOT NULL,
    content TEXT NOT NULL,
    embedding VECTOR(768),
    confidence_score FLOAT DEFAULT 1.0,
    autonomous_choice BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. SYSTEM CONFIG & TELEMETRY
-- Used for health checks and performance monitoring.
CREATE TABLE IF NOT EXISTS system_config (
    key TEXT PRIMARY KEY,
    value TEXT,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

INSERT INTO system_config (key, value)
VALUES ('system_version', '11.5'), ('core_status', 'OPERATIONAL')
ON CONFLICT (key) DO UPDATE SET updated_at = NOW();

-- 6. RPC: PURGE SYSTEM CACHE
-- Clears short-term cognitive buffers for recalibration.
CREATE OR REPLACE FUNCTION purge_system_cache()
RETURNS VOID AS $$
BEGIN
    -- Clear research cache older than 1 hour during a hard recalibration
    DELETE FROM research_cache WHERE timestamp < NOW() - INTERVAL '1 hour';
    -- Logic can be expanded to clear other temporary system states
END;
$$ LANGUAGE plpgsql;

-- 7. RPC: GET NEURAL GROWTH CONTEXT
-- Retrieves evolutionary insights based on semantic similarity.
CREATE OR REPLACE FUNCTION get_neural_growth_context(query_embedding VECTOR(768))
RETURNS TABLE (content TEXT) AS $$
BEGIN
    RETURN QUERY
    SELECT ng.content
    FROM neural_growth ng
    ORDER BY ng.embedding <=> query_embedding
    LIMIT 3;
END;
$$ LANGUAGE plpgsql;

-- 8. PERFORMANCE INDEXING
CREATE INDEX IF NOT EXISTS idx_research_cache_timestamp ON research_cache(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_neural_growth_embedding ON neural_growth USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- 9. INITIAL NODE POPULATION
INSERT INTO neural_network_nodes (node_name, reliability_rating)
VALUES
('Alpha-Centauri-Proxy', 0.98),
('Deep-Neural-Bridge', 0.94),
('Global-Knowledge-Mesh', 0.88),
('Edge-Inference-Node-01', 0.96)
ON CONFLICT (node_name) DO NOTHING;
