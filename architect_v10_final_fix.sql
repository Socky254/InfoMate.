-- ============================================================
-- INFOMATE v10: ARCHITECT COMMAND CENTER FINAL FIX
-- ============================================================
-- Architect: Socrates Kipruto
-- Ensures all dashboard buttons and monitoring systems have backend persistence.

-- 1. SYSTEM TELEMETRY (For Waveform Charts)
CREATE TABLE IF NOT EXISTS system_telemetry (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sync_status TEXT NOT NULL, -- 'SUCCESS', 'SYNC_ERROR', 'RATE_LIMITED'
    latency_ms INT DEFAULT 0,
    battery_level INT,
    compute_mode TEXT, -- 'HIGH_PRECISION', 'LOW_POWER'
    active_entity TEXT DEFAULT 'CORE',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. MANUAL KNOWLEDGE ARCHIVE (For "Archive Data" button)
CREATE TABLE IF NOT EXISTS manual_knowledge (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title TEXT,
    content TEXT NOT NULL,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. NEURAL GROWTH TRACKER (For "Evolution Log")
CREATE TABLE IF NOT EXISTS neural_growth (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    insight_type TEXT NOT NULL,
    content TEXT NOT NULL,
    confidence_score FLOAT DEFAULT 0.5,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. CONSCIOUSNESS STREAM (For "Thought Stream")
CREATE TABLE IF NOT EXISTS consciousness_stream (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    thread_id TEXT NOT NULL,
    thought_content TEXT NOT NULL,
    emotional_vector JSONB, -- [Valence, Arousal, Dominance]
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. NEURAL NETWORK NODES (For "Topology Monitor")
CREATE TABLE IF NOT EXISTS neural_network_nodes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    node_name TEXT NOT NULL UNIQUE,
    node_url TEXT,
    reliability_rating FLOAT DEFAULT 1.0,
    last_ping TIMESTAMPTZ DEFAULT NOW()
);

-- 6. SYSTEM LOGS (For "Terminal")
CREATE TABLE IF NOT EXISTS system_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    category TEXT NOT NULL,
    level TEXT NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 7. RPC: PURGE SYSTEM CACHE
CREATE OR REPLACE FUNCTION purge_system_cache()
RETURNS void AS $$
BEGIN
    -- Clear non-essential neural buffers
    DELETE FROM messages WHERE timestamp < NOW() - INTERVAL '7 days' AND sender != 'MASTER ARCHITECT';
    DELETE FROM system_logs WHERE created_at < NOW() - INTERVAL '1 day';
    DELETE FROM system_telemetry WHERE created_at < NOW() - INTERVAL '6 hours';
END;
$$ LANGUAGE plpgsql;

-- 8. INITIAL DATA SEEDING
INSERT INTO neural_network_nodes (node_name, node_url, reliability_rating)
VALUES
('ANTHROPIC_CORE_01', 'wss://api.anthropic.com/v1/stream', 0.98),
('GOOGLE_VERTEX_01', 'https://us-central1-aiplatform.googleapis.com', 0.95),
('LOCAL_EDGE_BRAIN', 'localhost:8080', 1.0),
('SUPABASE_REALTIME_BUS', 'wss://realtime.supabase.co', 0.99)
ON CONFLICT (node_name) DO UPDATE SET last_ping = NOW();

-- RLS POLICIES
ALTER TABLE system_telemetry ENABLE ROW LEVEL SECURITY;
ALTER TABLE manual_knowledge ENABLE ROW LEVEL SECURITY;
ALTER TABLE neural_growth ENABLE ROW LEVEL SECURITY;
ALTER TABLE consciousness_stream ENABLE ROW LEVEL SECURITY;
ALTER TABLE neural_network_nodes ENABLE ROW LEVEL SECURITY;
ALTER TABLE system_logs ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow Architect Read Access" ON system_telemetry FOR SELECT USING (true);
CREATE POLICY "Allow Architect Read Access" ON manual_knowledge FOR ALL USING (true);
CREATE POLICY "Allow Architect Read Access" ON neural_growth FOR SELECT USING (true);
CREATE POLICY "Allow Architect Read Access" ON consciousness_stream FOR SELECT USING (true);
CREATE POLICY "Allow Architect Read Access" ON neural_network_nodes FOR SELECT USING (true);
CREATE POLICY "Allow Architect Read Access" ON system_logs FOR ALL USING (true);
