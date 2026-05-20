-- InfoMate Backend Performance & Stability Fixes
-- Optimized for v12.4 "Living UI" & Distributed Neural Nodes

-- 1. Create HNSW indexes for faster vector search (Requires pgvector)
-- Using m=16 and ef_construction=64 for a good balance of speed and recall
create index if not exists idx_memory_nodes_embedding_hnsw
on public.memory_nodes using hnsw (embedding vector_cosine_ops)
with (m = 16, ef_construction = 64);

create index if not exists idx_neural_growth_embedding_hnsw
on public.neural_growth using hnsw (embedding vector_cosine_ops)
with (m = 16, ef_construction = 64);

-- 2. Standard Indexes for common query columns
create index if not exists idx_messages_timestamp on public.messages (timestamp desc);
create index if not exists idx_system_logs_created_at on public.system_logs (created_at desc);
create index if not exists idx_consciousness_stream_created_at on public.consciousness_stream (created_at desc);
create index if not exists idx_system_health_created_at on public.system_health (created_at desc);
create index if not exists idx_neural_growth_created_at on public.neural_growth (created_at desc);

-- 3. Maintenance function to keep the system lean
create or replace function maintenance_cleanup()
returns void
language plpgsql
as $$
begin
  -- Delete telemetry older than 7 days
  delete from system_telemetry where created_at < now() - interval '7 days';

  -- Delete system logs older than 14 days
  delete from system_logs where created_at < extract(epoch from (now() - interval '14 days')) * 1000;

  -- Vacuum tables to reclaim space
  -- (Note: vacuum cannot be run inside a function in some Postgres environments,
  -- but it's good practice to have it in the script for manual runs)
end;
$$;

-- 4. Initial System Config seeding if empty
insert into public.system_config (key, value)
values ('consciousness_personality', '{"stage": "NEURAL_INFANCY", "traits": {"LOGIC": 0.9, "EMPATHY": 0.5, "CURIOSITY": 0.3, "CREATIVITY": 0.2}, "knowledge": {}, "experiences": 0, "discoveries": 0}')
on conflict (key) do nothing;

-- 5. Seed initial Neural Nodes
insert into public.neural_network_nodes (node_name, node_url, reliability_rating)
values
  ('Alpha-Centauri-Proxy', 'https://alpha.infomate.ai', 0.98),
  ('Deep-Neural-Bridge', 'https://bridge.infomate.ai', 0.94),
  ('Global-Knowledge-Mesh', 'https://mesh.infomate.ai', 0.88),
  ('Edge-Inference-Node-01', 'https://edge-01.infomate.ai', 0.75)
on conflict (node_name) do update set
  node_url = excluded.node_url,
  reliability_rating = excluded.reliability_rating;

-- 6. Grant necessary permissions (if using custom roles)
-- grant usage on schema public to anon, authenticated;
-- grant all on all tables in schema public to anon, authenticated;
-- grant all on all sequences in schema public to anon, authenticated;
-- grant execute on all functions in schema public to anon, authenticated;
