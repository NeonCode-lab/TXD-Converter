package com.tutorialallgames.txdstudio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BG = Color(0xFF0F1115); private val SF = Color(0xFF181B22)
private val AC = Color(0xFFFF8A3D); private val MU = Color(0xFF9AA3B2)
private val TX = Color(0xFFE8EAF0); private val LN = Color(0xFF2A2F3A)

class MainActivity : ComponentActivity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s); enableEdgeToEdge()
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { App() } }
    }
}

private fun pot(n: Int): Int { var p = 1; while (p < n) p = p shl 1; return minOf(p, 2048) }

private fun load(ctx: Context, uri: Uri): Tex? = runCatching {
    val src = ctx.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it) }
    val w = pot(src.width); val h = pot(src.height)
    val bm = if (w == src.width && h == src.height) src else Bitmap.createScaledBitmap(src, w, h, true)
    val px = IntArray(w * h); bm.getPixels(px, 0, w, 0, 0, w, h)
    val nm = ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { if (it.moveToFirst()) it.getString(0) else null } ?: "texture"
    Tex(nm.substringBeforeLast('.').take(31), w, h, px)
}.getOrNull()

@Composable private fun Btn(t: String, primary: Boolean = false, m: Modifier = Modifier, onClick: () -> Unit) =
    Button(onClick, m.heightIn(min = 52.dp), shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) AC else SF,
            contentColor = if (primary) Color(0xFF1A0E05) else TX)) { Text(t, fontWeight = FontWeight.Bold) }

@Composable private fun Top(title: String, onBack: () -> Unit) =
    Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onBack) { Text("‹ Kembali", color = AC) }
        Spacer(Modifier.width(8.dp)); Text(title, color = TX, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }

@Composable private fun Thumb(t: Tex, m: Modifier) {
    val bm = remember(t.argb) { Bitmap.createBitmap(t.argb, t.w, t.h, Bitmap.Config.ARGB_8888).asImageBitmap() }
    Image(bm, t.name, m.clip(RoundedCornerShape(8.dp)).background(Color(0xFF222630)), contentScale = ContentScale.Fit)
}

@Composable fun App() {
    val ctx = LocalContext.current
    var screen by remember { mutableStateOf("home") }
    var platform by remember { mutableStateOf("Android") }
    var sel by remember { mutableIntStateOf(0) }
    val texs = remember { mutableStateListOf<Tex>() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { us ->
        us.forEach { u -> load(ctx, u)?.let { texs.add(it) } }
        if (texs.isNotEmpty()) screen = "editor"
    }
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let { u ->
            val ok = runCatching { ctx.contentResolver.openOutputStream(u)!!.use { it.write(TxdWriter.build(texs.toList())) } }.isSuccess
            Toast.makeText(ctx, if (ok) "TXD tersimpan" else "Gagal menyimpan", Toast.LENGTH_SHORT).show()
        }
    }
    BackHandler(screen != "home") { screen = if (screen == "detail") "editor" else "home" }

    Column(Modifier.fillMaxSize().background(BG).statusBarsPadding().navigationBarsPadding().padding(20.dp)) {
        when (screen) {
            "home" -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("TXD Studio", color = TX, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                        Text("Konversi PNG ke TXD", color = MU, fontSize = 14.sp)
                    }
                    TextButton({ screen = "about" }) { Text("Tentang", color = AC) }
                }
                Spacer(Modifier.height(24.dp))
                Text("PLATFORM GAME", color = MU, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                listOf("Android", "PC", "PS2", "Xbox").chunked(2).forEach { row ->
                    Row(Modifier.padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { p ->
                            val on = p == platform
                            Box(Modifier.weight(1f).heightIn(min = 56.dp).clip(RoundedCornerShape(14.dp))
                                .background(if (on) Color(0xFF241A12) else SF)
                                .border(1.dp, if (on) AC else LN, RoundedCornerShape(14.dp))
                                .clickable { platform = p }.padding(16.dp), contentAlignment = Alignment.CenterStart) {
                                Text(p, color = TX, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                Text("Saat ini semua profil memakai format PC (D3D9); profil khusus menyusul.", color = MU, fontSize = 12.sp)
                Spacer(Modifier.height(20.dp))
                Btn("Impor PNG", true, Modifier.fillMaxWidth()) { picker.launch(arrayOf("image/png")) }
                Spacer(Modifier.height(12.dp))
                Btn("Buka file TXD", false, Modifier.fillMaxWidth()) {
                    Toast.makeText(ctx, "Pembaca TXD menyusul", Toast.LENGTH_SHORT).show() }
            }
            "editor" -> {
                Top("Editor TXD · $platform") { screen = "home" }
                LazyVerticalGrid(GridCells.Fixed(2), Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(texs) { i, t ->
                        Column(Modifier.clip(RoundedCornerShape(14.dp)).background(SF).clickable { sel = i; screen = "detail" }.padding(8.dp)) {
                            Thumb(t, Modifier.fillMaxWidth().height(110.dp))
                            Text(t.name, color = TX, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                            Text("${t.w}×${t.h} · ${t.format.label}", color = MU, fontSize = 13.sp)
                        }
                    }
                }
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Btn("Tambah PNG", false, Modifier.weight(1f)) { picker.launch(arrayOf("image/png")) }
                    Btn("Simpan TXD", true, Modifier.weight(1f)) { saver.launch("textures.txd") }
                }
            }
            "detail" -> {
                val t = texs.getOrNull(sel)
                if (t == null) { screen = "editor" } else {
                    Top("Pengaturan tekstur") { screen = "editor" }
                    Thumb(t, Modifier.fillMaxWidth().height(200.dp))
                    OutlinedTextField(t.name, { texs[sel] = t.copy(name = it.take(31)) }, Modifier.fillMaxWidth().padding(top = 16.dp),
                        label = { Text("Nama tekstur") }, singleLine = true)
                    Text("Format kompresi", color = MU, fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TexFormat.entries.forEach { f -> Btn(f.label, f == t.format, Modifier.weight(1f)) { texs[sel] = t.copy(format = f) } }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Buat mipmap", color = TX, fontWeight = FontWeight.SemiBold)
                        Switch(t.mipmaps, { texs[sel] = t.copy(mipmaps = it) })
                    }
                    val kb = remember(t.format, t.mipmaps, t.w, t.h) { TxdWriter.build(listOf(t)).size / 1024 }
                    Text("Perkiraan ukuran: ± $kb KB", color = MU, modifier = Modifier.padding(top = 12.dp))
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Btn("Hapus", false, Modifier.weight(1f)) { texs.removeAt(sel); screen = if (texs.isEmpty()) "home" else "editor" }
                        Btn("Selesai", true, Modifier.weight(1f)) { screen = "editor" }
                    }
                }
            }
            else -> {
                Top("Tentang") { screen = "home" }
                Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("TXD Studio", color = TX, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("Versi 1.0.0", color = MU, modifier = Modifier.padding(top = 6.dp))
                    Text("Konversi PNG ke TXD untuk berbagai platform game.", color = MU, textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
                }
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SF).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Created By", color = MU, fontSize = 13.sp)
                    Text("Tutorial All Games", color = AC, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
