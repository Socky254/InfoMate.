import { serve } from "https://deno.land/std@0.168.0/http/server.ts"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  if (req.method === 'OPTIONS') return new Response('ok', { headers: corsHeaders })

  try {
    const { prompt } = await req.json()
    const apiKey = Deno.env.get('GEMINI_API_KEY') || Deno.env.get('AI_API_KEY')

    console.log(`Processing prompt: ${prompt?.substring(0, 50)}...`)

    // Gemini 1.5 Flash endpoint (using v1 stable or v1beta with correct ID)
    const url = `https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent?key=${apiKey}`

    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        contents: [{
          parts: [{ text: prompt }]
        }]
      }),
    })

    const data = await response.json()
    console.log("Raw Gemini Response:", JSON.stringify(data))

    // 1. Handle explicit API errors from Google
    if (data.error) {
      console.error("Gemini API Error:", data.error.message)
      return new Response(JSON.stringify({
        error: data.error.message,
        error_code: data.error.status || "GEMINI_ERROR"
      }), {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      })
    }

    // 2. Check for Safety Blocks
    const candidate = data.candidates?.[0]
    if (candidate?.finishReason === "SAFETY") {
      return new Response(JSON.stringify({
        error: "Neural safeguard triggered by Gemini safety filters.",
        error_code: "SAFETY_BLOCK"
      }), {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      })
    }

    // 3. Extraction with multiple fallbacks
    const output = candidate?.content?.parts?.[0]?.text ||
                   data.choices?.[0]?.message?.content ||
                   null

    if (!output) {
      console.error("Extraction Failed. Candidates:", JSON.stringify(data.candidates))
      return new Response(JSON.stringify({
        error: "No intelligence output detected. The neural bridge failed to extract content.",
        error_code: "PARSE_FAILURE"
      }), {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      })
    }

    return new Response(JSON.stringify({ output: output }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
    })
  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      status: 500,
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
    })
  }
})
