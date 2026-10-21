# VitQuest

VitQuest adalah aplikasi Android native untuk penurunan berat badan yang berkelanjutan, aktivitas harian, kalistenik, pencatatan kalori, hidrasi, dan kebiasaan sehat, dengan pencapaian berbasis konsistensi.

Aplikasi ini **offline-first**: semua fitur inti berjalan tanpa server dan tanpa akun. Satu-satunya fitur yang memakai internet adalah **estimasi foto makanan** (opsional, lihat [`docs/AI_SETUP.md`](docs/AI_SETUP.md)).

> VitQuest bukan alat medis. Semua target dan angka kalori adalah estimasi, bukan saran medis.

---

## Status pengembangan

Proyek dibangun bertahap. **Saat ini: Fase 6 selesai, ditambah tiga perubahan sesudahnya (database v8):** XP, hadiah, dan anggaran dihapus; penghitungan langkah dengan akselerometer dan di latar belakang; dan estimasi kalori dari foto makanan. Cloud sync sengaja **belum** dibuat: itu butuh persetujuan eksplisit dan keputusan backend (lihat `docs/SECURITY_PRIVACY.md`).

### Perubahan setelah Fase 6

| Fitur | Status |
|---|---|
| **XP, level, hadiah, riwayat hadiah, dan anggaran dihapus**, termasuk tab Hadiah dan kartu di Beranda. Navigasi sekarang 4 tab. **Pencapaian tetap ada** (di Progres). Migrasi v6→v7 membuang tabelnya; data lain tetap. Cadangan lama tetap bisa dipulihkan. | ✅ Berfungsi, ada test migrasi dan cadangan |
| **Penghitung langkah dari akselerometer**: untuk HP yang sensor langkah bawaannya hampir tidak menghitung. Gravitasi dibuang, lalu puncak gerakan dengan ritme jalan/lari dihitung setelah 8 langkah berturut-turut (agar mengangkat HP tidak terhitung). Per hari, angka **terbesar** dari dua penghitung HP yang dipakai; Health Connect dan input manual tetap lebih diutamakan. | ✅ Berfungsi, ada test dengan data gerak sintetis; ⚠️ akurasi di HP belum diukur |
| **Penghitungan langkah di latar belakang** (opsional, tab Aktivitas): layanan *foreground* tipe *health* dengan notifikasi tetap berisi langkah hari ini (tanpa angka di layar kunci), menyala lagi setelah HP restart atau app diperbarui. Satu pengumpul data dipakai bersama app yang terbuka, jadi tidak ada langkah ganda. | ✅ Berfungsi, ada test-nya; ⚠️ belum dicoba di HyperOS |
| **Diagnostik langkah** (Pengaturan → Diagnostik langkah): nilai mentah sensor dan waktunya, titik awal tersimpan, bacaan dan langkah akselerometer, angka hari ini per sumber, dan status Health Connect. Health Connect dibaca ulang tiap menit selama tab Aktivitas terbuka. | ✅ Berfungsi |
| **Foto makanan (estimasi AI)**: ambil atau pilih foto → server Supabase meminta Google Gemini mengenali makanan dan memperkirakan berat serta kalori → kamu memeriksa, mengubah nama/berat/kalori (kalori ikut berubah saat berat diubah), menghapus item, memilih waktu makan, lalu menyimpan. Semua item berlabel **estimasi AI** dan tingkat keyakinannya. | ✅ Berfungsi dengan server tiruan di test; ⚠️ belum dicoba dengan Supabase/Gemini sungguhan dan kamera HP |
| Persetujuan sebelum foto pertama, login anonim otomatis (tanpa akun), token terenkripsi Android Keystore, batas harian per HP dan total di server, HTTPS saja | ✅ Ada test-nya |
| **Salinan kecil foto** (maks. 320 px) disimpan di HP, tampil di catatan makan, ikut file cadangan, dan terhapus bersama catatan terakhirnya. Foto kamera ukuran penuh dihapus setelah diproses. | ✅ Berfungsi, ada test-nya |
| Panduan menyiapkan Gemini, Supabase, dan GitHub secret: [`docs/AI_SETUP.md`](docs/AI_SETUP.md). Tanpa pengaturan itu, APK tetap berjalan dan layar foto menjelaskan bahwa fitur belum diatur. | ✅ Ada |

### Yang sudah berfungsi di Fase 6 (Cadangan, privasi, aksesibilitas, kesiapan rilis)

| Fitur | Status |
|---|---|
| **Ekspor cadangan** (Pengaturan → Cadangan): semua catatan dan pengaturan ke satu file JSON di lokasi pilihanmu (penyimpanan HP, Google Drive, dll.) lewat pemilih file Android. VitQuest tidak mengunggah apa pun. | ✅ Berfungsi, ada test-nya |
| **Pulihkan dari cadangan**: minta konfirmasi, lalu mengganti semua data dalam satu transaksi. File yang bukan cadangan VitQuest, rusak, atau dari versi app yang lebih baru ditolak tanpa mengubah data. Cadangan dari versi lama dimigrasi otomatis. | ✅ Berfungsi, ada test-nya |
| **Review keamanan dan privasi** (`docs/SECURITY_PRIVACY.md`, diperbarui untuk v8): izin internet hanya untuk foto makanan, daftar izin dan komponen yang diekspor, isi file cadangan, dan temuan beserta perbaikannya | ✅ Selesai |
| Notifikasi air/gerak tidak menampilkan angka di layar kunci yang menyembunyikan konten sensitif | ✅ Berfungsi, ada test-nya |
| Teks privasi dan dialog "Hapus semua data" sekarang menyebut semua jenis data | ✅ Berfungsi |
| **Pengaman otomatis** (`PrivacyGuardTest`): build gagal jika ada izin baru yang belum ditinjau (termasuk dari library), kode jaringan di luar `core/ai/`, HTTP tanpa enkripsi, cadangan cloud diaktifkan, atau kode log/print di app | ✅ Ada test-nya |
| **Aksesibilitas**: kontras teks WCAG AA (≥ 4,5:1) untuk tema terang dan gelap; setiap kontrol punya label pembaca layar dan area sentuh ≥ 48 dp yang tidak bertabrakan, dicek di layar-layar utama (termasuk layar foto makanan) | ✅ Ada test-nya |
| **Test integrasi**: satu hari pemakaian lewat repository dan database asli, ekspor, hapus, lalu pulihkan | ✅ Ada test-nya |
| **Build rilis dengan R8** dibangun di CI setiap push (tanpa tanda tangan, tidak diunggah). Proses tanda tangan rilis lewat GitHub Secrets didokumentasikan di `docs/RELEASE.md`. | ✅ Build berhasil; belum dijalankan di HP |
| Test instrumented (migrasi semua versi dengan `MigrationTestHelper`, Accessibility Test Framework) dikompilasi di CI | ⚠️ Belum dijalankan (butuh HP/emulator) |

### Yang sudah berfungsi di Fase 5 (Hadiah dan pengingat)

> XP, hadiah, dan anggaran di bawah ini **sudah dihapus** setelah Fase 6 (lihat "Perubahan setelah Fase 6"). Pengingat tetap ada.

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
| Navigasi tab (sekarang 4: Beranda, Aktivitas, Nutrisi, Latihan), dengan rail di layar lebar | ✅ Berfungsi |
| Bahasa Indonesia dan Inggris (ikut sistem, atau dipilih manual di Pengaturan) | ✅ Berfungsi |
| Tema Ikuti sistem, Terang, dan Gelap (tersimpan) | ✅ Berfungsi |
| Hapus semua data (dengan konfirmasi) | ✅ Berfungsi |
| GitHub Actions: unit test, lint, build debug APK, upload APK dan laporan | ✅ Dikonfigurasi |

### Yang masih placeholder

- Tidak ada lagi layar "Segera hadir".
- **Belum ada:** cloud sync (butuh persetujuan), build rilis bertanda tangan (tugas terpisah), dan integrasi smartwatch langsung. Jam yang aplikasinya menulis langkah ke Health Connect sudah otomatis terbaca lewat Health Connect.

### Rencana fase berikutnya

1. ~~Fase 1: Fondasi dan build otomatis~~
2. ~~Fase 2: Pelacakan aktivitas (Health Connect, sensor langkah, input manual, riwayat)~~
3. ~~Fase 3: Berat badan, nutrisi (database makanan Indonesia), resep, dan hidrasi~~
4. ~~Fase 4: Latihan kalistenik, tutorial animasi gerakan, kebiasaan, dan progres~~
5. ~~Fase 5: XP, hadiah nyata, anggaran, dan pengingat~~
6. ~~Fase 6: Kesiapan rilis (cadangan lokal, review keamanan/privasi, aksesibilitas, build rilis R8)~~. Cloud sync opsional belum dikerjakan dan menunggu persetujuan.
7. ~~Sesudah Fase 6: hapus XP/hadiah, langkah dari akselerometer dan di latar belakang, foto makanan dengan AI~~

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
| Data | Room (profil, target, langkah, makanan, resep, berat, air, latihan, kebiasaan, pengaturan pengingat, catatan foto makanan), DataStore Preferences (tema, status onboarding, pilihan sumber langkah, langkah di latar, persetujuan foto AI, versi data awal), salinan foto di penyimpanan privat app |
| Latar belakang | WorkManager 2.12 (pengingat; satu pekerjaan unik per jenis), notifikasi Android dengan satu channel per jenis pengingat, foreground service tipe `health` untuk langkah (opsional) |
| Data kesehatan | Health Connect client 1.1.0 (hanya baca `StepsRecord`), Android `SensorManager` (`TYPE_STEP_COUNTER` dan `TYPE_ACCELEROMETER`) |
| Foto makanan | `HttpURLConnection` bawaan Android (tanpa library jaringan) ke Supabase Edge Function (Deno/TypeScript, `supabase/`), Google Gemini API di server, kamera lewat `FileProvider`, galeri lewat Photo Picker |
| Bahasa UI | Per-app language API Android (AppCompat). Pilihan bahasa disimpan oleh sistem/AppCompat, bukan duplikat di DataStore. |
| Build | Gradle 9.6.1 (wrapper, checksum terverifikasi), AGP 9.4.1, Kotlin 2.4.20, JDK 17+ (CI memakai 21) |
| Test | JUnit 4, kotlinx-coroutines-test, Robolectric, Compose UI test, Room in-memory |

Versi lengkap ada di [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

### Struktur proyek

```
app/src/main/java/io/github/vitoosh/vitquest/
├── MainActivity.kt, VitQuestApplication.kt
├── core/
│   ├── ai/             # Foto makanan: HTTP client, login anonim, token terenkripsi, pengecilan gambar (satu-satunya kode jaringan)
│   ├── model/          # Model domain (UserProfile, Goals, ThemeMode, ...)
│   ├── domain/         # Logika bisnis murni: target, kalori, langkah, berat, latihan (generator, pemutar, pose figur), kebiasaan
│   ├── data/           # Room (local/), DataStore (preferences/), repository/, backup/, photos/, sensor/, Hilt di/
│   ├── steps/          # Layanan penghitung langkah di latar dan receiver setelah boot
│   ├── designsystem/   # Tema, warna, spacing, komponen, ikon
│   ├── locale/         # Pilihan bahasa per-aplikasi
│   └── ui/             # Label lokal untuk enum dan format angka
├── feature/            # home, activity, nutrition (termasuk photo/), weight, hydration, workouts, habits, progress, reminders, onboarding, profile, settings
└── ui/                 # Root app dan navigasi
app/src/main/res/       # values/ (Inggris, default), values-in/ (Indonesia), values-night/
app/schemas/            # Skema Room yang diekspor (wajib di-commit untuk migrasi)
app/src/test/           # Unit test + Robolectric (DB, UI smoke test)
app/src/androidTest/    # Instrumented test (dijalankan di HP/emulator)
.github/workflows/android-build.yml
supabase/               # SQL kuota, Edge Function food-photo, dan test-nya (lihat docs/AI_SETUP.md)
```

### Database dan migrasi

- Versi 1: tabel `user_profile` dan `goals` (masing-masing satu baris).
- Versi 2 (Fase 2): `daily_steps` (satu baris per tanggal dan sumber) dan `step_sensor_state` (checkpoint penghitung langkah HP).
- Versi 3 (Fase 3): kolom `user_profile.sex` dan `goals.daily_calorie_target`, plus tabel `foods`, `food_servings`, `food_log_entries`, `recipes`, `recipe_ingredients`, `meal_plan_entries`, `shopping_items`, `weight_entries`, `body_measurements`, dan `water_logs`.
- Versi 4 (Fase 4): tabel `workout_sessions`, `workout_session_exercises` (salinan nama gerakan dan status tiap langkah), `habits`, dan `habit_check_ins`.
- Versi 5 (Fase 5): `reward_transactions` (ledger, unik per `currency` + `source_key`), `rewards`, `redemptions` (unik per `request_id`), `budget_entries`, `reminder_settings`, dan `reminder_window`.
- Versi 6: Reward Credit dihapus. Baris ledger credit dibuang, `rewards` menjadi `reward_items` (tanpa biaya credit), penukaran yang tidak dibatalkan menjadi `reward_history`, dan `budget_entries` mengikuti tabel baru.
- Versi 7: XP, hadiah, dan anggaran dihapus (`reward_transactions`, `reward_items`, `reward_history`, `budget_entries`).
- Versi 8: tabel `food_photos` dan kolom `food_log_entries.photo_id` untuk catatan dari foto makanan.
- Setiap migrasi diuji dengan membuat database versi lama dari skema JSON yang di-commit, lalu membukanya dengan Room versi terbaru. Room memvalidasi semua tabel setelah migrasi. Test instrumented `MigrationInstrumentedTest` menjalankan hal yang sama di SQLite milik HP.
- Skema JSON juga disertakan di APK (`assets/`), agar cadangan dari versi lama bisa dibangun ulang lalu dimigrasi saat dipulihkan. Saat menaikkan versi database, commit skema barunya dan tambahkan migrasinya ke `DatabaseMigrations.ALL`; test cadangan dan migrasi akan menangkap yang terlewat.
- **Catatan makan menyimpan salinan nama dan gizi** saat dicatat. Mengedit atau menghapus makanan buatan sendiri tidak mengubah catatan hari-hari sebelumnya.

### Gerakan latihan dan animasinya

- Pustaka gerakan ada di kode (`core/domain/workout/ExerciseCatalog.kt`), bukan di database, sehingga selalu sama dengan versi aplikasi. Riwayat menyimpan `exercise_key` dan salinan nama gerakan.
- Pose animasi (`ExercisePoses.kt`) hanya menentukan posisi pinggul/bahu, sudut badan, dan titik tangan/kaki. Siku dan lutut dihitung dengan inverse kinematics dua tulang, sehingga panjang anggota tubuh tetap dan tangan/kaki yang menapak tidak bergeser. `ExercisePosesTest` mengecek jangkauan, panjang tulang, batas frame, dan lantai untuk setiap gerakan.
- Nilai MET dibulatkan dari Compendium of Physical Activities dan hanya dipakai untuk perkiraan kalori yang diberi label.
- Generator bersifat deterministik: pengaturan yang sama dengan seed yang sama selalu menghasilkan rencana yang sama, sehingga pemutar bisa membangun ulang rencana dari rutenya.

### Langkah dari sensor HP

- `StepSensorTracker` adalah satu-satunya pengumpul data sensor, dipakai bersama oleh app yang terbuka dan layanan latar (`Holder.APP` / `Holder.SERVICE`), jadi data tidak pernah dihitung dua kali.
- `TYPE_STEP_COUNTER` dicatat sebagai sumber `PHONE_SENSOR` (nilai kumulatif dengan checkpoint per boot). Akselerometer diolah `MotionStepDetector` menjadi sumber `MOTION`: gravitasi dibuang dengan rata-rata bergerak (τ 1 dtk), sinyal dihaluskan (τ 0,06 dtk), lalu puncak ≥ maks(1,0 m/s², 0,4 × rata-rata puncak) berjarak 250 ms–2 dtk dihitung; penghitungan baru berjalan setelah 8 langkah berturut-turut, lalu 8 langkah itu langsung dihitung. Langkah disimpan tiap 15 detik dan saat berhenti.
- `StepSourceResolver`: manual > Health Connect > (sensor HP dan akselerometer, dipilih yang **lebih besar**). Angka dari sumber berbeda tidak pernah dijumlahkan.
- Layanan latar memakai akselerometer *wake-up* jika ada; jika tidak, wake lock parsial yang diperbarui tiap 10 menit.

### Foto makanan

- App → `POST /functions/v1/food-photo` (JPEG base64, maks. 1024 px) dengan header `apikey` (publishable key) dan token login anonim. Function mencatat kuota lewat RPC `consume_food_photo_quota` memakai token pemanggil, lalu memanggil Gemini `generateContent` dengan skema JSON (`responseSchema`) dan merapikan jawabannya.
- Login anonim memakai REST Supabase Auth (`/auth/v1/signup`, refresh lewat `/auth/v1/token?grant_type=refresh_token`). Token kedaluwarsa diperbarui otomatis; jika server menjawab 401, app login ulang dan mencoba sekali lagi.
- Hasil dicek ulang di HP (`FoodPhotoAnalyzer.parseEstimate`) sebelum ditampilkan, dan setiap nilai divalidasi lagi sebelum disimpan.
- Satu foto = satu baris `food_photos`, dan item-itemnya disimpan sebagai catatan makan biasa (`foodId` kosong, `photo_id` terisi) dalam satu transaksi. File foto di `files/food_photos/<id>.jpg`.
- Test server: `node supabase/tests/food-photo.harness.mjs` (menjalankan function dengan `fetch` tiruan, tanpa jaringan).

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
./gradlew assembleRelease        # cek build rilis R8 (tanpa tanda tangan, tidak bisa diinstall)
./gradlew connectedDebugAndroidTest  # instrumented test, butuh HP/emulator terhubung
```

Untuk menandatangani build lokal dengan keystore bersama: `export VITQUEST_DEBUG_KEYSTORE=/path/ke/vitquest-debug.keystore`.

---

## Privasi dan data

- **Disimpan lokal di HP:** profil (usia, jenis kelamin opsional, tinggi, berat, pengalaman, aktivitas, lokasi latihan, peralatan, preferensi makanan), target harian (termasuk target kalori), langkah harian per sumber, checkpoint sensor langkah, catatan makan dan salinan kecil foto makanan, makanan buatan sendiri dan favorit, rencana menu, daftar belanja, riwayat berat badan, ukuran tubuh, catatan minum, riwayat latihan, kebiasaan dan centangnya, pengaturan pengingat, pilihan tema, pilihan sumber langkah, status onboarding, dan persetujuan foto AI.
- **Izin yang diminta (hanya jika kamu mengaktifkan sumbernya):**
  - `android.permission.health.READ_STEPS`: membaca total langkah harian dari Health Connect. VitQuest **tidak menulis** apa pun ke Health Connect dan tidak membaca data kesehatan lain.
  - `ACTIVITY_RECOGNITION` (Aktivitas fisik): membaca sensor langkah HP (Android 10+).
  - `POST_NOTIFICATIONS` (Android 13+): menampilkan pengingat yang kamu aktifkan dan notifikasi penghitung langkah di latar. Isi notifikasi dibuat di HP dan tidak dikirim ke mana pun.
  - `FOREGROUND_SERVICE_HEALTH`: penghitungan langkah di latar, hanya jika kamu menyalakannya.
  - `INTERNET`: hanya untuk foto makanan (lihat di bawah).
- **Yang dikirim ke server hanya foto makanan yang kamu pilih untuk dianalisis**, setelah kamu menyetujui penjelasannya: lewat HTTPS ke server Supabase milik pemilik app, yang meneruskannya ke Google Gemini. Foto tidak disimpan di server. **Di tingkat gratis Gemini, Google boleh memakai isi yang dikirim untuk meningkatkan produknya**, jadi potret makanan saja. Data lain (langkah, berat, catatan, profil) tidak pernah dikirim. Tidak ada akun (hanya login anonim untuk batas harian), analitik, atau iklan.
- **Backup cloud dimatikan** untuk data aplikasi (`allowBackup="false"` dan `data_extraction_rules`). Di Android 12+, transfer langsung ke HP baru (device-to-device) masih diizinkan oleh sistem.
- **File cadangan** hanya dibuat saat kamu mengetuk Ekspor, disimpan di tempat yang kamu pilih, dan **tidak dienkripsi**: isinya data kesehatanmu, jadi simpan di tempat privat. Salinan foto makanan ikut file cadangan; langkah sensor, pilihan bahasa, dan token login anonim tidak.
- Detail lengkap: `docs/SECURITY_PRIVACY.md`.
- Data bisa dilihat dan diubah lewat **Profil** dan tab **Aktivitas**, dan dihapus seluruhnya lewat **Pengaturan → Hapus semua data**. Data yang ada di Health Connect tidak ikut terhapus; kelola data itu di aplikasi Health Connect.
- Uninstall aplikasi juga menghapus semua datanya.

---

## Batasan yang diketahui

- Aplikasi **belum diuji di perangkat fisik** (termasuk POCO X8 Pro). Verifikasi dilakukan lewat unit test, Robolectric, UI smoke test, lint, dan build di CI. Alur izin Health Connect dan sensor langkah perlu dicoba langsung di HP.
- **Health Connect hanya berisi data yang ditulis HP atau aplikasi lain.** Kalau tidak ada aplikasi (misalnya aplikasi kebugaran bawaan HP) yang menulis langkah ke Health Connect, tab Aktivitas akan menampilkan "Tidak ada data" untuk Health Connect. Pada mode Otomatis, sensor HP dipakai sebagai cadangan.
- Health Connect hanya mengizinkan membaca data sekitar 30 hari sebelum izin diberikan, jadi riwayat dibatasi 30 hari.
- **Tanpa penghitungan latar, sensor HP dibaca hanya saat VitQuest terbuka.** Akselerometer hanya menghitung saat app terbuka atau layanan latar menyala. Sensor langkah bawaan tetap menghitung di hardware, tapi:
  - penghitungan baru dimulai sejak sensor diaktifkan (langkah sebelumnya tidak diketahui tanggalnya);
  - jika aplikasi tidak dibuka melewati tengah malam, langkah pada rentang itu dibagi antar hari secara proporsional terhadap waktu (perkiraan);
  - langkah setelah terakhir kali aplikasi dibuka sampai HP dimatikan/restart bisa hilang.
- **Penghitung akselerometer adalah perkiraan.** Ambangnya dipilih dari data gerak sintetis, belum dikalibrasi dengan langkah sungguhan. Langkah sangat pelan, HP di tas yang berayun, atau naik kendaraan di jalan bergelombang bisa membuat angka kurang atau lebih. Bandingkan dengan menghitung manual 100 langkah, dan lihat Diagnostik langkah.
- **Penghitungan latar memakai baterai** (sensor terus menyala) dan belum diuji di HyperOS. Di HyperOS/MIUI mungkin perlu mengizinkan Autostart dan memilih "Tanpa batasan" pada pengaturan baterai VitQuest agar layanan tidak dihentikan.
- **Smartwatch** (misalnya Eggel Tempo 4 Active) tidak bisa disambungkan langsung karena tidak ada API publik. Jika aplikasi jamnya menulis langkah ke Health Connect, VitQuest otomatis membacanya; cek di Health Connect → Izin aplikasi.
- **Foto makanan belum diuji dengan Supabase dan Gemini sungguhan**, hanya dengan server tiruan. Kamera dan galeri juga belum dicoba di HP. Estimasi AI bisa jauh meleset, terutama berat porsi, minyak, santan, dan gula dalam minuman; selalu periksa sebelum menyimpan. Butuh internet, dan dibatasi kuota gratis Gemini serta batas harian di server. Project Supabase gratis dijeda otomatis setelah sekitar seminggu tidak dipakai.
- Cadangan bersifat manual: tidak ada cadangan otomatis terjadwal. File cadangan tidak dienkripsi dan tidak dilindungi kata sandi. Memulihkan selalu mengganti semua data (tidak digabung).
- Pemulihan cadangan dan build rilis R8 belum dicoba di HP sungguhan; pemilih file dan penyedia penyimpanan (misalnya Google Drive) bisa berperilaku berbeda antar HP.
- Usia disimpan apa adanya (tidak otomatis bertambah), jadi perlu diperbarui manual agar BMR tetap akurat. Satuan hanya metrik (kg, cm, ml).
- Nilai gizi makanan dan resep adalah estimasi (lihat bagian "Database makanan dan resep"). Belum ada pemindai barcode atau database online.
- Rencana menu dan daftar belanja mencakup 7 hari ke depan. Resep buatan sendiri belum bisa dibuat.
- Animasi gerakan adalah ilustrasi figur sederhana tampak samping/depan, bukan video. Gerakan bergantian sisi (misalnya lunge, bird dog) dicontohkan satu sisi; langkah tertulis menjelaskan cara bergantian.
- Kalori latihan adalah perkiraan kasar (MET), bukan hasil pengukuran. Timer pemutar berjalan selama aplikasi terbuka atau di latar belakang selama proses aplikasi masih hidup; tidak ada notifikasi latihan di latar belakang.
- Target kebiasaan bawaan (gerak 5, latihan 3, air 5, makanan 5 hari per minggu) belum bisa diubah; kebiasaan pilihan sendiri bisa diatur 1–7 hari.
- Pencapaian dihitung dari data yang tersimpan. Pencapaian target langkah dan air memakai target yang berlaku sekarang.
- **Pengingat belum diuji di HP sungguhan.** Penjadwalan dites di Robolectric dengan WorkManager test helper, tetapi perilaku Doze dan penghemat baterai HyperOS/MIUI hanya bisa dipastikan di perangkat. Di HyperOS, mungkin perlu mengizinkan Autostart dan "Tanpa batasan" pada baterai agar pengingat tepat waktu.
- Pengingat tidak punya tombol aksi di notifikasi (misalnya "+250 ml"); mengetuk notifikasi membuka app.
- Build rilis bertanda tangan (release signing) adalah tugas terpisah dan belum dikonfigurasi.
