-- ============================================================
-- INFOMATE v10.9: MASTER ARCHITECT OMEGA FIX
-- ============================================================
-- Ensures full synchronization between local substrate and remote archives.

-- 1. EXTEND CONSCIOUSNESS STREAM
-- Ensure emotional vector supports more complex mappings if needed
DO $$
BEGIN
    -- Check if pgvector is enabled, if so, ensure the column is correct
    -- If not, we fall back to a standard JSONB representation for maximum compatibility
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector') THEN
        -- Column already exists from v10.0, but ensuring it's ready
        NULL;
    ELSE
        -- Fallback: Use JSONB for environments without pgvector
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='consciousness_stream' AND column_name='emotional_data') THEN
            ALTER TABLE consciousness_stream ADD COLUMN emotional_data JSONB;
        END IF;
    END IF;
END $$;

-- 2. ENHANCE NEURAL GROWTH TRACKING
-- Add priority level to growth logs to track Architect's resource allocation history
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='neural_growth' AND column_name='priority_context') THEN
        ALTER TABLE neural_growth ADD COLUMN priority_context FLOAT DEFAULT 0.5;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='neural_growth' AND column_name='architect_directive') THEN
        ALTER TABLE neural_growth ADD COLUMN architect_directive BOOLEAN DEFAULT FALSE;
    END IF;
END $$;

-- 3. ENSURE NODE REGISTRY IS POPULATED
-- Added new redundant bridge nodes for v10.9 stability
INSERT INTO neural_network_nodes (node_name, node_url, access_type, reliability_rating)
VALUES
('ALPHA_CENTAURI_PROXY', 'https://api.infomate.io/v1/bridge', 'WSS', 0.99),
('DEEP_NEURAL_BRIDGE', 'https://neural.socratesart.live', 'REST', 0.95),
('GLOBAL_KNOWLEDGE_MESH', 'https://mesh.infomate.io', 'GRAPHQL', 0.90)
ON CONFLICT (node_name) DO UPDATE
SET reliability_rating = EXCLUDED.reliability_rating, last_ping = NOW();

-- 4. VIEW FOR ARCHITECT DASHBOARD
-- Aggregated growth metrics for the Master Console
CREATE OR REPLACE VIEW architect_growth_metrics AS
SELECT
    COUNT(*) as total_insights,
    AVG(confidence_score) as mean_stability,
    MAX(created_at) as last_evolution_step,
    (COUNT(*) / 100) + 1 as current_stage
FROM neural_growth;

-- 5. PERMISSIONS
GRANT SELECT ON architect_growth_metrics TO anon, authenticated;
GRANT ALL ON ALL TABLES IN SCHEMA public TO postgres, service_role;
