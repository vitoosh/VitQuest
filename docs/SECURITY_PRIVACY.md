# Review keamanan dan privasi VitQuest

Review untuk Fase 6 (database v6, tanpa cloud sync). Review ini dilakukan dengan membaca kode, manifest hasil merge, dan konfigurasi build/CI, ditambah test otomatis yang disebutkan di bawah. **Belum ada pengujian penetrasi atau pengujian di HP sungguhan.**

## Ringkasan

- VitQuest **tidak punya izin `INTERNET`**, jadi app tidak bisa mengirim data ke server mana pun. Ini dicek otomatis di setiap build (`PrivacyGuardTest`), termasuk izin yang ditambahkan library.
- Semua data disimpan di penyimpanan privat app di HP (Room/SQLite dan DataStore).
- Data app **tidak ikut cadangan cloud Google**. Pindah ke HP baru bisa lewat transfer langsung antar-perangkat (jika didukung HP), atau lewat **file cadangan** yang diekspor sendiri dari Pengaturan.
- Health Connect hanya dipakai untuk **membaca total langkah harian**. VitQuest tidak menulis ke Health Connect.
- Tidak ada akun, login, iklan, analitik, atau pelaporan crash ke pihak ketiga.

## Data yang disimpan di HP

| Data | Tempat | Catatan |
|---|---|---|
| Profil, target, langkah, makanan, resep, rencana menu, belanja, berat, ukuran tubuh, air, latihan, kebiasaan, ledger XP, hadiah, riwayat hadiah, anggaran, pengaturan pengingat | Room (`vitquest.db`) di penyimpanan privat app | Hanya bisa dibaca app ini (sandbox Android). Saat HP terkunci, isi file dilindungi enkripsi bawaan Android. |
| Tema, status onboarding, pilihan sumber langkah, anggaran bulanan, versi data bawaan | DataStore di penyimpanan privat app | |
| Checkpoint sensor langkah | Room (`step_sensor_state`) | Khusus perangkat dan sesi boot ini; tidak ikut file cadangan. |
| Bahasa app | Pengaturan bahasa per-app Android | Tidak ikut file cadangan. |

App tidak menambahkan enkripsi sendiri di atas enkripsi Android. Di HP yang sudah di-root, atau pada **APK debug** lewat `adb shell run-as`, data app bisa dibaca orang yang memegang HP dalam keadaan terbuka. APK rilis (belum didistribusikan) tidak `debuggable`.

## Apa yang keluar dari HP

| Jalur | Isi | Kendali pengguna |
|---|---|---|
| Internet | Tidak ada (tidak ada izin `INTERNET`) | — |
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
| `POST_NOTIFICATIONS` | Pengingat | Dari layar Pengingat (Android 13+) |
| `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`, `ACCESS_NETWORK_STATE` | Ditambahkan WorkManager (penjadwal pengingat) | Tidak diminta ke pengguna. VitQuest tidak memakai syarat jaringan dan tidak punya akses internet. |
| `<package>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | Ditambahkan androidx.core untuk melindungi receiver internal | Tidak diminta |

## Komponen yang bisa dibuka app lain

- `MainActivity`: launcher biasa. Tidak membaca data dari intent.
- `HealthPermissionsRationaleActivity` dan alias `ViewPermissionUsageActivity`: diwajibkan Health Connect untuk menampilkan penjelasan privasi. Hanya menampilkan teks; alias dilindungi izin sistem `START_VIEW_PERMISSION_USAGE`.
- Komponen library yang diekspor (dicek dari manifest hasil merge): `SystemJobService` WorkManager (dilindungi `BIND_JOB_SERVICE`), `DiagnosticsReceiver` WorkManager dan `ProfileInstallReceiver` (dilindungi `DUMP`, hanya sistem/adb), serta `HealthDataSdkService`, yang berasal dari library resmi Health Connect (bukan kode VitQuest) dan belum ditinjau lebih dalam di review ini.
- `PendingIntent` notifikasi memakai `FLAG_IMMUTABLE` dan intent eksplisit ke `MainActivity`.

## Log dan rahasia

- Kode app tidak memakai `android.util.Log`, `println`, `printStackTrace`, atau `System.out/err`. `PrivacyGuardTest` membaca seluruh source dan gagal jika ada.
- Tidak ada API key, password, atau keystore di repository. Satu-satunya secret CI adalah `VITQUEST_DEBUG_KEYSTORE_BASE64` (keystore **debug**, bukan kunci rilis), di-decode ke folder sementara runner dan tidak dicetak.
- Workflow CI memakai `permissions: contents: read`, dan setiap action dikunci ke commit SHA.

## Pemulihan cadangan

File cadangan diperlakukan sebagai input yang tidak dipercaya:

- Ukuran dibatasi 32 MB; file yang bukan JSON VitQuest ditolak.
- Nama tabel dan kolom dicocokkan dengan skema database yang ada sebelum dipakai di SQL; nilai dimasukkan lewat parameter (`ContentValues`), bukan disambung ke teks SQL.
- Semua penggantian berjalan dalam **satu transaksi**. Foreign key dicek sebelum commit (`PRAGMA foreign_key_check`). Jika ada yang salah, transaksi dibatalkan dan **data di HP tidak berubah** (`BackupRepositoryTest`).
- Cadangan dari versi app yang lebih lama dibangun ulang dari skema versi itu (disertakan di APK) lalu dimigrasi dengan migrasi yang sama seperti update app. Cadangan dari versi yang lebih baru ditolak.
- Pengaturan dari file hanya diterapkan jika nilainya dikenal versi ini.

## Temuan review ini dan perbaikannya

| Temuan | Status |
|---|---|
| Notifikasi air/gerak menampilkan jumlah air dan langkah, juga di layar kunci | Diperbaiki: visibilitas *private* + versi publik tanpa angka, ada test-nya |
| Teks privasi dan dialog "Hapus semua data" di Pengaturan hanya menyebut data Fase 1–3 | Diperbaiki: sekarang menyebut semua jenis data, termasuk latihan, kebiasaan, XP, hadiah, anggaran, dan pengingat |
| Tidak ada cara memindahkan data ke HP baru selain transfer bawaan HP (cadangan cloud sengaja dimatikan) | Ditambahkan ekspor/impor file cadangan lokal |
| Tidak ada pengaman otomatis untuk izin baru dari library atau log yang tidak sengaja | Ditambahkan `PrivacyGuardTest` |
| APK debug bisa dibaca lewat `adb run-as` | Diterima untuk build uji; dicatat di atas. Distribusi ke pengguna lain sebaiknya memakai build rilis bertanda tangan (lihat `docs/RELEASE.md`). |
| File cadangan tidak dienkripsi | Diterima untuk versi ini, dengan peringatan jelas di layar. Enkripsi dengan kata sandi bisa ditambahkan nanti. |
| Layar tidak diblokir dari screenshot (`FLAG_SECURE` tidak dipakai) | Disengaja, agar pengguna bisa menyimpan/berbagi progres sendiri. |

## Cloud sync

**Belum ada dan tidak dibuat tanpa persetujuan eksplisit.** Jika nanti disetujui, perlu diputuskan dan didesain dulu: penyedia backend (misalnya Supabase), autentikasi, otorisasi per pengguna (misalnya Row Level Security), tidak ada service-role key di APK, penanganan konflik antar-perangkat, retry, enkripsi saat transit, penghapusan data di server, dan pembaruan dokumen ini serta teks privasi di app. Menambah sync juga berarti menambah izin `INTERNET`, yang akan membuat `PrivacyGuardTest` gagal sampai keputusan itu dicatat.
