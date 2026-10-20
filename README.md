# VitQuest

VitQuest adalah aplikasi Android native untuk penurunan berat badan yang berkelanjutan, aktivitas harian, kalistenik, pencatatan kalori, hidrasi, dan kebiasaan sehat, dengan motivasi bergaya game (XP dan hadiah).

Aplikasi ini **offline-first**: semua fitur inti berjalan tanpa server dan tanpa akun.

> VitQuest bukan alat medis. Semua target dan angka kalori adalah estimasi, bukan saran medis.

---

## Status pengembangan

Proyek dibangun bertahap. **Saat ini: Fase 5 (XP, hadiah nyata, anggaran, pengingat) selesai.** Reward Credit (poin virtual untuk "menukar" hadiah) sudah dihapus di database v6 karena tidak bisa dipakai di dunia nyata.

### Yang sudah berfungsi di Fase 5 (Hadiah dan pengingat)

| Fitur | Status |
|---|---|
| **XP dan level**: XP didapat dari latihan (min. 5 menit, maks. 2/hari), target langkah, target air, mencatat makanan, timbang mingguan, centang kebiasaan, dan pencapaian. XP per level naik bertahap (100, 150, 200, ...). Aturan perolehan ditampilkan apa adanya di app. | ✅ Berfungsi, ada test-nya |
| XP dan level **bukan mata uang**. Hadiah adalah hal nyata yang kamu pilih, beli, atau lakukan sendiri. Tidak ada XP untuk makan lebih sedikit atau turun berat badan cepat. | ✅ Berfungsi |
| **Ledger XP yang bisa diaudit**: setiap perubahan XP adalah baris baru (tidak pernah diubah atau dihapus). Kunci unik per transaksi membuat sinkronisasi **idempoten**; aktivitas yang dihapus dikoreksi dengan baris pembalik. Perolehan dihitung dari 30 hari terakhir. | ✅ Berfungsi, ada test-nya |
| **Katalog hadiah**: 9 saran awal (barang dan pengalaman, tanpa hadiah makanan), hadiah buatan sendiri dengan **harga asli (rupiah)**, edit/hapus, **wishlist**, dan progres tabungan menuju tiap hadiah | ✅ Berfungsi |
| **"Sudah didapat"**: tandai hadiah yang benar-benar kamu dapatkan, opsional dengan jumlah yang dibayar (otomatis dicatat sebagai pengeluaran di Anggaran). Catatan dan pengeluarannya disimpan dalam satu transaksi database; ID permintaan unik membuat ketukan ganda atau retry tidak mencatat dua kali. Catatan yang salah bisa dihapus (beserta pengeluarannya). | ✅ Berfungsi, ada test-nya |
| **Riwayat** hadiah yang sudah didapat dan semua baris XP, termasuk baris koreksi | ✅ Berfungsi |
| **Anggaran uang sungguhan (rupiah)**: anggaran bulanan, catatan tabungan/pengeluaran, dan progres tabungan menuju hadiah di wishlist yang punya harga asli. Tidak ada pembayaran, transfer, atau pemesanan. | ✅ Berfungsi |
| **Pengingat** untuk air, jeda bergerak, latihan, catat makanan, dan timbang mingguan, masing-masing bisa dinyalakan/dimatikan. **Jam aktif** dan **jam tenang** diatur sendiri (tidak ada jam bawaan). Air default tiap 30 menit, gerak tiap 60 menit; latihan/makanan/berat memakai jam dan hari pilihanmu. | ✅ Berfungsi, ada test-nya |
| Pengingat **dilewati jika target hari itu sudah tercapai** dan otomatis mulai lagi besok. Satu jadwal unik per jenis (tidak ada duplikat), dibatalkan saat dimatikan atau saat semua data dihapus. | ✅ Berfungsi, ada test-nya |
| Izin notifikasi Android 13+ diminta dari layar Pengingat; jika ditolak, app menjelaskan dan membuka pengaturan notifikasi. Ada penjelasan dan tombol ke pengaturan baterai. | ✅ Berfungsi |
| Home: kartu level dan total XP. Tab Hadiah tidak lagi "Segera hadir". | ✅ Berfungsi |
| Migrasi database v4→v5 dan v5→v6 (credit dihapus; hadiah, penukaran yang tidak dibatalkan, dan anggaran tetap ada) | ✅ Ada test migrasinya |

### Yang sudah berfungsi di Fase 4 (Latihan dan kebiasaan)

| Fitur | Status |
|---|---|
| **Pustaka 38 gerakan** (pemula, menengah, mahir) untuk rumah, kantor, dan luar ruangan: squat, push-up dinding/lutut/standar, lunge, plank, glute bridge, calf raise, jalan di tempat, step jack, mobilitas, peregangan, plus gerakan dengan resistance band, dumbel, palang, tali skipping, atau kursi. Tiap gerakan punya deskripsi, langkah, otot yang dilatih, level, alat, rekomendasi repetisi/durasi, versi pemula, dan catatan keamanan (ID/EN). | ✅ Berfungsi, ada test-nya |
| **Tutorial animasi figur vektor** untuk setiap gerakan kecuali jalan cepat: figur digambar app (offline), bergerak dari posisi awal ke akhir, otot yang bekerja diberi warna, dan ada tombol Putar / Awal / Akhir. Jika animasi sistem dimatikan, posisi awal dan akhir ditampilkan sebagai gambar diam. | ✅ Berfungsi, ada test pose |
| **Generator latihan** berdasarkan durasi (5–30 menit), level, tempat, alat yang tersedia, dan fokus (seluruh tubuh, tubuh atas/bawah, otot inti, kardio, mobilitas): pemanasan, sirkuit inti beberapa putaran, dan pendinginan. Di kantor tidak ada latihan lantai atau lompatan. Tombol "Acak ulang" memberi variasi gerakan. | ✅ Berfungsi, ada test-nya |
| **Pemutar latihan**: hitung mundur persiapan, timer kerja/istirahat, jeda/lanjut, lewati, kembali/ulangi, "Set selesai" untuk gerakan berbasis repetisi, +15 dtk istirahat, aba-aba "Ganti sisi", getar saat pergantian, layar tetap menyala, dan ringkasan sesi. Status pemutar tetap tersimpan walaupun aplikasi ditutup sistem. | ✅ Berfungsi, ada test-nya |
| **Riwayat latihan**: ringkasan minggu ini, detail per gerakan (selesai/dilewati/belum dilakukan), hapus sesi, dan **catat latihan manual** (durasi, intensitas, catatan) | ✅ Berfungsi |
| Perkiraan kalori latihan dari nilai MET × berat badan × waktu aktif, selalu berlabel **perkiraan** dan hanya tampil jika berat badan diketahui | ✅ Berfungsi |
| **Kebiasaan** mingguan tanpa streak: gerak harian (target langkah), latihan, cukup minum, dan catat makanan dihitung otomatis dari data; kebiasaan pilihan sendiri bisa ditambah, diubah, dihapus, dan dicentang/dibatalkan untuk hari yang sudah lewat | ✅ Berfungsi |
| **Progres**: minggu ini vs minggu lalu, grafik menit latihan dan rata-rata langkah 8 minggu, tren berat 90 hari, dan **riwayat pencapaian** berbasis konsistensi (tidak ada pencapaian untuk makan lebih sedikit atau turun berat cepat) | ✅ Berfungsi |
| Home: kartu latihan hari ini, kartu kebiasaan, aksi cepat "Mulai latihan" | ✅ Berfungsi |
| Migrasi database v3→v4 | ✅ Ada test migrasinya |

### Yang sudah berfungsi di Fase 3 (Nutrisi, berat badan, hidrasi)

| Fitur | Status |
|---|---|
| **Penghitung kalori**: target kalori harian, kalori yang sudah dimakan, sisa kalori, serta protein, karbohidrat, dan lemak | ✅ Berfungsi |
| **BMR dan TDEE** (rumus Mifflin–St Jeor × faktor aktivitas) dengan penjelasan di app. Rekomendasi asupan: defisit maksimal 500 kkal (≤20% TDEE), dengan batas bawah 1.200 kkal (perempuan) / 1.500 kkal (laki-laki). Target juga bisa diatur manual. | ✅ Berfungsi, ada test-nya |
| Field opsional **jenis kelamin** di profil, hanya untuk rumus BMR. Kalau data belum lengkap, app memberi tahu apa yang kurang, tanpa mengarang angka. | ✅ Berfungsi |
| Perkiraan energi jalan kaki dari langkah. Hanya ditampilkan sebagai info, **tidak** ditambahkan ke target. | ✅ Berfungsi |
| **Catatan makan** per hari: Sarapan, Makan siang, Makan malam, Camilan. Navigasi antar hari, ubah jumlah, dan hapus. | ✅ Berfungsi |
| **Database makanan Indonesia** offline: 98 makanan (nasi, tempe, tahu, telur, ayam, ikan, sayur sop, gado-gado, pecel, buah, minuman, gorengan, dan lain-lain), masing-masing dengan porsi umum dan label sumber | ✅ Berfungsi |
| Cari makanan (ID/EN), favorit, terakhir dipakai, dan **buat makanan sendiri** (nilai per porsi atau per 100 g) | ✅ Berfungsi |
| **12 resep sehat** offline (tumis tempe brokoli, pepes nila, sop ayam sayuran, gado-gado rumahan, omelet sayur, oatmeal pisang, dan lain-lain): bahan, pengganti bahan, langkah, waktu, gizi per porsi, dan penyesuaian jumlah porsi | ✅ Berfungsi |
| Catat resep ke catatan makan, **rencana menu 7 hari**, dan **daftar belanja** otomatis dari rencana (item buatan sendiri tetap disimpan) | ✅ Berfungsi |
| **Berat badan**: satu catatan per hari dengan catatan opsional, grafik tren (30/90 hari/semua), rata-rata mingguan, perubahan 30 hari, laju per minggu, progres ke target, dan peringatan jika turun lebih dari ±1% per minggu | ✅ Berfungsi |
| **Ukuran tubuh** opsional: pinggang, pinggul, dada, lengan, dan paha | ✅ Berfungsi |
| **Hidrasi**: tambah cepat (ukuran gelas, 500 ml, atau jumlah lain), progres harian, riwayat 7 hari. Total dihitung per tanggal, jadi otomatis "reset" setiap hari. | ✅ Berfungsi |
| Home: langkah, kalori, air, dan berat hari ini, plus aksi cepat (catat makanan, +air, catat berat) | ✅ Berfungsi |
| Migrasi database v2→v3. Berat badan di profil lama otomatis menjadi entri pertama riwayat berat. | ✅ Ada test migrasinya |

### Yang sudah berfungsi di Fase 2 (Aktivitas)

| Fitur | Status |
|---|---|
| Tab **Aktivitas**: langkah hari ini, progres terhadap target, grafik 7 hari, ringkasan mingguan, riwayat 30 hari | ✅ Berfungsi |
| **Health Connect** (hanya baca total langkah harian). Ada penjelasan sebelum meminta izin, deteksi "tidak didukung / perlu dipasang / belum diizinkan / izin dicabut / gagal baca", serta halaman kebijakan privasi untuk layar izin Health Connect. | ✅ Berfungsi |
| **Sensor langkah HP** (`TYPE_STEP_COUNTER`) sebagai cadangan, dengan izin Aktivitas fisik. Menangani nilai kumulatif, restart proses, reboot HP, pergantian hari, dan zona waktu. | ✅ Berfungsi |
| **Input manual** per hari, yang menggantikan data otomatis untuk hari itu dan bisa dihapus lagi | ✅ Berfungsi |
| **Pilihan sumber**: Otomatis, Hanya Health Connect, Hanya sensor HP, atau Hanya manual. Sumber yang aktif selalu diberi label. | ✅ Berfungsi |
| **Anti double counting**: untuk setiap hari dipilih satu sumber (manual > Health Connect > sensor). Angka dari sumber berbeda tidak pernah dijumlahkan. | ✅ Berfungsi, ada test-nya |
| "Tidak ada data" dibedakan dari "0 langkah" di Home, grafik, dan riwayat | ✅ Berfungsi |
| Target langkah bisa diubah langsung dari tab Aktivitas | ✅ Berfungsi |
| Home menampilkan langkah hari ini beserta sumbernya | ✅ Berfungsi |
| Migrasi database v1→v2 tanpa kehilangan data profil | ✅ Ada test migrasinya |

### Yang sudah berfungsi sejak Fase 1

| Fitur | Status |
|---|---|
| Onboarding 4 langkah (data tubuh, pengalaman/aktivitas, preferensi latihan/makanan, target harian). Semua pertanyaan opsional dan bisa dilewati. | ✅ Berfungsi, tersimpan di Room |
| Saran target awal: langkah (dari tingkat aktivitas) dan minum (±30 ml/kg berat badan). Bisa diubah. | ✅ Berfungsi |
| Edit profil dan target kapan saja | ✅ Berfungsi |
| Navigasi 5 tab (Beranda, Aktivitas, Nutrisi, Latihan, Hadiah), dengan rail di layar lebar | ✅ Berfungsi |
| Bahasa Indonesia dan Inggris (ikut sistem, atau dipilih manual di Pengaturan) | ✅ Berfungsi |
| Tema Ikuti sistem, Terang, dan Gelap (tersimpan) | ✅ Berfungsi |
| Hapus semua data (dengan konfirmasi) | ✅ Berfungsi |
| GitHub Actions: unit test, lint, build debug APK, upload APK dan laporan | ✅ Dikonfigurasi |

### Yang masih placeholder

- Tidak ada lagi layar "Segera hadir". Fase 6 (sinkronisasi cloud opsional dan kesiapan rilis) hanya dikerjakan setelah ada persetujuan.

### Rencana fase berikutnya

1. ~~Fase 1: Fondasi dan build otomatis~~
2. ~~Fase 2: Pelacakan aktivitas (Health Connect, sensor langkah, input manual, riwayat)~~
3. ~~Fase 3: Berat badan, nutrisi (database makanan Indonesia), resep, dan hidrasi~~
4. ~~Fase 4: Latihan kalistenik, tutorial animasi gerakan, kebiasaan, dan progres~~
5. ~~Fase 5: XP, hadiah nyata, anggaran, dan pengingat~~
6. Fase 6 (opsional): Sinkronisasi cloud dan kesiapan rilis

Integrasi smartwatch sengaja ditunda sampai ada riset kompatibilitas.

---

## Download dan install APK di HP (tanpa Android Studio)

1. Buka repositori di GitHub, lalu buka tab **Actions**.
2. Pilih workflow **Android build** dan klik salah satu run yang berstatus hijau ✅.
3. Gulir ke bagian **Artifacts** di bawah, lalu unduh **`vitquest-debug-apk-<nomor>`**. File yang terunduh berupa `.zip`.
4. Ekstrak zip tersebut. Isinya `app-debug.apk`.
5. Pindahkan `app-debug.apk` ke HP (POCO X8 Pro), misalnya lewat kabel USB, Google Drive, atau Telegram "Saved Messages".
6. Buka file APK di HP. Kalau Android meminta, izinkan **Instal aplikasi tidak dikenal** untuk aplikasi yang kamu pakai membuka file (File Manager, Chrome, dll.).
7. Ketuk **Instal**. Google Play Protect mungkin menampilkan peringatan karena aplikasinya tidak berasal dari Play Store; pilih untuk tetap menginstal kalau kamu yakin file itu dari repositori ini.

> Kalau kamu membuka artifact dari HP, GitHub mengharuskan kamu login. Artifact disimpan selama 30 hari.

### Update ke versi baru tanpa kehilangan data

Android hanya mengizinkan update kalau APK baru ditandatangani dengan **kunci yang sama** dengan versi yang terpasang. Tanpa kunci bersama, setiap run CI memakai kunci debug acak. Akibatnya kamu harus uninstall dulu, dan **semua data di aplikasi ikut terhapus**.

Karena itu, workflow memakai debug keystore bersama dari GitHub Secret (lihat bagian berikut). Selama secret itu tidak berubah, APK baru bisa langsung di-install menimpa versi lama, dan datamu tetap aman.

---

## Debug signing (GitHub Secret)

Workflow membaca secret **`VITQUEST_DEBUG_KEYSTORE_BASE64`**. Isinya keystore debug (PKCS12) yang di-encode base64, dengan kredensial standar Android debug:

- alias: `androiddebugkey`
- store password: `android`
- key password: `android`

Ini **hanya untuk build debug/testing**, bukan untuk rilis. Keystore **tidak pernah di-commit** ke repositori (`*.keystore` dan `*.jks` ada di `.gitignore`).

### Cara menambahkan secret

1. Buka repositori di GitHub, lalu **Settings → Secrets and variables → Actions → New repository secret**.
2. Name: `VITQUEST_DEBUG_KEYSTORE_BASE64`
3. Secret: tempel seluruh isi base64 dari keystore (satu baris panjang).
4. Klik **Add secret**, lalu jalankan ulang workflow (**Actions → Android build → Run workflow**).

Kalau secret belum ada, build tetap berhasil, tapi workflow menampilkan peringatan bahwa APK memakai kunci sementara.

### Membuat keystore sendiri (opsional, butuh JDK)

```bash
keytool -genkeypair -keystore vitquest-debug.keystore -storetype PKCS12 \
  -alias androiddebugkey -storepass android -keypass android \
  -keyalg RSA -keysize 2048 -validity 10950 -dname "CN=Android Debug,O=Android,C=US"
base64 -w0 vitquest-debug.keystore > keystore-base64.txt   # macOS: base64 -i vitquest-debug.keystore
```

> Mengganti keystore berarti update berikutnya harus didahului uninstall, dan data aplikasi akan hilang.

---

## Detail teknis

| Item | Nilai |
|---|---|
| Application ID / package | `io.github.vitoosh.vitquest` (berbasis namespace GitHub, bukan domain terdaftar) |
| minSdk / targetSdk / compileSdk | 26 (Android 8.0) / 36 / 37 |
| Bahasa pemrograman | Kotlin (built-in Kotlin di AGP 9) |
| UI | Jetpack Compose + Material 3, Navigation Compose (type-safe routes), Material 3 adaptive navigation suite |
| Arsitektur | MVVM + Repository, Coroutines/Flow, Hilt (KSP) |
| Data | Room (profil, target, langkah, makanan, resep, berat, air, latihan, kebiasaan, ledger XP, hadiah, riwayat hadiah, anggaran, pengaturan pengingat), DataStore Preferences (tema, status onboarding, pilihan sumber langkah, versi data awal, anggaran bulanan) |
| Latar belakang | WorkManager 2.12 (pengingat; satu pekerjaan unik per jenis), notifikasi Android dengan satu channel per jenis pengingat |
| Data kesehatan | Health Connect client 1.1.0 (hanya baca `StepsRecord`), Android `SensorManager` (`TYPE_STEP_COUNTER`) |
| Bahasa UI | Per-app language API Android (AppCompat). Pilihan bahasa disimpan oleh sistem/AppCompat, bukan duplikat di DataStore. |
| Build | Gradle 9.6.1 (wrapper, checksum terverifikasi), AGP 9.4.1, Kotlin 2.4.20, JDK 17+ (CI memakai 21) |
| Test | JUnit 4, kotlinx-coroutines-test, Robolectric, Compose UI test, Room in-memory |

Versi lengkap ada di [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

### Struktur proyek

```
app/src/main/java/io/github/vitoosh/vitquest/
├── MainActivity.kt, VitQuestApplication.kt
├── core/
│   ├── model/          # Model domain (UserProfile, Goals, ThemeMode, ...)
│   ├── domain/         # Logika bisnis murni: target, kalori, langkah, berat, latihan (generator, pemutar, pose figur), kebiasaan
│   ├── data/           # Room (local/), DataStore (preferences/), repository/, Hilt di/
│   ├── designsystem/   # Tema, warna, spacing, komponen, ikon
│   ├── locale/         # Pilihan bahasa per-aplikasi
│   └── ui/             # Label lokal untuk enum dan format angka
├── feature/            # home, activity, nutrition, weight, hydration, workouts, habits, progress, onboarding, profile, settings
└── ui/                 # Root app dan navigasi
app/src/main/res/       # values/ (Inggris, default), values-in/ (Indonesia), values-night/
app/schemas/            # Skema Room yang diekspor (wajib di-commit untuk migrasi)
app/src/test/           # Unit test + Robolectric (DB, UI smoke test)
app/src/androidTest/    # Instrumented test (dijalankan di HP/emulator)
.github/workflows/android-build.yml
```

### Database dan migrasi

- Versi 1: tabel `user_profile` dan `goals` (masing-masing satu baris).
- Versi 2 (Fase 2): `daily_steps` (satu baris per tanggal dan sumber) dan `step_sensor_state` (checkpoint penghitung langkah HP).
- Versi 3 (Fase 3): kolom `user_profile.sex` dan `goals.daily_calorie_target`, plus tabel `foods`, `food_servings`, `food_log_entries`, `recipes`, `recipe_ingredients`, `meal_plan_entries`, `shopping_items`, `weight_entries`, `body_measurements`, dan `water_logs`.
- Versi 4 (Fase 4): tabel `workout_sessions`, `workout_session_exercises` (salinan nama gerakan dan status tiap langkah), `habits`, dan `habit_check_ins`.
- Versi 5 (Fase 5): `reward_transactions` (ledger, unik per `currency` + `source_key`), `rewards`, `redemptions` (unik per `request_id`), `budget_entries`, `reminder_settings`, dan `reminder_window`.
- Setiap migrasi diuji dengan membuat database versi lama dari skema JSON yang di-commit, lalu membukanya dengan Room versi terbaru. Room memvalidasi semua tabel setelah migrasi.
- **Catatan makan menyimpan salinan nama dan gizi** saat dicatat. Mengedit atau menghapus makanan buatan sendiri tidak mengubah catatan hari-hari sebelumnya.

### Gerakan latihan dan animasinya

- Pustaka gerakan ada di kode (`core/domain/workout/ExerciseCatalog.kt`), bukan di database, sehingga selalu sama dengan versi aplikasi. Riwayat menyimpan `exercise_key` dan salinan nama gerakan.
- Pose animasi (`ExercisePoses.kt`) hanya menentukan posisi pinggul/bahu, sudut badan, dan titik tangan/kaki. Siku dan lutut dihitung dengan inverse kinematics dua tulang, sehingga panjang anggota tubuh tetap dan tangan/kaki yang menapak tidak bergeser. `ExercisePosesTest` mengecek jangkauan, panjang tulang, batas frame, dan lantai untuk setiap gerakan.
- Nilai MET dibulatkan dari Compendium of Physical Activities dan hanya dipakai untuk perkiraan kalori yang diberi label.
- Generator bersifat deterministik: pengaturan yang sama dengan seed yang sama selalu menghasilkan rencana yang sama, sehingga pemutar bisa membangun ulang rencana dari rutenya.

### XP, ledger, dan hadiah

- Aturan perolehan ada di `core/domain/rewards/RewardRules.kt`; `LedgerReconciler` membandingkan aktivitas yang tercatat dengan ledger dan hanya **menambah** baris: `g:<kunci>#n` untuk perolehan dan `r:<kunci>#n` untuk koreksi. Menjalankannya berulang kali tidak menambah apa pun.
- Aktivitas lebih tua dari 30 hari tidak diberi XP dan tidak dikoreksi, sehingga total lama tetap stabil.
- "Sudah didapat" (`RewardDao.markObtained`) berjalan dalam satu transaksi Room: simpan catatan dengan `request_id` unik, dan jika ada harga yang dibayar, tambahkan pengeluaran di anggaran. Mendapat hadiah tidak mengurangi XP.

### Pengingat

- Waktu berikutnya dihitung oleh `ReminderPlanner` (murni dan dites: jam aktif yang melewati tengah malam, jam tenang, hari pilihan, dan "lewati sisa hari ini").
- Setiap jenis pengingat adalah satu pekerjaan WorkManager unik (`reminder-<jenis>`). Setelah berjalan, pekerjaan menjadwalkan berikutnya. Mengubah pengaturan mengganti jadwal; membuka app hanya melengkapi jadwal yang hilang.
- Saat waktunya tiba, worker mengecek ulang target hari itu (air, langkah, latihan, makanan, atau timbang dalam 6 hari terakhir) dan jam aktif/tenang sebelum menampilkan notifikasi.
- **Waktu tidak dijamin persis.** VitQuest tidak memakai alarm presisi (`SCHEDULE_EXACT_ALARM`). Doze, penghemat baterai, dan pengaturan HyperOS/MIUI (misalnya Autostart dan pembatasan baterai) bisa menunda atau menahan pengingat.

### Database makanan dan resep

- Data awal ada di `app/src/main/assets/seed/foods.json` dan `recipes.json`, dibuat dengan `tools/seed/gen_foods.py` dan `tools/seed/gen_recipes.py`. Ubah skripnya, jalankan ulang, lalu naikkan `SeedDataLoader.SEED_VERSION`.
- Makanan bawaan dicocokkan lewat `seed_key`. Saat versi seed naik, nilainya diperbarui **tanpa** menghapus favorit, makanan buatan sendiri, atau rencana menu.
- Gizi bahan resep dihitung otomatis dari makanan yang dirujuk, sehingga resep dan database makanan selalu konsisten.
- **Sumber nilai gizi**: setiap makanan berlabel sumbernya.
  - `TKPI`: nilai acuan Tabel Komposisi Pangan Indonesia, untuk nasi, nasi merah, tempe, dan tahu.
  - `USDA`: nilai acuan USDA FoodData Central, untuk bahan dasar, buah, dan sayur.
  - `ESTIMATE`: perkiraan untuk masakan campuran, misalnya gado-gado, soto, dan gorengan.
  - Semua nilai dibulatkan dan ditampilkan sebagai **estimasi**. Nilai ini disusun dari referensi umum dan **belum dicocokkan satu per satu** dengan tabel aslinya, karena situs TKPI tidak bisa diakses dari lingkungan pengembangan. Test `SeedDataTest` mengecek konsistensi energi terhadap makro (rumus Atwater).
  - Kalau ingin akurasi lebih tinggi, nilai di `tools/seed/gen_foods.py` bisa diganti dengan angka dari TKPI resmi.
- `exportSchema = true`. File JSON skema ada di `app/schemas/` dan harus di-commit.
- **Tidak ada destructive fallback**. Setiap perubahan skema wajib menambah `Migration` di `DatabaseMigrations.ALL`. Test `DatabaseSchemaTest` gagal kalau ada versi tanpa migrasi atau tanpa file skema.

---

## Build dan test secara lokal (opsional)

Butuh JDK 17+ dan Android SDK command-line tools. Android Studio **tidak** diperlukan.

```bash
# Sekali saja: arahkan ke Android SDK
echo "sdk.dir=/path/to/android-sdk" > local.properties
# atau: export ANDROID_HOME=/path/to/android-sdk

./gradlew testDebugUnitTest      # unit test (JVM + Robolectric)
./gradlew lintDebug              # Android Lint
./gradlew assembleDebug          # APK di app/build/outputs/apk/debug/
./gradlew connectedDebugAndroidTest  # instrumented test, butuh HP/emulator terhubung
```

Untuk menandatangani build lokal dengan keystore bersama: `export VITQUEST_DEBUG_KEYSTORE=/path/ke/vitquest-debug.keystore`.

---

## Privasi dan data

- **Disimpan lokal di HP:** profil (usia, jenis kelamin opsional, tinggi, berat, pengalaman, aktivitas, lokasi latihan, peralatan, preferensi makanan), target harian (termasuk target kalori), langkah harian per sumber, checkpoint sensor langkah, catatan makan, makanan buatan sendiri dan favorit, rencana menu, daftar belanja, riwayat berat badan, ukuran tubuh, catatan minum, riwayat latihan, kebiasaan dan centangnya, ledger XP, katalog hadiah, riwayat hadiah, catatan anggaran, pengaturan pengingat, pilihan tema, pilihan sumber langkah, dan status onboarding.
- **Izin yang diminta (hanya jika kamu mengaktifkan sumbernya):**
  - `android.permission.health.READ_STEPS`: membaca total langkah harian dari Health Connect. VitQuest **tidak menulis** apa pun ke Health Connect dan tidak membaca data kesehatan lain.
  - `ACTIVITY_RECOGNITION` (Aktivitas fisik): membaca sensor langkah HP (Android 10+).
  - `POST_NOTIFICATIONS` (Android 13+): menampilkan pengingat yang kamu aktifkan. Isi notifikasi dibuat di HP dan tidak dikirim ke mana pun.
- **Tidak ada yang dikirim ke server.** Aplikasi tidak punya akun, analitik, iklan, atau koneksi jaringan untuk data pengguna.
- **Backup cloud dimatikan** untuk data aplikasi (`allowBackup="false"` dan `data_extraction_rules`). Di Android 12+, transfer langsung ke HP baru (device-to-device) masih diizinkan oleh sistem.
- Data bisa dilihat dan diubah lewat **Profil** dan tab **Aktivitas**, dan dihapus seluruhnya lewat **Pengaturan → Hapus semua data**. Data yang ada di Health Connect tidak ikut terhapus; kelola data itu di aplikasi Health Connect.
- Uninstall aplikasi juga menghapus semua datanya.

---

## Batasan yang diketahui

- Aplikasi **belum diuji di perangkat fisik** (termasuk POCO X8 Pro). Verifikasi dilakukan lewat unit test, Robolectric, UI smoke test, lint, dan build di CI. Alur izin Health Connect dan sensor langkah perlu dicoba langsung di HP.
- **Health Connect hanya berisi data yang ditulis HP atau aplikasi lain.** Kalau tidak ada aplikasi (misalnya aplikasi kebugaran bawaan HP) yang menulis langkah ke Health Connect, tab Aktivitas akan menampilkan "Tidak ada data" untuk Health Connect. Pada mode Otomatis, sensor HP dipakai sebagai cadangan.
- Health Connect hanya mengizinkan membaca data sekitar 30 hari sebelum izin diberikan, jadi riwayat dibatasi 30 hari.
- **Sensor HP dibaca hanya saat VitQuest terbuka.** Hardware HP tetap menghitung di latar belakang, jadi tidak ada langkah yang terlewat. Tapi:
  - penghitungan baru dimulai sejak sensor diaktifkan (langkah sebelumnya tidak diketahui tanggalnya);
  - jika aplikasi tidak dibuka melewati tengah malam, langkah pada rentang itu dibagi antar hari secara proporsional terhadap waktu (perkiraan);
  - langkah setelah terakhir kali aplikasi dibuka sampai HP dimatikan/restart bisa hilang.
- Tidak ada pengumpulan sensor terus-menerus di latar belakang (tidak ada foreground service). Ini disengaja sampai perilakunya diuji di HyperOS.
- Tidak ada fitur ekspor/impor data. Kalau HP di-reset tanpa transfer device-to-device, data hilang. Fitur ekspor direncanakan di fase berikutnya.
- Usia disimpan apa adanya (tidak otomatis bertambah), jadi perlu diperbarui manual agar BMR tetap akurat. Satuan hanya metrik (kg, cm, ml).
- Nilai gizi makanan dan resep adalah estimasi (lihat bagian "Database makanan dan resep"). Belum ada pemindai barcode atau database online.
- Rencana menu dan daftar belanja mencakup 7 hari ke depan. Resep buatan sendiri belum bisa dibuat.
- Animasi gerakan adalah ilustrasi figur sederhana tampak samping/depan, bukan video. Gerakan bergantian sisi (misalnya lunge, bird dog) dicontohkan satu sisi; langkah tertulis menjelaskan cara bergantian.
- Kalori latihan adalah perkiraan kasar (MET), bukan hasil pengukuran. Timer pemutar berjalan selama aplikasi terbuka atau di latar belakang selama proses aplikasi masih hidup; tidak ada notifikasi latihan di latar belakang.
- Target kebiasaan bawaan (gerak 5, latihan 3, air 5, makanan 5 hari per minggu) belum bisa diubah; kebiasaan pilihan sendiri bisa diatur 1–7 hari.
- Pencapaian dihitung dari data yang tersimpan. Pencapaian target langkah dan air memakai target yang berlaku sekarang.
- **Pengingat belum diuji di HP sungguhan.** Penjadwalan dites di Robolectric dengan WorkManager test helper, tetapi perilaku Doze dan penghemat baterai HyperOS/MIUI hanya bisa dipastikan di perangkat. Di HyperOS, mungkin perlu mengizinkan Autostart dan "Tanpa batasan" pada baterai agar pengingat tepat waktu.
- Pengingat tidak punya tombol aksi di notifikasi (misalnya "+250 ml"); mengetuk notifikasi membuka app.
- XP dan anggaran rupiah tidak terhubung ke pembayaran apa pun. Harga hadiah adalah angka yang kamu isi sendiri.
- Build rilis bertanda tangan (release signing) adalah tugas terpisah dan belum dikonfigurasi.
