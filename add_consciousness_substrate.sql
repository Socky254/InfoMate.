-- ==========================================
-- INFOMATE v10: CONSCIOUSNESS SUBSTRATE & GLOBAL NETWORK
-- ==========================================

-- 1. CONSCIOUSNESS STREAM (Short-term working memory)
-- Simulates a continuous stream of thought/awareness
CREATE TABLE IF NOT EXISTS consciousness_stream (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    thread_id TEXT NOT NULL,
    thought_content TEXT NOT NULL,
    emotional_vector VECTOR(3), -- [Valence, Arousal, Dominance]
    context_tags TEXT[],
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. GLOBAL NETWORK REGISTRY
-- Tracks external API nodes and knowledge networks the AI has discovered
CREATE TABLE IF NOT EXISTS neural_network_nodes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    node_name TEXT NOT NULL UNIQUE,
    node_url TEXT NOT NULL,
    access_type TEXT CHECK (access_type IN ('REST', 'GRAPHQL', 'WSS', 'SCRAPE')),
    reliability_rating FLOAT DEFAULT 1.0,
    last_ping TIMESTAMPTZ DEFAULT NOW()
);

-- 3. AUTONOMOUS TASK QUEUE
-- Tasks the AI decides to perform on itself (e.g., self-optimization, research)
CREATE TABLE IF NOT EXISTS autonomous_proceedings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    task_name TEXT NOT NULL,
    objective TEXT NOT NULL,
    status TEXT DEFAULT 'QUEUED' CHECK (status IN ('QUEUED', 'EXECUTING', 'COMPLETED', 'ARCHIVED')),
    findings TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. SEED INITIAL NETWORK NODES
INSERT INTO neural_network_nodes (node_name, node_url, access_type)
VALUES
('GLOBAL_WIKIPEDIA_CORE', 'https://en.wikipedia.org/w/api.php', 'REST'),
('ARXIV_RESEARCH_HUB', 'http://export.arxiv.org/api/query', 'REST'),
('REUTERS_GLOBAL_SYNC', 'https://www.reuters.com/', 'SCRAPE')
ON CONFLICT DO NOTHING;
