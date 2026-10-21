# Review keamanan dan privasi VitQuest

Review untuk Fase 6, diperbarui untuk database v8: XP, hadiah, dan anggaran dihapus (v7), penghitungan langkah di latar belakang, dan estimasi foto makanan lewat internet (v8). Tidak ada cloud sync. Review ini dilakukan dengan membaca kode, manifest hasil merge, dan konfigurasi build/CI, ditambah test otomatis yang disebutkan di bawah. **Belum ada pengujian penetrasi atau pengujian di HP sungguhan.**

## Ringkasan

- VitQuest memakai izin `INTERNET` **hanya untuk estimasi foto makanan**, setelah pengguna menyetujui penjelasannya. Hanya foto yang dipilih untuk dianalisis yang dikirim, lewat HTTPS ke server Supabase milik pemilik app, yang meneruskannya ke Google Gemini. Data kesehatan lain (langkah, berat, catatan makan, profil) tidak pernah dikirim. `PrivacyGuardTest` memastikan kode jaringan hanya ada di `core/ai/` dan lalu lintas tanpa enkripsi (HTTP) dilarang.
- Jika APK dibuat tanpa pengaturan Supabase (lihat `docs/AI_SETUP.md`), fitur foto mati dan app tidak menghubungi server mana pun.
- Semua data disimpan di penyimpanan privat app di HP (Room/SQLite dan DataStore).
- Data app **tidak ikut cadangan cloud Google**. Pindah ke HP baru bisa lewat transfer langsung antar-perangkat (jika didukung HP), atau lewat **file cadangan** yang diekspor sendiri dari Pengaturan.
- Health Connect hanya dipakai untuk **membaca total langkah harian**. VitQuest tidak menulis ke Health Connect.
- Tidak ada akun, iklan, analitik, atau pelaporan crash ke pihak ketiga. Untuk fitur foto, app login **anonim** otomatis ke Supabase (tanpa nama, email, atau nomor HP) agar batas harian bisa dihitung per HP.

## Data yang disimpan di HP

| Data | Tempat | Catatan |
|---|---|---|
| Profil, target, langkah, makanan, resep, rencana menu, belanja, berat, ukuran tubuh, air, latihan, kebiasaan, pengaturan pengingat, catatan foto makanan | Room (`vitquest.db`) di penyimpanan privat app | Hanya bisa dibaca app ini (sandbox Android). Saat HP terkunci, isi file dilindungi enkripsi bawaan Android. |
| Tema, status onboarding, pilihan sumber langkah, penghitungan langkah di latar, persetujuan foto AI, versi data bawaan | DataStore di penyimpanan privat app | |
| Salinan kecil foto makanan (JPEG, maks. 320 px) | `files/food_photos/` di penyimpanan privat app | Ikut file cadangan. Dihapus saat catatan terakhir dari foto itu dihapus, dan saat semua data dihapus. |
| Token login anonim Supabase | `no_backup/` di penyimpanan privat app | Dienkripsi AES-GCM dengan kunci Android Keystore yang tidak bisa diekspor. Tidak ikut cadangan apa pun. |
| Foto asli dari kamera | `cache/camera/` sementara | Ditulis aplikasi kamera lewat `FileProvider`, lalu dihapus segera setelah salinan kecil dibuat. Foto dari galeri tidak disalin utuh. |
| Checkpoint sensor langkah | Room (`step_sensor_state`) | Khusus perangkat dan sesi boot ini; tidak ikut file cadangan. |
| Bahasa app | Pengaturan bahasa per-app Android | Tidak ikut file cadangan. |

App tidak menambahkan enkripsi sendiri di atas enkripsi Android. Di HP yang sudah di-root, atau pada **APK debug** lewat `adb shell run-as`, data app bisa dibaca orang yang memegang HP dalam keadaan terbuka. APK rilis (belum didistribusikan) tidak `debuggable`.

## Apa yang keluar dari HP

| Jalur | Isi | Kendali pengguna |
|---|---|---|
| Internet (hanya fitur foto makanan) | Foto yang dipilih (JPEG maks. 1024 px), bahasa app, dan token login anonim, ke Supabase Edge Function `food-photo` lewat HTTPS. Function memeriksa batas harian lalu mengirim foto ke Google Gemini. Foto tidak disimpan di server dan tidak dicatat di log. **Di tingkat gratis Gemini, Google boleh memakai isi yang dikirim untuk meningkatkan produknya.** | Hanya saat pengguna mengambil/memilih foto, setelah menyetujui penjelasan di layar. Hasil estimasi selalu diperiksa dan bisa diubah sebelum disimpan. |
| Health Connect | Tidak ada yang ditulis; hanya membaca langkah | Izin bisa dicabut kapan saja di Health Connect |
| File cadangan (Pengaturan → Cadangan) | Semua catatan dan pengaturan di atas, sebagai JSON **tidak terenkripsi** | Hanya dibuat saat pengguna mengetuk Ekspor, dan disimpan di lokasi yang dipilih sendiri lewat pemilih file Android. App hanya mendapat akses ke file itu. Jika disimpan ke Google Drive atau lokasi cloud lain, file itu ikut aturan privasi layanan tersebut. Peringatan ditampilkan di layar. |
| Notifikasi pengingat | Pengingat air dan gerak bisa menyebut jumlah air/langkah hari ini | Notifikasi bertanda *private* dengan versi publik tanpa angka: di layar kunci yang disetel "sembunyikan konten sensitif", hanya teks umum yang tampil (`ReminderNotificationsTest`). |
| Transfer antar-perangkat Android 12+ | Data app, jika pengguna memakai fitur transfer HP | Mengikuti fitur bawaan HP; cadangan cloud tetap dikecualikan. |

## Izin

Semua izin di manifest hasil merge harus ada di daftar ini; menambah izin membuat `PrivacyGuardTest` gagal sampai daftar diperbarui dengan alasan.

| Izin | Untuk | Kapan diminta |
|---|---|---|
| `health.READ_STEPS` | Membaca total langkah harian dari Health Connect | Saat pengguna memilih Health Connect |
| `ACTIVITY_RECOGNITION` | Sensor langkah HP | Saat pengguna memakai sensor HP |
| `POST_NOTIFICATIONS` | Pengingat dan notifikasi penghitung langkah | Dari layar Pengingat atau saat menyalakan penghitungan latar (Android 13+) |
| `INTERNET` | Estimasi foto makanan | Tidak diminta (izin normal); dipakai hanya setelah persetujuan di layar foto |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_HEALTH` | Penghitungan langkah di latar belakang (layanan tipe *health* dengan notifikasi tetap) | Hanya jika pengguna menyalakannya di tab Aktivitas |
| `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `ACCESS_NETWORK_STATE` | WorkManager (penjadwal pengingat), dan VitQuest sendiri untuk memulai ulang penghitungan langkah setelah HP menyala lagi atau app diperbarui. Wake lock parsial hanya dipakai di HP tanpa akselerometer *wake-up*, selama layanan langkah berjalan. | Tidak diminta ke pengguna. Pengingat tidak memakai syarat jaringan. |
| `<package>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | Ditambahkan androidx.core untuk melindungi receiver internal | Tidak diminta |

## Komponen yang bisa dibuka app lain

- `MainActivity`: launcher biasa. Tidak membaca data dari intent.
- `HealthPermissionsRationaleActivity` dan alias `ViewPermissionUsageActivity`: diwajibkan Health Connect untuk menampilkan penjelasan privasi. Hanya menampilkan teks; alias dilindungi izin sistem `START_VIEW_PERMISSION_USAGE`.
- Komponen library yang diekspor (dicek dari manifest hasil merge): `SystemJobService` WorkManager (dilindungi `BIND_JOB_SERVICE`), `DiagnosticsReceiver` WorkManager dan `ProfileInstallReceiver` (dilindungi `DUMP`, hanya sistem/adb), serta `HealthDataSdkService`, yang berasal dari library resmi Health Connect (bukan kode VitQuest) dan belum ditinjau lebih dalam di review ini.
- `StepTrackingService`, `StepTrackingBootReceiver`, dan `FileProvider` (foto kamera) **tidak diekspor**. FileProvider hanya membagikan `cache/camera/` dan izin URI diberikan sementara ke aplikasi kamera.
- `PendingIntent` notifikasi memakai `FLAG_IMMUTABLE` dan intent eksplisit ke `MainActivity`.

## Log dan rahasia

- Kode app tidak memakai `android.util.Log`, `println`, `printStackTrace`, atau `System.out/err`. `PrivacyGuardTest` membaca seluruh source dan gagal jika ada.
- Tidak ada API key, password, atau keystore di repository. Secret CI: `VITQUEST_DEBUG_KEYSTORE_BASE64` (keystore **debug**, bukan kunci rilis, di-decode ke folder sementara runner dan tidak dicetak), serta `VITQUEST_SUPABASE_URL` dan `VITQUEST_SUPABASE_PUBLISHABLE_KEY`.
- URL dan *publishable key* Supabase dimasukkan ke APK dan **memang publik**. Key ini hanya bisa login anonim dan memanggil yang diizinkan aturan database: tabel kuota dikunci Row Level Security tanpa policy, dan fungsi `consume_food_photo_quota` hanya bisa dipanggil pengguna yang login dan hanya menambah hitungannya sendiri.
- **API key Gemini hanya ada di Secrets Supabase** (server). *Secret key* / `service_role` Supabase tidak dipakai dan tidak pernah dimasukkan ke app.
- Penyalahgunaan dibatasi: batas foto per pengguna per hari dan total per hari (dikunci maks. 100 dan 1000 di SQL), dihitung **sebelum** Gemini dipanggil; ukuran dan jenis gambar dicek di server. Hasil Gemini dicek ulang di server dan di HP (maks. 15 item, 1–3000 g, 0–5000 kcal).
- Workflow CI memakai `permissions: contents: read`, dan setiap action dikunci ke commit SHA.

## Pemulihan cadangan

File cadangan diperlakukan sebagai input yang tidak dipercaya:

- Ukuran dibatasi 64 MB (termasuk salinan foto); file yang bukan JSON VitQuest ditolak.
- Nama tabel dan kolom dicocokkan dengan skema database yang ada sebelum dipakai di SQL; nilai dimasukkan lewat parameter (`ContentValues`), bukan disambung ke teks SQL.
- Semua penggantian berjalan dalam **satu transaksi**. Foreign key dicek sebelum commit (`PRAGMA foreign_key_check`). Jika ada yang salah, transaksi dibatalkan dan **data di HP tidak berubah** (`BackupRepositoryTest`).
- Cadangan dari versi app yang lebih lama dibangun ulang dari skema versi itu (disertakan di APK) lalu dimigrasi dengan migrasi yang sama seperti update app. Cadangan dari versi yang lebih baru ditolak.
- Pengaturan dari file hanya diterapkan jika nilainya dikenal versi ini.
- Salinan foto di file hanya diterima jika path-nya persis `food_photos/<id>.jpg` dan `<id>` dirujuk catatan di file yang sama; path lain (misalnya `../databases/...`) atau isi yang bukan Base64 membuat file dianggap rusak. Foto ditulis ke folder sementara dulu dan baru dipindah setelah transaksi database berhasil (`BackupRepositoryTest`).

## Temuan review ini dan perbaikannya

| Temuan | Status |
|---|---|
| Notifikasi air/gerak menampilkan jumlah air dan langkah, juga di layar kunci | Diperbaiki: visibilitas *private* + versi publik tanpa angka, ada test-nya |
| Teks privasi dan dialog "Hapus semua data" di Pengaturan hanya menyebut data Fase 1–3 | Diperbaiki: sekarang menyebut semua jenis data, termasuk latihan, kebiasaan, pengingat, dan foto makanan |
| Fitur foto butuh internet, padahal app sebelumnya tanpa internet | Diterima atas permintaan pemilik app, dibatasi: hanya foto yang dipilih, setelah persetujuan, hanya HTTPS, kode jaringan hanya di `core/ai/` (dicek `PrivacyGuardTest`) |
| Foto diproses Gemini tingkat gratis, yang boleh dipakai Google | Diterima atas keputusan pemilik app ("hanya foto makanan"); dijelaskan di layar persetujuan dan di `docs/AI_SETUP.md` |
| Login anonim bisa dibuat berulang oleh skrip untuk menghabiskan kuota Gemini | Dibatasi dengan batas total per hari di server dan batas laju login anonim bawaan Supabase. Belum ada CAPTCHA atau Play Integrity. |
| Tidak ada cara memindahkan data ke HP baru selain transfer bawaan HP (cadangan cloud sengaja dimatikan) | Ditambahkan ekspor/impor file cadangan lokal |
| Tidak ada pengaman otomatis untuk izin baru dari library atau log yang tidak sengaja | Ditambahkan `PrivacyGuardTest` |
| APK debug bisa dibaca lewat `adb run-as` | Diterima untuk build uji; dicatat di atas. Distribusi ke pengguna lain sebaiknya memakai build rilis bertanda tangan (lihat `docs/RELEASE.md`). |
| File cadangan tidak dienkripsi | Diterima untuk versi ini, dengan peringatan jelas di layar. Enkripsi dengan kata sandi bisa ditambahkan nanti. |
| Layar tidak diblokir dari screenshot (`FLAG_SECURE` tidak dipakai) | Disengaja, agar pengguna bisa menyimpan/berbagi progres sendiri. |

## Cloud sync

**Belum ada dan tidak dibuat tanpa persetujuan eksplisit.** Fitur foto makanan bukan sync: tidak ada catatan yang dikirim atau disimpan di server. Jika sync nanti disetujui, perlu diputuskan dan didesain dulu: penyedia backend (misalnya Supabase), autentikasi, otorisasi per pengguna (misalnya Row Level Security), tidak ada service-role key di APK, penanganan konflik antar-perangkat, retry, enkripsi saat transit, penghapusan data di server, dan pembaruan dokumen ini serta teks privasi di app. Kode sync juga harus ditambahkan ke pengecualian jaringan di `PrivacyGuardTest`, yang akan gagal sampai keputusan itu dicatat.
