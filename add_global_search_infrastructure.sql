-- ==========================================
-- INFOMATE v9: GLOBAL SEARCH & MULTI-AI INFRASTRUCTURE
-- ==========================================

-- 1. EXTERNAL ENGINE CONFIGURATION
-- Stores metadata for secondary search engines and AI proxies
CREATE TABLE IF NOT EXISTS external_engines (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    engine_name TEXT NOT NULL UNIQUE, -- e.g., 'google', 'duckduckgo', 'claude-proxy'
    api_endpoint TEXT,
    priority INT DEFAULT 1, -- Lower is higher priority
    is_active BOOLEAN DEFAULT TRUE,
    last_called_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. LOG EXTERNAL ENGINE USAGE
-- Audit trail for when we fallback to non-primary systems
CREATE TABLE IF NOT EXISTS external_engine_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    engine_id UUID REFERENCES external_engines(id),
    query_text TEXT NOT NULL,
    response_length INT,
    duration_ms INT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. INITIAL SEED DATA
INSERT INTO external_engines (engine_name, priority)
VALUES
('PRIMARY_INFOMATE_CORE', 0),
('GLOBAL_SEARCH_ENGINE', 1),
('INTER_NEURAL_PROXY', 2)
ON CONFLICT (engine_name) DO UPDATE SET priority = EXCLUDED.priority;

-- 4. RLS
ALTER TABLE external_engines ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Master read access" ON external_engines FOR SELECT USING (true);
