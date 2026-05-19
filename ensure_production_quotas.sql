-- ==========================================
-- MAINTENANCE: ENSURE PRODUCTION QUOTAS & TENANT SEEDING
-- ==========================================

-- 1. Create the Default Production Tenant if it doesn't exist
INSERT INTO tenants (id, name, plan_type, is_active)
VALUES ('00000000-0000-0000-0000-000000000001', 'Default Production Tenant', 'ENTERPRISE', TRUE)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    plan_type = EXCLUDED.plan_type,
    updated_at = NOW();

-- 2. Ensure the Master Architect (Socrates) is mapped to this tenant if needed
-- (Assumes users table exists and we want to link a specific email/uid)
-- UPDATE auth.users SET raw_user_meta_data = raw_user_meta_data || '{"tenant_id": "00000000-0000-0000-0000-000000000001"}' WHERE email = 'socratesart@live';

-- 3. Seed/Update AI Quotas for the Production Tenant
INSERT INTO ai_quotas (tenant_id, daily_request_limit, monthly_request_limit, max_tokens_per_request, rate_limit_per_minute)
VALUES ('00000000-0000-0000-0000-000000000001', 5000, 150000, 8192, 60)
ON CONFLICT (tenant_id) DO UPDATE SET
    daily_request_limit = EXCLUDED.daily_request_limit,
    monthly_request_limit = EXCLUDED.monthly_request_limit,
    max_tokens_per_request = EXCLUDED.max_tokens_per_request,
    rate_limit_per_minute = EXCLUDED.rate_limit_per_minute,
    updated_at = NOW();

-- 4. Initialize Usage Stats for today to prevent cold-start latency in counting
INSERT INTO ai_usage_stats (tenant_id, usage_date, request_count, token_count)
VALUES ('00000000-0000-0000-0000-000000000001', CURRENT_DATE, 0, 0)
ON CONFLICT (tenant_id, usage_date) DO NOTHING;

-- 5. Verification Query
SELECT t.name, q.daily_request_limit, q.rate_limit_per_minute
FROM tenants t
JOIN ai_quotas q ON t.id = q.tenant_id
WHERE t.id = '00000000-0000-0000-0000-000000000001';
