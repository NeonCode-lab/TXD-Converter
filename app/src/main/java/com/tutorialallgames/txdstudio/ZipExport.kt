package com.tutorialallgames.txdstudio

import android.graphics.Bitmap
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Ekspor ZIP berisi dua versi: dengan alpha dan tanpa alpha.
 *  - Editor : ZIP berisi dua file TXD (with_alpha/textures.txd dan without_alpha/textures.txd)
 *  - Pembaca: ZIP berisi PNG tiap tekstur (with_alpha/NAMA.png dan without_alpha/NAMA.png)
 */
object ZipExport {
    private const val OPAQUE = -0x1000000 // 0xFF000000

    /** alpha = true: alpha dipakai (DXT1 dinaikkan ke DXT5). alpha = false: semua piksel opaque. */
    private fun variant(t: Tex, alpha: Boolean): Tex =
        if (alpha) t.copy(alpha = true, format = if (t.format == TexFormat.DXT1) TexFormat.DXT5 else t.format)
        else t.copy(alpha = false)

    /** Dipanggil di thread latar. Menulis dua TXD ke dalam satu ZIP, satu per satu supaya hemat memori. */
    fun writeTxdZip(list: List<Tex>, out: OutputStream) {
        require(list.isNotEmpty()) { "Tidak ada tekstur" }
        ZipOutputStream(out).use { zip ->
            zip.setLevel(1)
            for ((folder, alpha) in listOf("with_alpha" to true, "without_alpha" to false)) {
                zip.putNextEntry(ZipEntry("$folder/textures.txd"))
                TxdWriter.write(list.map { variant(it, alpha) }, zip)
                zip.closeEntry()
            }
        }
    }

    private fun safe(name: String): String =
        name.map { if (it.isLetterOrDigit() || it == '_' || it == '-' || it == '.') it else '_' }
            .joinToString("").ifEmpty { "texture" }

    private fun unique(base: String, used: MutableSet<String>): String {
        var n = base
        var i = 2
        while (!used.add(n.lowercase())) { n = "${base}_$i"; i++ }
        return n
    }

    private fun usable(f: TxdFile, l: Level, fmt: Fmt): Boolean {
        val need = TxdReader.needBytes(l.w, l.h, fmt)
        return l.size >= need && l.w.toLong() * l.h <= TxdReader.MAX_PIXELS && l.off.toLong() + need <= f.data.size
    }

    private fun putPng(zip: ZipOutputStream, name: String, px: IntArray, w: Int, h: Int) {
        val bm = Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
        try {
            zip.putNextEntry(ZipEntry(name))
            bm.compress(Bitmap.CompressFormat.PNG, 100, zip)
            zip.closeEntry()
        } finally {
            bm.recycle()
        }
    }

    /**
     * Dipanggil di thread latar. Menulis PNG ukuran penuh tiap tekstur yang bisa dibaca.
     * Mengembalikan (jumlah berhasil, jumlah dilewati). Gagal kalau tidak ada satu pun yang bisa diekspor.
     */
    fun writePngZip(f: TxdFile, out: OutputStream): Pair<Int, Int> {
        var ok = 0
        var skip = 0
        val used = HashSet<String>()
        ZipOutputStream(out).use { zip ->
            zip.setLevel(1)
            for (e in f.entries) {
                val fmt = e.fmt
                val lv = if (fmt == null) null else e.levels.firstOrNull { usable(f, it, fmt) }
                if (fmt == null || lv == null) { skip++; continue }
                try {
                    val px = TxdReader.decode(f.data, lv, fmt, true)
                    val name = unique(safe(e.name), used)
                    putPng(zip, "with_alpha/$name.png", px, lv.w, lv.h)
                    for (i in px.indices) px[i] = px[i] or OPAQUE
                    putPng(zip, "without_alpha/$name.png", px, lv.w, lv.h)
                    ok++
                } catch (t: Throwable) {
                    skip++
                }
            }
            if (ok == 0) throw TxdFormatException("Tidak ada tekstur yang bisa diekspor")
        }
        return ok to skip
    }
}
