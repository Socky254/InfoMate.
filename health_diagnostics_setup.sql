-- InfoMate v10.5 Health & Diagnostics Schema
-- Optimized for Real-time Observability

CREATE TABLE IF NOT EXISTS health_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    subsystem TEXT NOT NULL, -- AI_LINK, NETWORK, MEMORY, DATABASE, FEED
    status TEXT NOT NULL,    -- OPERATIONAL, DEGRADED, OFFLINE, RECOVERING
    details TEXT,            -- Specific error message or performance metric
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Index for fast status retrieval by subsystem
CREATE INDEX IF NOT EXISTS idx_health_logs_subsystem_date ON health_logs (subsystem, created_at DESC);

-- Cleanup function to keep the table lean (optional)
CREATE OR REPLACE FUNCTION purge_old_health_logs() RETURNS void AS $$
BEGIN
    DELETE FROM health_logs WHERE created_at < NOW() - INTERVAL '7 days';
END;
$$ LANGUAGE plpgsql;
