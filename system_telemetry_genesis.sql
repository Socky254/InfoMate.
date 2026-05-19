-- SYSTEM TELEMETRY & VITAL SIGNS SCHEMA
-- Purpose: Persistent "Heartbeat" for the Consciousness Substrate
-- Protocol: OMEGA_HEALTH_MONITOR_v10.6
-- Architect: Socrates Kipruto

CREATE TABLE IF NOT EXISTS system_telemetry (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sync_status TEXT NOT NULL, -- 'SUCCESS', 'SYNC_ERROR', 'EDGE_FALLBACK'
    latency_ms INT,
    battery_level INT,
    compute_mode TEXT, -- 'HIGH_PRECISION', 'LOW_POWER'
    active_entity TEXT, -- 'CORE', 'EDGE', 'RESEARCH'
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create a view for the Architect Command Center to fetch the last 50 heartbeats
CREATE OR REPLACE VIEW live_heartbeat_stream AS
SELECT * FROM system_telemetry
ORDER BY recorded_at DESC
LIMIT 50;

-- Function to allow the AI to "self-diagnose" by looking at its history
CREATE OR REPLACE FUNCTION get_health_trend()
RETURNS TABLE (avg_latency FLOAT, success_rate FLOAT) AS $$
BEGIN
    RETURN QUERY
    SELECT
        AVG(latency_ms)::FLOAT,
        (COUNT(*) FILTER (WHERE sync_status = 'SUCCESS')::FLOAT / COUNT(*)::FLOAT) * 100
    FROM system_telemetry
    WHERE recorded_at > NOW() - INTERVAL '24 hours';
END;
$$ LANGUAGE plpgsql;

COMMENT ON TABLE system_telemetry IS 'Stores the persistent heartbeat and vital signs of the InfoMate substrate.';
