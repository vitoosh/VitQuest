// VitQuest food photo estimate.
//
// The app sends a JPEG of a meal. This function checks the caller's daily limit with
// the caller's own Supabase token, asks Google Gemini to name the foods and estimate
// the portion and energy of each, and returns a small JSON answer. The photo is not
// stored and nothing about it is logged.
//
// Secrets (Supabase dashboard > Edge Functions > Secrets):
//   GEMINI_API_KEY      required
//   GEMINI_MODEL        optional, default "gemini-flash-latest"
//   DAILY_PHOTO_LIMIT   optional, photos per user per day, default 30 (capped at 100 in SQL)
//   DAILY_TOTAL_LIMIT   optional, photos for all users per day, default 300 (capped at 1000)
// SUPABASE_URL is provided by Supabase.

const GEMINI_API_KEY = Deno.env.get("GEMINI_API_KEY") ?? "";
const GEMINI_MODEL = Deno.env.get("GEMINI_MODEL") ?? "gemini-flash-latest";
const DAILY_PHOTO_LIMIT = Number(Deno.env.get("DAILY_PHOTO_LIMIT") ?? "30");
const DAILY_TOTAL_LIMIT = Number(Deno.env.get("DAILY_TOTAL_LIMIT") ?? "300");
const SUPABASE_URL = Deno.env.get("SUPABASE_URL") ?? "";

// About 2.2 MB of JPEG; the app sends about 200 KB.
const MAX_IMAGE_BASE64 = 3_000_000;
const ALLOWED_MIME = new Set(["image/jpeg", "image/png", "image/webp"]);

const PROMPT = `You estimate food for a calorie diary used in Indonesia.
Look at the photo and list every distinct food and drink that is clearly visible,
including rice, side dishes, sambal and sauces, fried items, and drinks (consider sugar,
milk and syrup). Use common Indonesian dish names where they apply (for example
"Nasi putih", "Ayam goreng", "Tempe goreng", "Es teh manis").
For each item give:
- name_id: the name in Indonesian; name_en: the name in English
- grams: the estimated edible weight of the visible portion (drinks in ml as grams)
- kcal: energy for that portion; protein_g, carbs_g, fat_g for that portion
- confidence: "high", "medium" or "low"
Account for cooking oil and coconut milk where the dish usually has them.
Do not invent items that are not visible. If the photo shows no food or drink,
set is_food to false and return no items. "note" may hold one short hint for the user,
in the requested language, or be empty.`;

const RESPONSE_SCHEMA = {
  type: "OBJECT",
  properties: {
    is_food: { type: "BOOLEAN" },
    items: {
      type: "ARRAY",
      items: {
        type: "OBJECT",
        properties: {
          name_id: { type: "STRING" },
          name_en: { type: "STRING" },
          grams: { type: "NUMBER" },
          kcal: { type: "NUMBER" },
          protein_g: { type: "NUMBER" },
          carbs_g: { type: "NUMBER" },
          fat_g: { type: "NUMBER" },
          confidence: { type: "STRING", enum: ["high", "medium", "low"] },
        },
        required: ["name_id", "name_en", "grams", "kcal", "confidence"],
      },
    },
    note: { type: "STRING" },
  },
  required: ["is_food", "items"],
};

function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}

function clamp(value: unknown, min: number, max: number): number | null {
  const n = typeof value === "number" ? value : Number(value);
  return Number.isFinite(n) ? Math.min(max, Math.max(min, n)) : null;
}

function text(value: unknown, max: number): string {
  return typeof value === "string" ? value.trim().slice(0, max) : "";
}

/** Counts this photo for the caller. Returns "ok", "limit" or "unauthorized". */
async function consumeQuota(authorization: string, apikey: string): Promise<"ok" | "limit" | "unauthorized" | "error"> {
  const response = await fetch(`${SUPABASE_URL}/rest/v1/rpc/consume_food_photo_quota`, {
    method: "POST",
    headers: { Authorization: authorization, apikey, "Content-Type": "application/json" },
    body: JSON.stringify({ per_user_limit: DAILY_PHOTO_LIMIT, total_limit: DAILY_TOTAL_LIMIT }),
  });
  if (response.status === 401 || response.status === 403) return "unauthorized";
  if (!response.ok) return "error";
  return (await response.json()) === true ? "ok" : "limit";
}

async function askGemini(imageBase64: string, mimeType: string, language: string): Promise<Response> {
  const url = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(GEMINI_MODEL)}:generateContent`;
  const reply = await fetch(url, {
    method: "POST",
    headers: { "x-goog-api-key": GEMINI_API_KEY, "Content-Type": "application/json" },
    signal: AbortSignal.timeout(60_000),
    body: JSON.stringify({
      contents: [{
        role: "user",
        parts: [
          { text: `${PROMPT}\nWrite the note in ${language === "en" ? "English" : "Indonesian"}.` },
          { inline_data: { mime_type: mimeType, data: imageBase64 } },
        ],
      }],
      generationConfig: { responseMimeType: "application/json", responseSchema: RESPONSE_SCHEMA, temperature: 0.2 },
    }),
  });
  if (reply.status === 429) return json(429, { error: "model_quota" });
  if (!reply.ok) return json(502, { error: "model_error" });

  const data = await reply.json();
  const raw = data?.candidates?.[0]?.content?.parts?.[0]?.text;
  if (typeof raw !== "string") return json(502, { error: "no_answer" });
  let parsed: Record<string, unknown>;
  try {
    parsed = JSON.parse(raw);
  } catch {
    return json(502, { error: "bad_answer" });
  }

  const items = (Array.isArray(parsed.items) ? parsed.items : []).slice(0, 15).flatMap((item: Record<string, unknown>) => {
    const grams = clamp(item.grams, 1, 3000);
    const kcal = clamp(item.kcal, 0, 5000);
    const nameId = text(item.name_id, 80);
    if (!nameId || grams === null || kcal === null) return [];
    return [{
      name_id: nameId,
      name_en: text(item.name_en, 80) || nameId,
      grams,
      kcal,
      protein_g: clamp(item.protein_g, 0, 3000),
      carbs_g: clamp(item.carbs_g, 0, 3000),
      fat_g: clamp(item.fat_g, 0, 3000),
      confidence: ["high", "medium", "low"].includes(item.confidence as string) ? item.confidence : "low",
    }];
  });
  return json(200, { is_food: parsed.is_food === true && items.length > 0, items, note: text(parsed.note, 300) || null });
}

Deno.serve(async (request: Request) => {
  if (request.method !== "POST") return json(405, { error: "method" });
  if (!GEMINI_API_KEY || !SUPABASE_URL) return json(500, { error: "not_configured" });

  const authorization = request.headers.get("Authorization") ?? "";
  const apikey = request.headers.get("apikey") ?? "";
  if (!authorization.startsWith("Bearer ") || !apikey) return json(401, { error: "sign_in" });

  let body: Record<string, unknown>;
  try {
    body = await request.json();
  } catch {
    return json(400, { error: "body" });
  }
  const imageBase64 = typeof body.image_base64 === "string" ? body.image_base64 : "";
  const mimeType = typeof body.mime_type === "string" ? body.mime_type : "";
  const language = body.language === "en" ? "en" : "id";
  if (!imageBase64 || imageBase64.length > MAX_IMAGE_BASE64 || !ALLOWED_MIME.has(mimeType)) {
    return json(400, { error: "image" });
  }

  // Counted before calling the model, so failed or abusive calls still use up the limit.
  const quota = await consumeQuota(authorization, apikey);
  if (quota === "unauthorized") return json(401, { error: "sign_in" });
  if (quota === "limit") return json(429, { error: "daily_limit" });
  if (quota === "error") return json(502, { error: "quota" });

  try {
    return await askGemini(imageBase64, mimeType, language);
  } catch {
    return json(502, { error: "model_unreachable" });
  }
});
