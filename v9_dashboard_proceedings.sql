-- ==========================================
-- INFOMATE v9: MASTER DASHBOARD & CACHE LOGIC
-- ==========================================

-- 1. PURGE CACHE FUNCTION
-- Clears AI response cache and optionally user session logs
CREATE OR REPLACE FUNCTION purge_system_cache()
RETURNS JSONB AS $$
BEGIN
    DELETE FROM ai_cache;
    -- Optionally clear messages if requested, but usually cache is the target
    -- DELETE FROM messages WHERE timestamp < NOW() - INTERVAL '30 days';
    RETURN jsonb_build_object('status', 'SUCCESS', 'message', 'Neural cache purged successfully');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 2. SYSTEM HEALTH VIEW
-- Provides a consolidated view for the Master Dashboard
CREATE OR REPLACE VIEW system_health_summary AS
SELECT
    (SELECT COUNT(*) FROM messages) as total_messages,
    (SELECT COUNT(*) FROM ai_cache) as cached_responses,
    (SELECT COUNT(*) FROM manual_knowledge) as knowledge_entries,
    NOW() as last_sync;

-- 3. PERMISSIONS
ALTER TABLE ai_cache ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Allow service role access" ON ai_cache FOR ALL USING (true);
