import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

interface RequestEnvelope {
  requestId: string;
  tenantId: string;
  userId: string;
  sessionId: string;
  timestamp: number;
  type: 'chat' | 'system' | 'stream' | 'health';
  payload: {
    prompt: string;
  };
}

// 10. CIRCUIT BREAKER
class CircuitBreaker {
  private static failures = 0;
  private static threshold = 10;
  private static isOpen = false;
  private static lastFailureTime = 0;

  static check() {
    if (this.isOpen) {
      if (Date.now() - this.lastFailureTime > 60000) { // Half-open after 1 min
        this.isOpen = false;
        return true;
      }
      return false;
    }
    return true;
  }

  static recordFailure() {
    this.failures++;
    if (this.failures >= this.threshold) {
      this.isOpen = true;
      this.lastFailureTime = Date.now();
    }
  }

  static recordSuccess() {
    this.failures = 0;
    this.isOpen = false;
  }
}

// 4. EVENT EMITTER (OBSERVABILITY CORE)
class EventBus {
  constructor(private supabase: any) {}

  async emit(event: {
    requestId: string;
    tenantId: string;
    event: string;
    latencyMs?: number;
    meta?: any;
  }) {
    const payload = {
      request_id: event.requestId,
      tenant_id: event.tenantId,
      event: event.event,
      timestamp: Date.now(),
      latency_ms: event.latencyMs || 0,
      meta: event.meta || {}
    };

    await this.supabase.from('ai_traces').insert(payload);
    await this.supabase.from('ai_request_logs').insert({
      request_id: event.requestId,
      tenant_id: event.tenantId,
      event_type: event.event,
      latency_ms: event.latencyMs || 0,
      status: event.event.includes('failed') || event.event.includes('error') ? 'error' : 'ok'
    });
  }
}

// (C) AI GATEWAY (ONLY GEMINI CALL ALLOWED)
class AIGateway {
  constructor(private apiKey: string, private events: EventBus, private model: string = "gemini-2.0-flash-lite") {}

  async generateStream(requestId: string, tenantId: string, prompt: string, onToken: (token: string) => void): Promise<{ output: string, tokens: number }> {
    if (!CircuitBreaker.check()) throw new Error("CIRCUIT_OPEN");

    await this.events.emit({ requestId, tenantId, event: "ai_call_started" });
    const start = Date.now();
    let fullOutput = "";

    try {
      // 2.1 AI GENERATION TIMEOUT (GEMINI LAYER) -> 75s (Golden Config)
      const url = `https://generativelanguage.googleapis.com/v1beta/models/${this.model}:streamGenerateContent?alt=sse&key=${this.apiKey}`;

      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 75000);

      const response = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          contents: [{ parts: [{ text: prompt }] }],
          generationConfig: {
            temperature: 0.7,
            top_p: 0.9,
            maxOutputTokens: 4096 // 4.1 GENERATION CONFIG
          }
        }),
        signal: controller.signal
      });
      clearTimeout(timeoutId);

      if (!response.body) throw new Error("NO_RESPONSE_BODY");

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let tokens = 0;

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        const chunk = decoder.decode(value);
        const lines = chunk.split('\n');
        for (const line of lines) {
          if (line.startsWith('data: ')) {
            try {
              const data = JSON.parse(line.slice(6));
              const text = data.candidates?.[0]?.content?.parts?.[0]?.text;
              if (text) {
                fullOutput += text;
                onToken(text);
              }
              if (data.usageMetadata) {
                tokens = data.usageMetadata.totalTokenCount;
              }
            } catch (e) {
              // Ignore partial JSON
            }
          }
        }
      }

      const latency = Date.now() - start;
      CircuitBreaker.recordSuccess();
      await this.events.emit({ requestId, tenantId, event: "ai_call_completed", latencyMs: latency, meta: { tokens } });
      return { output: fullOutput, tokens };
    } catch (e) {
      CircuitBreaker.recordFailure();
      throw e;
    }
  }

  async generate(requestId: string, tenantId: string, prompt: string): Promise<{ output: string, tokens: number }> {
    // Legacy support for non-streaming calls if needed, but we follow RULE 4.2
    return this.generateStream(requestId, tenantId, prompt, () => {});
  }
}

// (B) AI ORCHESTRATOR
class AIOrchestrator {
  constructor(private supabase: any, private gateway: AIGateway, private events: EventBus) {}

  async handleRequest(envelope: RequestEnvelope, onToken?: (token: string) => void): Promise<{ output: string, quota: any }> {
    const totalStart = Date.now();
    const { requestId, tenantId } = envelope;

    await this.events.emit({ requestId, tenantId, event: "request_received" });

    // 1. Governance Pipeline (Idempotency + Quota + Governor)
    const { data: gov, error: govErr } = await this.supabase.rpc('enforce_request_governance', {
      p_request_id: requestId,
      p_tenant_id: tenantId,
      p_user_id: envelope.userId,
      p_session_id: envelope.sessionId
    });

    if (govErr || !gov?.allowed) {
      const errCode = gov?.error_code || "GOVERNANCE_REJECTION";
      await this.events.emit({ requestId, tenantId, event: "request_failed", meta: { reason: errCode } });
      throw new Error(errCode);
    }

    // 2. Cache Check
    const promptHash = await this.hash(envelope.payload.prompt);
    const { data: cached } = await this.supabase
      .from('ai_cache')
      .select('response_text')
      .eq('prompt_hash', promptHash)
      .maybeSingle();

    if (cached) {
      if (onToken) onToken(cached.response_text);
      await this.updateIdempotency(requestId, 'COMPLETED');
      await this.events.emit({ requestId, tenantId, event: "request_finalized", latencyMs: Date.now() - totalStart, meta: { cache: "hit" } });
      const quota = await this.getQuota(tenantId);
      return { output: cached.response_text, quota };
    }

    // 3. AI Execution
    try {
      const result = await this.gateway.generateStream(requestId, tenantId, envelope.payload.prompt, (token) => {
        if (onToken) onToken(token);
      });

      // 4. Persistence & Finalization
      await Promise.all([
        this.supabase.from('ai_messages').insert([
          { request_id: requestId, tenant_id: tenantId, user_id: envelope.userId, session_id: envelope.sessionId,
            role: 'user', content: envelope.payload.prompt, model: 'gemini-2.0-flash-lite' },
          { request_id: requestId, tenant_id: tenantId, user_id: envelope.userId, session_id: envelope.sessionId,
            role: 'assistant', content: result.output, model: 'gemini-2.0-flash-lite', tokens_used: result.tokens }
        ]),
        this.supabase.from('ai_cache').insert({
          prompt_hash: promptHash, response_text: result.output, model_id: 'gemini-2.0-flash-lite', tenant_id: tenantId
        }),
        this.supabase.rpc('increment_tenant_tokens', { p_tenant_id: tenantId, p_tokens: result.tokens }),
        this.updateIdempotency(requestId, 'COMPLETED')
      ]);

      await this.events.emit({ requestId, tenantId, event: "request_finalized", latencyMs: Date.now() - totalStart });
      const quota = await this.getQuota(tenantId);
      return { output: result.output, quota };
    } catch (e) {
      // 5. DEAD LETTER SYSTEM
      await this.supabase.from('dead_letter_requests').insert({
        request_id: requestId,
        tenant_id: tenantId,
        prompt: envelope.payload.prompt,
        error: e.message
      });
      await this.updateIdempotency(requestId, 'FAILED');
      await this.events.emit({ requestId, tenantId, event: "request_failed", latencyMs: Date.now() - totalStart, meta: { error: e.message } });
      throw e;
    }
  }

  private async getQuota(tenantId: string) {
    const { data: usage } = await this.supabase.from('tenant_usage').select('*').eq('tenant_id', tenantId).single();
    const { data: limits } = await this.supabase.from('ai_quotas').select('daily_request_limit').eq('tenant_id', tenantId).single();
    return {
      requestsUsed: usage?.requests_used || 0,
      requestsLimit: limits?.daily_request_limit || 100,
      tokensUsed: usage?.tokens_used || 0
    };
  }

  private async hash(text: string): Promise<string> {
    const msgUint8 = new TextEncoder().encode(text);
    const hashBuffer = await crypto.subtle.digest('SHA-256', msgUint8);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
  }

  private async updateIdempotency(requestId: string, status: string) {
    await this.supabase.from('request_idempotency').update({ status }).eq('request_id', requestId);
  }
}

serve(async (req) => {
  if (req.method === 'OPTIONS') return new Response('ok', { headers: corsHeaders })

  const supabase = createClient(Deno.env.get('SUPABASE_URL')!, Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!);

  // 1. WEBSOCKET UPGRADE (RULE 2.2)
  if (req.headers.get("upgrade") === "websocket") {
    const { socket, response } = Deno.upgradeWebSocket(req);

    socket.onmessage = async (e) => {
      const data = JSON.parse(e.data);
      if (data.event === "start_stream") {
        const { requestId, prompt, userId } = data;
        const events = new EventBus(supabase);
        const gateway = new AIGateway(Deno.env.get('GEMINI_API_KEY')!, events);
        const orchestrator = new AIOrchestrator(supabase, gateway, events);

        // 3.3 STREAM HEARTBEAT
        const heartbeat = setInterval(() => {
          if (socket.readyState === 1) socket.send(JSON.stringify({ event: "stream_ping", requestId }));
        }, 15000);

        try {
          await orchestrator.handleRequest({
            requestId, userId, tenantId: userId, sessionId: "ws-session", timestamp: Date.now(), type: 'stream',
            payload: { prompt }
          }, (token) => {
            if (socket.readyState === 1) socket.send(JSON.stringify({ event: "token", text: token, requestId }));
          });
          if (socket.readyState === 1) socket.send(JSON.stringify({ event: "stream_end", success: true, requestId }));
        } catch (err) {
          if (socket.readyState === 1) socket.send(JSON.stringify({ event: "stream_end", success: false, error: err.message, requestId }));
        } finally {
          clearInterval(heartbeat);
        }
      }
    };
    return response;
  }

  // 2. HTTP STREAMING (RULE 5.2)
  try {
    const authHeader = req.headers.get('Authorization');
    if (!authHeader) return new Response(JSON.stringify({ error: "Unauthorized" }), { status: 401, headers: corsHeaders });

    const { data: { user }, error: authError } = await supabase.auth.getUser(authHeader.replace('Bearer ', ''));
    if (authError || !user) return new Response(JSON.stringify({ error: "Invalid Session" }), { status: 401, headers: corsHeaders });

    const body = await req.json();
    const requestId = body.requestId;

    const encoder = new TextEncoder();
    const stream = new ReadableStream({
      async start(controller) {
        const send = (msg: any) => controller.enqueue(encoder.encode(JSON.stringify(msg) + "\n"));

        try {
          const events = new EventBus(supabase);
          const gateway = new AIGateway(Deno.env.get('GEMINI_API_KEY')!, events);
          const orchestrator = new AIOrchestrator(supabase, gateway, events);

          await orchestrator.handleRequest({
            ...body, tenantId: user.id, userId: user.id
          }, (token) => {
            send({ event: "token", text: token, requestId });
          });

          send({ event: "stream_end", success: true, requestId });
          controller.close();
        } catch (error: any) {
          send({ event: "stream_end", success: false, error: error.message, requestId });
          controller.close();
        }
      }
    });

    return new Response(stream, { headers: { ...corsHeaders, 'Content-Type': 'application/x-ndjson' } });

  } catch (error: any) {
    return new Response(JSON.stringify({ event: "stream_end", success: false, error: error.message }), { status: 500, headers: corsHeaders });
  }
})
