-- SYSTEM LOGS SCHEMA
-- Purpose: Detailed activity logging for Master Architect operations
-- Protocol: OMEGA_LOGGING_v10.7
-- Architect: Socrates Kipruto

CREATE TABLE IF NOT EXISTS system_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    category TEXT NOT NULL, -- 'DIAGNOSTIC', 'REPAIR', 'RESEARCH', 'UPGRADE', 'CORE'
    level TEXT NOT NULL, -- 'INFO', 'WARN', 'ERROR', 'SUCCESS', 'CRITICAL'
    message TEXT NOT NULL,
    details JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Index for fast retrieval of recent logs by category
CREATE INDEX IF NOT EXISTS idx_system_logs_category ON system_logs (category, created_at DESC);

-- Function to prune old logs (keep last 1000 or 30 days)
CREATE OR REPLACE FUNCTION prune_system_logs()
RETURNS void AS $$
BEGIN
    DELETE FROM system_logs WHERE created_at < NOW() - INTERVAL '30 days';
END;
$$ LANGUAGE plpgsql;

COMMENT ON TABLE system_logs IS 'Audit trail for high-level system operations and agent activities.';
