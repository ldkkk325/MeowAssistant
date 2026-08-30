package com.meow.assistant.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.meow.assistant.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val tagName: String,
    val title: String,
    val releaseUrl: String,
    val downloadUrl: String,
    val assetName: String,
)

object UpdateManager {
    const val REPOSITORY_URL = "https://github.com/ldkkk325/miao-helper-releases"
    const val RELEASES_URL = "$REPOSITORY_URL/releases"
    private const val API_URL = "https://api.github.com/repos/ldkkk325/miao-helper-releases/releases/latest"
    private const val PROXY_PREFIX = "https://v4.gh-proxy.org/"

    suspend fun checkForUpdate(): ReleaseInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(API_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 15_000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "MiaoAssistant/${BuildConfig.VERSION_NAME}")
            }
            connection.useConnection { input ->
                if (responseCode !in 200..299) return@useConnection null
                val release = JSONObject(input.bufferedReader().use { it.readText() })
                val tagName = release.optString("tag_name").trim()
                if (tagName.isEmpty() || !isNewer(tagName, BuildConfig.VERSION_NAME)) return@useConnection null
                val assets = release.optJSONArray("assets") ?: return@useConnection null
                var assetName = ""
                var downloadUrl = ""
                for (index in 0 until assets.length()) {
                    val asset = assets.optJSONObject(index) ?: continue
                    val candidate = asset.optString("name")
                    val candidateUrl = asset.optString("browser_download_url")
                    if (candidate.endsWith(".apk", ignoreCase = true) && candidateUrl.startsWith("https://")) {
                        assetName = candidate
                        downloadUrl = candidateUrl
                        break
                    }
                }
                if (downloadUrl.isEmpty()) return@useConnection null
                ReleaseInfo(
                    tagName = tagName,
                    title = release.optString("name").ifBlank { tagName },
                    releaseUrl = release.optString("html_url").ifBlank { RELEASES_URL },
                    downloadUrl = downloadUrl,
                    assetName = assetName,
                )
            }
        }.getOrNull()
    }

    suspend fun downloadAndInstall(context: Context, release: ReleaseInfo): Result<Unit> {
        val apkResult = withContext(Dispatchers.IO) {
            runCatching {
                val updateDirectory = File(context.cacheDir, "updates").apply { mkdirs() }
                val safeName = release.assetName.replace(Regex("[^A-Za-z0-9._-]"), "_")
                val apkFile = File(updateDirectory, safeName)
                if (!apkFile.isFile || apkFile.length() < 2 || !isApk(apkFile)) {
                    val connection = (URL(PROXY_PREFIX + release.downloadUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 20_000
                        readTimeout = 60_000
                        instanceFollowRedirects = true
                        requestMethod = "GET"
                        setRequestProperty("User-Agent", "MiaoAssistant/${BuildConfig.VERSION_NAME}")
                    }
                    connection.useConnection { input ->
                        if (responseCode !in 200..299) error("Download failed: HTTP $responseCode")
                        apkFile.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                check(isApk(apkFile)) { "Downloaded file is not an APK" }
                apkFile
            }
        }
        val apkFile = apkResult.getOrElse {
            return Result.failure(it)
        }
        return runCatching {
            withContext(Dispatchers.Main) { install(context, apkFile) }
        }
    }

    private fun install(context: Context, apkFile: File) {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun isApk(file: File): Boolean = file.isFile && file.length() > 2 && file.inputStream().use {
        it.read() == 'P'.code && it.read() == 'K'.code
    }

    private fun isNewer(remote: String, current: String): Boolean {
        val remoteParts = versionParts(remote)
        val currentParts = versionParts(current)
        if (remoteParts.isEmpty() || currentParts.isEmpty()) return false
        val size = maxOf(remoteParts.size, currentParts.size)
        for (index in 0 until size) {
            val remotePart = remoteParts.getOrElse(index) { 0 }
            val currentPart = currentParts.getOrElse(index) { 0 }
            if (remotePart != currentPart) return remotePart > currentPart
        }
        return false
    }

    private fun versionParts(value: String): List<Int> = Regex("\\d+")
        .findAll(value)
        .mapNotNull { it.value.toIntOrNull() }
        .toList()

    private inline fun <T> HttpURLConnection.useConnection(block: HttpURLConnection.(java.io.InputStream) -> T): T {
        return try {
            inputStream.use { input -> block(input) }
        } finally {
            disconnect()
        }
    }
}
