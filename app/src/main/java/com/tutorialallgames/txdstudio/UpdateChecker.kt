package com.tutorialallgames.txdstudio

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cek update: saat aplikasi dibuka, baca file update.json di repo (cabang main).
 * Kalau versionCode di file itu lebih besar dari versi yang terpasang, tampilkan dialog update.
 * Untuk mengumumkan update: naikkan versionCode di update.json, isi message dan url unduhan.
 */
private const val UPDATE_URL =
    "https://raw.githubusercontent.com/NeonCode-lab/TXD-Converter/main/update.json"

private val AC = Color(0xFFFF8A3D)

private data class UpdateInfo(val code: Int, val name: String, val message: String, val url: String)

@Suppress("DEPRECATION")
private fun installedCode(ctx: Context): Int =
    runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionCode }.getOrDefault(Int.MAX_VALUE)

/** Dipanggil di thread latar. Mengembalikan null kalau offline, file belum ada, atau isinya rusak. */
private fun fetchUpdate(): UpdateInfo? = runCatching {
    val c = URL(UPDATE_URL).openConnection() as HttpURLConnection
    c.connectTimeout = 6000
    c.readTimeout = 6000
    try {
        if (c.responseCode != 200) return@runCatching null
        val j = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
        UpdateInfo(j.getInt("versionCode"), j.optString("versionName"), j.optString("message"), j.optString("url"))
    } finally {
        c.disconnect()
    }
}.getOrNull()

@Composable
fun UpdateDialog() {
    val ctx = LocalContext.current
    var info by remember { mutableStateOf<UpdateInfo?>(null) }
    LaunchedEffect(Unit) {
        val r = withContext(Dispatchers.IO) { fetchUpdate() }
        if (r != null && r.code > installedCode(ctx)) info = r
    }
    info?.let { u ->
        AlertDialog(
            onDismissRequest = { info = null },
            title = { Text(if (u.name.isNotBlank()) "Update tersedia · v${u.name}" else "Update tersedia") },
            text = { Text(u.message.ifBlank { "Versi baru TXD Studio sudah tersedia." }) },
            confirmButton = {
                // Hanya tautan https yang dibuka
                if (u.url.startsWith("https://")) {
                    TextButton(onClick = {
                        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u.url))) }
                        info = null
                    }) { Text("Perbarui", color = AC) }
                }
            },
            dismissButton = { TextButton(onClick = { info = null }) { Text("Nanti", color = AC) } }
        )
    }
}
