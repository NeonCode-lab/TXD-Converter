<div align="center">

# 🎨 TXD Studio

### Ubah PNG jadi file `.txd` langsung dari HP Android ✨

![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![minSdk](https://img.shields.io/badge/minSdk-24-FF8A3D?style=for-the-badge)

**Created with ♡ by Tutorial All Games**

[![YouTube](https://img.shields.io/badge/YouTube-kri--sky09-FF0000?style=for-the-badge&logo=youtube&logoColor=white)](https://www.youtube.com/@kri-sky09)
[![TikTok](https://img.shields.io/badge/TikTok-Neoncode-000000?style=for-the-badge&logo=tiktok&logoColor=white)](https://www.tiktok.com/@neoncode)

</div>

---

## 🌸 Tentang

**TXD Studio** adalah aplikasi Android ringan untuk mengonversi gambar **PNG** menjadi tekstur **TXD**, dan membaca isi file TXD.
Semua diatur per gambar, langsung di layar HP, tanpa perlu PC.

---

## ✨ Fitur

| | Fitur | Keterangan |
|---|---|---|
| 🖼️ | **Impor banyak PNG** | Pilih beberapa gambar sekaligus |
| 📐 | **Auto pangkat dua** | Ukuran dibulatkan otomatis (maks 2048) |
| 🧩 | **4 format** | DXT1, DXT3, DXT5, dan 8888 |
| 🫧 | **Alpha per gambar** | Centang = dengan alpha, kosongkan = tanpa alpha |
| 🪞 | **Mipmap** | Hidupkan atau matikan per gambar |
| 📏 | **Batas resolusi** | 256, 512, 1024, atau 2048 |
| 🏷️ | **Nama otomatis unik** | Nama kembar dicegah dan diberi peringatan |
| ⚡ | **Proses di latar** | Impor dan simpan tidak membuat layar macet |
| 📦 | **Estimasi ukuran** | Perkiraan KB tampil di layar pengaturan |
| 📖 | **Pembaca TXD** | Buka file `.txd`, lihat daftar tekstur, pratinjau, dan info format |
| 🔔 | **Notifikasi update** | Muncul otomatis saat ada versi baru, tinggal ketuk *Perbarui* |

---

## 🫧 Cara kerja opsi Alpha

Tinggal **centang** atau **kosongkan** kotak *"Dengan alpha"* di pengaturan tiap gambar.

| Kotak alpha | DXT1 | DXT3 / DXT5 | 8888 |
|---|---|---|---|
| ✅ **Dicentang** | Alpha 1-bit (transparan/tidak) | Alpha penuh | 32-bit beralpha |
| ⬜ **Kosong** | Tanpa alpha (paling kecil) | *Dinonaktifkan* | Jadi **888** (tanpa alpha) |

> 💡 **Tips:** Kotak alpha otomatis tercentang kalau PNG-mu punya bagian transparan.

---

## 📱 Cara pakai

1. 🏠 Buka aplikasi, lalu ketuk **Impor PNG**
2. 🖼️ Pilih satu atau beberapa gambar
3. 🎛️ Ketuk gambar untuk mengatur **nama, alpha, format, mipmap, resolusi**
4. 💾 Ketuk **Simpan TXD**, lalu pilih lokasi penyimpanan
5. 🎉 Selesai!

### 📖 Membaca file TXD

1. Di layar utama ketuk **Buka file TXD**, lalu pilih file `.txd`
2. Semua tekstur tampil sebagai kartu dengan pratinjau
3. Ketuk satu tekstur untuk melihat pratinjau besar, ukuran, format, alpha, mipmap, dan ukuran data

---

## 🔧 Build APK

<details>
<summary><b>📲 Lewat GitHub (tanpa PC)</b></summary>

1. Buka tab **Actions**
2. Pilih **Build APK** lalu **Run workflow** (juga jalan otomatis tiap push)
3. Tunggu centang hijau ✅
4. Unduh artifact **`TXD-Studio-debug`**, isinya `app-debug.apk`

</details>

<details>
<summary><b>💻 Lewat Android Studio</b></summary>

1. **File → Open**, pilih folder proyek ini
2. Tunggu Gradle sync selesai
3. **Build → Build APK(s)**

</details>

<details>
<summary><b>🔔 Merilis update ke pengguna</b></summary>

1. Naikkan `versionCode` dan `versionName` di `app/build.gradle.kts`
2. Build APK, unggah, lalu ambil tautan unduhannya
3. Di `update.json` (cabang `main`), naikkan `versionCode`/`versionName`, isi `message` dan `url`
4. Pengguna yang membuka aplikasi akan melihat dialog *Update tersedia*

> ⚠️ Agar update bisa menimpa versi lama, setiap APK harus ditandatangani dengan kunci yang sama. Lihat bagian *Tanda tangan APK* di bawah.

</details>

<details>
<summary><b>🔑 Tanda tangan APK yang stabil</b></summary>

Secara bawaan GitHub Actions membuat kunci debug baru di tiap build, sehingga APK berikutnya tidak bisa menimpa APK lama. Supaya stabil, gunakan satu keystore yang sama di setiap build. `app/build.gradle.kts` sudah siap membaca keystore dari environment variable `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, dan `KEY_PASSWORD`. Kalau variabel itu tidak ada, build tetap jalan memakai kunci debug bawaan.

</details>

---

## 🗂️ Struktur proyek

```
TXD-Converter/
├── 📁 .github/workflows/build.yml   ← build APK otomatis
├── 📄 update.json                  ← info versi terbaru untuk notifikasi update
└── 📁 app/src/main/
    ├── 📄 AndroidManifest.xml
    ├── 📁 res/                     ← ikon aplikasi
    └── 📁 java/com/tutorialallgames/txdstudio/
        ├── 📄 MainActivity.kt       ← tampilan & alur aplikasi
        ├── 📄 Tex.kt                ← model tekstur & format
        ├── 📄 TxdWriter.kt          ← penulis file TXD
        ├── 📄 TxdReader.kt          ← pembaca file TXD
        ├── 📄 TxdViewer.kt          ← layar daftar & detail hasil baca TXD
        └── 📄 UpdateChecker.kt      ← notifikasi & cek update
```

---

## 🛠️ Dibangun dengan

- 🟣 **Kotlin 2.0**
- 🎨 **Jetpack Compose** + **Material 3**
- 🤖 **Android Gradle Plugin 8.7**

---

## 🚧 Belum tersedia

- [ ] 🎮 Format khusus GPU (PVR / ETC), baik untuk menulis maupun membaca
- [ ] 🎨 Membaca TXD berpalet (8/4-bit) dan cubemap
- [ ] ✏️ Mengedit TXD yang dibuka (saat ini hanya dibaca)

---
## 🛑Link video cara pakai app
https://youtu.be/QouH8acI6-E?si=2m18RhHFNOWPMukW

## ❣️Link download
https://sfl.gl/Bksi


<div align="center">

**Dibuat dengan ♡ oleh Tutorial All Games**

📺 YouTube: [kri-sky09](https://www.youtube.com/@kri-sky09) &nbsp;·&nbsp; 🎵 TikTok: [Neoncode](https://www.tiktok.com/@neoncode)

⭐ Kalau suka, kasih bintang ya! ⭐

</div>
