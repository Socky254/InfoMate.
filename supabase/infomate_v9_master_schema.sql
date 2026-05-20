-- InfoMate v12.4 Master Schema
-- Created by Socrates Kipruto
-- Optimized for Google Gemini & pgvector (768 Dimensions)

-- 1. Enable pgvector extension
create extension if not exists vector;

-- 2. Messages Table: Stores the core conversational history
create table if not exists public.messages (
    id uuid default gen_random_uuid() primary key,
    content text not null,
    sender text not null, -- 'OPERATOR', 'MASTER ARCHITECT', 'INFOMATE', 'SYSTEM'
    message_type text default 'TEXT',
    trigger_phrase text default '',
    timestamp bigint default extract(epoch from now()) * 1000,
    created_at timestamptz default now()
);

-- 3. Memory Nodes Table: Vector-enabled long-term memory (RAG)
create table if not exists public.memory_nodes (
    id uuid default gen_random_uuid() primary key,
    content text not null,
    embedding vector(768), -- Optimized for Google Gemini Embedding-004
    type text, -- 'fact', 'skill', 'preference', etc.
    importance float default 0.5,
    agent_source text,
    created_at timestamptz default now()
);

-- 4. Neural Growth Table: Archives evolutionary steps and insights
create table if not exists public.neural_growth (
    id uuid default gen_random_uuid() primary key,
    insight_type text not null,
    content text not null,
    confidence_score float default 0.9,
    embedding vector(768),
    autonomous_choice boolean default true,
    created_at timestamptz default now()
);

-- 5. Consciousness Stream Table: Real-time awareness logs for "Living UI"
create table if not exists public.consciousness_stream (
    id uuid default gen_random_uuid() primary key,
    thread_id text not null,
    thought_content text not null,
    emotional_vector text, -- Stored as string for UI flexibility, e.g., "[0.5, 0.8, 0.2]"
    created_at timestamptz default now()
);

-- 6. System Config Table: Stores personality state and global settings
create table if not exists public.system_config (
    key text primary key,
    value jsonb not null,
    updated_at timestamptz default now()
);

-- 7. System Health Table: Tracks status of core subsystems
create table if not exists public.system_health (
    id uuid default gen_random_uuid() primary key,
    api_connected boolean default true,
    status_code text,
    error_log text,
    severity_level int default 0,
    timestamp bigint default extract(epoch from now()) * 1000,
    created_at timestamptz default now()
);

-- 8. User Preferences Table
create table if not exists public.user_preferences (
    id uuid default gen_random_uuid() primary key,
    user_email text default 'socratesart@live' unique, -- Added UNIQUE for upsert stability
    voice_gender text default 'FEMALE',
    last_updated bigint default extract(epoch from now()) * 1000
);

-- 9. System Logs Table: General terminal logging
create table if not exists public.system_logs (
    id uuid default gen_random_uuid() primary key,
    category text default 'CORE',
    level text default 'INFO',
    message text not null,
    created_at bigint default extract(epoch from now()) * 1000
);

-- 10. Autonomous Proceedings Table
create table if not exists public.autonomous_proceedings (
    id uuid default gen_random_uuid() primary key,
    task_name text,
    objective text,
    status text default 'QUEUED',
    result text,
    timestamp timestamptz default now()
);

-- 11. System Proposals Table
create table if not exists public.system_proposals (
    id uuid default gen_random_uuid() primary key,
    title text,
    description text,
    proposed_logic text,
    status text default 'PENDING'
);

-- 12. Neural Network Nodes Table
create table if not exists public.neural_network_nodes (
    node_name text primary key,
    node_url text, -- Added to support distributed architecture
    reliability_rating float default 0.5,
    last_ping timestamptz default now()
);

-- 13. Wisdom Archives Table
create table if not exists public.wisdom_archives (
    id uuid default gen_random_uuid() primary key,
    content text not null,
    significance float default 0.5,
    created_at timestamptz default now()
);

-- 14. Cognitive Logs Table
create table if not exists public.cognitive_logs (
    id uuid default gen_random_uuid() primary key,
    message_id uuid,
    step_title text,
    step_content text,
    step_index int,
    duration_ms int
);

-- 15. Agent States Table
create table if not exists public.agent_states (
    agent_id text primary key,
    growth_index float,
    role text,
    entropy float,
    updated_at timestamptz default now()
);

-- 16. Manual Knowledge Table
create table if not exists public.manual_knowledge (
    id uuid default gen_random_uuid() primary key,
    title text,
    content text,
    created_at bigint default extract(epoch from now()) * 1000
);

-- 17. System Telemetry Table
create table if not exists public.system_telemetry (
    id uuid default gen_random_uuid() primary key,
    sync_status text,
    latency_ms int,
    battery_level int,
    compute_mode text,
    active_entity text,
    created_at timestamptz default now()
);

-- RPC FUNCTIONS --

-- Drop existing functions to allow changing return types
drop function if exists match_memory_nodes(vector, float, int);
drop function if exists get_neural_growth_context(vector);
drop function if exists match_vectors(vector, float, int);
drop function if exists purge_system_cache();
drop function if exists prune_low_significance_wisdom(float);

-- Vector Similarity Search for Memory Nodes
create or replace function match_memory_nodes (
  query_embedding vector(768),
  match_threshold float,
  match_count int
)
returns table (
  id uuid,
  content text,
  type text,
  importance float,
  similarity float
)
language plpgsql
as $$
begin
  return query
  select
    memory_nodes.id,
    memory_nodes.content,
    memory_nodes.type,
    memory_nodes.importance,
    1 - (memory_nodes.embedding <=> query_embedding) as similarity
  from memory_nodes
  where 1 - (memory_nodes.embedding <=> query_embedding) > match_threshold
  order by similarity desc
  limit match_count;
end;
$$;

-- Vector Similarity Search for Neural Growth Context
create or replace function get_neural_growth_context (
  query_embedding vector(768)
)
returns table (
  id uuid,
  content text,
  similarity float
)
language plpgsql
as $$
begin
  return query
  select
    neural_growth.id,
    neural_growth.content,
    1 - (neural_growth.embedding <=> query_embedding) as similarity
  from neural_growth
  where 1 - (neural_growth.embedding <=> query_embedding) > 0.3
  order by similarity desc
  limit 5;
end;
$$;

-- Generic match_vectors function
create or replace function match_vectors (
  query_embedding vector(768),
  match_threshold float,
  match_count int
)
returns table (
  id uuid,
  content text,
  similarity float
)
language plpgsql
as $$
begin
  return query
  select
    id,
    content,
    1 - (embedding <=> query_embedding) as similarity
  from memory_nodes
  where 1 - (embedding <=> query_embedding) > match_threshold
  order by similarity desc
  limit match_count;
end;
$$;

-- Purge Cache RPC
create or replace function purge_system_cache()
returns void
language plpgsql
as $$
begin
  -- Clear transient session data but keep long-term growth
  truncate table messages;
  truncate table cognitive_logs;
  truncate table consciousness_stream;
  delete from system_logs where created_at < extract(epoch from (now() - interval '1 hour')) * 1000;
end;
$$;

-- Prune Low Significance Wisdom
create or replace function prune_low_significance_wisdom(threshold float)
returns void
language plpgsql
as $$
begin
  delete from wisdom_archives where significance < threshold;
end;
$$;

-- ROW LEVEL SECURITY (RLS) --
alter table messages enable row level security;
create policy "Public Access" on messages for all using (true) with check (true);

alter table memory_nodes enable row level security;
create policy "Public Access" on memory_nodes for all using (true) with check (true);

alter table neural_growth enable row level security;
create policy "Public Access" on neural_growth for all using (true) with check (true);

alter table consciousness_stream enable row level security;
create policy "Public Access" on consciousness_stream for all using (true) with check (true);

alter table system_config enable row level security;
create policy "Public Access" on system_config for all using (true) with check (true);

alter table system_health enable row level security;
create policy "Public Access" on system_health for all using (true) with check (true);

alter table user_preferences enable row level security;
create policy "Public Access" on user_preferences for all using (true) with check (true);

alter table system_logs enable row level security;
create policy "Public Access" on system_logs for all using (true) with check (true);

alter table autonomous_proceedings enable row level security;
create policy "Public Access" on autonomous_proceedings for all using (true) with check (true);

alter table system_proposals enable row level security;
create policy "Public Access" on system_proposals for all using (true) with check (true);

alter table neural_network_nodes enable row level security;
create policy "Public Access" on neural_network_nodes for all using (true) with check (true);

alter table wisdom_archives enable row level security;
create policy "Public Access" on wisdom_archives for all using (true) with check (true);

alter table cognitive_logs enable row level security;
create policy "Public Access" on cognitive_logs for all using (true) with check (true);

alter table agent_states enable row level security;
create policy "Public Access" on agent_states for all using (true) with check (true);

alter table manual_knowledge enable row level security;
create policy "Public Access" on manual_knowledge for all using (true) with check (true);

alter table system_telemetry enable row level security;
create policy "Public Access" on system_telemetry for all using (true) with check (true);
