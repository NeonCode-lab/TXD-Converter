<div align="center">

# 🎨 TXD Studio

### Ubah PNG jadi `.txd`, baca isi `.txd`, dan ekspor ke ZIP, semuanya dari HP Android ✨

![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![minSdk](https://img.shields.io/badge/minSdk-24-FF8A3D?style=for-the-badge)
![Versi](https://img.shields.io/badge/Versi-1.2.0-FF8A3D?style=for-the-badge)

**Created with ♡ by Tutorial All Games**

[![YouTube](https://img.shields.io/badge/YouTube-kri--sky09-FF0000?style=for-the-badge&logo=youtube&logoColor=white)](https://www.youtube.com/@kri-sky09)
[![TikTok](https://img.shields.io/badge/TikTok-Neoncode-000000?style=for-the-badge&logo=tiktok&logoColor=white)](https://www.tiktok.com/@neoncode)

</div>

---

## 🌸 Tentang

**TXD Studio** adalah aplikasi Android ringan untuk bekerja dengan tekstur **TXD**:

- 🖼️ **Konversi** gambar PNG menjadi file TXD
- 📖 **Baca** isi file TXD yang sudah ada
- 📦 **Ekspor** ke file ZIP, dengan PNG yang disortir otomatis ke folder *with alpha* dan *without alpha*

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
| 📖 | **Pembaca TXD** | Buka file TXD, lihat daftar tekstur beserta pratinjau dan detailnya |
| 📦 | **Ekspor ZIP PNG** | PNG dari TXD disortir otomatis ke `with_alpha` dan `without_alpha` |
| 🗜️ | **Simpan ZIP dari editor** | Satu ZIP berisi dua TXD: with alpha dan without alpha |
| 🔔 | **Cek update** | Notifikasi update dan tombol cek manual di layar Tentang |
| ⚡ | **Proses di latar** | Impor, baca, dan simpan tidak membuat layar macet |
| 📏 | **Estimasi ukuran** | Perkiraan KB tampil di layar pengaturan |

---

## 🫧 Cara kerja opsi Alpha

Tinggal **centang** atau **kosongkan** kotak *"Dengan alpha"* di pengaturan tiap gambar.

| Kotak alpha | DXT1 | DXT3 / DXT5 | 8888 |
|---|---|---|---|
| ✅ **Dicentang** | Alpha 1-bit (transparan/tidak) | Alpha penuh | 32-bit beralpha |
| ⬜ **Kosong** | Tanpa alpha (paling kecil) | *Dinonaktifkan* | Jadi **888** (tanpa alpha) |

> 💡 **Tips:** Kotak alpha otomatis tercentang kalau PNG-mu punya bagian transparan.

---

## 📦 Ekspor ZIP

### Dari pembaca TXD (PNG disortir otomatis)

Buka file TXD, lalu ketuk **Ekspor ZIP**. Tiap tekstur diekspor sebagai PNG ukuran penuh dan masuk ke **satu** folder saja:

```
textures_png.zip
├── 📁 with_alpha/       ← tekstur yang benar-benar punya bagian transparan
│   └── NAMA.png
└── 📁 without_alpha/    ← tekstur tanpa alpha (atau alpha-nya penuh opaque)
    └── NAMA.png
```

### Dari editor (dua versi TXD)

Ketuk **Simpan ZIP** di editor untuk mendapat satu ZIP berisi dua file TXD:

```
textures.zip
├── 📁 with_alpha/textures.txd
└── 📁 without_alpha/textures.txd
```

---

## 📱 Cara pakai

### Konversi PNG ke TXD

1. 🏠 Buka aplikasi, lalu ketuk **Impor PNG**
2. 🖼️ Pilih satu atau beberapa gambar
3. 🎛️ Ketuk gambar untuk mengatur **nama, alpha, format, mipmap, resolusi**
4. 💾 Ketuk **Simpan TXD** (atau **Simpan ZIP**), lalu pilih lokasi penyimpanan
5. 🎉 Selesai!

### Baca dan ekspor TXD

1. 📖 Di beranda, ketuk **Buka file TXD** dan pilih file `.txd`
2. 🔍 Ketuk tekstur untuk melihat pratinjau dan detailnya
3. 📦 Ketuk **Ekspor ZIP** untuk menyimpan semua PNG, sudah disortir ke `with_alpha` dan `without_alpha`

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

---

## 🗂️ Struktur proyek

```
TXD-Converter/
├── 📁 .github/workflows/build.yml   ← build APK otomatis
├── 📄 update.json                   ← info versi untuk notifikasi update
└── 📁 app/src/main/
    ├── 📄 AndroidManifest.xml
    ├── 📁 res/                     ← ikon aplikasi
    └── 📁 java/com/tutorialallgames/txdstudio/
        ├── 📄 MainActivity.kt       ← tampilan & alur aplikasi
        ├── 📄 Tex.kt                ← model tekstur & format
        ├── 📄 TxdWriter.kt          ← penulis file TXD
        ├── 📄 TxdReader.kt          ← pembaca file TXD
        ├── 📄 TxdViewer.kt          ← layar daftar & detail TXD
        ├── 📄 ZipExport.kt          ← ekspor ZIP (with/without alpha)
        └── 📄 UpdateChecker.kt      ← cek update
```

---

## 🛠️ Dibangun dengan

- 🟣 **Kotlin 2.0**
- 🎨 **Jetpack Compose** + **Material 3**
- 🤖 **Android Gradle Plugin 8.7**

---

## 🚧 Belum tersedia

- [ ] 🎨 Pembaca untuk TXD berpalet (8/4-bit) dan cubemap
- [ ] 🎮 Format khusus GPU (PVR / ETC)

---
## 🛑Link video cara pakai app
https://youtube.com/shorts/g8K2lDD75SQ?si=N7RGH0Wo9DYhzKpb

## ❣️Link download
https://sfl.gl/cbVHx


<div align="center">

**Dibuat dengan ♡ oleh Tutorial All Games**

📺 YouTube: [kri-sky09](https://www.youtube.com/@kri-sky09) &nbsp;·&nbsp; 🎵 TikTok: [Neoncode](https://www.tiktok.com/@neoncode)

⭐ Kalau suka, kasih bintang ya! ⭐

</div>
