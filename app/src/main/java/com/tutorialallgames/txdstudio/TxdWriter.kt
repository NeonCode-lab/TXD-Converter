package com.tutorialallgames.txdstudio

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

enum class TexFormat(val label: String, val block: Int) {
    DXT1("DXT1", 8), DXT3("DXT3", 16), DXT5("DXT5", 16), ARGB8888("8888", 0)
}

data class Tex(
    val name: String, val w: Int, val h: Int, val argb: IntArray,
    val format: TexFormat = TexFormat.DXT5, val mipmaps: Boolean = true
)

/** Penulis TXD profil PC (RenderWare D3D9, versi GTA SA 0x1803FFFF). */
object TxdWriter {
    private const val VER = 0x1803FFFF
    private val LE = ByteOrder.LITTLE_ENDIAN

    private fun chunk(type: Int, body: ByteArray): ByteArray =
        ByteBuffer.allocate(12 + body.size).order(LE)
            .putInt(type).putInt(body.size).putInt(VER).put(body).array()

    fun build(list: List<Tex>): ByteArray {
        val o = ByteArrayOutputStream()
        o.write(chunk(0x01, byteArrayOf(list.size.toByte(), (list.size shr 8).toByte(), 0, 0)))
        list.forEach { o.write(native(it)) }
        o.write(chunk(0x03, ByteArray(0)))
        return chunk(0x16, o.toByteArray())
    }

    private fun name32(s: String): ByteArray {
        val b = ByteArray(32); val n = s.toByteArray(Charsets.US_ASCII)
        System.arraycopy(n, 0, b, 0, minOf(31, n.size)); return b
    }

    private fun native(t: Tex): ByteArray {
        val lv = mips(t); val f = t.format; val mip = lv.size > 1
        val h = ByteBuffer.allocate(88).order(LE)
        h.putInt(9)                                   // platform: D3D9
        h.putInt(if (mip) 0x1106 else 0x1102)         // filter + addressing
        h.put(name32(t.name)); h.put(ByteArray(32))   // name, mask name
        val base = when (f) { TexFormat.DXT1 -> 0x0200; TexFormat.ARGB8888 -> 0x0500; else -> 0x0300 }
        h.putInt(base or (if (mip) 0x8000 else 0))    // raster format
        h.putInt(when (f) {
            TexFormat.DXT1 -> 0x31545844; TexFormat.DXT3 -> 0x33545844
            TexFormat.DXT5 -> 0x35545844; else -> 21 })
        h.putShort(t.w.toShort()); h.putShort(t.h.toShort())
        h.put((if (f == TexFormat.ARGB8888) 32 else 16).toByte())
        h.put(lv.size.toByte()); h.put(4.toByte())
        h.put(((if (f == TexFormat.DXT1) 0 else 1) or (if (f.block > 0) 8 else 0)).toByte())
        val body = ByteArrayOutputStream()
        body.write(h.array())
        for (l in lv) {
            body.write(ByteBuffer.allocate(4).order(LE).putInt(l.size).array()); body.write(l)
        }
        return chunk(0x15, chunk(0x01, body.toByteArray()) + chunk(0x03, ByteArray(0)))
    }

    private fun mips(t: Tex): List<ByteArray> {
        val r = ArrayList<ByteArray>()
        var w = t.w; var h = t.h; var p = t.argb
        while (true) {
            r += encode(p, w, h, t.format)
            if (!t.mipmaps || (w == 1 && h == 1)) break
            val nw = maxOf(1, w / 2); val nh = maxOf(1, h / 2)
            p = down(p, w, h, nw, nh); w = nw; h = nh
        }
        return r
    }

    private fun down(p: IntArray, w: Int, h: Int, nw: Int, nh: Int): IntArray {
        val o = IntArray(nw * nh)
        for (y in 0 until nh) for (x in 0 until nw) {
            var a = 0; var r = 0; var g = 0; var b = 0
            for (dy in 0..1) for (dx in 0..1) {
                val c = p[minOf(h - 1, y * 2 + dy) * w + minOf(w - 1, x * 2 + dx)]
                a += c ushr 24; r += (c shr 16) and 255; g += (c shr 8) and 255; b += c and 255
            }
            o[y * nw + x] = ((a / 4) shl 24) or ((r / 4) shl 16) or ((g / 4) shl 8) or (b / 4)
        }
        return o
    }

    private fun encode(p: IntArray, w: Int, h: Int, f: TexFormat): ByteArray {
        if (f == TexFormat.ARGB8888) {
            val b = ByteBuffer.allocate(w * h * 4).order(LE)   // little-endian ARGB int = urutan byte BGRA
            for (c in p) b.putInt(c)
            return b.array()
        }
        val bw = maxOf(1, (w + 3) / 4); val bh = maxOf(1, (h + 3) / 4)
        val o = ByteBuffer.allocate(bw * bh * f.block).order(LE)
        val blk = IntArray(16)
        for (by in 0 until bh) for (bx in 0 until bw) {
            for (i in 0 until 16) blk[i] = p[minOf(h - 1, by * 4 + i / 4) * w + minOf(w - 1, bx * 4 + i % 4)]
            if (f == TexFormat.DXT3) alpha4(o, blk)
            if (f == TexFormat.DXT5) alpha5(o, blk)
            color(o, blk)
        }
        return o.array()
    }

    private fun alpha4(o: ByteBuffer, b: IntArray) {
        for (i in 0 until 16 step 2) o.put((((b[i] ushr 28)) or ((b[i + 1] ushr 28) shl 4)).toByte())
    }

    private fun alpha5(o: ByteBuffer, b: IntArray) {
        var mn = 255; var mx = 0
        for (c in b) { val a = c ushr 24; mn = minOf(mn, a); mx = maxOf(mx, a) }
        val pal = IntArray(8); pal[0] = mx; pal[1] = mn
        if (mx > mn) for (k in 1..6) pal[k + 1] = ((7 - k) * mx + k * mn) / 7
        else { for (k in 1..4) pal[k + 1] = ((5 - k) * mx + k * mn) / 5; pal[6] = 0; pal[7] = 255 }
        var bits = 0L
        for (i in 0 until 16) {
            val a = b[i] ushr 24; var best = 0; var bd = Int.MAX_VALUE
            for (k in 0..7) { val d = abs(a - pal[k]); if (d < bd) { bd = d; best = k } }
            bits = bits or (best.toLong() shl (3 * i))
        }
        o.put(mx.toByte()); o.put(mn.toByte())
        for (i in 0 until 6) o.put((bits shr (8 * i)).toByte())
    }

    private fun p565(r: Int, g: Int, b: Int) = ((r * 31 / 255) shl 11) or ((g * 63 / 255) shl 5) or (b * 31 / 255)
    private fun rgb(v: Int) = intArrayOf(((v shr 11) and 31) * 255 / 31, ((v shr 5) and 63) * 255 / 63, (v and 31) * 255 / 31)

    private fun color(o: ByteBuffer, blk: IntArray) {
        var nr = 255; var ng = 255; var nb = 255; var xr = 0; var xg = 0; var xb = 0
        for (c in blk) {
            val r = (c shr 16) and 255; val g = (c shr 8) and 255; val b = c and 255
            nr = minOf(nr, r); ng = minOf(ng, g); nb = minOf(nb, b)
            xr = maxOf(xr, r); xg = maxOf(xg, g); xb = maxOf(xb, b)
        }
        var c0 = p565(xr, xg, xb); var c1 = p565(nr, ng, nb)
        if (c0 < c1) { val t = c0; c0 = c1; c1 = t }
        val a = rgb(c0); val b = rgb(c1)
        val pal = arrayOf(a, b, IntArray(3) { (2 * a[it] + b[it]) / 3 }, IntArray(3) { (a[it] + 2 * b[it]) / 3 })
        o.putShort(c0.toShort()); o.putShort(c1.toShort())
        var bits = 0
        for (i in 0 until 16) {
            val r = (blk[i] shr 16) and 255; val g = (blk[i] shr 8) and 255; val bl = blk[i] and 255
            var best = 0; var bd = Int.MAX_VALUE
            for (k in 0..3) {
                val dr = r - pal[k][0]; val dg = g - pal[k][1]; val db = bl - pal[k][2]
                val d = dr * dr + dg * dg + db * db
                if (d < bd) { bd = d; best = k }
            }
            bits = bits or (best shl (2 * i))
        }
        o.putInt(bits)
    }
}
