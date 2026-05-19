-- MULTI-SOURCE ENTITY TRACKING SCHEMA
-- Purpose: Enhance the message logs to track specific AI entities (Core, Edge, Search, etc.)
-- Architect: Socrates Kipruto

-- 1. Update messages table to include entity metadata if not exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='messages' AND column_name='entity_source') THEN
        ALTER TABLE messages ADD COLUMN entity_source TEXT DEFAULT 'UNKNOWN';
    END IF;
END $$;

-- 2. Create a view for analyzing entity performance
CREATE OR REPLACE VIEW entity_performance_report AS
SELECT
    entity_source,
    COUNT(*) as total_responses,
    AVG(LENGTH(content)) as avg_response_length,
    MAX(timestamp) as last_active
FROM messages
WHERE sender != 'OPERATOR' AND sender != 'MASTER ARCHITECT'
GROUP BY entity_source;

-- 3. Function to log inter-entity feedback
CREATE TABLE IF NOT EXISTS neural_feedback (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    query_text TEXT,
    core_response_id UUID REFERENCES messages(id),
    edge_response_id UUID REFERENCES messages(id),
    search_response_id UUID REFERENCES messages(id),
    synthesis_quality FLOAT DEFAULT 0.0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 4. RPC to fetch comprehensive multi-source logs for the Architect
CREATE OR REPLACE FUNCTION get_multi_source_logs(p_limit INT DEFAULT 50)
RETURNS TABLE (
    msg_id UUID,
    sender TEXT,
    source TEXT,
    content TEXT,
    ts BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT id, sender, entity_source, content, timestamp
    FROM messages
    ORDER BY timestamp DESC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

-- Comment for the Architect
COMMENT ON TABLE neural_feedback IS 'Logs comparative responses from Core, Edge, and Search entities for system tuning.';
