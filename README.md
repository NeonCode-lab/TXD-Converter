<div align="center">

# 🎨 TXD Studio

### Ubah PNG jadi file `.txd` langsung dari HP Android ✨

![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![minSdk](https://img.shields.io/badge/minSdk-24-FF8A3D?style=for-the-badge)

**Created with ♡ by Tutorial All Games**

</div>

---

## 🌸 Tentang

**TXD Studio** adalah aplikasi Android ringan untuk mengonversi gambar **PNG** menjadi tekstur **TXD**.
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
└── 📁 app/src/main/
    ├── 📄 AndroidManifest.xml
    └── 📁 java/com/tutorialallgames/txdstudio/
        ├── 📄 MainActivity.kt       ← tampilan & alur aplikasi
        ├── 📄 Tex.kt                ← model tekstur & format
        └── 📄 TxdWriter.kt          ← penulis file TXD
```

---

## 🛠️ Dibangun dengan

- 🟣 **Kotlin 2.0**
- 🎨 **Jetpack Compose** + **Material 3**
- 🤖 **Android Gradle Plugin 8.7**

---

## 🚧 Belum tersedia

- [ ] 📖 Pembaca file TXD (tombol *Buka file TXD* masih placeholder)
- [ ] 🎮 Format khusus GPU (PVR / ETC)

---

## ❣️Link download
https://sfl.gl/yLlYEoAN


<div align="center">

**Dibuat dengan ♡ oleh Tutorial All Games**

⭐ Kalau suka, kasih bintang ya! ⭐

</div>
