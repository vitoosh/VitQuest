# Kesiapan rilis VitQuest

Status saat ini: **belum ada rilis**. Pengujian memakai APK debug dari GitHub Actions (lihat README). Build rilis bertanda tangan dan distribusinya adalah tugas terpisah yang butuh persetujuan eksplisit; dokumen ini menjelaskan apa yang sudah siap dan langkah yang akan dipakai nanti.

## Yang sudah disiapkan

| Hal | Status |
|---|---|
| Build rilis dengan R8 (shrink kode dan resource) | Aktif. CI menjalankan `assembleRelease` di setiap push untuk memastikan R8 tidak merusak build. |
| APK rilis | **Tidak bertanda tangan, tidak bisa diinstall, dan sengaja tidak diunggah** sebagai artifact. |
| Kelas yang dimuat lewat refleksi (Room `_Impl`, worker pengingat, activity) | Dicek ada di mapping R8 build lokal. Library yang dipakai membawa aturan R8 sendiri; `app/proguard-rules.pro` sengaja kosong. |
| Test unit, Robolectric, lint | Berjalan di CI untuk setiap push. |
| Test instrumented (migrasi dengan `MigrationTestHelper`, Accessibility Test Framework) | Dikompilasi di CI. **Belum pernah dijalankan**, karena butuh HP/emulator: `./gradlew connectedDebugAndroidTest`. |
| `versionCode` | Nomor run GitHub Actions; `versionName` diubah manual di `app/build.gradle.kts`. |

Belum diverifikasi: APK rilis hasil R8 belum pernah dijalankan di perangkat. Jalankan daftar uji di bawah memakai build rilis bertanda tangan sebelum dibagikan.

## Proses tanda tangan rilis (setelah disetujui)

Jangan pernah meng-commit keystore, password, atau file `*.jks`/`*.keystore`.

1. Buat keystore rilis di komputer sendiri (sekali saja, simpan cadangannya di tempat aman; kehilangan keystore berarti update tidak bisa diinstall di atas versi lama):
   ```bash
   keytool -genkeypair -v -keystore vitquest-release.jks -alias vitquest \
     -keyalg RSA -keysize 4096 -validity 10000
   ```
2. Simpan di **GitHub → Settings → Secrets and variables → Actions**:
   - `VITQUEST_RELEASE_KEYSTORE_BASE64` (isi `base64 -w0 vitquest-release.jks`)
   - `VITQUEST_RELEASE_STORE_PASSWORD`
   - `VITQUEST_RELEASE_KEY_ALIAS`
   - `VITQUEST_RELEASE_KEY_PASSWORD`
3. Tambahkan `signingConfigs.release` yang membaca nilai dari environment variable (tidak pernah dari file di repo), dan pakai hanya jika semua variabel ada.
4. Buat job workflow terpisah yang hanya berjalan lewat `workflow_dispatch` atau tag versi, memakai **GitHub Environment dengan required reviewer**, sehingga setiap build bertanda tangan butuh persetujuan manual.
5. Verifikasi hasilnya dengan `apksigner verify --print-certs` dan simpan sidik jari sertifikatnya.
6. Artifact rilis disimpan dengan masa simpan singkat; publikasi (GitHub Release atau toko aplikasi) tetap langkah manual yang disetujui terpisah.

Catatan: APK rilis memakai kunci yang berbeda dari APK debug, jadi tidak bisa diinstall di atas APK debug. Pindahkan data dengan **Pengaturan → Cadangan → Ekspor**, uninstall APK debug, install rilis, lalu **Pulihkan**.

Jika suatu saat dipublikasikan di Google Play: Health Connect mewajibkan deklarasi izin dan kebijakan privasi di Play Console, dan ada formulir Data Safety. Isi `docs/SECURITY_PRIVACY.md` bisa menjadi dasarnya.

## Daftar uji di perangkat sebelum rilis

Belum ada satu pun yang dijalankan di HP sungguhan. Untuk POCO X8 Pro (HyperOS):

- [ ] Install bersih dan update di atas versi sebelumnya dengan data asli (migrasi database).
- [ ] Onboarding, edit profil, ganti bahasa (Indonesia/Inggris), tema terang/gelap.
- [ ] Health Connect: izin diberikan, ditolak, dicabut; sensor HP saat izin aktivitas ditolak; input manual.
- [ ] Catat makanan, air, berat, latihan (pemutar latihan sampai selesai dan berhenti di tengah), kebiasaan.
- [ ] XP bertambah sekali per aktivitas; menghapus aktivitas menambah baris koreksi.
- [ ] Hadiah: tambah, wishlist, tabungan, "Sudah didapat" dengan dan tanpa harga, hapus catatan.
- [ ] Pengingat di jam aktif dan jam tenang, dengan penghemat baterai HyperOS aktif dan tidak; setelah restart HP.
- [ ] Notifikasi di layar kunci dengan "sembunyikan konten sensitif".
- [ ] Ekspor cadangan ke penyimpanan HP dan ke Google Drive; pulihkan di HP lain; coba file yang salah.
- [ ] Hapus semua data.
- [ ] TalkBack di layar utama, ukuran font terbesar, tampilan layar kecil.
- [ ] `./gradlew connectedDebugAndroidTest` di HP atau emulator.
- [ ] Build rilis bertanda tangan: semua langkah di atas sekali lagi (cek efek R8).
