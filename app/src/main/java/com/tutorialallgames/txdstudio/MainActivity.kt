package com.tutorialallgames.txdstudio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val BG = Color(0xFF0F1115); private val SF = Color(0xFF181B22)
private val AC = Color(0xFFFF8A3D); private val MU = Color(0xFF9AA3B2)
private val TX = Color(0xFFE8EAF0); private val LN = Color(0xFF2A2F3A)
private val ON_AC = Color(0xFF1A0E05)

class MainActivity : ComponentActivity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s); enableEdgeToEdge()
        // UpdateDialog = notifikasi update otomatis (baca update.json di cabang main saat aplikasi dibuka).
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { App(); UpdateDialog() } }
    }
}

private val IMAGE_TYPES = arrayOf("image/png", "image/jpeg", "image/webp")

/** Bulatkan ke pangkat dua terdekat ke atas, maksimal 2048. */
private fun pot(n: Int): Int { var p = 1; while (p < n) p = p shl 1; return minOf(p, 2048) }

private fun cleanName(raw: String): String =
    raw.map { if (it.code in 33..126 && it != '/' && it != '\\') it else '_' }.joinToString("").take(31).ifEmpty { "texture" }

private fun uniqueName(base: String, taken: Set<String>): String {
    if (base.lowercase() !in taken) return base
    var i = 1
    while (true) {
        val suf = "_$i"
        val cand = base.take(31 - suf.length) + suf
        if (cand.lowercase() !in taken) return cand
        i++
    }
}

private fun thumbnail(bm: Bitmap): Bitmap {
    val s = 256f / maxOf(bm.width, bm.height)
    return if (s >= 1f) bm.copy(Bitmap.Config.ARGB_8888, false)
    else Bitmap.createScaledBitmap(bm, maxOf(1, (bm.width * s).toInt()), maxOf(1, (bm.height * s).toInt()), true)
}

/** Nama file yang tampil untuk [uri]; cadangan "file.txd" kalau tidak ketemu. */
private fun displayName(ctx: Context, uri: Uri): String =
    ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { if (it.moveToFirst()) it.getString(0) else null } ?: "file.txd"

/** Dipanggil di thread latar. Mengembalikan null jika PNG gagal dibaca. */
private fun load(ctx: Context, uri: Uri, taken: Set<String>): Tex? = runCatching {
    // Baca ukuran dulu; gambar raksasa di-subsample supaya tidak kehabisan memori saat decode.
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    ctx.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 4096) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    val src = ctx.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, opts) }
        ?: return@runCatching null
    val w = pot(src.width); val h = pot(src.height)
    val bm = if (w == src.width && h == src.height) src else Bitmap.createScaledBitmap(src, w, h, true)
    val px = IntArray(w * h); bm.getPixels(px, 0, w, 0, 0, w, h)
    val hasAlpha = px.any { (it ushr 24) < 255 }
    val th = thumbnail(bm)
    if (bm !== src) bm.recycle()
    src.recycle()
    val raw = ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { if (it.moveToFirst()) it.getString(0) else null } ?: "texture"
    val name = uniqueName(cleanName(raw.substringBeforeLast('.')), taken)
    Tex(
        name, w, h, px, th,
        format = if (hasAlpha) TexFormat.DXT5 else TexFormat.DXT1,
        mipmaps = true, alpha = hasAlpha
    )
}.getOrNull()

@Composable private fun Btn(
    t: String, primary: Boolean = false, m: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit
) = Button(
    onClick, m.heightIn(min = 52.dp), enabled = enabled, shape = RoundedCornerShape(14.dp),
    contentPadding = PaddingValues(horizontal = 8.dp),
    colors = ButtonDefaults.buttonColors(
        containerColor = if (primary) AC else SF,
        contentColor = if (primary) ON_AC else TX,
        disabledContainerColor = SF,
        disabledContentColor = MU.copy(alpha = 0.4f)
    )
) { Text(t, fontWeight = FontWeight.Bold) }

@Composable private fun Top(title: String, onBack: () -> Unit) =
    Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onBack) { Text("‹ Kembali", color = AC) }
        Spacer(Modifier.width(8.dp)); Text(title, color = TX, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }

@Composable private fun Thumb(t: Tex, m: Modifier) {
    val bm = remember(t.thumb) { t.thumb.asImageBitmap() }
    Image(bm, t.name, m.clip(RoundedCornerShape(8.dp)).background(Color(0xFF222630)), contentScale = ContentScale.Fit)
}

@Composable private fun Social(label: String, handle: String, onClick: () -> Unit) =
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MU, fontSize = 14.sp)
        Text("$handle  ↗", color = AC, fontWeight = FontWeight.SemiBold)
    }

@Composable fun App() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var screen by remember { mutableStateOf("home") }
    var sel by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf<String?>(null) }
    val texs = remember { mutableStateListOf<Tex>() }
    var txd by remember { mutableStateOf<TxdFile?>(null) }
    var txdName by remember { mutableStateOf("") }
    var txdSel by remember { mutableIntStateOf(0) }
    val toast = { m: String -> Toast.makeText(ctx, m, Toast.LENGTH_SHORT).show() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { us ->
        if (us.isNotEmpty()) {
            busy = "Mengimpor gambar…"
            scope.launch {
                val taken = texs.map { it.name.lowercase() }.toMutableSet()
                val res = withContext(Dispatchers.Default) {
                    us.map { u -> load(ctx, u, taken)?.also { taken.add(it.name.lowercase()) } }
                }
                val ok = res.filterNotNull()
                texs.addAll(ok)
                busy = null
                val fail = res.size - ok.size
                if (fail > 0) toast("$fail gambar gagal dibaca (pastikan file gambar valid)")
                if (texs.isNotEmpty()) screen = "editor"
            }
        }
    }
    val txdPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            busy = "Membaca TXD…"
            scope.launch {
                val res = withContext(Dispatchers.IO) {
                    runCatching {
                        val bytes = ctx.contentResolver.openInputStream(uri)!!.use { TxdReader.readBytes(it) }
                        Pair(TxdReader.parse(bytes), displayName(ctx, uri))
                    }
                }
                busy = null
                res.onSuccess { (f, nm) ->
                    txd = f; txdName = nm; txdSel = 0; screen = "txd"
                }.onFailure { t ->
                    toast(
                        when (t) {
                            is TxdFormatException -> t.message ?: "File TXD tidak valid"
                            is OutOfMemoryError -> "Memori tidak cukup untuk membuka file ini"
                            else -> "Gagal membaca file"
                        }
                    )
                }
            }
        }
    }
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) {
            val snapshot = texs.toList()
            busy = "Menyimpan TXD…"
            scope.launch {
                val err = withContext(Dispatchers.IO) {
                    try {
                        ctx.contentResolver.openOutputStream(uri)!!.buffered(1 shl 16)
                            .use { TxdWriter.write(snapshot, it) }
                        null
                    } catch (t: Throwable) {
                        // Jangan tinggalkan file TXD kosong/setengah jadi kalau gagal
                        runCatching { DocumentsContract.deleteDocument(ctx.contentResolver, uri) }
                        t
                    }
                }
                busy = null
                toast(
                    when (err) {
                        null -> "TXD tersimpan"
                        is OutOfMemoryError -> "Memori tidak cukup. Kurangi resolusi atau jumlah tekstur"
                        else -> "Gagal menyimpan"
                    }
                )
            }
        }
    }
    // Editor: satu ZIP berisi dua TXD (with_alpha dan without_alpha)
    val zipSaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            val snapshot = texs.toList()
            busy = "Menyimpan ZIP…"
            scope.launch {
                val err = withContext(Dispatchers.IO) {
                    try {
                        ctx.contentResolver.openOutputStream(uri)!!.buffered(1 shl 16)
                            .use { ZipExport.writeTxdZip(snapshot, it) }
                        null
                    } catch (t: Throwable) {
                        runCatching { DocumentsContract.deleteDocument(ctx.contentResolver, uri) }
                        t
                    }
                }
                busy = null
                toast(
                    when (err) {
                        null -> "ZIP tersimpan (with alpha + without alpha)"
                        is OutOfMemoryError -> "Memori tidak cukup. Kurangi resolusi atau jumlah tekstur"
                        else -> "Gagal menyimpan ZIP"
                    }
                )
            }
        }
    }
    // Pembaca TXD: satu ZIP berisi PNG tiap tekstur, disortir otomatis ke with_alpha / without_alpha
    val pngZipSaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        val f = txd
        if (uri != null && f != null) {
            busy = "Mengekspor PNG…"
            scope.launch {
                val res = withContext(Dispatchers.IO) {
                    runCatching {
                        ctx.contentResolver.openOutputStream(uri)!!.buffered(1 shl 16)
                            .use { ZipExport.writePngZip(f, it) }
                    }.onFailure { runCatching { DocumentsContract.deleteDocument(ctx.contentResolver, uri) } }
                }
                busy = null
                res.onSuccess { (ok, skip) ->
                    toast(if (skip > 0) "$ok tekstur diekspor, $skip dilewati" else "$ok tekstur diekspor")
                }.onFailure { t ->
                    toast(
                        when (t) {
                            is TxdFormatException -> t.message ?: "Gagal mengekspor"
                            is OutOfMemoryError -> "Memori tidak cukup untuk mengekspor file ini"
                            else -> "Gagal mengekspor"
                        }
                    )
                }
            }
        }
    }
    BackHandler(screen != "home" || busy != null) {
        if (busy == null) screen = when (screen) {
            "detail" -> "editor"
            "txdDetail" -> "txd"
            else -> "home"
        }
    }

    Box(Modifier.fillMaxSize().background(BG)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp)) {
            when (screen) {
                "home" -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("TXD Studio", color = TX, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                            Text("Konversi, baca & ekspor TXD · Android", color = MU, fontSize = 14.sp)
                        }
                        TextButton({ screen = "about" }) { Text("Tentang", color = AC) }
                    }
                    Spacer(Modifier.height(24.dp))
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SF)
                            .border(1.dp, LN, RoundedCornerShape(14.dp)).padding(16.dp)
                    ) {
                        Text("Khusus Android", color = AC, fontWeight = FontWeight.Bold)
                        Text(
                            "Format: DXT1, DXT3, DXT5, dan 8888, dengan atau tanpa alpha. " +
                                "Atur per gambar setelah impor. Buka file TXD untuk melihat isinya " +
                                "dan ekspor ke ZIP (PNG disortir with alpha / without alpha).",
                            color = MU, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Btn("Impor PNG", true, Modifier.fillMaxWidth()) { picker.launch(IMAGE_TYPES) }
                    Spacer(Modifier.height(12.dp))
                    Btn("Buka file TXD", false, Modifier.fillMaxWidth()) { txdPicker.launch(arrayOf("*/*")) }
                }

                "editor" -> {
                    Top("Editor TXD · ${texs.size} tekstur") { screen = "home" }
                    LazyVerticalGrid(
                        GridCells.Fixed(2), Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(texs) { i, t ->
                            val d = t.outDims()
                            Column(
                                Modifier.clip(RoundedCornerShape(14.dp)).background(SF)
                                    .clickable { sel = i; screen = "detail" }.padding(8.dp)
                            ) {
                                Thumb(t, Modifier.fillMaxWidth().height(110.dp))
                                Text(t.name, color = TX, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                                Text("${d.first}×${d.second} · ${t.formatLabel()}", color = MU, fontSize = 13.sp)
                                Text(if (t.alpha) "Dengan alpha" else "Tanpa alpha", color = if (t.alpha) AC else MU, fontSize = 12.sp)
                            }
                        }
                    }
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Btn("Tambah PNG", false, Modifier.weight(1f)) { picker.launch(IMAGE_TYPES) }
                        Btn("Simpan TXD", true, Modifier.weight(1f)) {
                            val names = texs.map { it.name.lowercase() }
                            when {
                                texs.isEmpty() -> toast("Belum ada tekstur")
                                names.any { it.isBlank() } -> toast("Ada nama tekstur yang kosong")
                                names.size != names.toSet().size -> toast("Ada nama tekstur yang kembar")
                                else -> saver.launch("textures.txd")
                            }
                        }
                    }
                    Btn("Simpan ZIP (with alpha + without alpha)", false, Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        val names = texs.map { it.name.lowercase() }
                        when {
                            texs.isEmpty() -> toast("Belum ada tekstur")
                            names.any { it.isBlank() } -> toast("Ada nama tekstur yang kosong")
                            names.size != names.toSet().size -> toast("Ada nama tekstur yang kembar")
                            else -> zipSaver.launch("textures.zip")
                        }
                    }
                }

                "detail" -> {
                    val t = texs.getOrNull(sel)
                    if (t == null) { LaunchedEffect(Unit) { screen = "editor" } } else {
                        val d = t.outDims()
                        val setAlpha = { on: Boolean ->
                            val f = when {
                                !on && (t.format == TexFormat.DXT3 || t.format == TexFormat.DXT5) -> TexFormat.DXT1
                                on && t.format == TexFormat.DXT1 -> TexFormat.DXT5
                                else -> t.format
                            }
                            texs[sel] = t.copy(alpha = on, format = f)
                        }
                        Top("Pengaturan tekstur") { screen = "editor" }
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            Thumb(t, Modifier.fillMaxWidth().height(200.dp))
                            val dupName = texs.withIndex().any { (i, o) -> i != sel && o.name.equals(t.name, ignoreCase = true) }
                            OutlinedTextField(
                                t.name,
                                { v -> texs[sel] = t.copy(name = v.filter { it.code in 33..126 && it != '/' && it != '\\' }.take(31)) },
                                Modifier.fillMaxWidth().padding(top = 16.dp),
                                label = { Text("Nama tekstur") }, singleLine = true,
                                isError = dupName || t.name.isBlank(),
                                supportingText = { if (dupName) Text("Nama sudah dipakai tekstur lain") else if (t.name.isBlank()) Text("Nama tidak boleh kosong") }
                            )

                            Row(
                                Modifier.fillMaxWidth().padding(top = 16.dp).clip(RoundedCornerShape(14.dp)).background(SF)
                                    .clickable { setAlpha(!t.alpha) }.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    t.alpha, { setAlpha(it) },
                                    colors = CheckboxDefaults.colors(checkedColor = AC, checkmarkColor = ON_AC, uncheckedColor = MU)
                                )
                                Column(Modifier.padding(start = 4.dp)) {
                                    Text("Dengan alpha", color = TX, fontWeight = FontWeight.SemiBold)
                                    Text("Centang jika gambar punya bagian transparan. Kosongkan untuk tanpa alpha.", color = MU, fontSize = 12.sp)
                                }
                            }

                            Text("Format kompresi", color = MU, fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TexFormat.entries.forEach { f ->
                                    val label = if (f == TexFormat.ARGB8888) (if (t.alpha) "8888" else "888") else f.label
                                    val off = !t.alpha && (f == TexFormat.DXT3 || f == TexFormat.DXT5)
                                    Btn(label, f == t.fmt, Modifier.weight(1f), enabled = !off) { texs[sel] = t.copy(format = f) }
                                }
                            }
                            Text(
                                if (t.alpha) "DXT1: terkecil, alpha 1-bit · DXT3: alpha tajam · DXT5: alpha halus · 8888: tanpa kompresi"
                                else "Tanpa alpha: DXT3/DXT5 dinonaktifkan. DXT1 = terkecil, 888 = tanpa kompresi.",
                                color = MU, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)
                            )

                            val sizes = listOf(256, 512, 1024, 2048).filter { it <= maxOf(t.w, t.h) }
                            if (sizes.isNotEmpty()) {
                                Text("Resolusi maksimal", color = MU, fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    sizes.forEach { s ->
                                        Btn("$s", maxOf(d.first, d.second) == s, Modifier.weight(1f)) { texs[sel] = t.copy(maxSize = s) }
                                    }
                                }
                            }

                            Row(
                                Modifier.fillMaxWidth().padding(top = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Buat mipmap", color = TX, fontWeight = FontWeight.SemiBold)
                                Switch(
                                    t.mipmaps, { texs[sel] = t.copy(mipmaps = it) },
                                    colors = SwitchDefaults.colors(checkedTrackColor = AC, checkedThumbColor = ON_AC)
                                )
                            }
                            val kb = maxOf(1, TxdWriter.estimateBytes(t) / 1024)
                            Text(
                                "Hasil: ${d.first}×${d.second} · ${t.formatLabel()} · ${if (t.alpha) "alpha" else "tanpa alpha"} · ± $kb KB",
                                color = MU, modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Btn("Hapus", false, Modifier.weight(1f)) {
                                texs.removeAt(sel); screen = if (texs.isEmpty()) "home" else "editor"
                            }
                            Btn("Selesai", true, Modifier.weight(1f)) { screen = "editor" }
                        }
                    }
                }

                "txd" -> {
                    val f = txd
                    if (f == null) { LaunchedEffect(Unit) { screen = "home" } } else {
                        TxdListScreen("$txdName · ${f.entries.size} tekstur", f, { screen = "home" }) { i ->
                            txdSel = i; screen = "txdDetail"
                        }
                        Btn("Ekspor ZIP (PNG with alpha + without alpha)", false, Modifier.fillMaxWidth().padding(top = 12.dp)) {
                            pngZipSaver.launch("textures_png.zip")
                        }
                    }
                }

                "txdDetail" -> {
                    val f = txd
                    val e = f?.entries?.getOrNull(txdSel)
                    if (f == null || e == null) { LaunchedEffect(Unit) { screen = "txd" } } else {
                        TxdDetailScreen(f, e) { screen = "txd" }
                    }
                }

                else -> {
                    val uriHandler = LocalUriHandler.current
                    val open = { url: String ->
                        try { uriHandler.openUri(url) } catch (e: Exception) { toast("Tidak bisa membuka tautan") }
                    }
                    Top("Tentang") { screen = "home" }
                    Column(
                        Modifier.weight(1f).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                    ) {
                        Text("TXD Studio", color = TX, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text("Versi 1.2.0 · Android", color = MU, modifier = Modifier.padding(top = 6.dp))
                        Text(
                            "Konversi PNG ke TXD, baca isi file TXD, dan ekspor ke ZIP (PNG disortir " +
                                "otomatis ke with alpha / without alpha). Khusus Android.",
                            color = MU, textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp)
                        )
                        UpdateCheckRow()
                    }
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SF).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Created By", color = MU, fontSize = 13.sp)
                        Text("Tutorial All Games", color = AC, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = LN)
                        Text("Ikuti di sosial media", color = MU, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp))
                        Social("YouTube", "kri-sky09") { open("https://www.youtube.com/@kri-sky09") }
                        Social("TikTok", "Neoncode") { open("https://www.tiktok.com/@neoncode") }
                    }
                }
            }
        }

        // Overlay saat proses berat berjalan di latar
        busy?.let { msg ->
            Box(
                Modifier.fillMaxSize().background(Color(0xCC000000))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AC)
                    Text(msg, color = TX, modifier = Modifier.padding(top = 16.dp))
                }
            }
        }
    }
}
