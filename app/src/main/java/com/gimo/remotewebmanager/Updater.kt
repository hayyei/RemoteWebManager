package com.gimo.remotewebmanager

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** 基于 GitHub Releases 的应用内自升级，需仓库公开。 */
object Updater {
    private const val RELEASE_API = "https://api.github.com/repos/hayyei/RemoteWebManager/releases/latest"
    private const val PREFS = "updater"
    private const val KEY_LAST_CHECK = "last_check_ms"
    private const val KEY_PENDING_APK = "pending_apk"
    private const val CHECK_INTERVAL_MS = 24 * 3600 * 1000L

    private data class UpdateInfo(val version: String, val notes: String, val apkUrl: String)

    private fun currentVersion(a: AppCompatActivity): String = try {
        a.packageManager.getPackageInfo(a.packageName, 0).versionName ?: "0"
    } catch (_: Exception) { "0" }

    private fun isNewer(remote: String, current: String): Boolean {
        val r = remote.split('.').map { it.trim().filter(Char::isDigit).toIntOrNull() ?: 0 }
        val c = current.split('.').map { it.trim().filter(Char::isDigit).toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(r.size, c.size)) {
            val a = r.getOrElse(i) { 0 }; val b = c.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    /** 手动检查：有提示、有结果反馈。 */
    fun checkNow(activity: AppCompatActivity) {
        Toast.makeText(activity, "正在检查更新…", Toast.LENGTH_SHORT).show()
        activity.lifecycleScope.launch {
            val info = withContext(Dispatchers.IO) { fetchLatest() }
            when {
                info == null -> Toast.makeText(activity, "检查更新失败，请检查网络", Toast.LENGTH_SHORT).show()
                !isNewer(info.version, currentVersion(activity)) -> Toast.makeText(activity, "已是最新版本", Toast.LENGTH_SHORT).show()
                else -> confirmAndDownload(activity, info)
            }
        }
    }

    /** 打开应用时自动检查（24 小时节流），静默失败。 */
    fun maybeAutoCheck(activity: AppCompatActivity) {
        val prefs = activity.getSharedPreferences(PREFS, AppCompatActivity.MODE_PRIVATE)
        if (System.currentTimeMillis() - prefs.getLong(KEY_LAST_CHECK, 0) < CHECK_INTERVAL_MS) { resumePending(activity); return }
        prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
        activity.lifecycleScope.launch {
            val info = withContext(Dispatchers.IO) { fetchLatest() } ?: return@launch
            if (isNewer(info.version, currentVersion(activity))) confirmAndDownload(activity, info)
        }
    }

    /** 用户授权「安装未知应用」回来后继续装上次下载好的包。 */
    fun resumePending(activity: AppCompatActivity) {
        if (Build.VERSION.SDK_INT >= 26 && !activity.packageManager.canRequestPackageInstalls()) return
        val prefs = activity.getSharedPreferences(PREFS, AppCompatActivity.MODE_PRIVATE)
        val path = prefs.getString(KEY_PENDING_APK, null) ?: return
        prefs.edit().remove(KEY_PENDING_APK).apply()
        val apk = File(path)
        if (apk.isFile) install(activity, apk)
    }

    private fun confirmAndDownload(activity: AppCompatActivity, info: UpdateInfo) {
        AlertDialog.Builder(activity)
            .setTitle("发现新版本 v${info.version}")
            .setMessage(info.notes.ifBlank { "是否下载并安装新版本？" })
            .setPositiveButton("下载更新") { _, _ -> download(activity, info) }
            .setNegativeButton("以后再说", null)
            .show()
    }

    private fun download(activity: AppCompatActivity, info: UpdateInfo) {
        val view = activity.layoutInflater.inflate(R.layout.dialog_download, null)
        val bar = view.findViewById<android.widget.ProgressBar>(R.id.bar)
        val pct = view.findViewById<android.widget.TextView>(R.id.pct)
        val dlg = AlertDialog.Builder(activity)
            .setTitle("正在下载 v${info.version}").setView(view).setCancelable(false).create()
        dlg.show()
        activity.lifecycleScope.launch {
            try {
                val file = File(activity.cacheDir, "updates/RemoteWebManager-${info.version}.apk")
                withContext(Dispatchers.IO) {
                    downloadApk(info.apkUrl, file) { p ->
                        activity.runOnUiThread { bar.progress = p; pct.text = "$p%" }
                    }
                }
                dlg.dismiss()
                install(activity, file)
            } catch (e: Exception) {
                dlg.dismiss()
                Toast.makeText(activity, "下载失败：${e.message ?: "网络错误"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun install(activity: AppCompatActivity, apk: File) {
        if (Build.VERSION.SDK_INT >= 26 && !activity.packageManager.canRequestPackageInstalls()) {
            activity.getSharedPreferences(PREFS, AppCompatActivity.MODE_PRIVATE)
                .edit().putString(KEY_PENDING_APK, apk.absolutePath).apply()
            Toast.makeText(activity, "请允许「安装未知应用」后自动继续安装", Toast.LENGTH_LONG).show()
            activity.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}")))
            return
        }
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", apk)
        activity.startActivity(
            Intent(Intent.ACTION_INSTALL_PACKAGE).setData(uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        )
    }

    private fun fetchLatest(): UpdateInfo? = try {
        val conn = URL(RELEASE_API).openConnection() as HttpURLConnection
        conn.connectTimeout = 10000; conn.readTimeout = 10000
        conn.setRequestProperty("User-Agent", "RemoteWebManager-App")
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        val text = if (conn.responseCode == 200) conn.inputStream.bufferedReader().use { it.readText() } else null
        conn.disconnect()
        if (text == null) null else parseRelease(text)
    } catch (_: Exception) { null }

    private fun parseRelease(text: String): UpdateInfo? {
        val obj = JSONObject(text)
        var apkUrl: String? = null
        val assets = obj.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            if (a.getString("name").endsWith(".apk", ignoreCase = true)) { apkUrl = a.getString("browser_download_url"); break }
        }
        apkUrl ?: return null
        return UpdateInfo(version = obj.getString("tag_name").removePrefix("v"), notes = obj.optString("body"), apkUrl = apkUrl)
    }

    private fun downloadApk(url: String, target: File, onProgress: (Int) -> Unit) {
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, target.name + ".part")
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000; conn.readTimeout = 60000
        conn.setRequestProperty("User-Agent", "RemoteWebManager-App")
        val total = conn.contentLengthLong
        conn.inputStream.use { input ->
            tmp.outputStream().use { out ->
                val buf = ByteArray(64 * 1024)
                var read = 0L; var lastPct = -1
                while (true) {
                    val n = input.read(buf); if (n < 0) break
                    out.write(buf, 0, n); read += n
                    if (total > 0) {
                        val p = (read * 100 / total).toInt()
                        if (p != lastPct) { lastPct = p; onProgress(p) }
                    }
                }
            }
        }
        if (total > 0 && tmp.length() < total) { tmp.delete(); throw IllegalStateException("下载不完整") }
        if (!tmp.renameTo(target)) { tmp.delete(); throw IllegalStateException("保存失败") }
    }
}
