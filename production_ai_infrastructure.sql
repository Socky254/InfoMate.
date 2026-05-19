-- ==========================================
-- PRODUCTION-GRADE MULTI-TENANT AI INFRASTRUCTURE
-- ==========================================

-- 1. TENANT MANAGEMENT
-- Foundation for multi-user/multi-org isolation
CREATE TABLE IF NOT EXISTS tenants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    api_key_hash TEXT UNIQUE, -- For external API access if needed
    plan_type TEXT DEFAULT 'FREE' CHECK (plan_type IN ('FREE', 'PRO', 'ENTERPRISE')),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. TENANT QUOTA CONFIGURATION
-- Defines strict limits per tenant to prevent runaway AI loops
CREATE TABLE IF NOT EXISTS ai_quotas (
    tenant_id UUID PRIMARY KEY REFERENCES tenants(id) ON DELETE CASCADE,
    daily_request_limit INT DEFAULT 100,
    monthly_request_limit INT DEFAULT 2000,
    max_tokens_per_request INT DEFAULT 4000,
    rate_limit_per_minute INT DEFAULT 5,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. REAL-TIME USAGE TRACKING
-- Tracks consumption for the Quota Engine (Rule 3 & 4)
CREATE TABLE IF NOT EXISTS ai_usage_stats (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    usage_date DATE DEFAULT CURRENT_DATE,
    request_count INT DEFAULT 0,
    token_count INT DEFAULT 0,
    last_request_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(tenant_id, usage_date)
);

-- 4. GLOBAL AI CACHE (RULE 4: COST SAVING)
-- Deduplicates identical prompts across the system or per tenant
CREATE TABLE IF NOT EXISTS ai_cache (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    prompt_hash TEXT NOT NULL, -- SHA-256 of the sanitized prompt
    response_text TEXT NOT NULL,
    model_id TEXT NOT NULL DEFAULT 'gemini-2.0-flash-lite',
    tenant_id UUID REFERENCES tenants(id), -- Nullable for global cache sharing
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_ai_cache_lookup ON ai_cache (prompt_hash);

-- 5. SYSTEM AUDIT & HEALTH (RULE 2: NO AI IN LOGS)
-- Purely technical logging for the Health System
CREATE TABLE IF NOT EXISTS system_audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID REFERENCES tenants(id),
    level TEXT NOT NULL CHECK (level IN ('INFO', 'WARN', 'ERROR', 'CRITICAL')),
    component TEXT NOT NULL, -- e.g., 'GATEWAY', 'ORCHESTRATOR', 'QUOTA'
    message TEXT NOT NULL,
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 6. ATOMIC QUOTA ENFORCEMENT FUNCTION
-- Handles "Check & Consume" in a single transaction
CREATE OR REPLACE FUNCTION orchestrator_check_quota(p_tenant_id UUID)
RETURNS JSONB AS $$
DECLARE
    v_limit INT;
    v_current INT;
    v_quota_record RECORD;
BEGIN
    -- 1. Get Limits
    SELECT daily_request_limit INTO v_limit FROM ai_quotas WHERE tenant_id = p_tenant_id;
    IF v_limit IS NULL THEN v_limit := 100; END IF;

    -- 2. Upsert usage for today
    INSERT INTO ai_usage_stats (tenant_id, usage_date, request_count)
    VALUES (p_tenant_id, CURRENT_DATE, 0)
    ON CONFLICT (tenant_id, usage_date) DO NOTHING;

    -- 3. Check and Increment
    UPDATE ai_usage_stats
    SET request_count = request_count + 1,
        last_request_at = NOW()
    WHERE tenant_id = p_tenant_id
      AND usage_date = CURRENT_DATE
      AND request_count < v_limit
    RETURNING request_count INTO v_current;

    IF v_current IS NULL THEN
        RETURN jsonb_build_object('allowed', false, 'error', 'DAILY_QUOTA_EXCEEDED');
    END IF;

    RETURN jsonb_build_object('allowed', true, 'current_usage', v_current, 'limit', v_limit);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 7. INITIAL BOOTSTRAP (Default Tenant)
-- Creates the initial environment for Socrates (The Architect)
DO $$
DECLARE
    v_tenant_id UUID;
BEGIN
    INSERT INTO tenants (name, plan_type)
    VALUES ('Default Production Tenant', 'ENTERPRISE')
    RETURNING id INTO v_tenant_id;

    INSERT INTO ai_quotas (tenant_id, daily_request_limit, monthly_request_limit)
    VALUES (v_tenant_id, 1000, 30000);
END $$;
