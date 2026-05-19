-- ==========================================
-- MANDATORY SYSTEM SCHEMA (CORE ENFORCEMENT)
-- ==========================================

-- 1. AI MESSAGES (Updated for traceability)
CREATE TABLE IF NOT EXISTS ai_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    request_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    session_id UUID NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('user', 'assistant', 'system')),
    content TEXT NOT NULL,
    model TEXT NOT NULL,
    tokens_used INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. AI REQUEST LOGS (Traceability & Latency Tracking)
CREATE TABLE IF NOT EXISTS ai_request_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    request_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    event_type TEXT NOT NULL, -- 'INIT', 'VALIDATED', 'AUTHED', 'QUEUED', 'STREAMING', 'FINALIZED', 'STORED'
    status TEXT NOT NULL,
    latency_ms INT,
    error_code TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. TENANT USAGE (Quota & Token Enforcement)
CREATE TABLE IF NOT EXISTS tenant_usage (
    tenant_id UUID PRIMARY KEY REFERENCES tenants(id) ON DELETE CASCADE,
    requests_used INT DEFAULT 0,
    tokens_used BIGINT DEFAULT 0,
    last_reset TIMESTAMPTZ DEFAULT NOW()
);

-- 4. REQUEST IDEMPOTENCY (Consistency Control)
CREATE TABLE IF NOT EXISTS request_idempotency (
    request_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    status TEXT NOT NULL, -- 'PROCESSING', 'COMPLETED', 'FAILED'
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. DEAD LETTER SYSTEM (Failed Stream Recovery)
CREATE TABLE IF NOT EXISTS dead_letter_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    request_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    prompt TEXT NOT NULL,
    error TEXT,
    retry_count INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 6. SYSTEM GOVERNOR (Global Control)
CREATE TABLE IF NOT EXISTS system_governor (
    id INT PRIMARY KEY DEFAULT 1,
    is_kill_switch_active BOOLEAN DEFAULT FALSE,
    global_concurrency_limit INT DEFAULT 50,
    current_active_calls INT DEFAULT 0,
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT one_row CHECK (id = 1)
);
INSERT INTO system_governor (id) VALUES (1) ON CONFLICT DO NOTHING;

-- 7. INDICES FOR SCALE
CREATE INDEX IF NOT EXISTS idx_ai_messages_request_id ON ai_messages(request_id);
CREATE INDEX IF NOT EXISTS idx_ai_messages_tenant_session ON ai_messages(tenant_id, session_id);
CREATE INDEX IF NOT EXISTS idx_ai_request_logs_request_id ON ai_request_logs(request_id);

-- 5. TRACE SYSTEM (REQUEST VISIBILITY ENGINE)
-- This lets you rebuild full request history for debugging
CREATE TABLE IF NOT EXISTS ai_traces (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    request_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    event TEXT NOT NULL,
    timestamp BIGINT NOT NULL,
    latency_ms INT DEFAULT 0,
    meta JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_ai_traces_request_id ON ai_traces(request_id);

-- 9. UPDATED QUOTA FUNCTION (With Idempotency & Governor)
CREATE OR REPLACE FUNCTION enforce_request_governance(
    p_request_id UUID,
    p_tenant_id UUID,
    p_user_id UUID,
    p_session_id UUID
)
RETURNS JSONB AS $$
DECLARE
    v_limit INT;
    v_used INT;
    v_governor RECORD;
    v_idempotency TEXT;
BEGIN
    -- 1. Check Global Governor (Kill Switch & Concurrency)
    SELECT * INTO v_governor FROM system_governor WHERE id = 1;
    IF v_governor.is_kill_switch_active THEN
        RETURN jsonb_build_object('allowed', false, 'error_code', 'SYSTEM_THROTTLED', 'message', 'Global kill switch active');
    END IF;

    -- 2. Check Idempotency
    SELECT status INTO v_idempotency FROM request_idempotency WHERE request_id = p_request_id;
    IF v_idempotency IS NOT NULL THEN
        RETURN jsonb_build_object('allowed', false, 'error_code', 'DUPLICATE_REQUEST', 'status', v_idempotency);
    END IF;

    -- 3. Get Tenant Limits
    SELECT daily_request_limit INTO v_limit FROM ai_quotas WHERE tenant_id = p_tenant_id;
    IF v_limit IS NULL THEN v_limit := 100; END IF;

    -- 4. Atomic Quota Update
    UPDATE tenant_usage
    SET requests_used = requests_used + 1
    WHERE tenant_id = p_tenant_id AND requests_used < v_limit;

    IF NOT FOUND THEN
        -- Check if it's because record doesn't exist or limit exceeded
        INSERT INTO tenant_usage (tenant_id, requests_used)
        SELECT p_tenant_id, 1
        WHERE NOT EXISTS (SELECT 1 FROM tenant_usage WHERE tenant_id = p_tenant_id);

        IF NOT FOUND THEN
           RETURN jsonb_build_object('allowed', false, 'error_code', 'QUOTA_EXCEEDED');
        END IF;
    END IF;

    -- 5. Mark Idempotency as PROCESSING
    INSERT INTO request_idempotency (request_id, tenant_id, status)
    VALUES (p_request_id, p_tenant_id, 'PROCESSING');

    -- 6. Initial Log Entry
    INSERT INTO ai_request_logs (request_id, tenant_id, event_type, status)
    VALUES (p_request_id, p_tenant_id, 'INIT', 'SUCCESS');

    RETURN jsonb_build_object('allowed', true);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 10. TOKEN INCREMENT FUNCTION
CREATE OR REPLACE FUNCTION increment_tenant_tokens(p_tenant_id UUID, p_tokens INT)
RETURNS VOID AS $$
BEGIN
    UPDATE tenant_usage
    SET tokens_used = tokens_used + p_tokens
    WHERE tenant_id = p_tenant_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
