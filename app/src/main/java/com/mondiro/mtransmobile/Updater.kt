package com.mondiro.mtransmobile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.io.File

data class UpdateInfo(val latest: String, val hasUpdate: Boolean, val url: String)

/** Self-update from GitHub Releases of the public MTrans-Mobile repo. */
object Updater {
    private const val LATEST = "https://api.github.com/repos/satthupc00/MTrans-Mobile/releases/latest"

    private fun parts(v: String) = v.trimStart('v', 'V').split('.', '-').take(3).map { it.toIntOrNull() ?: 0 }

    fun newer(a: String, b: String): Boolean {
        val x = parts(a)
        val y = parts(b)
        for (i in 0 until 3) {
            val d = x.getOrElse(i) { 0 } - y.getOrElse(i) { 0 }
            if (d != 0) return d > 0
        }
        return false
    }

    suspend fun check(current: String): UpdateInfo = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(LATEST)
            .header("User-Agent", "MTrans-Mobile")
            .header("Accept", "application/vnd.github+json")
            .build()
        GoogleTranslate.http.newCall(req).execute().use { resp ->
            if (resp.code == 404) return@withContext UpdateInfo(current, false, "")
            if (!resp.isSuccessful) throw TranslateException("GitHub trả lỗi ${resp.code}")
            val json = JSONObject(resp.body?.string() ?: "{}")
            val latest = json.optString("tag_name").trimStart('v')
            val assets = json.optJSONArray("assets")
            var url = ""
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    if (a.optString("name").endsWith(".apk")) {
                        url = a.optString("browser_download_url")
                        break
                    }
                }
            }
            UpdateInfo(latest, url.isNotEmpty() && newer(latest, current), url)
        }
    }

    /** Download the APK and open the system installer. Returns false if install permission is needed first. */
    suspend fun install(context: Context, url: String): Boolean {
        if (!url.startsWith("https://github.com/satthupc00/MTrans-Mobile/")) throw TranslateException("Đường dẫn cập nhật không hợp lệ")
        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
            val i = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
            return false
        }
        val file = withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            val f = File(dir, "MTrans-Mobile.apk")
            val req = Request.Builder().url(url).header("User-Agent", "MTrans-Mobile").build()
            GoogleTranslate.http.newBuilder().readTimeout(120, java.util.concurrent.TimeUnit.SECONDS).build()
                .newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) throw TranslateException("Tải bản cập nhật lỗi ${resp.code}")
                    resp.body!!.byteStream().use { input -> f.outputStream().use { input.copyTo(it) } }
                }
            f
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return true
    }
}
