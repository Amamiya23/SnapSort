package com.snapsort.app.data.update

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class UpdateRepository {
    suspend fun checkForUpdates(
        currentVersionName: String,
        currentVersionCode: Int
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val release = fetchLatestRelease()
            if (release.tagName.isBlank() || release.releaseUrl.isBlank()) {
                return@withContext UpdateCheckResult.Failed("更新信息不完整，请稍后重试。")
            }
            val updateAvailable = UpdateVersionComparator.isUpdateAvailable(
                currentVersionName = currentVersionName,
                currentVersionCode = currentVersionCode,
                latestTagName = release.tagName
            )
            if (updateAvailable) {
                UpdateCheckResult.Available(release)
            } else {
                UpdateCheckResult.UpToDate
            }
        } catch (_: IOException) {
            UpdateCheckResult.Failed("无法连接到 GitHub，请检查网络后重试。")
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: RuntimeException) {
            UpdateCheckResult.Failed("无法读取更新信息，请稍后重试。")
        }
    }

    private fun fetchLatestRelease(): ReleaseInfo {
        val connection = (URL(LatestReleaseUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2026-03-10")
        }

        return connection.useResponse { response ->
            if (response.responseCode !in 200..299) {
                throw IOException("GitHub release request failed: ${response.responseCode}")
            }
            val json = JSONObject(response.inputStream.bufferedReader().use { it.readText() })
            ReleaseInfo(
                tagName = json.optString("tag_name").trim(),
                releaseUrl = json.optString("html_url").trim()
            )
        }
    }

    private inline fun <T> HttpURLConnection.useResponse(block: (HttpURLConnection) -> T): T {
        return try {
            block(this)
        } finally {
            disconnect()
        }
    }

    private companion object {
        private const val LatestReleaseUrl = "https://api.github.com/repos/Amamiya23/SnapSort/releases/latest"
    }
}

data class ReleaseInfo(
    val tagName: String,
    val releaseUrl: String
)

sealed interface UpdateCheckResult {
    data class Available(val release: ReleaseInfo) : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data class Failed(val message: String) : UpdateCheckResult
}
