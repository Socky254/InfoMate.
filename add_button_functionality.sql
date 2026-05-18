-- ==========================================
-- INFOMATE v9: PLUS BUTTON ACTION LOGIC
-- ==========================================
-- This script supports the UI's "Add" (+) button for manual data entry.

-- 1. ADD CUSTOM KNOWLEDGE TABLE
-- Used for manual additions via the + button
CREATE TABLE IF NOT EXISTS manual_knowledge (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title TEXT,
    content TEXT NOT NULL,
    tags TEXT[],
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. ENABLE SEARCH FOR MANUAL KNOWLEDGE
-- Function to allow the AI to retrieve manually added info
CREATE OR REPLACE FUNCTION search_manual_knowledge(query_text TEXT)
RETURNS SETOF manual_knowledge AS $$
BEGIN
    RETURN QUERY
    SELECT * FROM manual_knowledge
    WHERE content ILIKE '%' || query_text || '%'
    OR title ILIKE '%' || query_text || '%'
    ORDER BY created_at DESC;
END;
$$ LANGUAGE plpgsql;

-- 3. RLS POLICIES
ALTER TABLE manual_knowledge ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Allow authenticated access" ON manual_knowledge FOR ALL USING (true);
