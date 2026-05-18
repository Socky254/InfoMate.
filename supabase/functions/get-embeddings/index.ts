import { serve } from "https://deno.land/std@0.168.0/http/server.ts"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  if (req.method === 'OPTIONS') return new Response('ok', { headers: corsHeaders })

  try {
    const { input } = await req.json()
    const apiKey = Deno.env.get('AI_API_KEY')

    // Google Gemini Embedding API endpoint
    const url = `https://generativelanguage.googleapis.com/v1beta/models/embedding-001:embedContent?key=${apiKey}`

    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        model: "models/embedding-001",
        content: {
          parts: [{ text: input }]
        }
      }),
    })

    const data = await response.json()
    const embedding = data.embedding?.values || []

    return new Response(JSON.stringify({ embedding: embedding }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
    })
  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      status: 500,
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
    })
  }
})
