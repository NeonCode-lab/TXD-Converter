package com.tutorialallgames.txdstudio

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cek update: baca file update.json di repo (cabang main).
 * Kalau versionCode di file itu lebih besar dari versi yang terpasang, tampilkan dialog update.
 * Untuk mengumumkan update: naikkan versionCode di update.json, isi message dan url unduhan.
 *
 * - [UpdateDialog]   : cek otomatis saat aplikasi dibuka (diam kalau tidak ada update / gagal).
 * - [UpdateCheckRow] : tombol "Cek update" manual di layar Tentang, selalu menampilkan hasilnya.
 */
private const val UPDATE_URL =
    "https://raw.githubusercontent.com/NeonCode-lab/TXD-Converter/main/update.json"

private val AC = Color(0xFFFF8A3D)
private val MU = Color(0xFF9AA3B2)

private data class UpdateInfo(val code: Int, val name: String, val message: String, val url: String)

private sealed interface Check {
    data class Available(val info: UpdateInfo) : Check
    data class UpToDate(val installed: Int, val server: Int) : Check
    data class Failed(val reason: String) : Check
}

@Suppress("DEPRECATION")
private fun installedCode(ctx: Context): Int =
    runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionCode }.getOrDefault(Int.MAX_VALUE)

/** Dipanggil di thread latar. Selalu mengembalikan hasil, tidak pernah melempar error. */
private fun check(ctx: Context): Check = try {
    val c = URL(UPDATE_URL).openConnection() as HttpURLConnection
    c.connectTimeout = 6000
    c.readTimeout = 6000
    try {
        if (c.responseCode != 200) {
            Check.Failed("Server menjawab kode ${c.responseCode}")
        } else {
            val j = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
            val u = UpdateInfo(j.getInt("versionCode"), j.optString("versionName"), j.optString("message"), j.optString("url"))
            val have = installedCode(ctx)
            if (u.code > have) Check.Available(u) else Check.UpToDate(have, u.code)
        }
    } finally {
        c.disconnect()
    }
} catch (e: JSONException) {
    Check.Failed("Isi update.json tidak valid")
} catch (e: IOException) {
    Check.Failed("Tidak bisa terhubung ke GitHub (cek internet / jaringan)")
} catch (e: Exception) {
    Check.Failed("Terjadi kesalahan: ${e.javaClass.simpleName}")
}

@Composable
private fun UpdateInfoDialog(u: UpdateInfo, onClose: () -> Unit) {
    val ctx = LocalContext.current
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (u.name.isNotBlank()) "Update tersedia · v${u.name}" else "Update tersedia") },
        text = { Text(u.message.ifBlank { "Versi baru TXD Studio sudah tersedia." }) },
        confirmButton = {
            // Hanya tautan https yang dibuka
            if (u.url.startsWith("https://")) {
                TextButton(onClick = {
                    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u.url))) }
                    onClose()
                }) { Text("Perbarui", color = AC) }
            }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Nanti", color = AC) } }
    )
}

/** Cek otomatis saat aplikasi dibuka. Diam kalau tidak ada update atau gagal. */
@Composable
fun UpdateDialog() {
    val ctx = LocalContext.current
    var info by remember { mutableStateOf<UpdateInfo?>(null) }
    LaunchedEffect(Unit) {
        val r = withContext(Dispatchers.IO) { check(ctx) }
        if (r is Check.Available) info = r.info
    }
    info?.let { UpdateInfoDialog(it) { info = null } }
}

/** Tombol "Cek update" manual: selalu menampilkan hasil (tersedia / terbaru / gagal + alasannya). */
@Composable
fun UpdateCheckRow() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<UpdateInfo?>(null) }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        TextButton(
            enabled = !busy,
            onClick = {
                busy = true
                status = null
                scope.launch {
                    val r = withContext(Dispatchers.IO) { check(ctx) }
                    busy = false
                    when (r) {
                        is Check.Available -> {
                            info = r.info
                            status = "Update tersedia (versi terpasang: ${r.info.code - 0 .coerceAtMost(0)})"
                        }
                        is Check.UpToDate -> status =
                            "Sudah versi terbaru ✓\n(kode terpasang ${r.installed} · kode di server ${r.server})"
                        is Check.Failed -> status = "Gagal memeriksa: ${r.reason}"
                    }
                }
            }
        ) { Text(if (busy) "Memeriksa…" else "Cek update", color = AC) }
        status?.let { Text(it, color = MU, fontSize = 12.sp, textAlign = TextAlign.Center) }
    }
    info?.let { UpdateInfoDialog(it) { info = null } }
}
