# TXD Studio (Android)
Created By Tutorial All Games

Aplikasi Android untuk mengonversi PNG ke file `.txd`. **Khusus Android** (tidak ada pilihan platform lain).

## Fitur
- Impor banyak PNG sekaligus (otomatis dibulatkan ke pangkat dua, maks 2048)
- Format per gambar: **DXT1, DXT3, DXT5, 8888**
- **Dengan alpha** (centang) / **tanpa alpha** (kosongkan), diatur per gambar
  - Otomatis tercentang jika PNG punya piksel transparan
  - Tanpa alpha: DXT3/DXT5 dinonaktifkan (otomatis DXT1), 8888 menjadi 888
  - DXT1 + alpha memakai alpha 1-bit
- Mipmap on/off, batas resolusi maksimal (256 / 512 / 1024 / 2048)
- Nama tekstur otomatis dibuat unik, ukuran file diperkirakan di layar pengaturan
- Proses impor dan simpan berjalan di latar (UI tidak macet)

## Build APK
**Android Studio:** File > Open folder ini > tunggu sync > Build > Build APK(s).
**GitHub (tanpa PC):** tab Actions > "Build APK" > Run workflow (juga jalan otomatis tiap push) >
unduh artifact `TXD-Studio-debug` (isinya app-debug.apk).

## Status / belum ada
- Pembaca TXD (tombol "Buka file TXD" masih placeholder)
- Format khusus GPU (PVR / ETC) belum didukung
