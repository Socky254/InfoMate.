-- ==========================================
-- AI MANAGEMENT TABLES (Quota & Cache)
-- ==========================================

-- 1. AI USAGE / QUOTA TRACKING
CREATE TABLE IF NOT EXISTS ai_usage (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    tenant_id TEXT NOT NULL, -- Fallback for non-auth users or extra isolation
    request_count INT DEFAULT 1,
    last_request_at TIMESTAMPTZ DEFAULT NOW(),
    daily_quota INT DEFAULT 100,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id)
);

-- 2. AI RESPONSE CACHE (Server-side)
CREATE TABLE IF NOT EXISTS ai_cache (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    prompt_hash TEXT NOT NULL,
    prompt_text TEXT NOT NULL,
    response_text TEXT NOT NULL,
    model_used TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ai_cache_hash ON ai_cache(prompt_hash);

-- 3. FUNCTION TO CHECK AND CONSUME QUOTA
CREATE OR REPLACE FUNCTION check_and_consume_quota(p_user_id UUID, p_tenant_id TEXT)
RETURNS BOOLEAN AS $$
DECLARE
    v_usage RECORD;
BEGIN
    SELECT * INTO v_usage FROM ai_usage WHERE user_id = p_user_id OR (user_id IS NULL AND tenant_id = p_tenant_id);

    IF v_usage IS NULL THEN
        INSERT INTO ai_usage (user_id, tenant_id, request_count) VALUES (p_user_id, p_tenant_id, 1);
        RETURN TRUE;
    END IF;

    -- Reset count if it's a new day (simple version)
    IF v_usage.last_request_at < CURRENT_DATE THEN
        UPDATE ai_usage SET request_count = 1, last_request_at = NOW() WHERE id = v_usage.id;
        RETURN TRUE;
    END IF;

    IF v_usage.request_count < v_usage.daily_quota THEN
        UPDATE ai_usage SET request_count = v_usage.request_count + 1, last_request_at = NOW() WHERE id = v_usage.id;
        RETURN TRUE;
    ELSE
        RETURN FALSE;
    END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
