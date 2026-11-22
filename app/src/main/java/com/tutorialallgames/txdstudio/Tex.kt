package com.tutorialallgames.txdstudio

import android.graphics.Bitmap

enum class TexFormat(val label: String, val block: Int) {
    DXT1("DXT1", 8), DXT3("DXT3", 16), DXT5("DXT5", 16), ARGB8888("8888", 0)
}

/**
 * Satu tekstur dalam proyek.
 * [argb] = piksel penuh (sudah pangkat dua), [thumb] = pratinjau kecil untuk UI.
 * [alpha] = true -> "dengan alpha"; false -> semua piksel dibuat opaque dan flag alpha dimatikan.
 * [maxSize] = batas sisi terpanjang hasil akhir (diturunkan dengan membagi dua).
 */
data class Tex(
    val name: String,
    val w: Int,
    val h: Int,
    val argb: IntArray,
    val thumb: Bitmap,
    val format: TexFormat = TexFormat.DXT5,
    val mipmaps: Boolean = true,
    val alpha: Boolean = true,
    val maxSize: Int = 2048
)

/** Tanpa alpha, DXT3/DXT5 tidak berguna -> otomatis jadi DXT1. */
val Tex.fmt: TexFormat
    get() = if (!alpha && (format == TexFormat.DXT3 || format == TexFormat.DXT5)) TexFormat.DXT1 else format

/** Ukuran akhir setelah batas [Tex.maxSize] diterapkan. */
fun Tex.outDims(): Pair<Int, Int> {
    var cw = w; var ch = h
    val lim = maxOf(1, maxSize)
    while (maxOf(cw, ch) > lim && (cw > 1 || ch > 1)) { cw = maxOf(1, cw / 2); ch = maxOf(1, ch / 2) }
    return cw to ch
}

fun Tex.formatLabel(): String = when (fmt) {
    TexFormat.ARGB8888 -> if (alpha) "8888" else "888"
    else -> fmt.label
}
