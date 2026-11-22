package com.tutorialallgames.txdstudio

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

/**
 * Penulis TXD untuk Android (RenderWare D3D9, versi GTA SA 0x1803FFFF, tekstur DXT/32-bit).
 *
 * Pemetaan alpha:
 *  - DXT1 + alpha  : raster 1555, alpha 1-bit (piksel alpha < 128 transparan)
 *  - DXT1 tanpa    : raster 565
 *  - DXT3/DXT5     : raster 4444 (selalu beralpha)
 *  - 8888 + alpha  : A8R8G8B8 (d3d 21)
 *  - 8888 tanpa    : X8R8G8B8 (d3d 22, "888")
 */
object TxdWriter {
    private const val VER = 0x1803FFFF
    private const val OPAQUE = -0x1000000 // 0xFF000000
    private val LE = ByteOrder.LITTLE_ENDIAN

    private fun chunk(type: Int, body: ByteArray): ByteArray =
        ByteBuffer.allocate(12 + body.size).order(LE)
            .putInt(type).putInt(body.size).putInt(VER).put(body).array()

    fun build(list: List<Tex>): ByteArray {
        require(list.isNotEmpty()) { "Tidak ada tekstur" }
        val o = ByteArrayOutputStream()
        o.write(chunk(0x01, byteArrayOf(list.size.toByte(), (list.size shr 8).toByte(), 0, 0)))
        list.forEach { o.write(native(it)) }
        o.write(chunk(0x03, ByteArray(0)))
        return chunk(0x16, o.toByteArray())
    }

    private fun levelBytes(w: Int, h: Int, f: TexFormat): Int =
        if (f.block == 0) w * h * 4 else maxOf(1, (w + 3) / 4) * maxOf(1, (h + 3) / 4) * f.block

    /** Perkiraan ukuran file satu tekstur tanpa meng-encode (murah, aman dipanggil di UI). */
    fun estimateBytes(t: Tex): Int {
        val (w0, h0) = t.outDims()
        val f = t.fmt
        var w = w0; var h = h0; var sum = 0
        while (true) {
            sum += 4 + levelBytes(w, h, f)
            if (!t.mipmaps || (w == 1 && h == 1)) break
            w = maxOf(1, w / 2); h = maxOf(1, h / 2)
        }
        return sum + 164 // header chunk + header native 88 byte
    }

    private fun name32(s: String): ByteArray {
        val b = ByteArray(32); val n = s.toByteArray(Charsets.US_ASCII)
        System.arraycopy(n, 0, b, 0, minOf(31, n.size)); return b
    }

    private fun native(t: Tex): ByteArray {
        val f = t.fmt
        val alpha = t.alpha
        val lv = mips(t)
        val mip = lv.size > 1
        val (w, h) = t.outDims()
        val hd = ByteBuffer.allocate(88).order(LE)
        hd.putInt(9)                                   // platform: D3D9
        hd.putInt(if (mip) 0x1106 else 0x1102)         // filter + addressing (wrap)
        hd.put(name32(t.name)); hd.put(ByteArray(32))  // nama, nama mask
        val base = when (f) {
            TexFormat.DXT1 -> if (alpha) 0x0100 else 0x0200
            TexFormat.DXT3, TexFormat.DXT5 -> 0x0300
            TexFormat.ARGB8888 -> if (alpha) 0x0500 else 0x0600
        }
        hd.putInt(base or (if (mip) 0x8000 else 0))    // raster format
        hd.putInt(
            when (f) {
                TexFormat.DXT1 -> 0x31545844
                TexFormat.DXT3 -> 0x33545844
                TexFormat.DXT5 -> 0x35545844
                TexFormat.ARGB8888 -> if (alpha) 21 else 22
            }
        )
        hd.putShort(w.toShort()); hd.putShort(h.toShort())
        hd.put((if (f.block == 0) 32 else 16).toByte())
        hd.put(lv.size.toByte()); hd.put(4.toByte())
        hd.put(((if (alpha) 1 else 0) or (if (f.block > 0) 8 else 0)).toByte())
        val body = ByteArrayOutputStream()
        body.write(hd.array())
        for (l in lv) {
            body.write(ByteBuffer.allocate(4).order(LE).putInt(l.size).array()); body.write(l)
        }
        return chunk(0x15, chunk(0x01, body.toByteArray()) + chunk(0x03, ByteArray(0)))
    }

    private fun mips(t: Tex): List<ByteArray> {
        val f = t.fmt
        var w = t.w; var h = t.h
        var p = if (t.alpha) t.argb else IntArray(t.argb.size) { t.argb[it] or OPAQUE }
        val lim = maxOf(1, t.maxSize)
        while (maxOf(w, h) > lim && (w > 1 || h > 1)) {
            val nw = maxOf(1, w / 2); val nh = maxOf(1, h / 2)
            p = down(p, w, h, nw, nh); w = nw; h = nh
        }
        val r = ArrayList<ByteArray>()
        while (true) {
            r.add(encode(p, w, h, f, t.alpha))
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

    private fun encode(p: IntArray, w: Int, h: Int, f: TexFormat, alpha: Boolean): ByteArray {
        if (f.block == 0) {
            val b = ByteBuffer.allocate(w * h * 4).order(LE) // int ARGB little-endian = byte BGRA
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
            color(o, blk, f == TexFormat.DXT1 && alpha)
        }
        return o.array()
    }

    private fun alpha4(o: ByteBuffer, b: IntArray) {
        fun q(c: Int) = (((c ushr 24) * 15 + 127) / 255)
        for (i in 0 until 16 step 2) o.put((q(b[i]) or (q(b[i + 1]) shl 4)).toByte())
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

    private fun p565(r: Int, g: Int, b: Int) =
        (((r * 31 + 127) / 255) shl 11) or (((g * 63 + 127) / 255) shl 5) or ((b * 31 + 127) / 255)

    private fun rgb(v: Int): IntArray {
        val r = (v shr 11) and 31; val g = (v shr 5) and 63; val b = v and 31
        return intArrayOf((r shl 3) or (r shr 2), (g shl 2) or (g shr 4), (b shl 3) or (b shr 2))
    }

    /** Blok warna DXT. [oneBit] = mode DXT1 dengan alpha 1-bit (piksel alpha < 128 -> indeks 3 transparan). */
    private fun color(o: ByteBuffer, blk: IntArray, oneBit: Boolean) {
        var nr = 255; var ng = 255; var nb = 255; var xr = 0; var xg = 0; var xb = 0
        var n = 0; var clear = false
        for (c in blk) {
            if (oneBit && (c ushr 24) < 128) { clear = true; continue }
            val r = (c shr 16) and 255; val g = (c shr 8) and 255; val b = c and 255
            n++
            nr = minOf(nr, r); ng = minOf(ng, g); nb = minOf(nb, b)
            xr = maxOf(xr, r); xg = maxOf(xg, g); xb = maxOf(xb, b)
        }
        if (n == 0) { // blok transparan penuh
            o.putShort(0.toShort()); o.putShort(0.toShort()); o.putInt(-1)
            return
        }
        // inset bounding box 1/16 supaya kuantisasi lebih akurat
        val ir = (xr - nr) shr 4; val ig = (xg - ng) shr 4; val ib = (xb - nb) shr 4
        var c0 = p565(xr - ir, xg - ig, xb - ib); var c1 = p565(nr + ir, ng + ig, nb + ib)
        if (clear) { if (c0 > c1) { val t = c0; c0 = c1; c1 = t } }       // mode 3 warna: c0 <= c1
        else if (c0 < c1) { val t = c0; c0 = c1; c1 = t }                  // mode 4 warna: c0 > c1
        val a = rgb(c0); val b = rgb(c1)
        val pal = if (clear) arrayOf(a, b, IntArray(3) { (a[it] + b[it]) / 2 })
        else arrayOf(a, b, IntArray(3) { (2 * a[it] + b[it]) / 3 }, IntArray(3) { (a[it] + 2 * b[it]) / 3 })
        o.putShort(c0.toShort()); o.putShort(c1.toShort())
        var bits = 0
        for (i in 0 until 16) {
            val px = blk[i]
            var idx = 3
            if (!(clear && (px ushr 24) < 128)) {
                val r = (px shr 16) and 255; val g = (px shr 8) and 255; val bl = px and 255
                var bd = Int.MAX_VALUE
                for (k in pal.indices) {
                    val dr = r - pal[k][0]; val dg = g - pal[k][1]; val db = bl - pal[k][2]
                    val d = dr * dr + dg * dg + db * db
                    if (d < bd) { bd = d; idx = k }
                }
            }
            bits = bits or (idx shl (2 * i))
        }
        o.putInt(bits)
    }
}
