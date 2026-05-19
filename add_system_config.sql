-- Create a table for global system configurations, including updates
CREATE TABLE IF NOT EXISTS public.system_config (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    key TEXT UNIQUE NOT NULL,
    value JSONB NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Insert initial version config
-- version_code: The build number to check against
-- download_url: The direct link to the new APK (e.g., from GitHub Releases)
-- changelog: What's new in this version
INSERT INTO public.system_config (key, value)
VALUES (
    'latest_update',
    '{
        "version_code": 1,
        "version_name": "1.0",
        "download_url": "https://github.com/SocratesKipruto/InfoMate/releases/latest",
        "changelog": "Initial Production Release",
        "critical": false
    }'
)
ON CONFLICT (key) DO UPDATE
SET value = EXCLUDED.value, updated_at = NOW();

ALTER TABLE public.system_config ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Allow Public Read" ON public.system_config FOR SELECT USING (true);
