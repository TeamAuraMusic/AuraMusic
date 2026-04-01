/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.utils

import com.auramusic.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONObject

data class ReleaseInfo(
    val tagName: String,
    val versionName: String,
    val description: String,
    val releaseDate: String,
    val assets: List<ReleaseAsset>
)

data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val size: Long,
    val architecture: String,
    val variant: String // "foss" or "gms"
)

object Updater {
    private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build()
    var lastCheckTime = -1L
        private set
    
    private var cachedReleaseInfo: ReleaseInfo? = null
    private var cachedAllReleases: List<ReleaseInfo> = emptyList()
    
    private const val CHECK_INTERVAL_MILLIS = 2 * 60 * 60 * 1000L // 2 hours
    private const val GITHUB_API_BASE = "https://api.github.com/repos/chila254/AuraMusic"

    /**
     * Compares two version strings.
     * Returns: 1 if v1 > v2, -1 if v1 < v2, 0 if equal
     */
    fun compareVersions(v1: String, v2: String): Int {
        val cleanV1 = extractVersionNumber(v1)
        val cleanV2 = extractVersionNumber(v2)
        
        val v1Parts = cleanV1.split(".").map { it.toIntOrNull() ?: 0 }
        val v2Parts = cleanV2.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLength = maxOf(v1Parts.size, v2Parts.size)
        
        for (i in 0 until maxLength) {
            val part1 = v1Parts.getOrNull(i) ?: 0
            val part2 = v2Parts.getOrNull(i) ?: 0
            when {
                part1 > part2 -> return 1
                part1 < part2 -> return -1
            }
        }
        return 0
    }
    
    /**
     * Extracts version number from a string like "AuraMusic v1.0.7" or "v1.0.7"
     * Returns just the version number part (e.g., "1.0.7")
     */
    private fun extractVersionNumber(versionString: String): String {
        // Try to find a pattern like "v1.0.7" or "1.0.7" at the end of the string
        val regex = Regex("""(?:v|ver|version)?(\d+\.\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
        val match = regex.find(versionString)
        return match?.groupValues?.getOrNull(1) ?: versionString.trimStart('v')
    }

    /**
     * Checks if the latest version is newer than the current version.
     * Returns true if an update is available (latestVersion > currentVersion)
     */
    fun isUpdateAvailable(currentVersion: String, latestVersion: String): Boolean {
        return compareVersions(latestVersion, currentVersion) > 0
    }

    /**
     * Get the current app's architecture and variant
     */
    private fun getCurrentAppVariant(): Pair<String, String> {
        val architecture = BuildConfig.ARCHITECTURE
        val variant = if (BuildConfig.CAST_AVAILABLE) "gms" else "foss"
        return architecture to variant
    }

    /**
     * Parse release assets from GitHub API response
     */
    private fun parseAssets(assetsArray: JSONArray): List<ReleaseAsset> {
        val assets = mutableListOf<ReleaseAsset>()
        
        for (i in 0 until assetsArray.length()) {
            val asset = assetsArray.getJSONObject(i)
            val name = asset.getString("name")
            
            // Skip non-APK files
            if (!name.endsWith(".apk")) continue
            
            val downloadUrl = asset.getString("browser_download_url")
            val size = asset.getLong("size")
            
            // Parse architecture and variant from filename
            val (arch, variant) = when {
                name == "Auramusic.apk" || name == "AuraMusic.apk" -> "universal" to "foss"
                name == "Auramusic-with-Google-Cast.apk" || name == "AuraMusic-with-Google-Cast.apk" -> "universal" to "gms"
                name.startsWith("app-") && name.endsWith("-release.apk") -> {
                    val arch = name.removePrefix("app-").removeSuffix("-release.apk")
                    arch to "foss"
                }
                name.startsWith("app-") && name.endsWith("-with-Google-Cast.apk") -> {
                    val arch = name.removePrefix("app-").removeSuffix("-with-Google-Cast.apk")
                    arch to "gms"
                }
                else -> null to null
            }
            
            if (arch != null && variant != null) {
                assets.add(ReleaseAsset(name, downloadUrl, size, arch, variant))
            }
        }
        
        return assets
    }

    /**
     * Fetch latest release from GitHub API
     * Falls back to fetching all releases if latest endpoint fails
     */
    suspend fun getLatestRelease(forceRefresh: Boolean = false): Result<ReleaseInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                // Return cached if available and not forcing refresh
                if (cachedReleaseInfo != null && !forceRefresh) {
                    return@runCatching cachedReleaseInfo!!
                }
                
                // First try the /releases/latest endpoint
                val request = Request.Builder()
                    .url("$GITHUB_API_BASE/releases/latest")
                    .build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""
                
                var releaseInfo: ReleaseInfo? = null
                
                // Try to parse as latest release
                if (response.isSuccessful && body.isNotEmpty()) {
                    try {
                        val json = JSONObject(body)
                        if (json.has("tag_name")) {
                            releaseInfo = ReleaseInfo(
                                tagName = json.getString("tag_name"),
                                versionName = json.getString("name"),
                                description = json.getString("body"),
                                releaseDate = json.getString("published_at"),
                                assets = parseAssets(json.getJSONArray("assets"))
                            )
                        }
                    } catch (e: Exception) {
                        // Failed to parse, will try fallback
                    }
                }
                
                // Fallback: fetch all releases and use the first one (most recent)
                if (releaseInfo == null) {
                    val allReleasesRequest = Request.Builder()
                        .url("$GITHUB_API_BASE/releases?per_page=1")
                        .build()
                    val allReleasesResponse = client.newCall(allReleasesRequest).execute().body?.string() ?: ""
                    
                    if (allReleasesResponse.isNotEmpty()) {
                        val jsonArray = JSONArray(allReleasesResponse)
                        if (jsonArray.length() > 0) {
                            val releaseObj = jsonArray.getJSONObject(0)
                            releaseInfo = ReleaseInfo(
                                tagName = releaseObj.getString("tag_name"),
                                versionName = releaseObj.getString("name"),
                                description = releaseObj.getString("body"),
                                releaseDate = releaseObj.getString("published_at"),
                                assets = parseAssets(releaseObj.getJSONArray("assets"))
                            )
                        }
                    }
                }
                
                releaseInfo ?: throw Exception("No releases found")
            }.also { result ->
                if (result.isSuccess) {
                    cachedReleaseInfo = result.getOrNull()
                    lastCheckTime = System.currentTimeMillis()
                }
            }
        }

    /**
     * Fetch all releases from GitHub API (paginated)
     */
    suspend fun getAllReleases(forceRefresh: Boolean = false): Result<List<ReleaseInfo>> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (cachedAllReleases.isNotEmpty() && !forceRefresh) {
                    return@runCatching cachedAllReleases
                }
                
                val releases = mutableListOf<ReleaseInfo>()
                var page = 1
                var hasMore = true
                
                while (hasMore && page <= 10) { // Limit to 10 pages
                    val request = Request.Builder()
                        .url("$GITHUB_API_BASE/releases?page=$page&per_page=30")
                        .build()
                    val response = client.newCall(request).execute().body?.string() ?: ""
                    val json = JSONArray(response)
                    
                    if (json.length() == 0) {
                        hasMore = false
                        break
                    }
                    
                    for (i in 0 until json.length()) {
                        val releaseObj = json.getJSONObject(i)
                        releases.add(ReleaseInfo(
                            tagName = releaseObj.getString("tag_name"),
                            versionName = releaseObj.getString("name"),
                            description = releaseObj.getString("body"),
                            releaseDate = releaseObj.getString("published_at"),
                            assets = parseAssets(releaseObj.getJSONArray("assets"))
                        ))
                    }
                    
                    page++
                }
                
                cachedAllReleases = releases
                releases
            }
        }

    /**
     * Get the download URL for the correct app variant
     */
    fun getDownloadUrlForCurrentVariant(releaseInfo: ReleaseInfo): String? {
        val (currentArch, currentVariant) = getCurrentAppVariant()
        
        // First try to find exact match
        val exactMatch = releaseInfo.assets
            .find { it.architecture == currentArch && it.variant == currentVariant }
            ?.downloadUrl
        
        if (exactMatch != null) return exactMatch
        
        // Fallback: if we can't find exact match, try to find any APK for this variant
        val fallbackMatch = releaseInfo.assets
            .find { it.variant == currentVariant }
            ?.downloadUrl
        
        if (fallbackMatch != null) return fallbackMatch
        
        // Last resort: return any available APK
        return releaseInfo.assets.firstOrNull()?.downloadUrl
    }

    /**
     * Get all available download URLs for a release
     */
    fun getAllDownloadUrls(releaseInfo: ReleaseInfo): Map<String, String> {
        return releaseInfo.assets.associate { "${it.architecture}-${it.variant}" to it.downloadUrl }
    }

    /**
     * Check if update is needed (respects 2-hour cache)
     */
    suspend fun checkForUpdate(forceRefresh: Boolean = false): Result<Pair<ReleaseInfo?, Boolean>> =
        withContext(Dispatchers.IO) {
            runCatching {
                // Check if we should fetch (2 hour interval)
                val shouldFetch = forceRefresh || 
                    (System.currentTimeMillis() - lastCheckTime) > CHECK_INTERVAL_MILLIS
                
                if (!shouldFetch && cachedReleaseInfo != null) {
                    val hasUpdate = isUpdateAvailable(
                        BuildConfig.VERSION_NAME,
                        cachedReleaseInfo!!.versionName
                    )
                    return@runCatching cachedReleaseInfo!! to hasUpdate
                }
                
                val result = getLatestRelease(forceRefresh = true)
                if (result.isSuccess) {
                    val releaseInfo = result.getOrThrow()
                    val hasUpdate = isUpdateAvailable(
                        BuildConfig.VERSION_NAME,
                        releaseInfo.versionName
                    )
                    releaseInfo to hasUpdate
                } else {
                    throw result.exceptionOrNull() ?: Exception("Unknown error")
                }
            }
        }

    /**
     * Get the download URL for the correct app variant
     * Returns null if no matching asset is found
     */
    fun getLatestDownloadUrl(): String? {
        return cachedReleaseInfo?.let { getDownloadUrlForCurrentVariant(it) }
    }
    
    /**
     * Get the latest release info (cached)
     */
    fun getCachedLatestRelease(): ReleaseInfo? = cachedReleaseInfo
}