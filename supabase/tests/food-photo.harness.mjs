// Checks the food-photo function's logic with Supabase and Gemini replaced by fakes.
// Run from the repository root (needs Node 18+, no Deno):
//   npx -y -p typescript@5.6.3 tsc --strict --target es2022 --module es2022 --lib es2022,dom \
//     --skipLibCheck --outDir supabase/tests/out supabase/tests/deno.d.ts supabase/functions/food-photo/index.ts
//   node supabase/tests/food-photo.harness.mjs
// It does not call the real services; test those with a real photo after deploying (docs/AI_SETUP.md).
let handler;
const env = { GEMINI_API_KEY: "test-key", SUPABASE_URL: "https://proj.supabase.co" };
globalThis.Deno = { env: { get: (k) => env[k] }, serve: (h) => { handler = h; } };
let quotaReply, geminiReply, calls = [];
globalThis.fetch = async (url, init) => {
  calls.push({ url, init });
  if (url.includes("/rest/v1/rpc/consume_food_photo_quota")) return quotaReply();
  if (url.includes("generativelanguage.googleapis.com")) return geminiReply();
  throw new Error("unexpected " + url);
};
await import("./out/index.js");
const req = (body, headers = { Authorization: "Bearer tok", apikey: "pk" }) =>
  new Request("https://x/functions/v1/food-photo", { method: "POST", headers, body: JSON.stringify(body) });
const ok = { image_base64: "AAAA", mime_type: "image/jpeg", language: "id" };
const results = [];
async function check(name, fn) { try { await fn(); results.push("PASS " + name); } catch (e) { results.push("FAIL " + name + ": " + e.message); } }
const eq = (a, b) => { if (JSON.stringify(a) !== JSON.stringify(b)) throw new Error(JSON.stringify(a) + " != " + JSON.stringify(b)); };

await check("rejects missing auth", async () => { eq((await handler(req(ok, {}))).status, 401); });
await check("rejects bad mime", async () => { eq((await handler(req({ ...ok, mime_type: "text/html" }))).status, 400); });
await check("invalid token -> 401", async () => { quotaReply = () => new Response("{}", { status: 401 }); eq((await handler(req(ok))).status, 401); });
await check("over limit -> 429, model not called", async () => {
  calls = []; quotaReply = () => new Response("false", { status: 200 });
  eq((await handler(req(ok))).status, 429); eq(calls.some(c => c.url.includes("generativelanguage")), false);
});
await check("forwards caller token to quota", async () => {
  calls = []; quotaReply = () => new Response("true", { status: 200 });
  geminiReply = () => new Response(JSON.stringify({ candidates: [{ content: { parts: [{ text: JSON.stringify({ is_food: true, items: [], note: "" }) }] } }] }), { status: 200 });
  await handler(req(ok));
  const q = calls.find(c => c.url.includes("rpc")); eq(q.init.headers.Authorization, "Bearer tok"); eq(q.init.headers.apikey, "pk");
  const g = calls.find(c => c.url.includes("generativelanguage")); eq(g.init.headers["x-goog-api-key"], "test-key");
  eq(g.url.includes("gemini-flash-latest"), true);
});
await check("sanitizes model answer", async () => {
  quotaReply = () => new Response("true", { status: 200 });
  const answer = { is_food: true, note: "Cek sambal", items: [
    { name_id: "Nasi putih", name_en: "White rice", grams: 150, kcal: 195, protein_g: 4, carbs_g: 42, fat_g: 0.4, confidence: "high" },
    { name_id: "Ayam goreng", name_en: "", grams: 99999, kcal: -5, confidence: "weird" },
    { name_id: "", name_en: "Nothing", grams: 10, kcal: 10, confidence: "low" },
    { name_id: "Bad", name_en: "Bad", grams: "x", kcal: 10, confidence: "low" } ] };
  geminiReply = () => new Response(JSON.stringify({ candidates: [{ content: { parts: [{ text: JSON.stringify(answer) }] } }] }), { status: 200 });
  const res = await handler(req(ok)); eq(res.status, 200);
  const body = await res.json();
  eq(body.items.length, 2); eq(body.items[1], { name_id: "Ayam goreng", name_en: "Ayam goreng", grams: 3000, kcal: 0, protein_g: null, carbs_g: null, fat_g: null, confidence: "low" });
  eq(body.is_food, true); eq(body.note, "Cek sambal");
});
await check("no food -> is_food false", async () => {
  geminiReply = () => new Response(JSON.stringify({ candidates: [{ content: { parts: [{ text: JSON.stringify({ is_food: false, items: [] }) }] } }] }), { status: 200 });
  const body = await (await handler(req(ok))).json(); eq(body.is_food, false);
});
await check("model quota -> 429", async () => { geminiReply = () => new Response("{}", { status: 429 }); eq((await handler(req(ok))).status, 429); });
await check("model garbage -> 502", async () => {
  geminiReply = () => new Response(JSON.stringify({ candidates: [{ content: { parts: [{ text: "not json" }] } }] }), { status: 200 });
  eq((await handler(req(ok))).status, 502);
});
console.log(results.join("\n"));
if (results.some((r) => r.startsWith("FAIL"))) process.exit(1);
