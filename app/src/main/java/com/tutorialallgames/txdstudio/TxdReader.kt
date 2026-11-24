package com.tutorialallgames.txdstudio

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.InputStream

/** File yang dibuka bukan TXD yang valid / rusak. Pesannya aman ditampilkan ke pengguna. */
class TxdFormatException(message: String) : Exception(message)

/** Format piksel yang bisa dibaca (RenderWare D3D8/D3D9). */
enum class Fmt(val label: String, val block: Int, val bpp: Int) {
    DXT1("DXT1", 8, 0), DXT3("DXT3", 16, 0), DXT5("DXT5", 16, 0),
    ARGB8888("8888", 0, 4), XRGB8888("888", 0, 4), BGR888("888", 0, 3),
    RGB565("565", 0, 2), ARGB1555("1555", 0, 2), XRGB1555("555", 0, 2), ARGB4444("4444", 0, 2)
}

/** Satu level mipmap: ukuran piksel + posisi datanya di dalam file. */
class Level(val w: Int, val h: Int, val off: Int, val size: Int)

/**
 * Satu tekstur di dalam TXD. [fmt] null = format tidak didukung, alasannya ada di [note].
 * Piksel tidak di-decode di sini; decode dilakukan malas lewat [TxdFile.preview].
 */
class TxdEntry(
    val name: String,
    val w: Int,
    val h: Int,
    val fmt: Fmt?,
    val alpha: Boolean,
    val levels: List<Level>,
    val note: String?
) {
    val sizeLabel: String get() = if (w > 0 && h > 0) "${w}×${h}" else "?"
    val formatLabel: String get() = fmt?.label ?: "?"
    val dataBytes: Int get() = levels.sumOf { it.size }
}

/** Hasil baca TXD. [declared] = jumlah tekstur yang tertulis di header (bisa > entries kalau file terpotong). */
class TxdFile(val entries: List<TxdEntry>, val declared: Int, val data: ByteArray) {

    /** Dipanggil di thread latar. Mengembalikan null kalau tidak bisa di-decode. */
    fun preview(e: TxdEntry, maxSide: Int): Bitmap? = try {
        render(e, maxSide)
    } catch (t: Throwable) {
        null
    }

    private fun render(e: TxdEntry, maxSide: Int): Bitmap? {
        val f = e.fmt ?: return null
        fun usable(l: Level): Boolean =
            l.size >= TxdReader.needBytes(l.w, l.h, f) &&
                l.w.toLong() * l.h <= TxdReader.MAX_PIXELS &&
                l.off.toLong() + TxdReader.needBytes(l.w, l.h, f) <= data.size
        // Pakai level mipmap terkecil yang masih >= ukuran pratinjau, jadi decode ringan.
        val lv = e.levels.lastOrNull { maxOf(it.w, it.h) >= maxSide && usable(it) }
            ?: e.levels.firstOrNull { usable(it) }
            ?: return null
        val px = TxdReader.decode(data, lv, f, e.alpha)
        val bm = Bitmap.createBitmap(px, lv.w, lv.h, Bitmap.Config.ARGB_8888)
        val m = maxOf(lv.w, lv.h)
        if (m <= maxSide) return bm
        val sc = maxSide.toFloat() / m
        val nb = Bitmap.createScaledBitmap(bm, maxOf(1, (lv.w * sc).toInt()), maxOf(1, (lv.h * sc).toInt()), true)
        if (nb !== bm) bm.recycle()
        return nb
    }
}

/**
 * Pembaca TXD RenderWare (platform D3D8 dan D3D9, termasuk file buatan TxdWriter).
 * Format berpalet (8/4-bit), cubemap, dan format khusus GPU (PVR/ETC) ditampilkan di daftar
 * tetapi diberi catatan "belum didukung" dan tidak punya pratinjau.
 */
object TxdReader {
    private const val MAX_BYTES = 64 * 1024 * 1024
    private const val OPAQUE = -0x1000000 // 0xFF000000
    private const val FOURCC_DXT1 = 0x31545844
    private const val FOURCC_DXT3 = 0x33545844
    private const val FOURCC_DXT5 = 0x35545844
    internal const val MAX_PIXELS = 4096L * 4096L

    fun readBytes(input: InputStream): ByteArray {
        val out = ByteArrayOutputStream(1 shl 16)
        val buf = ByteArray(1 shl 16)
        var total = 0
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            total += n
            if (total > MAX_BYTES) throw TxdFormatException("File terlalu besar (maksimal 64 MB)")
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    fun parse(d: ByteArray): TxdFile = try {
        parseUnsafe(d)
    } catch (e: IndexOutOfBoundsException) {
        throw TxdFormatException("File TXD rusak atau terpotong")
    }

    private fun u8(d: ByteArray, i: Int) = d[i].toInt() and 255
    private fun u16(d: ByteArray, i: Int) = u8(d, i) or (u8(d, i + 1) shl 8)
    private fun i32(d: ByteArray, i: Int) = u16(d, i) or (u16(d, i + 2) shl 16)

    private fun parseUnsafe(d: ByteArray): TxdFile {
        if (d.size < 12 || i32(d, 0) != 0x16) throw TxdFormatException("Bukan file TXD yang valid")
        val total = i32(d, 4)
        val end = if (total < 0 || 12L + total > d.size) d.size else 12 + total
        var p = 12
        if (p + 16 > end || i32(d, p) != 0x01) throw TxdFormatException("Header TXD tidak dikenal")
        val sSize = i32(d, p + 4)
        if (sSize < 4 || sSize > end) throw TxdFormatException("Header TXD tidak dikenal")
        val declared = u16(d, p + 12)
        p += 12 + sSize
        val list = ArrayList<TxdEntry>()
        while (list.size < declared && p + 12 <= end) {
            val type = i32(d, p)
            val sz = i32(d, p + 4)
            if (sz < 0 || p.toLong() + 12 + sz > end) break // terpotong
            if (type == 0x15) list.add(readNative(d, p + 12, p + 12 + sz, list.size))
            p += 12 + sz
        }
        if (list.isEmpty()) throw TxdFormatException("Tidak ada tekstur yang bisa dibaca di file ini")
        return TxdFile(list, declared, d)
    }

    private fun failed(name: String, w: Int, h: Int, note: String) =
        TxdEntry(name, w, h, null, false, emptyList(), note)

    private fun readNative(d: ByteArray, base: Int, end: Int, index: Int): TxdEntry {
        val fallback = "Tekstur ${index + 1}"
        if (base + 16 > end || i32(d, base) != 0x01) return failed(fallback, 0, 0, "Struktur tekstur tidak dikenal")
        val ssz = i32(d, base + 4)
        val s = base + 12
        val sEnd = if (ssz < 0 || s.toLong() + ssz > end) end else s + ssz
        val platform = i32(d, s)
        if (platform != 8 && platform != 9) {
            return failed(
                fallback, 0, 0,
                "Platform 0x${Integer.toHexString(platform).uppercase()} belum didukung (kemungkinan format khusus GPU)"
            )
        }
        if (s + 88 > sEnd) return failed(fallback, 0, 0, "Header tekstur terpotong")

        val name = String(d, s + 8, 32, Charsets.US_ASCII).substringBefore('\u0000').ifEmpty { fallback }
        val raster = i32(d, s + 72)
        val x = i32(d, s + 76) // D3D9: format D3D / FourCC. D3D8: penanda alpha.
        val w = u16(d, s + 80)
        val h = u16(d, s + 82)
        val depth = u8(d, s + 84)
        val nLev = u8(d, s + 85)
        val flags = u8(d, s + 87) // D3D9: bit0 alpha, bit1 cubemap. D3D8: jenis kompresi.
        val alpha = if (platform == 9) (flags and 1) != 0 else x != 0
        if (w == 0 || h == 0) return failed(name, w, h, "Ukuran tekstur tidak valid")

        var note: String? = null
        val fmt: Fmt? = when {
            (raster and 0x6000) != 0 -> { note = "Format berpalet (8/4-bit) belum didukung"; null }
            platform == 9 && (flags and 2) != 0 -> { note = "Cubemap belum didukung"; null }
            platform == 9 && x == FOURCC_DXT1 -> Fmt.DXT1
            platform == 9 && x == FOURCC_DXT3 -> Fmt.DXT3
            platform == 9 && x == FOURCC_DXT5 -> Fmt.DXT5
            platform == 8 && flags == 1 -> Fmt.DXT1
            platform == 8 && flags == 3 -> Fmt.DXT3
            platform == 8 && flags == 5 -> Fmt.DXT5
            platform == 9 && x == 21 -> Fmt.ARGB8888
            platform == 9 && x == 22 -> Fmt.XRGB8888
            platform == 9 && x == 23 -> Fmt.RGB565
            platform == 9 && x == 24 -> Fmt.XRGB1555
            platform == 9 && x == 25 -> Fmt.ARGB1555
            platform == 9 && x == 26 -> Fmt.ARGB4444
            else -> when (raster and 0x0F00) {
                0x0100 -> Fmt.ARGB1555
                0x0200 -> Fmt.RGB565
                0x0300 -> Fmt.ARGB4444
                0x0500 -> Fmt.ARGB8888
                0x0600 -> if (depth == 24) Fmt.BGR888 else Fmt.XRGB8888
                0x0700 -> Fmt.XRGB1555
                else -> null
            }
        }
        if (fmt == null && note == null) {
            note = "Format belum didukung (raster 0x${Integer.toHexString(raster)}, d3d 0x${Integer.toHexString(x)})"
        }

        val levels = ArrayList<Level>()
        var p = s + 88
        for (i in 0 until nLev) {
            if (p + 4 > sEnd) break
            val sz = i32(d, p)
            if (sz < 0 || p.toLong() + 4 + sz > sEnd) break
            levels.add(Level(maxOf(1, w shr i), maxOf(1, h shr i), p + 4, sz))
            p += 4 + sz
        }
        if (fmt != null && levels.isEmpty()) note = "Data piksel tidak ditemukan atau terpotong"
        return TxdEntry(name, w, h, fmt, alpha, levels, note)
    }

    internal fun needBytes(w: Int, h: Int, f: Fmt): Long =
        if (f.block > 0) ((w + 3) / 4).toLong() * ((h + 3) / 4) * f.block else w.toLong() * h * f.bpp

    /** Decode satu level jadi piksel ARGB. [alpha] false = semua piksel dibuat opaque. */
    internal fun decode(d: ByteArray, lv: Level, f: Fmt, alpha: Boolean): IntArray {
        val out = IntArray(lv.w * lv.h)
        if (f.block > 0) decodeDxt(d, lv.off, lv.w, lv.h, f, out) else decodeRaw(d, lv.off, lv.w, lv.h, f, out)
        if (!alpha) for (i in out.indices) out[i] = out[i] or OPAQUE
        return out
    }

    private fun rgb565(v: Int): Int {
        val r = (v shr 11) and 31; val g = (v shr 5) and 63; val b = v and 31
        return OPAQUE or (((r shl 3) or (r shr 2)) shl 16) or (((g shl 2) or (g shr 4)) shl 8) or ((b shl 3) or (b shr 2))
    }

    private fun mix(a: Int, b: Int, wa: Int, wb: Int, div: Int): Int {
        val r = (((a shr 16) and 255) * wa + ((b shr 16) and 255) * wb) / div
        val g = (((a shr 8) and 255) * wa + ((b shr 8) and 255) * wb) / div
        val bl = ((a and 255) * wa + (b and 255) * wb) / div
        return OPAQUE or (r shl 16) or (g shl 8) or bl
    }

    /** Blok warna DXT (8 byte). DXT1 boleh 3 warna + transparan; DXT3/5 selalu 4 warna. */
    private fun colorBlock(d: ByteArray, o: Int, dxt1: Boolean, px: IntArray) {
        val c0 = u16(d, o); val c1 = u16(d, o + 2)
        val p0 = rgb565(c0); val p1 = rgb565(c1)
        val p2: Int; val p3: Int
        if (!dxt1 || c0 > c1) {
            p2 = mix(p0, p1, 2, 1, 3); p3 = mix(p0, p1, 1, 2, 3)
        } else {
            p2 = mix(p0, p1, 1, 1, 2); p3 = 0
        }
        val bits = i32(d, o + 4)
        for (i in 0 until 16) {
            px[i] = when ((bits ushr (2 * i)) and 3) { 0 -> p0; 1 -> p1; 2 -> p2; else -> p3 }
        }
    }

    private fun alpha5(d: ByteArray, o: Int, al: IntArray) {
        val a0 = u8(d, o); val a1 = u8(d, o + 1)
        val pal = IntArray(8); pal[0] = a0; pal[1] = a1
        if (a0 > a1) {
            for (k in 1..6) pal[k + 1] = ((7 - k) * a0 + k * a1) / 7
        } else {
            for (k in 1..4) pal[k + 1] = ((5 - k) * a0 + k * a1) / 5
            pal[6] = 0; pal[7] = 255
        }
        var bits = 0L
        for (i in 0 until 6) bits = bits or (u8(d, o + 2 + i).toLong() shl (8 * i))
        for (i in 0 until 16) al[i] = pal[((bits ushr (3 * i)) and 7L).toInt()]
    }

    private fun decodeDxt(d: ByteArray, off: Int, w: Int, h: Int, f: Fmt, out: IntArray) {
        val bw = (w + 3) / 4; val bh = (h + 3) / 4
        val px = IntArray(16); val al = IntArray(16)
        val dxt1 = f == Fmt.DXT1
        for (blkY in 0 until bh) for (blkX in 0 until bw) {
            val o = off + (blkY * bw + blkX) * f.block
            when (f) {
                Fmt.DXT3 -> {
                    for (i in 0 until 16) al[i] = ((u8(d, o + i / 2) shr (4 * (i and 1))) and 15) * 17
                    colorBlock(d, o + 8, false, px)
                }
                Fmt.DXT5 -> {
                    alpha5(d, o, al)
                    colorBlock(d, o + 8, false, px)
                }
                else -> colorBlock(d, o, true, px)
            }
            for (i in 0 until 16) {
                val x = blkX * 4 + (i and 3); val y = blkY * 4 + (i shr 2)
                if (x < w && y < h) {
                    out[y * w + x] = if (dxt1) px[i] else (px[i] and 0x00FFFFFF) or (al[i] shl 24)
                }
            }
        }
    }

    private fun decodeRaw(d: ByteArray, off: Int, w: Int, h: Int, f: Fmt, out: IntArray) {
        val n = w * h
        for (i in 0 until n) {
            val o = off + i * f.bpp
            out[i] = when (f) {
                Fmt.ARGB8888 -> i32(d, o)
                Fmt.XRGB8888 -> i32(d, o) or OPAQUE
                Fmt.BGR888 -> OPAQUE or (u8(d, o + 2) shl 16) or (u8(d, o + 1) shl 8) or u8(d, o)
                Fmt.RGB565 -> rgb565(u16(d, o))
                Fmt.ARGB1555, Fmt.XRGB1555 -> {
                    val v = u16(d, o)
                    val r = (v shr 10) and 31; val g = (v shr 5) and 31; val b = v and 31
                    val a = if (f == Fmt.ARGB1555 && (v and 0x8000) == 0) 0 else 255
                    (a shl 24) or (((r shl 3) or (r shr 2)) shl 16) or (((g shl 3) or (g shr 2)) shl 8) or ((b shl 3) or (b shr 2))
                }
                Fmt.ARGB4444 -> {
                    val v = u16(d, o)
                    val a = (v shr 12) and 15; val r = (v shr 8) and 15; val g = (v shr 4) and 15; val b = v and 15
                    ((a * 17) shl 24) or ((r * 17) shl 16) or ((g * 17) shl 8) or (b * 17)
                }
                else -> 0
            }
        }
    }
}
