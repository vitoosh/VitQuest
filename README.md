# VitQuest

VitQuest adalah aplikasi Android native untuk penurunan berat badan yang berkelanjutan, aktivitas harian, kalistenik, pencatatan kalori, hidrasi, dan kebiasaan sehat, dengan motivasi bergaya game (XP dan hadiah).

Aplikasi ini **offline-first**: semua fitur inti berjalan tanpa server dan tanpa akun.

> VitQuest bukan alat medis. Semua target dan angka kalori adalah estimasi, bukan saran medis.

---

## Status pengembangan

Proyek dibangun bertahap. **Saat ini: Fase 2 (pelacakan aktivitas) selesai.**

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
| Home: sapaan, target harian, berat badan saat ini dan target (berlabel "Diisi manual") | ✅ Berfungsi, memakai data asli dari profil |
| Navigasi 5 tab (Beranda, Aktivitas, Nutrisi, Latihan, Hadiah), dengan rail di layar lebar | ✅ Berfungsi |
| Bahasa Indonesia dan Inggris (ikut sistem, atau dipilih manual di Pengaturan) | ✅ Berfungsi |
| Tema Ikuti sistem, Terang, dan Gelap (tersimpan) | ✅ Berfungsi |
| Hapus semua data (dengan konfirmasi) | ✅ Berfungsi |
| GitHub Actions: unit test, lint, build debug APK, upload APK dan laporan | ✅ Dikonfigurasi |

### Yang masih placeholder ("Segera hadir")

- Tab **Nutrisi, Latihan, Hadiah** hanya menampilkan layar "Segera hadir" berisi rencana fiturnya.
- Kalori, hidrasi, latihan, XP, dan Reward Credit **belum dilacak**. Tidak ada angka contoh atau angka karangan yang ditampilkan.
- Belum ada notifikasi atau pengingat.

### Rencana fase berikutnya

1. ~~Fase 1: Fondasi dan build otomatis~~
2. ~~Fase 2: Pelacakan aktivitas (Health Connect, sensor langkah, input manual, riwayat)~~
3. Fase 3: Berat badan, nutrisi (database makanan Indonesia), resep, dan hidrasi
4. Fase 4: Latihan kalistenik dan kebiasaan, termasuk **tutorial bergambar untuk setiap gerakan** (selain jalan kaki) berupa animasi figur vektor yang digambar app (posisi awal ke posisi akhir, plus penanda otot yang dilatih). Animasi ini offline dan konsisten di tema terang/gelap.
5. Fase 5: XP, Reward Credit, hadiah, dan pengingat
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
| Data | Room (profil, target, langkah harian per sumber, checkpoint sensor), DataStore Preferences (tema, status onboarding, pilihan sumber langkah) |
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
│   ├── domain/         # Logika bisnis murni: saran target, parsing input, batas nilai
│   ├── data/           # Room (local/), DataStore (preferences/), repository/, Hilt di/
│   ├── designsystem/   # Tema, warna, spacing, komponen, ikon
│   ├── locale/         # Pilihan bahasa per-aplikasi
│   └── ui/             # Label lokal untuk enum dan format angka
├── feature/            # home, onboarding, profile, settings, comingsoon
└── ui/                 # Root app dan navigasi
app/src/main/res/       # values/ (Inggris, default), values-in/ (Indonesia), values-night/
app/schemas/            # Skema Room yang diekspor (wajib di-commit untuk migrasi)
app/src/test/           # Unit test + Robolectric (DB, UI smoke test)
app/src/androidTest/    # Instrumented test (dijalankan di HP/emulator)
.github/workflows/android-build.yml
```

### Database dan migrasi

- Versi 1: tabel `user_profile` dan `goals` (masing-masing satu baris).
- Versi 2 (Fase 2): `daily_steps` (satu baris per tanggal dan sumber) dan `step_sensor_state` (checkpoint penghitung langkah HP). Migrasi `MIGRATION_1_2` diuji dengan membuat database v1 dari skema JSON yang di-commit, lalu membukanya dengan Room versi terbaru.
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

- **Disimpan lokal di HP:** profil (usia, tinggi, berat, pengalaman, aktivitas, lokasi latihan, peralatan, preferensi makanan), target harian, langkah harian per sumber, checkpoint sensor langkah, pilihan tema, pilihan sumber langkah, dan status onboarding.
- **Izin yang diminta (hanya jika kamu mengaktifkan sumbernya):**
  - `android.permission.health.READ_STEPS`: membaca total langkah harian dari Health Connect. VitQuest **tidak menulis** apa pun ke Health Connect dan tidak membaca data kesehatan lain.
  - `ACTIVITY_RECOGNITION` (Aktivitas fisik): membaca sensor langkah HP (Android 10+).
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
- Usia disimpan apa adanya (tidak otomatis bertambah). Satuan hanya metrik (kg, cm, ml).
- Fase berikutnya (notifikasi, sensor langkah) akan terpengaruh pembatasan baterai HyperOS/MIUI. Ini akan didokumentasikan dan diuji di perangkat saat fitur tersebut dibuat.
- Build rilis bertanda tangan (release signing) adalah tugas terpisah dan belum dikonfigurasi.
