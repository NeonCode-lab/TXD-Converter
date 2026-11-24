package com.tutorialallgames.txdstudio

import android.graphics.Bitmap
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val SF = Color(0xFF181B22)
private val AC = Color(0xFFFF8A3D)
private val MU = Color(0xFF9AA3B2)
private val TX = Color(0xFFE8EAF0)

@Composable
private fun TxdTop(title: String, onBack: () -> Unit) =
    Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onBack) { Text("‹ Kembali", color = AC) }
        Spacer(Modifier.width(8.dp))
        Text(title, color = TX, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }

/** Pratinjau tekstur. Di-decode malas di thread latar, jadi daftar besar tetap lancar. */
@Composable
private fun TxdPreview(f: TxdFile, e: TxdEntry, side: Int, m: Modifier) {
    var done by remember(e, side) { mutableStateOf(false) }
    val bm by produceState<Bitmap?>(null, e, side) {
        value = withContext(Dispatchers.Default) { f.preview(e, side) }
        done = true
    }
    Box(m.clip(RoundedCornerShape(8.dp)).background(Color(0xFF222630)), contentAlignment = Alignment.Center) {
        val b = bm
        if (b != null) {
            val img = remember(b) { b.asImageBitmap() }
            Image(img, e.name, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        } else {
            Text(if (done || e.fmt == null) "Tidak bisa ditampilkan" else "…", color = MU, fontSize = 12.sp)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) =
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MU, fontSize = 14.sp)
        Text(value, color = TX, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }

/** Daftar semua tekstur di dalam file TXD. */
@Composable
fun ColumnScope.TxdListScreen(title: String, f: TxdFile, onBack: () -> Unit, onOpen: (Int) -> Unit) {
    TxdTop(title, onBack)
    if (f.entries.size < f.declared) {
        Text(
            "File terpotong: hanya ${f.entries.size} dari ${f.declared} tekstur yang terbaca",
            color = AC, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp)
        )
    }
    LazyVerticalGrid(
        GridCells.Fixed(2), Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(f.entries) { i, e ->
            Column(
                Modifier.clip(RoundedCornerShape(14.dp)).background(SF)
                    .clickable { onOpen(i) }.padding(8.dp)
            ) {
                TxdPreview(f, e, 256, Modifier.fillMaxWidth().height(110.dp))
                Text(
                    e.name, color = TX, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp)
                )
                Text("${e.sizeLabel} · ${e.formatLabel}", color = MU, fontSize = 13.sp, maxLines = 1)
                Text(
                    when {
                        e.note != null -> "Belum didukung"
                        e.alpha -> "Dengan alpha"
                        else -> "Tanpa alpha"
                    },
                    color = if (e.alpha && e.note == null) AC else MU, fontSize = 12.sp
                )
            }
        }
    }
}

/** Detail satu tekstur: pratinjau besar + info format. */
@Composable
fun ColumnScope.TxdDetailScreen(f: TxdFile, e: TxdEntry, onBack: () -> Unit) {
    TxdTop(e.name, onBack)
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        TxdPreview(f, e, 512, Modifier.fillMaxWidth().height(280.dp))
        Column(Modifier.fillMaxWidth().padding(top = 16.dp).clip(RoundedCornerShape(14.dp)).background(SF).padding(16.dp)) {
            InfoRow("Nama", e.name)
            InfoRow("Ukuran", e.sizeLabel)
            InfoRow("Format", e.formatLabel)
            InfoRow("Alpha", if (e.alpha) "Ya" else "Tidak")
            InfoRow("Mipmap", if (e.levels.size > 1) "${e.levels.size} level" else "Tidak ada")
            InfoRow("Ukuran data", "± ${maxOf(1, e.dataBytes / 1024)} KB")
        }
        e.note?.let { Text(it, color = AC, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp)) }
    }
}
