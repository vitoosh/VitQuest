# Menyiapkan estimasi foto makanan (Supabase + Gemini)

Fitur **Foto makanan** mengirim foto makanan ke server kecil milikmu sendiri di Supabase. Server itu meminta Google Gemini mengenali makanan dan memperkirakan porsi serta kalorinya, lalu mengirim hasilnya kembali ke app. Kamu memeriksa dan mengubah hasilnya sebelum disimpan ke diary.

```
HP (VitQuest) ──HTTPS──▶ Supabase Edge Function "food-photo" ──HTTPS──▶ Google Gemini API
   publishable key            cek login anonim + batas harian              GEMINI_API_KEY
   + token login anonim       (tabel kuota di database Supabase)            (hanya di server)
```

Yang perlu kamu buat: **API key Gemini**, **project Supabase**, dan **dua GitHub secret**. Semuanya gratis untuk pemakaian pribadi. Perkiraan waktu: 20–30 menit.

> **Aturan penting soal kunci**
> - **API key Gemini** hanya dimasukkan ke **Secrets Supabase**. Jangan ditaruh di app, di GitHub, atau dikirim ke siapa pun (termasuk ke chat AI).
> - **Secret key / `service_role` Supabase** tidak dipakai sama sekali. Jangan disalin ke mana pun.
> - Yang masuk ke app hanya **Project URL** dan **publishable key** Supabase. Keduanya memang publik (siapa pun yang membongkar APK bisa melihatnya); akses dibatasi oleh login anonim, aturan database, dan batas harian.

Nama menu di dashboard Google dan Supabase kadang berubah. Kalau ada yang tidak persis sama, cari menu dengan nama yang mirip.

---

## Langkah 1: Buat API key Gemini

1. Buka **https://aistudio.google.com** dan login dengan akun Google.
2. Klik **Get API key** (atau menu **API keys**) → **Create API key**. Jika diminta memilih project Google Cloud, pilih "buat project baru".
3. Salin key-nya (diawali `AIza...`) dan simpan sementara di tempat aman, misalnya password manager. Key ini dipakai di Langkah 5.

Catatan:
- Tanpa menambahkan billing, key berjalan di **tingkat gratis** dengan batas permintaan per menit dan per hari (lihat halaman *Rate limits* di AI Studio). Jika batas tercapai, app menampilkan "Batas foto hari ini sudah tercapai".
- Di tingkat gratis, **Google boleh memakai isi yang dikirim untuk meningkatkan produknya**. Karena itu app meminta persetujuan sebelum foto pertama dan menyarankan hanya memotret makanan.

## Langkah 2: Buat project Supabase

1. Buka **https://supabase.com** → **Start your project** → login (bisa dengan GitHub).
2. **New project**:
   - Name: `vitquest` (bebas)
   - Database password: buat yang kuat dan simpan di password manager (app tidak memakainya)
   - Region: **Southeast Asia (Singapore)**, paling dekat ke Indonesia
   - Plan: **Free**
3. Tunggu sampai project selesai dibuat (1–2 menit).

## Langkah 3: Aktifkan login anonim

App tidak punya akun. Setiap HP login otomatis sebagai pengguna anonim, supaya batas harian bisa dihitung per HP.

1. Di project Supabase: **Authentication** → **Sign In / Providers** (atau **Providers**).
2. Cari **Allow anonymous sign-ins** dan nyalakan. Klik **Save**.

## Langkah 4: Buat tabel kuota (SQL)

1. Buka **SQL Editor** → **New query**.
2. Salin seluruh isi file [`supabase/migrations/20261021000000_food_photo_quota.sql`](../supabase/migrations/20261021000000_food_photo_quota.sql) dari repository ini, tempel, lalu klik **Run**.
3. Harus muncul "Success. No rows returned".

Ini membuat dua tabel hitungan (per pengguna per hari, dan total per hari) dan fungsi `consume_food_photo_quota`. Tabelnya terkunci (Row Level Security tanpa policy), jadi app tidak bisa membaca atau mengubahnya langsung.

## Langkah 5: Pasang Edge Function `food-photo`

### Cara A: lewat dashboard (tanpa install apa pun)

1. Buka **Edge Functions** → **Deploy a new function** → **Via Editor**.
2. Nama function harus persis: **`food-photo`**.
3. Hapus contoh kode, lalu tempel seluruh isi [`supabase/functions/food-photo/index.ts`](../supabase/functions/food-photo/index.ts). Klik **Deploy function**.
4. Buka function `food-photo` → **Details** / **Settings**, lalu **matikan "Verify JWT"** (bisa bernama "Enforce JWT verification" atau "Verify JWT with legacy secret"). Simpan.
   Function ini memeriksa login sendiri: setiap permintaan dihitung dengan token pengguna lewat `consume_food_photo_quota`, yang menolak permintaan tanpa login yang sah.
5. Buka **Edge Functions** → **Secrets** (atau **Project Settings → Edge Functions**) → **Add new secret**:
   - Name: `GEMINI_API_KEY`, Value: key dari Langkah 1. Simpan.

### Cara B: lewat Supabase CLI (opsional)

```bash
npm install -g supabase          # atau: brew install supabase/tap/supabase
supabase login
supabase link --project-ref <project-ref>   # project-ref = bagian <ref> dari https://<ref>.supabase.co
supabase db push                             # menjalankan file di supabase/migrations/
supabase functions deploy food-photo --no-verify-jwt
supabase secrets set GEMINI_API_KEY=<key-gemini>
```

Hati-hati: perintah `secrets set` bisa tersimpan di riwayat terminal. Hapus dari riwayat setelahnya, atau masukkan secret lewat dashboard.

### Secret opsional

| Secret | Default | Gunanya |
|---|---|---|
| `GEMINI_MODEL` | `gemini-flash-latest` | Model Gemini yang dipakai. Jika model default tidak tersedia untuk key-mu, isi dengan nama model Flash yang tercantum di AI Studio (misalnya `gemini-2.5-flash`). |
| `DAILY_PHOTO_LIMIT` | `30` | Foto per HP per hari (maksimal 100, dibatasi di SQL). |
| `DAILY_TOTAL_LIMIT` | `300` | Foto semua pengguna per hari (maksimal 1000). Menjaga kuota gratis Gemini tidak habis. |

## Langkah 6: Ambil URL dan publishable key

1. Buka **Project Settings** → **API Keys** (dan **Data API** / **API** untuk URL).
2. Salin:
   - **Project URL**: `https://<ref>.supabase.co`
   - **Publishable key**: diawali `sb_publishable_...`. Jika project-mu hanya menampilkan key lama, pakai key **`anon` / public**.
3. **Jangan** salin *secret key* (`sb_secret_...`) atau `service_role`.

## Langkah 7: Simpan sebagai GitHub secret

1. Buka repository di GitHub → **Settings** → **Secrets and variables** → **Actions** → **New repository secret**.
2. Tambahkan dua secret:
   - `VITQUEST_SUPABASE_URL` = Project URL
   - `VITQUEST_SUPABASE_PUBLISHABLE_KEY` = publishable key
3. Sekalian pastikan `VITQUEST_DEBUG_KEYSTORE_BASE64` sudah ada (lihat README, "Debug signing"), supaya APK baru bisa dipasang di atas versi lama tanpa menghapus data.

Kalau mem-build sendiri di komputer, nilai yang sama bisa diberikan lewat variabel lingkungan dengan nama yang sama, atau di `~/.gradle/gradle.properties`:

```properties
vitquest.supabaseUrl=https://<ref>.supabase.co
vitquest.supabasePublishableKey=sb_publishable_...
```

## Langkah 8: Build ulang dan coba

1. GitHub → **Actions** → **Android build** → **Run workflow** (pilih branch yang dipakai). Di ringkasan run harus tertulis "Food photo estimates are enabled in this build."
2. Unduh APK dari artefak run, pasang di HP.
3. Di app: **Nutrisi** → tombol **tambah** di salah satu waktu makan → **Foto makanan (estimasi AI)** → **Saya mengerti, lanjut** → ambil atau pilih foto.
4. Periksa item, berat, dan kalori, lalu **Simpan ke catatan**. Catatan muncul dengan foto kecil dan label "Estimasi AI".

---

## Jika ada masalah

| Yang terlihat di app | Kemungkinan penyebab | Yang dicek |
|---|---|---|
| "Estimasi foto makanan belum diatur di build ini" | APK dibuat tanpa kedua GitHub secret | Langkah 7, lalu build ulang |
| "Estimasi gagal" | Login anonim mati, function belum ada / namanya beda, Verify JWT masih menyala, SQL belum dijalankan, key Gemini salah, atau nama model tidak tersedia | **Edge Functions → food-photo → Logs / Invocations** untuk melihat kode status. Langkah 3, 4, 5 |
| "Batas foto hari ini sudah tercapai" | Batas per HP atau total tercapai, atau kuota gratis Gemini habis | Tunggu besok (dihitung per tanggal UTC di server), atau naikkan `DAILY_PHOTO_LIMIT` |
| "Tidak ada koneksi" | HP offline atau koneksi terputus | Coba lagi |
| Lama tidak dipakai lalu gagal semua | Project Supabase gratis **dijeda otomatis** setelah sekitar seminggu tanpa aktivitas | Dashboard Supabase → **Restore project** |

Arti respons function (terlihat di Logs/Invocations):

| Status | Body | Arti |
|---|---|---|
| 401 | `sign_in` | Token tidak sah. App otomatis login ulang sekali. Jika terus 401: login anonim mati, atau Verify JWT menyala. |
| 429 | `daily_limit` / `model_quota` | Batas harian VitQuest / kuota Gemini habis |
| 500 | `not_configured` | Secret `GEMINI_API_KEY` belum diisi |
| 502 | `quota` | Fungsi SQL kuota belum dibuat (Langkah 4) |
| 502 | `model_error` / `no_answer` / `bad_answer` | Key Gemini salah, model tidak tersedia, atau jawaban Gemini tidak terbaca |

## Privasi dan biaya

- Foto **tidak disimpan** di Supabase dan function tidak menulis log berisi foto atau hasilnya. Yang disimpan di database hanya hitungan foto per pengguna anonim per hari (tanpa nama, email, atau isi foto).
- Foto diteruskan ke Google Gemini. Di tingkat gratis, Google boleh memakainya untuk meningkatkan produknya.
- App menyimpan **salinan kecil** (maks. 320 px) di HP untuk diary. Salinan ini ikut file cadangan dan terhapus bersama catatannya.
- Token login anonim disimpan di HP terenkripsi dengan Android Keystore dan tidak ikut cadangan.
- Pengguna anonim tercatat di **Authentication → Users**. Boleh dihapus kapan saja; hitungan kuotanya ikut terhapus, dan app akan login anonim baru saat foto berikutnya.
- Fungsi `prune_food_photo_usage()` membersihkan hitungan lebih dari 7 hari. Opsional: jalankan manual di SQL Editor (`select public.prune_food_photo_usage();`), atau jadwalkan dengan extension `pg_cron`.

## Yang belum diuji

Kode server dan app sudah dites dengan tiruan (mock) Supabase dan Gemini. **Panggilan sungguhan ke Supabase dan Gemini, serta kamera dan galeri di HP, belum pernah dicoba.** Laporkan kode status dari Logs jika ada yang gagal.
