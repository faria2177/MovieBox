package com.beegear.app

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.SocketException
import java.net.URL
import java.net.URLDecoder
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

data class Category(val name: String, val url: String)
data class MediaEntry(
    val name: String,
    val url: String,
    val isFolder: Boolean,
    val isVideo: Boolean,
    val isImage: Boolean,
    val lastModifiedMs: Long = 0L,
    val sourceOrder: Int = 0
)
data class FolderLocation(val title: String, val url: String, val poster: String? = null)
data class Recommendation(val title: String, val url: String, val poster: String?, val subtitle: String, val score: Long = 0L)
data class SearchHit(val title: String, val url: String, val poster: String?, val subtitle: String, val isVideo: Boolean)

/* ============================================================================
   THEME & BACKGROUND COLOR PALETTES (GLASS UI + RICHER DARK EMERALD)
   ============================================================================ */
private data class BgPalette(
    val id: String,
    val label: String,
    val swatchColor: Color,
    val lightBg: Color,
    val lightCard: Color,
    val lightTag: Color,
    val lightPrimaryText: Color,
    val lightMutedText: Color,
    val lightAccent: Color,
    val darkBg: Color,
    val darkCard: Color,
    val darkTag: Color,
    val darkPrimaryText: Color,
    val darkMutedText: Color,
    val darkAccent: Color
)

private val bgPalettes = listOf(
    BgPalette(
        id = "cool_blue",
        label = "Cool Blue",
        swatchColor = Color(0xFF6579E8),
        lightBg = Color(0xFFF5F7FC),
        lightCard = Color(0xFFE8EDF7),
        lightTag = Color(0xFFE8EDFA),
        lightPrimaryText = Color(0xFF263B62),
        lightMutedText = Color(0xFF718097),
        lightAccent = Color(0xFF6579E8),
        darkBg = Color(0xFF11131B),
        darkCard = Color(0xFF222633),
        darkTag = Color(0xFF30384B),
        darkPrimaryText = Color(0xFFF4F6FC),
        darkMutedText = Color(0xFFADB7CA),
        darkAccent = Color(0xFF7A8DF5)
    ),
    BgPalette(
        id = "indigo_mist",
        label = "Indigo Mist",
        swatchColor = Color(0xFF7E69E8),
        lightBg = Color(0xFFF6F4FC),
        lightCard = Color(0xFFEBE7F8),
        lightTag = Color(0xFFE5DFFA),
        lightPrimaryText = Color(0xFF322659),
        lightMutedText = Color(0xFF756B94),
        lightAccent = Color(0xFF7E69E8),
        darkBg = Color(0xFF14111E),
        darkCard = Color(0xFF252036),
        darkTag = Color(0xFF342E4C),
        darkPrimaryText = Color(0xFFF5F2FF),
        darkMutedText = Color(0xFFB5ADD1),
        darkAccent = Color(0xFF9B88FA)
    ),
    BgPalette(
        id = "emerald_mint",
        label = "Emerald Mint",
        swatchColor = Color(0xFF2E9E7B),
        lightBg = Color(0xFFF2FAF7),
        lightCard = Color(0xFFE2F2EC),
        lightTag = Color(0xFFD9EDE5),
        lightPrimaryText = Color(0xFF1B4538),
        lightMutedText = Color(0xFF5D8074),
        lightAccent = Color(0xFF268F6E),
        darkBg = Color(0xFF0B241C),
        darkCard = Color(0xD915382C),
        darkTag = Color(0xE01F4D3D),
        darkPrimaryText = Color(0xFFEFFAF6),
        darkMutedText = Color(0xFFA7C9BD),
        darkAccent = Color(0xFF47C49C)
    ),
    BgPalette(
        id = "rose_slate",
        label = "Rose Slate",
        swatchColor = Color(0xFFD96484),
        lightBg = Color(0xFFFCF4F6),
        lightCard = Color(0xFFF6E6EA),
        lightTag = Color(0xFFF2DCE2),
        lightPrimaryText = Color(0xFF542332),
        lightMutedText = Color(0xFF8C6470),
        lightAccent = Color(0xFFD15678),
        darkBg = Color(0xFF1A1114),
        darkCard = Color(0xFF2E1F24),
        darkTag = Color(0xFF402B32),
        darkPrimaryText = Color(0xFFFFF2F5),
        darkMutedText = Color(0xFFCCAAB4),
        darkAccent = Color(0xFFF07898)
    ),
    BgPalette(
        id = "warm_sand",
        label = "Warm Sand",
        swatchColor = Color(0xFFC98A3E),
        lightBg = Color(0xFFFAF7F2),
        lightCard = Color(0xFFF1EAE0),
        lightTag = Color(0xFFECE2D4),
        lightPrimaryText = Color(0xFF4A3720),
        lightMutedText = Color(0xFF85735D),
        lightAccent = Color(0xFFBF7E30),
        darkBg = Color(0xFF181510),
        darkCard = Color(0xFF2A241C),
        darkTag = Color(0xFF3A3227),
        darkPrimaryText = Color(0xFFFCF7F0),
        darkMutedText = Color(0xFFC7B9A5),
        darkAccent = Color(0xFFE5A454)
    )
)

private var appDarkMode by mutableStateOf(false)
private var selectedPaletteId by mutableStateOf("cool_blue")
private var downloadsEnabled by mutableStateOf(true)
private var downloadOnlyMp4Mkv by mutableStateOf(false)

private val currentPalette: BgPalette
    get() = bgPalettes.firstOrNull { it.id == selectedPaletteId } ?: bgPalettes.first()

private val bgDark: Color
    get() = if (appDarkMode) currentPalette.darkBg else currentPalette.lightBg
private val cardDark: Color
    get() = if (appDarkMode) currentPalette.darkCard else currentPalette.lightCard
private val tagDark: Color
    get() = if (appDarkMode) currentPalette.darkTag else currentPalette.lightTag
private val textMuted: Color
    get() = if (appDarkMode) currentPalette.darkMutedText else currentPalette.lightMutedText
private val primaryText: Color
    get() = if (appDarkMode) currentPalette.darkPrimaryText else currentPalette.lightPrimaryText
private val accentRed: Color
    get() = if (appDarkMode) currentPalette.darkAccent else currentPalette.lightAccent
private val starGold = Color(0xFFE0A83A)

private fun Modifier.appSurfaceBackground(): Modifier {
    return if (appDarkMode && selectedPaletteId == "emerald_mint") {
        this.background(
            Brush.verticalGradient(
                listOf(
                    Color(0xFF0A261D),
                    Color(0xF00E3126),
                    Color(0xFF092018)
                )
            )
        )
    } else {
        this.background(bgDark)
    }
}

/* ============================================================================
   SINGLE-ACTIVE DOWNLOAD QUEUE MANAGER (AUTO-RESUME ON ABORT + MOVIES/BeeGear)
   ============================================================================ */
private enum class DownloadStatus {
    QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED
}

private data class DownloadTaskItem(
    val id: String,
    val title: String,
    val fileName: String,
    val url: String,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = -1L,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val errorMessage: String = ""
)

private object DownloadQueueManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val tasks = mutableStateListOf<DownloadTaskItem>()
    private var activeJob: Job? = null
    private var activeTaskId: String? = null
    private var loaded = false
    private var appRef: Context? = null

    private fun stateFile(context: Context): File {
        val dir = File(context.filesDir, "beegear_data").apply { if (!exists()) mkdirs() }
        return File(dir, "download_queue.json")
    }

    private fun tempFile(context: Context, fileName: String): File {
        val dir = File(context.filesDir, "beegear_dl_temp").apply { if (!exists()) mkdirs() }
        return File(dir, "$fileName.part")
    }

    fun ensureLoaded(context: Context) {
        appRef = context.applicationContext
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            try {
                val file = stateFile(context)
                if (file.exists() && file.length() > 0L) {
                    val arr = JSONArray(file.readText())
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val rawStatus = obj.optString("status", DownloadStatus.PAUSED.name)
                        val parsedStatus = try {
                            val st = DownloadStatus.valueOf(rawStatus)
                            if (st == DownloadStatus.DOWNLOADING) DownloadStatus.PAUSED else st
                        } catch (_: Exception) {
                            DownloadStatus.PAUSED
                        }
                        tasks.add(
                            DownloadTaskItem(
                                id = obj.optString("id"),
                                title = obj.optString("title"),
                                fileName = obj.optString("fileName"),
                                url = obj.optString("url"),
                                downloadedBytes = obj.optLong("downloadedBytes", 0L),
                                totalBytes = obj.optLong("totalBytes", -1L),
                                status = parsedStatus,
                                errorMessage = obj.optString("errorMessage", "")
                            )
                        )
                    }
                }
            } catch (_: Exception) {
            }
            loaded = true
        }
    }

    fun onAppResumed(context: Context) {
        ensureLoaded(context)
        if (activeJob?.isActive == true) return
        val resumeCandidate = tasks.firstOrNull {
            it.status == DownloadStatus.QUEUED ||
                (it.status == DownloadStatus.PAUSED && it.errorMessage.contains("abort", ignoreCase = true))
        } ?: return
        resumeOrStartSingle(context, resumeCandidate.id)
    }

    private fun persist(context: Context) {
        try {
            val arr = JSONArray()
            tasks.forEach { t ->
                arr.put(
                    JSONObject()
                        .put("id", t.id)
                        .put("title", t.title)
                        .put("fileName", t.fileName)
                        .put("url", t.url)
                        .put("downloadedBytes", t.downloadedBytes)
                        .put("totalBytes", t.totalBytes)
                        .put("status", t.status.name)
                        .put("errorMessage", t.errorMessage)
                )
            }
            stateFile(context).writeText(arr.toString())
        } catch (_: Exception) {
        }
    }

    private fun sanitizeFileName(rawTitle: String, url: String): String {
        val decoded = try {
            URLDecoder.decode(rawTitle.ifBlank {
                URL(url).path.substringAfterLast("/").ifBlank { "video.mp4" }
            }, "UTF-8")
        } catch (_: Exception) {
            rawTitle.ifBlank { "video.mp4" }
        }
        return decoded.replace(Regex("[\\\\/:*?\"<>|]+"), "_").trim().ifBlank { "beegear_video.mp4" }
    }

    fun enqueue(context: Context, video: MediaEntry) {
        ensureLoaded(context)
        if (!downloadsEnabled) {
            Toast.makeText(context, "Settings থেকে ডাউনলোড বন্ধ করা আছে।", Toast.LENGTH_SHORT).show()
            return
        }
        val lower = video.url.substringBefore("?").lowercase()
        if (downloadOnlyMp4Mkv && !(lower.endsWith(".mp4") || lower.endsWith(".mkv"))) {
            Toast.makeText(context, "Settings অনুযায়ী শুধু MP4 ও MKV ফাইল ডাউনলোড অনুমোদিত।", Toast.LENGTH_SHORT).show()
            return
        }

        val existingIndex = tasks.indexOfFirst { it.url == video.url }
        if (existingIndex >= 0) {
            val existing = tasks[existingIndex]
            if (existing.status == DownloadStatus.COMPLETED) {
                Toast.makeText(context, "এই ফাইলটি ইতিমধ্যে ডাউনলোড সম্পন্ন হয়েছে।", Toast.LENGTH_SHORT).show()
                return
            }
            if (existing.status == DownloadStatus.PAUSED || existing.status == DownloadStatus.FAILED) {
                resumeOrStartSingle(context, existing.id)
            } else {
                Toast.makeText(context, "ফাইলটি ডাউনলোড লিস্টে আছে।", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val cleanName = sanitizeFileName(video.name, video.url)
        val item = DownloadTaskItem(
            id = System.currentTimeMillis().toString() + "_" + video.url.hashCode(),
            title = video.name,
            fileName = cleanName,
            url = video.url,
            status = DownloadStatus.QUEUED
        )
        tasks.add(item)
        persist(context)
        Toast.makeText(context, "ডাউনলোড লিস্টে যুক্ত হয়েছে: $cleanName", Toast.LENGTH_SHORT).show()
        startNextIfIdle(context)
    }

    fun pauseTask(context: Context, id: String) {
        ensureLoaded(context)
        val idx = tasks.indexOfFirst { it.id == id }
        if (idx < 0) return
        if (activeTaskId == id) {
            activeJob?.cancel()
            activeJob = null
            activeTaskId = null
        }
        val current = tasks[idx]
        if (current.status == DownloadStatus.DOWNLOADING || current.status == DownloadStatus.QUEUED) {
            tasks[idx] = current.copy(status = DownloadStatus.PAUSED, errorMessage = "")
            persist(context)
        }
        startNextIfIdle(context)
    }

    fun resumeOrStartSingle(context: Context, id: String) {
        ensureLoaded(context)
        val targetIdx = tasks.indexOfFirst { it.id == id }
        if (targetIdx < 0) return

        if (activeTaskId != null && activeTaskId != id) {
            val runningIdx = tasks.indexOfFirst { it.id == activeTaskId }
            activeJob?.cancel()
            activeJob = null
            activeTaskId = null
            if (runningIdx >= 0) {
                tasks[runningIdx] = tasks[runningIdx].copy(status = DownloadStatus.PAUSED, errorMessage = "")
            }
        }

        val target = tasks[targetIdx]
        if (target.status != DownloadStatus.COMPLETED) {
            tasks[targetIdx] = target.copy(status = DownloadStatus.QUEUED, errorMessage = "")
            persist(context)
            runTask(context, id)
        }
    }

    fun deleteTask(context: Context, id: String) {
        ensureLoaded(context)
        val idx = tasks.indexOfFirst { it.id == id }
        if (idx < 0) return
        val item = tasks[idx]
        if (activeTaskId == id) {
            activeJob?.cancel()
            activeJob = null
            activeTaskId = null
        }
        try {
            tempFile(context, item.fileName).delete()
            val publicFile = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "BeeGear/${item.fileName}"
            )
            if (publicFile.exists()) publicFile.delete()
        } catch (_: Exception) {
        }
        tasks.removeAt(idx)
        persist(context)
        startNextIfIdle(context)
    }

    private fun startNextIfIdle(context: Context) {
        if (activeJob?.isActive == true) return
        val next = tasks.firstOrNull { it.status == DownloadStatus.QUEUED } ?: return
        runTask(context, next.id)
    }

    private fun runTask(context: Context, id: String) {
        if (activeJob?.isActive == true) return
        val idx = tasks.indexOfFirst { it.id == id }
        if (idx < 0) return

        activeTaskId = id
        tasks[idx] = tasks[idx].copy(status = DownloadStatus.DOWNLOADING, errorMessage = "")
        persist(context)

        activeJob = scope.launch {
            val appCtx = context.applicationContext
            val taskSnapshot = tasks.firstOrNull { it.id == id } ?: return@launch
            val partFile = tempFile(appCtx, taskSnapshot.fileName)
            var conn: HttpURLConnection? = null
            var abortedBySystem = false

            try {
                var existingBytes = if (partFile.exists()) partFile.length() else 0L
                conn = (URL(taskSnapshot.url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 25000
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                    if (existingBytes > 0L) {
                        setRequestProperty("Range", "bytes=$existingBytes-")
                    }
                }
                conn.connect()

                val responseCode = conn.responseCode
                val appendMode = responseCode == HttpURLConnection.HTTP_PARTIAL
                if (!appendMode && existingBytes > 0L) {
                    existingBytes = 0L
                    partFile.delete()
                }
                if (responseCode !in 200..299) {
                    throw Exception("Server HTTP $responseCode")
                }

                val contentLength = conn.contentLengthLong
                val totalBytes = if (contentLength > 0L) existingBytes + contentLength else -1L
                updateTask(id) { it.copy(downloadedBytes = existingBytes, totalBytes = totalBytes) }

                var downloaded = existingBytes
                var lastUiUpdate = 0L

                conn.inputStream.use { input ->
                    FileOutputStream(partFile, appendMode).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        while (isActive) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read

                            val now = System.currentTimeMillis()
                            if (now - lastUiUpdate >= 450L) {
                                lastUiUpdate = now
                                updateTask(id) {
                                    it.copy(
                                        downloadedBytes = downloaded,
                                        totalBytes = if (totalBytes > 0L) totalBytes else it.totalBytes
                                    )
                                }
                            }
                        }
                    }
                }

                if (!isActive) return@launch

                movePartFileToPublicMovies(appCtx, partFile, taskSnapshot.fileName)

                updateTask(id) {
                    it.copy(
                        downloadedBytes = downloaded,
                        totalBytes = if (totalBytes > 0L) totalBytes else downloaded,
                        status = DownloadStatus.COMPLETED,
                        errorMessage = ""
                    )
                }
                persist(appCtx)
                withContext(Dispatchers.Main) {
                    Toast.makeText(appCtx, "সম্পন্ন: Movies/BeeGear/${taskSnapshot.fileName}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Download interrupted"
                val isConnectionAbort = e is SocketException ||
                    msg.contains("connection abort", ignoreCase = true) ||
                    msg.contains("broken pipe", ignoreCase = true) ||
                    msg.contains("reset by peer", ignoreCase = true) ||
                    msg.contains("timed out", ignoreCase = true)

                val savedBytes = if (partFile.exists()) partFile.length() else 0L
                if (isConnectionAbort || !isActive) {
                    abortedBySystem = true
                    updateTask(id) {
                        it.copy(
                            downloadedBytes = maxOf(savedBytes, it.downloadedBytes),
                            status = DownloadStatus.PAUSED,
                            errorMessage = "Connection aborted (auto-resume ready)"
                        )
                    }
                    persist(appCtx)
                } else {
                    updateTask(id) {
                        it.copy(
                            downloadedBytes = maxOf(savedBytes, it.downloadedBytes),
                            status = DownloadStatus.FAILED,
                            errorMessage = msg
                        )
                    }
                    persist(appCtx)
                }
            } finally {
                try {
                    conn?.disconnect()
                } catch (_: Exception) {
                }
                if (activeTaskId == id) {
                    activeTaskId = null
                    activeJob = null
                    if (!abortedBySystem) {
                        startNextIfIdle(appCtx)
                    }
                }
            }
        }
    }

    private fun updateTask(id: String, transform: (DownloadTaskItem) -> DownloadTaskItem) {
        val idx = tasks.indexOfFirst { it.id == id }
        if (idx >= 0) {
            tasks[idx] = transform(tasks[idx])
        }
    }

    private fun movePartFileToPublicMovies(context: Context, partFile: File, fileName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, guessVideoMime(fileName))
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/BeeGear")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = resolver.insert(collection, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out: OutputStream ->
                    partFile.inputStream().use { input ->
                        input.copyTo(out, 16 * 1024)
                    }
                }
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                partFile.delete()
                return
            }
        }

        val moviesDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "BeeGear"
        ).apply { if (!exists()) mkdirs() }
        val destFile = File(moviesDir, fileName)
        if (destFile.exists()) destFile.delete()
        partFile.inputStream().use { input ->
            FileOutputStream(destFile).use { out ->
                input.copyTo(out, 16 * 1024)
            }
        }
        partFile.delete()
        MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), null, null)
    }

    private fun guessVideoMime(fileName: String): String {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".mkv") -> "video/x-matroska"
            lower.endsWith(".webm") -> "video/webm"
            lower.endsWith(".avi") -> "video/x-msvideo"
            lower.endsWith(".mov") -> "video/quicktime"
            else -> "video/mp4"
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024.0) {
        String.format(Locale.US, "%.2f GB", mb / 1024.0)
    } else {
        String.format(Locale.US, "%.1f MB", mb)
    }
}

/* ============================================================================
   INTERNAL STORAGE ONLY THUMBNAIL & FOLDER MAPPING STORE
   ============================================================================ */
private object InternalStorageStore {
    private val folderPosterMap = ConcurrentHashMap<String, String>()
    private var mapLoaded = false
    private val scanSemaphore = Semaphore(4)
    private val downloadSemaphore = Semaphore(5)

    private fun md5(input: String): String {
        return try {
            val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            input.hashCode().toString()
        }
    }

    private fun dataDir(context: Context): File {
        return File(context.filesDir, "beegear_data").apply { if (!exists()) mkdirs() }
    }

    private fun thumbDir(context: Context): File {
        return File(context.filesDir, "beegear_thumbnails").apply { if (!exists()) mkdirs() }
    }

    private fun mapFile(context: Context): File {
        return File(dataDir(context), "folder_posters.json")
    }

    private fun ensureMapLoaded(context: Context) {
        if (mapLoaded) return
        synchronized(this) {
            if (mapLoaded) return
            try {
                val file = mapFile(context)
                if (file.exists() && file.length() > 0L) {
                    val obj = JSONObject(file.readText())
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val value = obj.optString(key, "")
                        if (key.isNotBlank() && value.isNotBlank()) {
                            folderPosterMap[key] = value
                        }
                    }
                }
            } catch (_: Exception) {
            }
            mapLoaded = true
        }
    }

    @Synchronized
    private fun persistMap(context: Context) {
        try {
            val obj = JSONObject()
            folderPosterMap.forEach { (k, v) -> obj.put(k, v) }
            mapFile(context).writeText(obj.toString())
        } catch (_: Exception) {
        }
    }

    fun getMappedPoster(context: Context, folderUrl: String): String? {
        ensureMapLoaded(context)
        return folderPosterMap[folderUrl]
    }

    fun saveMappedPoster(context: Context, folderUrl: String, posterUrl: String) {
        ensureMapLoaded(context)
        if (folderPosterMap[folderUrl] == posterUrl) return
        folderPosterMap[folderUrl] = posterUrl
        persistMap(context)
    }

    suspend fun getOrFetchFolderPoster(context: Context, folderUrl: String): String? {
        getMappedPoster(context, folderUrl)?.let { return it }
        return scanSemaphore.withPermit {
            getMappedPoster(context, folderUrl)?.let { return@withPermit it }
            val found = findPosterDeep(folderUrl)
            if (!found.isNullOrBlank()) {
                saveMappedPoster(context, folderUrl, found)
            }
            found
        }
    }

    private fun readBitmapFromInternalFile(file: File): Bitmap? {
        if (!file.exists() || file.length() <= 0L) return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            var sample = 1
            while (bounds.outWidth / sample > 480 || bounds.outHeight / sample > 720) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeFile(file.absolutePath, opts)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun loadThumbnailFromInternalStorage(context: Context, url: String): Bitmap? = withContext(Dispatchers.IO) {
        val file = File(thumbDir(context), "${md5(url)}.img")

        if (file.exists() && file.length() > 0L) {
            val existing = readBitmapFromInternalFile(file)
            if (existing != null) return@withContext existing
            file.delete()
        }

        downloadSemaphore.withPermit {
            if (file.exists() && file.length() > 0L) {
                val existing = readBitmapFromInternalFile(file)
                if (existing != null) return@withContext existing
            }
            try {
                val bytes = URL(url).openConnection().apply {
                    connectTimeout = 8000
                    readTimeout = 12000
                }.getInputStream().use { it.readBytes() }

                if (bytes.isNotEmpty()) {
                    FileOutputStream(file).use { it.write(bytes) }
                    return@withPermit readBitmapFromInternalFile(file)
                }
            } catch (_: Exception) {
            }
            null
        }
    }
}

private fun readRecommendationList(raw: String?): List<Recommendation> {
    if (raw.isNullOrBlank()) return emptyList()
    return try {
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            Recommendation(
                item.optString("title"),
                item.optString("url"),
                item.optString("poster").ifBlank { null },
                item.optString("subtitle"),
                item.optLong("score", 0L)
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private val categories = listOf(
    Category("English Movies", "http://172.16.50.7/DHAKA-FLIX-7/English%20Movies/"),
    Category("English Movies 1080p", "http://172.16.50.14/DHAKA-FLIX-14/English%20Movies%20%281080p%29/"),
    Category("IMDb Top 250", "http://172.16.50.14/DHAKA-FLIX-14/IMDb%20Top-250%20Movies/"),
    Category("Hindi Movies", "http://172.16.50.14/DHAKA-FLIX-14/Hindi%20Movies/"),
    Category("South Indian Hindi Dubbed", "http://172.16.50.14/DHAKA-FLIX-14/SOUTH%20INDIAN%20MOVIES/Hindi%20Dubbed/"),
    Category("TV & Web Series", "http://172.16.50.12/DHAKA-FLIX-12/TV-WEB-Series/"),
    Category("Anime & Cartoon", "http://172.16.50.10/DHAKA-FLIX-10/Anime%20%26%20Cartoon%20TV%20Series/"),
    Category("Korean TV & Web Series", "http://172.16.50.14/DHAKA-FLIX-14/KOREAN%20TV%20%26%20WEB%20Series/"),
    Category("Animation Movies", "http://172.16.50.14/DHAKA-FLIX-14/Animation%20Movies/")
)

private val videoExtensions = listOf(".mp4", ".mkv", ".avi", ".mov", ".webm", ".m4v", ".ts")
private val imageExtensions = listOf(".jpg", ".jpeg", ".png", ".webp", ".bmp")

private val datePatterns = listOf(
    "yyyy-MM-dd HH:mm",
    "yyyy-MM-dd",
    "dd-MMM-yyyy HH:mm",
    "dd-MMM-yyyy",
    "dd/MMM/yyyy HH:mm",
    "MM/dd/yyyy hh:mm:ss a",
    "MM/dd/yyyy HH:mm:ss",
    "MM/dd/yyyy"
)

private fun parseDateToMillis(rawText: String): Long {
    val match = Regex(
        "(\\d{4}-\\d{2}-\\d{2}(?:\\s+\\d{2}:\\d{2})?|\\d{2}-[A-Za-z]{3}-\\d{4}(?:\\s+\\d{2}:\\d{2})?|\\d{2}/[A-Za-z]{3}/\\d{4}(?:\\s+\\d{2}:\\d{2})?|\\d{2}/\\d{2}/\\d{4}(?:\\s+\\d{1,2}:\\d{2}(?::\\d{2})?(?:\\s*[APap][Mm])?)?)"
    ).find(rawText)?.value ?: return 0L

    for (pattern in datePatterns) {
        try {
            val sdf = SimpleDateFormat(pattern, Locale.ENGLISH).apply {
                timeZone = TimeZone.getDefault()
            }
            val parsed = sdf.parse(match.trim())
            if (parsed != null) return parsed.time
        } catch (_: Exception) {
        }
    }
    return 0L
}

private fun yearPriorityBoost(name: String): Long {
    val decoded = try { URLDecoder.decode(name, "UTF-8") } catch (_: Exception) { name }
    val years = Regex("\\b(19\\d{2}|20\\d{2})\\b").findAll(decoded)
        .mapNotNull { it.value.toIntOrNull() }
        .filter { it in 1990..2030 }
        .maxOrNull() ?: return 0L
    return (years - 1990).toLong() * 86_400_000L * 365L
}

private suspend fun scanFolder(url: String, timeoutMs: Int = 14000): Result<List<MediaEntry>> = withContext(Dispatchers.IO) {
    try {
        val doc = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(timeoutMs).ignoreContentType(true).get()
        val host = URL(url).host
        var seq = 0
        val items = doc.select("a[href]").mapNotNull { link ->
            val href = link.attr("href").trim()
            if (href.isBlank() || href.startsWith("#") || href.startsWith("?") ||
                href == "../" || href == "./" || href.startsWith("javascript:")
            ) return@mapNotNull null
            val address = link.absUrl("href")
            if (address.isBlank()) return@mapNotNull null
            val parsed = try { URL(address) } catch (_: Exception) { return@mapNotNull null }
            if (parsed.host != host) return@mapNotNull null
            val path = parsed.path.lowercase()
            val isVideo = videoExtensions.any { path.endsWith(it) }
            val isImage = imageExtensions.any { path.endsWith(it) }
            val isFolder = !isVideo && !isImage && (href.endsWith("/") || parsed.path.endsWith("/"))
            if (!isFolder && !isVideo && !isImage) return@mapNotNull null
            val name = link.text().trim().ifBlank {
                try {
                    URLDecoder.decode(parsed.path.trimEnd('/').substringAfterLast("/"), "UTF-8")
                } catch (_: Exception) {
                    parsed.path.trimEnd('/').substringAfterLast("/")
                }
            }
            val rowText = link.parent()?.text() ?: ""
            val modifiedMs = parseDateToMillis(rowText)
            val order = seq++
            MediaEntry(
                name = name,
                url = address,
                isFolder = isFolder,
                isVideo = isVideo,
                isImage = isImage,
                lastModifiedMs = modifiedMs,
                sourceOrder = order
            )
        }.distinctBy { it.url }
            .sortedWith(compareByDescending<MediaEntry> { it.isFolder }.thenBy { it.name.lowercase() })
        Result.success(items)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private suspend fun findPosterDeep(folderUrl: String): String? = withContext(Dispatchers.IO) {
    val level1 = scanFolder(folderUrl, 9000).getOrNull().orEmpty()
    val poster1 = level1.firstOrNull { it.isImage }?.url
    if (poster1 != null) return@withContext poster1

    val subFolder1 = level1.firstOrNull { it.isFolder } ?: return@withContext null
    val level2 = scanFolder(subFolder1.url, 9000).getOrNull().orEmpty()
    val poster2 = level2.firstOrNull { it.isImage }?.url
    if (poster2 != null) return@withContext poster2

    val subFolder2 = level2.firstOrNull { it.isFolder } ?: return@withContext null
    val level3 = scanFolder(subFolder2.url, 9000).getOrNull().orEmpty()
    level3.firstOrNull { it.isImage }?.url
}

private fun latestFreshnessScore(entry: MediaEntry, nestedMaxModified: Long = 0L): Long {
    val maxDate = maxOf(entry.lastModifiedMs, nestedMaxModified)
    val yearBoost = yearPriorityBoost(entry.name)
    val positionalBoost = entry.sourceOrder.toLong() * 1_000L
    return if (maxDate > 0L) maxDate + yearBoost else yearBoost + positionalBoost
}

/* ============================================================================
   RECOMMENDATIONS LOADER (DEEP YEAR / ALPHA / SEASON SUBFOLDER SUPPORT + RETRY)
   ============================================================================ */
private suspend fun scanCategoryRecommendations(category: Category): List<Recommendation> = withContext(Dispatchers.IO) {
    val results = mutableListOf<Recommendation>()
    val seenUrls = HashSet<String>()
    val topLevel = scanFolder(category.url, 15000).getOrNull().orEmpty()

    val prioritizedFolders = topLevel.filter { it.isFolder }
        .sortedWith(
            compareByDescending<MediaEntry> {
                val clean = it.name.trim()
                clean.matches(Regex("^(19\\d{2}|20\\d{2}).*"))
            }.thenByDescending { latestFreshnessScore(it) }
        )
        .take(16)

    for (folder in prioritizedFolders) {
        if (results.size >= 12) break
        val inside = scanFolder(folder.url, 14000).getOrNull().orEmpty()
        val directVideos = inside.any { it.isVideo }
        val directPoster = inside.firstOrNull { it.isImage }?.url
        val maxInsideDate = inside.maxOfOrNull { it.lastModifiedMs } ?: 0L

        if (directVideos) {
            if (seenUrls.add(folder.url)) {
                results.add(
                    Recommendation(
                        title = folder.name,
                        url = folder.url,
                        poster = directPoster,
                        subtitle = category.name,
                        score = latestFreshnessScore(folder, maxInsideDate)
                    )
                )
            }
            continue
        }

        val nestedFolders = inside.filter { it.isFolder }
            .sortedByDescending { latestFreshnessScore(it) }
            .take(14)

        for (child in nestedFolders) {
            if (results.size >= 12) break
            val childItems = scanFolder(child.url, 13000).getOrNull().orEmpty()
            val childPoster = childItems.firstOrNull { it.isImage }?.url ?: directPoster
            val childMaxDate = maxOf(maxInsideDate, childItems.maxOfOrNull { it.lastModifiedMs } ?: 0L)

            if (childItems.any { it.isVideo }) {
                if (seenUrls.add(child.url)) {
                    results.add(
                        Recommendation(
                            title = child.name,
                            url = child.url,
                            poster = childPoster,
                            subtitle = category.name,
                            score = latestFreshnessScore(child, childMaxDate)
                        )
                    )
                }
                continue
            }

            val deeperFolders = childItems.filter { it.isFolder }
                .sortedByDescending { latestFreshnessScore(it) }
                .take(8)

            for (deep in deeperFolders) {
                if (results.size >= 12) break
                val deepItems = scanFolder(deep.url, 12000).getOrNull().orEmpty()
                val deepPoster = deepItems.firstOrNull { it.isImage }?.url ?: childPoster
                val deepMaxDate = maxOf(childMaxDate, deepItems.maxOfOrNull { it.lastModifiedMs } ?: 0L)

                if (deepItems.any { it.isVideo }) {
                    val isSeasonNode = deep.name.contains("season", ignoreCase = true) ||
                        deep.name.matches(Regex("^(s\\d+|part\\s*\\d+).*", RegexOption.IGNORE_CASE))
                    val targetUrl = if (isSeasonNode) child.url else deep.url
                    val targetTitle = if (isSeasonNode) child.name else deep.name
                    if (seenUrls.add(targetUrl)) {
                        results.add(
                            Recommendation(
                                title = targetTitle,
                                url = targetUrl,
                                poster = deepPoster,
                                subtitle = category.name,
                                score = latestFreshnessScore(deep, deepMaxDate)
                            )
                        )
                    }
                    if (isSeasonNode) break
                }
            }
        }
    }

    results
}

private suspend fun loadRecommendations(): List<Recommendation> = withContext(Dispatchers.IO) {
    val collected = Collections.synchronizedList(mutableListOf<Recommendation>())

    coroutineScope {
        val jobs = categories.map { category ->
            async(Dispatchers.IO) {
                var items = scanCategoryRecommendations(category)
                if (items.isEmpty()) {
                    delay(450)
                    items = scanCategoryRecommendations(category)
                }
                collected.addAll(items)
            }
        }
        jobs.awaitAll()
    }

    collected.distinctBy { it.url }
        .sortedByDescending { it.score }
        .take(90)
}

private fun normalizeSearchText(input: String): String {
    return try {
        URLDecoder.decode(input, "UTF-8")
    } catch (_: Exception) {
        input
    }.lowercase()
        .replace(Regex("[._\\-()+\\[\\]]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private suspend fun searchAllCategories(query: String): List<SearchHit> = withContext(Dispatchers.IO) {
    val normalizedQuery = normalizeSearchText(query)
    if (normalizedQuery.isBlank()) return@withContext emptyList()

    val tokens = normalizedQuery.split(" ").filter { it.isNotBlank() }
    val found = Collections.synchronizedList(mutableListOf<SearchHit>())
    val seen = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    fun matchesQuery(rawName: String): Boolean {
        val target = normalizeSearchText(rawName)
        if (target.contains(normalizedQuery)) return true
        return tokens.size > 1 && tokens.all { token -> target.contains(token) }
    }

    coroutineScope {
        val tasks = categories.map { category ->
            async(Dispatchers.IO) {
                val root = scanFolder(category.url, 12000).getOrNull().orEmpty()
                for (entry in root) {
                    if (found.size >= 80) break
                    if (matchesQuery(entry.name) && seen.add(entry.url)) {
                        found.add(
                            SearchHit(
                                title = entry.name,
                                url = entry.url,
                                poster = null,
                                subtitle = category.name,
                                isVideo = entry.isVideo
                            )
                        )
                    }
                }
            }
        }
        tasks.awaitAll()
    }

    found.take(80)
}

private fun subtitleMimeType(uriOrName: String): String? {
    val path = uriOrName.substringBefore("?").lowercase()
    return when {
        path.endsWith(".srt") -> MimeTypes.APPLICATION_SUBRIP
        path.endsWith(".vtt") -> MimeTypes.TEXT_VTT
        path.endsWith(".ass") || path.endsWith(".ssa") -> MimeTypes.TEXT_SSA
        else -> null
    }
}

private suspend fun findSameFolderSubtitles(videoUrl: String): List<MediaItem.SubtitleConfiguration> =
    withContext(Dispatchers.IO) {
        try {
            val video = URL(videoUrl)
            val parentUrl = videoUrl.substringBeforeLast("/") + "/"
            val videoBase = video.path.substringAfterLast("/").substringBeforeLast(".")
                .lowercase().replace(Regex("[._-]+"), " ").trim()
            val doc = Jsoup.connect(parentUrl).userAgent("Mozilla/5.0")
                .timeout(10000).ignoreContentType(true).get()
            doc.select("a[href]").mapNotNull { link ->
                val address = link.absUrl("href")
                if (address.isBlank()) return@mapNotNull null
                val parsed = try { URL(address) } catch (_: Exception) { return@mapNotNull null }
                if (parsed.host != video.host) return@mapNotNull null
                val name = parsed.path.substringAfterLast("/")
                val mime = subtitleMimeType(name) ?: return@mapNotNull null
                val subtitleBase = name.substringBeforeLast(".")
                    .lowercase().replace(Regex("[._-]+"), " ").trim()
                val similarity = subtitleBase == videoBase ||
                    videoBase.startsWith(subtitleBase) || subtitleBase.startsWith(videoBase)
                if (!similarity) return@mapNotNull null
                MediaItem.SubtitleConfiguration.Builder(Uri.parse(address))
                    .setMimeType(mime)
                    .setLanguage("und")
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
            }.distinctBy { it.uri.toString() }
        } catch (_: Exception) {
            emptyList()
        }
    }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val prefs = getSharedPreferences("beegear_settings", MODE_PRIVATE)
        appDarkMode = prefs.getBoolean("dark_theme", false)
        selectedPaletteId = prefs.getString("bg_palette", "cool_blue") ?: "cool_blue"
        downloadsEnabled = prefs.getBoolean("downloads_enabled", true)
        downloadOnlyMp4Mkv = prefs.getBoolean("download_only_mp4_mkv", false)
        DownloadQueueManager.ensureLoaded(this)
        applySystemBarColors(appDarkMode, bgDark)

        setContent {
            MaterialTheme {
                val isDark = appDarkMode
                val currentBg = bgDark
                SideEffect {
                    applySystemBarColors(isDark, currentBg)
                }
                LaunchedEffect(isDark, currentBg) {
                    applySystemBarColors(isDark, currentBg)
                }
                BeeGearApp()
            }
        }
    }

    private fun applySystemBarColors(isDark: Boolean, background: Color) {
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = !isDark
        insetsController.isAppearanceLightNavigationBars = !isDark
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        window.decorView.setBackgroundColor(
            android.graphics.Color.argb(
                255,
                (background.red * 255).toInt(),
                (background.green * 255).toInt(),
                (background.blue * 255).toInt()
            )
        )
    }
}

@Composable
fun BeeGearApp() {
    val appContext = LocalContext.current
    var tab by remember { mutableStateOf("HOME") }
    var currentTitle by remember { mutableStateOf("") }
    var currentUrl by remember { mutableStateOf("") }
    var currentPoster by remember { mutableStateOf<String?>(null) }
    var stack by remember { mutableStateOf(emptyList<FolderLocation>()) }
    var entries by remember { mutableStateOf(emptyList<MediaEntry>()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var playingUrl by remember { mutableStateOf<String?>(null) }
    var playingTitle by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    var showDownloads by remember { mutableStateOf(false) }
    var recommendations by remember { mutableStateOf(emptyList<Recommendation>()) }
    var recommendationsLoading by remember { mutableStateOf(true) }
    var favorites by remember {
        mutableStateOf(
            readRecommendationList(
                appContext.getSharedPreferences("beegear_settings", Context.MODE_PRIVATE)
                    .getString("saved_items", "[]")
            )
        )
    }
    var recentlyWatched by remember {
        mutableStateOf(
            readRecommendationList(
                appContext.getSharedPreferences("beegear_settings", Context.MODE_PRIVATE)
                    .getString("recent_items", "[]")
            )
        )
    }

    LaunchedEffect(recentlyWatched) {
        val array = JSONArray()
        recentlyWatched.forEach { item ->
            array.put(
                JSONObject().put("title", item.title).put("url", item.url)
                    .put("poster", item.poster ?: "").put("subtitle", item.subtitle)
                    .put("score", item.score)
            )
        }
        appContext.getSharedPreferences("beegear_settings", Context.MODE_PRIVATE)
            .edit().putString("recent_items", array.toString()).apply()
    }

    LaunchedEffect(favorites) {
        val array = JSONArray()
        favorites.forEach { item ->
            array.put(
                JSONObject().put("title", item.title).put("url", item.url)
                    .put("poster", item.poster ?: "").put("subtitle", item.subtitle)
                    .put("score", item.score)
            )
        }
        appContext.getSharedPreferences("beegear_settings", Context.MODE_PRIVATE)
            .edit().putString("saved_items", array.toString()).apply()
    }

    fun goHomeDirectly() {
        playingUrl = null
        playingTitle = ""
        showSettings = false
        showDownloads = false
        stack = emptyList()
        currentUrl = ""
        currentTitle = ""
        currentPoster = null
        tab = "HOME"
    }

    fun openFolder(title: String, url: String, poster: String? = null) {
        showSettings = false
        showDownloads = false
        if (currentUrl.isNotBlank()) {
            stack = stack + FolderLocation(currentTitle, currentUrl, currentPoster)
        }
        currentTitle = title
        currentUrl = url
        currentPoster = poster ?: InternalStorageStore.getMappedPoster(appContext, url)
    }

    fun back() {
        if (playingUrl != null) {
            playingUrl = null
        } else if (showDownloads) {
            showDownloads = false
        } else if (showSettings) {
            showSettings = false
        } else if (stack.isNotEmpty()) {
            val prev = stack.last()
            stack = stack.dropLast(1)
            currentTitle = prev.title
            currentUrl = prev.url
            currentPoster = prev.poster
        } else if (currentUrl.isNotBlank()) {
            currentUrl = ""
            currentTitle = ""
            currentPoster = null
        } else {
            tab = "HOME"
        }
    }

    BackHandler(enabled = playingUrl != null || showDownloads || showSettings || currentUrl.isNotBlank() || tab != "HOME") {
        back()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val recommendationScope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                DownloadQueueManager.onAppResumed(appContext)
                if (recommendations.isEmpty()) {
                    recommendationScope.launch {
                        recommendationsLoading = true
                        recommendations = loadRecommendations()
                        recommendationsLoading = false
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(currentUrl) {
        if (currentUrl.isNotBlank()) {
            loading = true
            error = ""
            entries = emptyList()
            val result = scanFolder(currentUrl, 15000)
            val fetched = result.getOrNull().orEmpty()
            entries = fetched
            if (currentPoster == null) {
                val direct = fetched.firstOrNull { it.isImage }?.url
                if (direct != null) {
                    currentPoster = direct
                    InternalStorageStore.saveMappedPoster(appContext, currentUrl, direct)
                }
            }
            if (result.isFailure) {
                error = result.exceptionOrNull()?.localizedMessage ?: "Server connection failed"
            }
            loading = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .then(if (playingUrl == null) Modifier.statusBarsPadding().navigationBarsPadding() else Modifier)
            .appSurfaceBackground()
    ) {
        when {
            playingUrl != null -> VideoPlayerScreen(playingUrl!!, playingTitle) { playingUrl = null }

            showDownloads -> DownloadQueueScreen(onBack = { showDownloads = false })

            showSettings -> SettingsScreen(
                onBack = { showSettings = false },
                onOpenDownloads = { showDownloads = true }
            )

            currentUrl.isNotBlank() -> MediaDetailsScreen(
                title = currentTitle,
                initialPoster = currentPoster,
                entries = entries,
                loading = loading,
                error = error,
                isFavorite = favorites.any { it.url == currentUrl },
                onToggleFavorite = {
                    favorites = if (favorites.any { it.url == currentUrl }) {
                        favorites.filterNot { it.url == currentUrl }
                    } else {
                        favorites + Recommendation(currentTitle, currentUrl, currentPoster, "Saved Item")
                    }
                },
                onBack = { back() },
                onGoHome = { goHomeDirectly() },
                onOpenDownloads = { showDownloads = true },
                onFolder = { openFolder(it.name, it.url) },
                onVideo = { video ->
                    recentlyWatched = (listOf(Recommendation(video.name, video.url, currentPoster, "Continue Watching")) + recentlyWatched)
                        .distinctBy { it.url }.take(12)
                    playingTitle = video.name
                    playingUrl = video.url
                },
                onDownloadVideo = { video ->
                    DownloadQueueManager.enqueue(appContext, video)
                }
            )

            tab == "SEARCH" -> SearchScreen(
                onOpenFolder = { hit -> stack = emptyList(); openFolder(hit.title, hit.url, hit.poster) },
                onPlayVideo = { hit ->
                    recentlyWatched = (listOf(Recommendation(hit.title, hit.url, hit.poster, "Continue Watching")) + recentlyWatched)
                        .distinctBy { it.url }.take(12)
                    playingTitle = hit.title
                    playingUrl = hit.url
                }
            )

            tab == "BROWSE" -> BrowseScreen(
                onCategory = { stack = emptyList(); openFolder(it.name, it.url) },
                onOpenSettings = { showSettings = true },
                onOpenDownloads = { showDownloads = true }
            )

            tab == "TRENDING" -> TrendingOrSavedScreen(
                screenTitle = "Latest Updates",
                items = recommendations,
                onOpen = { stack = emptyList(); openFolder(it.title, it.url, it.poster) }
            )

            tab == "FAVORITES" -> TrendingOrSavedScreen(
                screenTitle = "My Favorites",
                items = favorites,
                emptyMessage = "এখনও কোনো মুভি ফেভারিট লিস্টে যোগ করা হয়নি।",
                onOpen = { stack = emptyList(); openFolder(it.title, it.url, it.poster) }
            )

            else -> HomeScreen(
                recommendations = recommendations,
                isDarkTheme = appDarkMode,
                onToggleTheme = {
                    appDarkMode = !appDarkMode
                    appContext.getSharedPreferences("beegear_settings", Context.MODE_PRIVATE)
                        .edit().putBoolean("dark_theme", appDarkMode).apply()
                },
                onOpenSettings = { showSettings = true },
                onOpenDownloads = { showDownloads = true },
                loading = recommendationsLoading,
                continueWatching = recentlyWatched,
                onMenuClick = { tab = "BROWSE" },
                onSeeAll = { tab = "BROWSE" },
                onCategory = { stack = emptyList(); openFolder(it.name, it.url) },
                onOpen = { stack = emptyList(); openFolder(it.title, it.url, it.poster) },
                onContinueOpen = { item -> playingTitle = item.title; playingUrl = item.url },
                onSearchClick = { tab = "SEARCH" }
            )
        }

        if (playingUrl == null && currentUrl.isBlank() && !showSettings && !showDownloads) {
            FloatingBottomNav(
                tab = tab,
                onSelect = { tab = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 22.dp, vertical = 16.dp)
            )
        }
    }
}

/* ============================================================================
   SETTINGS SCREEN (THEME + BACKGROUND COLOR PRESETS + DOWNLOAD OPTIONS)
   ============================================================================ */
@Composable
private fun SettingsScreen(
    onBack: () -> Unit,
    onOpenDownloads: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("beegear_settings", Context.MODE_PRIVATE) }
    val activeCount = DownloadQueueManager.tasks.count {
        it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED
    }

    Column(
        Modifier
            .fillMaxSize()
            .appSurfaceBackground()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(cardDark)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Text("‹", color = primaryText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Text("Settings", color = primaryText, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }

        Spacer(Modifier.height(20.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardDark)
                .border(1.dp, primaryText.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Text("Theme Mode", color = primaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Day (Light) এবং Dark মোডের সাথে স্ট্যাটাস বার স্বয়ংক্রিয়ভাবে মানিয়ে নেবে।", color = textMuted, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (!appDarkMode) accentRed else tagDark)
                        .clickable {
                            appDarkMode = false
                            prefs.edit().putBoolean("dark_theme", false).apply()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "☀  Day Mode",
                        color = if (!appDarkMode) Color.White else primaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (appDarkMode) accentRed else tagDark)
                        .clickable {
                            appDarkMode = true
                            prefs.edit().putBoolean("dark_theme", true).apply()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "☾  Dark Mode",
                        color = if (appDarkMode) Color.White else primaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardDark)
                .border(1.dp, primaryText.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Text("Background & Font Color", color = primaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("গ্লাস UI ঠিক রেখে ব্যাকগ্রাউন্ড ও ফন্টের রঙ পরিবর্তন করুন।", color = textMuted, fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))

            bgPalettes.forEach { palette ->
                val isSelected = palette.id == selectedPaletteId
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) accentRed.copy(alpha = 0.16f) else tagDark)
                        .border(
                            1.5.dp,
                            if (isSelected) accentRed else Color.Transparent,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            selectedPaletteId = palette.id
                            prefs.edit().putString("bg_palette", palette.id).apply()
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(palette.swatchColor)
                            .border(2.dp, Color.White, CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        palette.label,
                        color = primaryText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Text("Active", color = accentRed, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardDark)
                .border(1.dp, primaryText.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Text("Download Settings", color = primaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("সেভ লোকেশন: Internal Storage / Movies / BeeGear", color = textMuted, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Enable Video Downloads", color = primaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("মুভি ফাইলে ডাউনলোড বাটন সক্রিয় রাখুন", color = textMuted, fontSize = 11.sp)
                }
                Switch(
                    checked = downloadsEnabled,
                    onCheckedChange = {
                        downloadsEnabled = it
                        prefs.edit().putBoolean("downloads_enabled", it).apply()
                    }
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Only MP4 & MKV Files", color = primaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("অন্যান্য ফরম্যাট ডাউনলোড ব্লক রাখুন", color = textMuted, fontSize = 11.sp)
                }
                Switch(
                    checked = downloadOnlyMp4Mkv,
                    onCheckedChange = {
                        downloadOnlyMp4Mkv = it
                        prefs.edit().putBoolean("download_only_mp4_mkv", it).apply()
                    }
                )
            }

            Spacer(Modifier.height(14.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentRed)
                    .clickable { onOpenDownloads() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (activeCount > 0) "Open Download Manager ($activeCount Active)"
                    else "Open Download Manager (${DownloadQueueManager.tasks.size})",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

/* ============================================================================
   SINGLE-ACTIVE DOWNLOAD QUEUE SCREEN (PAUSE / RESUME / 3-DOT DELETE)
   ============================================================================ */
@Composable
private fun DownloadQueueScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val tasks = DownloadQueueManager.tasks

    Column(
        Modifier
            .fillMaxSize()
            .appSurfaceBackground()
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(cardDark)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Text("‹", color = primaryText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Downloads", color = primaryText, fontSize = 23.sp, fontWeight = FontWeight.Black)
                Text("Movies/BeeGear • একসাথে ১টি ডাউনলোড চলবে", color = textMuted, fontSize = 11.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        if (tasks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("এখনও কোনো ডাউনলোড লিস্টে যোগ করা হয়নি।", color = textMuted, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    var menuExpanded by remember(task.id) { mutableStateOf(false) }
                    val progress = if (task.totalBytes > 0L) {
                        (task.downloadedBytes.toFloat() / task.totalBytes.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardDark)
                            .border(1.dp, primaryText.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    task.fileName,
                                    color = primaryText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(4.dp))
                                val statusLabel = when (task.status) {
                                    DownloadStatus.DOWNLOADING -> {
                                        val pct = if (task.totalBytes > 0L) " (${(progress * 100).toInt()}%)" else ""
                                        "Downloading • ${formatBytes(task.downloadedBytes)} / ${if (task.totalBytes > 0L) formatBytes(task.totalBytes) else "..."}$pct"
                                    }
                                    DownloadStatus.QUEUED -> "Queued • অপেক্ষায় আছে"
                                    DownloadStatus.PAUSED -> {
                                        val note = if (task.errorMessage.isNotBlank()) " • ${task.errorMessage}" else ""
                                        "Paused • ${formatBytes(task.downloadedBytes)}$note"
                                    }
                                    DownloadStatus.COMPLETED -> "Completed • Movies/BeeGear"
                                    DownloadStatus.FAILED -> "Failed • ${task.errorMessage}"
                                }
                                Text(
                                    statusLabel,
                                    color = if (task.status == DownloadStatus.FAILED) Color(0xFFE55353) else textMuted,
                                    fontSize = 11.sp
                                )
                            }

                            if (task.status != DownloadStatus.COMPLETED) {
                                Box(
                                    Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(accentRed.copy(alpha = 0.18f))
                                        .clickable {
                                            if (task.status == DownloadStatus.DOWNLOADING) {
                                                DownloadQueueManager.pauseTask(context, task.id)
                                            } else {
                                                DownloadQueueManager.resumeOrStartSingle(context, task.id)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (task.status == DownloadStatus.DOWNLOADING) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = "Pause or Start",
                                        tint = accentRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(4.dp))

                            Box {
                                Box(
                                    Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .clickable { menuExpanded = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MoreVert,
                                        contentDescription = "Options",
                                        tint = primaryText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    if (task.status != DownloadStatus.COMPLETED) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    if (task.status == DownloadStatus.DOWNLOADING) "Pause" else "Start / Resume"
                                                )
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                if (task.status == DownloadStatus.DOWNLOADING) {
                                                    DownloadQueueManager.pauseTask(context, task.id)
                                                } else {
                                                    DownloadQueueManager.resumeOrStartSingle(context, task.id)
                                                }
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = Color(0xFFE55353)) },
                                        onClick = {
                                            menuExpanded = false
                                            DownloadQueueManager.deleteTask(context, task.id)
                                        }
                                    )
                                }
                            }
                        }

                        if (task.status == DownloadStatus.DOWNLOADING || task.status == DownloadStatus.PAUSED) {
                            Spacer(Modifier.height(10.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(tagDark)
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(progress)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(accentRed)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ============================================================================
   1. HOME SCREEN
   ============================================================================ */
@Composable
private fun HomeScreen(
    recommendations: List<Recommendation>,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDownloads: () -> Unit,
    loading: Boolean,
    continueWatching: List<Recommendation>,
    onMenuClick: () -> Unit,
    onSeeAll: () -> Unit,
    onOpen: (Recommendation) -> Unit,
    onCategory: (Category) -> Unit,
    onContinueOpen: (Recommendation) -> Unit,
    onSearchClick: () -> Unit
) {
    val pageState = rememberScrollState()
    val tabState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val activeDownloads = DownloadQueueManager.tasks.count {
        it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED
    }

    Column(
        Modifier
            .fillMaxSize()
            .appSurfaceBackground()
            .verticalScroll(pageState)
            .padding(bottom = 112.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Bee", color = primaryText, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
            Text("Gear", color = accentRed, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.weight(1f))

            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(cardDark)
                    .clickable { onOpenDownloads() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = "Downloads",
                    tint = if (activeDownloads > 0) accentRed else primaryText,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(Modifier.width(8.dp))

            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(cardDark)
                    .clickable { onToggleTheme() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isDarkTheme) "☀" else "☾",
                    color = primaryText,
                    fontSize = 18.sp
                )
            }
            Spacer(Modifier.width(8.dp))

            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(cardDark)
                    .clickable { onOpenSettings() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = primaryText,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(Modifier.width(8.dp))

            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(cardDark)
                    .border(1.dp, primaryText.copy(alpha = 0.14f), CircleShape)
                    .clickable { onMenuClick() },
                contentAlignment = Alignment.Center
            ) {
                Text("☰", color = primaryText, fontSize = 19.sp)
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(tabState)
                .padding(start = 18.dp, end = 18.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf("Home" to null) + categories.map { it.name to it }
            tabs.forEach { (label, category) ->
                Text(
                    label,
                    color = if (category == null) accentRed else primaryText,
                    fontSize = 13.sp,
                    fontWeight = if (category == null) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.clickable {
                        if (category != null) onCategory(category)
                        else scope.launch { pageState.animateScrollTo(0) }
                    }
                )
            }
        }

        val featured = recommendations.take(8)
        val featuredState = rememberLazyListState()
        LaunchedEffect(featured.size) {
            if (featured.size > 1) {
                while (true) {
                    delay(5500)
                    val next = (featuredState.firstVisibleItemIndex + 1) % featured.size
                    featuredState.animateScrollToItem(next)
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Featured for You", color = primaryText, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Latest updates", color = textMuted, fontSize = 11.sp)
        }

        if (featured.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(230.dp), contentAlignment = Alignment.Center) {
                if (loading) CircularProgressIndicator(color = accentRed)
                else Text("Your ISP library is ready to explore", color = textMuted)
            }
        } else {
            LazyRow(
                state = featuredState,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(featured, key = { it.url }) { item ->
                    Box(
                        Modifier
                            .width(278.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(22.dp))
                            .background(cardDark)
                            .clickable { onOpen(item) }
                    ) {
                        if (item.poster != null) NetworkPoster(item.poster, Modifier.fillMaxSize())
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000), Color(0xF0000000))))
                        )
                        Column(Modifier.align(Alignment.BottomStart).padding(17.dp)) {
                            Text(
                                "NEW • ${item.subtitle.uppercase()}",
                                color = Color.White.copy(alpha = .82f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                item.title,
                                color = Color.White,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.White)
                                        .clickable { onOpen(item) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text("▶  Play", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.White.copy(alpha = .20f))
                                        .border(1.dp, Color.White.copy(alpha = .45f), RoundedCornerShape(50))
                                        .clickable { onOpen(item) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text("＋ My List", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Continue Watching", color = primaryText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        if (continueWatching.isEmpty()) {
            Text(
                "Play something from your ISP library and it will appear here.",
                color = textMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
            )
        } else {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                items(continueWatching, key = { it.url }) { item ->
                    Column(
                        Modifier
                            .width(210.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(cardDark)
                            .clickable { onContinueOpen(item) }
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .background(tagDark)
                        ) {
                            if (item.poster != null) NetworkPoster(item.poster, Modifier.fillMaxSize())
                            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(accentRed))
                        }
                        Text(
                            item.title,
                            color = primaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.padding(9.dp)
                        )
                    }
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Browse by Category", color = primaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(
                "Search all",
                color = accentRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onSearchClick() }
            )
        }

        GlassCategorySection(
            title = "Latest Movies",
            items = recommendations.filter { !it.subtitle.contains("Series", true) }.ifEmpty { recommendations },
            onSeeAll = onSeeAll,
            onOpen = onOpen
        )

        categories.forEach { category ->
            val sectionItems = recommendations.filter {
                it.subtitle.equals(category.name, ignoreCase = true) ||
                    it.subtitle.contains(category.name, ignoreCase = true)
            }
            GlassCategorySection(
                title = category.name,
                items = sectionItems,
                onSeeAll = { onCategory(category) },
                onOpen = onOpen,
                emptyLoading = loading && recommendations.isEmpty(),
                emptyText = if (loading) "Loading latest updates from ISP server..." else "Open to browse this category"
            )
        }

        GlassCategorySection(
            title = "Latest Series & Episodes",
            items = recommendations.filter { it.subtitle.contains("Series", true) }.ifEmpty { recommendations },
            onSeeAll = onSeeAll,
            onOpen = onOpen
        )
    }
}

@Composable
private fun GlassCategorySection(
    title: String,
    items: List<Recommendation>,
    onSeeAll: () -> Unit,
    onOpen: (Recommendation) -> Unit,
    emptyLoading: Boolean = false,
    emptyText: String = "No preview items loaded"
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(cardDark)
            .border(1.dp, primaryText.copy(alpha = .08f), RoundedCornerShape(20.dp))
            .padding(vertical = 13.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = primaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(
                "See all  ›",
                color = accentRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onSeeAll() }
            )
        }
        if (items.isEmpty()) {
            if (emptyLoading) {
                Box(Modifier.fillMaxWidth().height(82.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentRed, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                }
            } else {
                Text(emptyText, color = textMuted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp))
            }
        } else {
            LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                items(items.take(12), key = { it.url }) { item ->
                    Column(
                        Modifier
                            .width(124.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(bgDark.copy(alpha = 0.65f))
                            .border(1.dp, primaryText.copy(alpha = .07f), RoundedCornerShape(13.dp))
                            .clickable { onOpen(item) }
                    ) {
                        Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).background(tagDark)) {
                            if (item.poster != null) NetworkPoster(item.poster, Modifier.fillMaxSize())
                            else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("BeeGear", color = accentRed, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Text(
                            item.title,
                            color = primaryText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }
    }
}

/* ============================================================================
   2. DETAILS & FOLDER SCREEN (IN-FOLDER SEARCH + BeeGear HOME BUTTON)
   ============================================================================ */
@Composable
private fun MediaDetailsScreen(
    title: String,
    initialPoster: String?,
    entries: List<MediaEntry>,
    loading: Boolean,
    error: String,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    onGoHome: () -> Unit,
    onOpenDownloads: () -> Unit,
    onFolder: (MediaEntry) -> Unit,
    onVideo: (MediaEntry) -> Unit,
    onDownloadVideo: (MediaEntry) -> Unit
) {
    var folderSearchOpen by remember(title) { mutableStateOf(false) }
    var folderTitleQuery by remember(title) { mutableStateOf("") }

    val normalizedQuery = remember(folderTitleQuery) { normalizeSearchText(folderTitleQuery) }
    val filteredFolders = remember(entries, normalizedQuery) {
        val baseFolders = entries.filter { it.isFolder }
        if (normalizedQuery.isBlank()) baseFolders
        else baseFolders.filter { normalizeSearchText(it.name).contains(normalizedQuery) }
    }
    val videos = remember(entries) { entries.filter { it.isVideo } }
    val firstVideo = videos.firstOrNull()
    val folderRows = remember(filteredFolders) { filteredFolders.chunked(2) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().appSurfaceBackground(),
        contentPadding = PaddingValues(bottom = 36.dp)
    ) {
        item {
            Box(Modifier.fillMaxWidth().height(310.dp)) {
                if (initialPoster != null) NetworkPoster(initialPoster, Modifier.fillMaxSize())
                else Box(Modifier.fillMaxSize().background(cardDark))

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent, bgDark)))
                )

                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("‹", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.weight(1f))
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xD912110F))
                            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                            .clickable { onGoHome() }
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Bee",
                            color = Color(0xFFF6F3EE),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                        Text(
                            "Gear",
                            color = Color(0xFFD99B48),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (folderSearchOpen) accentRed else Color.Black.copy(alpha = 0.55f))
                                .clickable {
                                    folderSearchOpen = !folderSearchOpen
                                    if (!folderSearchOpen) folderTitleQuery = ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search Title in Folder",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .clickable { onOpenDownloads() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = "Downloads",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                        .clickable(enabled = firstVideo != null) { if (firstVideo != null) onVideo(firstVideo) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("▶", color = Color.White, fontSize = 20.sp)
                }
            }
        }

        item {
            val totalFolderCount = entries.count { it.isFolder }
            Column(
                Modifier
                    .fillMaxWidth()
                    .offset(y = (-24).dp)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .appSurfaceBackground()
                    .padding(horizontal = 22.dp, vertical = 16.dp)
            ) {
                Text(
                    text = title,
                    color = primaryText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = if (videos.isNotEmpty()) "${videos.size} Video File(s) • $totalFolderCount Folder(s)" else "$totalFolderCount Folder(s) Available",
                    color = textMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    MetaChip("HD Stream"); Spacer(Modifier.width(8.dp))
                    MetaChip("16+"); Spacer(Modifier.width(8.dp))
                    MetaChip("★ 4.5", textColor = starGold)
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(tagDark)
                            .clickable { onToggleFavorite() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isFavorite) "♥" else "♡", color = if (isFavorite) accentRed else primaryText, fontSize = 16.sp)
                    }
                }

                if (folderSearchOpen) {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(cardDark)
                            .border(1.dp, accentRed.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = accentRed,
                            modifier = Modifier.size(18.dp)
                        )
                        BasicTextField(
                            value = folderTitleQuery,
                            onValueChange = { folderTitleQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(color = primaryText, fontSize = 14.sp),
                            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                            decorationBox = { inner ->
                                Box {
                                    if (folderTitleQuery.isEmpty()) {
                                        Text("শুধু এই ফোল্ডারের টাইটেল খুঁজুন...", color = textMuted, fontSize = 13.sp)
                                    }
                                    inner()
                                }
                            }
                        )
                        if (folderTitleQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear",
                                tint = textMuted,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { folderTitleQuery = "" }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text("Story Line", color = primaryText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Streaming directly from your local ISP media server ($title). Tap any video to play or add to your download queue.",
                    color = textMuted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(14.dp))

                if (loading) {
                    Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accentRed)
                    }
                } else if (error.isNotBlank()) {
                    Text("Server error: $error", color = Color(0xFFFF7777), fontSize = 13.sp)
                } else if (filteredFolders.isEmpty() && videos.isEmpty()) {
                    Text(
                        if (normalizedQuery.isNotBlank()) "\"$folderTitleQuery\" নামে এই ফোল্ডারে কোনো টাইটেল পাওয়া যায়নি।"
                        else "এই ফোল্ডারে কোনো কনটেন্ট পাওয়া যায়নি।",
                        color = textMuted,
                        fontSize = 13.sp
                    )
                }
            }
        }

        if (!loading && error.isBlank() && videos.isNotEmpty() && normalizedQuery.isBlank()) {
            item {
                Text(
                    "Playable Files (${videos.size})",
                    color = primaryText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp)
                )
            }
            items(videos, key = { it.url }) { video ->
                val queuedTask = DownloadQueueManager.tasks.firstOrNull { it.url == video.url }

                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardDark)
                        .padding(14.dp)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onVideo(video) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(accentRed), contentAlignment = Alignment.Center) {
                            Text("▶", color = Color.White, fontSize = 12.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            video.name,
                            color = primaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(accentRed)
                                .clickable { onVideo(video) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("▶ Play Now", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        if (downloadsEnabled) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(tagDark)
                                    .border(1.dp, accentRed.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        if (queuedTask != null) onOpenDownloads()
                                        else onDownloadVideo(video)
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Download,
                                        contentDescription = "Download",
                                        tint = primaryText,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    val dlText = when (queuedTask?.status) {
                                        DownloadStatus.DOWNLOADING -> "Downloading..."
                                        DownloadStatus.QUEUED -> "In Queue"
                                        DownloadStatus.PAUSED -> "Paused"
                                        DownloadStatus.COMPLETED -> "Downloaded"
                                        else -> "Download"
                                    }
                                    Text(dlText, color = primaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!loading && error.isBlank() && filteredFolders.isNotEmpty()) {
            item {
                Text(
                    if (normalizedQuery.isNotBlank()) "Matching Titles (${filteredFolders.size})"
                    else "Folders & Seasons (${filteredFolders.size})",
                    color = primaryText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 12.dp)
                )
            }
            items(folderRows, key = { row -> row.joinToString("|") { it.url } }) { pair ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { folder ->
                        FolderCard(folder, Modifier.weight(1f)) { onFolder(folder) }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MetaChip(label: String, textColor: Color = primaryText) {
    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(tagDark).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(label, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FolderCard(folder: MediaEntry, modifier: Modifier, onClick: () -> Unit) {
    val context = LocalContext.current
    var poster by remember(folder.url) { mutableStateOf(InternalStorageStore.getMappedPoster(context, folder.url)) }
    LaunchedEffect(folder.url) {
        if (poster == null) {
            poster = InternalStorageStore.getOrFetchFolderPoster(context, folder.url)
        }
    }
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(cardDark).clickable { onClick() }) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(tagDark),
            contentAlignment = Alignment.Center
        ) {
            if (poster != null) NetworkPoster(poster!!, Modifier.fillMaxSize())
            else Text("BEEGEAR", color = accentRed, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
        Text(
            folder.name,
            Modifier.padding(10.dp),
            color = primaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/* ============================================================================
   3. BROWSE & SAVED SCREENS
   ============================================================================ */
@Composable
private fun SearchScreen(onOpenFolder: (SearchHit) -> Unit, onPlayVideo: (SearchHit) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<SearchHit>()) }
    var loading by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().appSurfaceBackground().padding(horizontal = 18.dp).padding(bottom = 94.dp)) {
        Text("Search", color = primaryText, fontSize = 26.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 22.dp, bottom = 14.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cardDark)
                .border(1.dp, primaryText.copy(alpha = .10f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⌕", color = textMuted, fontSize = 22.sp)
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = primaryText, fontSize = 15.sp),
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) Text("Search ISP categories and titles", color = textMuted, fontSize = 13.sp)
                        inner()
                    }
                }
            )
            Text(
                "Search",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentRed)
                    .clickable(enabled = query.isNotBlank() && !loading) {
                        scope.launch {
                            loading = true
                            searched = true
                            error = ""
                            results = searchAllCategories(query)
                            loading = false
                        }
                    }
                    .padding(horizontal = 13.dp, vertical = 10.dp)
            )
        }
        if (loading) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accentRed)
            }
        } else if (error.isNotBlank()) {
            Text(error, color = Color(0xFFFF7777), modifier = Modifier.padding(12.dp))
        } else if (searched && results.isEmpty()) {
            Text("No matching title found in the configured ISP folders.", color = textMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 20.dp))
        } else if (!searched) {
            Text("Tip: যেকোনো ফোল্ডারের ভেতরে ঢুকে ওপরের সার্চ আইকন থেকে শুধু সেই ফোল্ডারের টাইটেলও খুঁজতে পারবেন।", color = textMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 18.dp))
        } else {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 12.dp)) {
                Text("${results.size} result(s)", color = textMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                results.forEach { hit ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(cardDark)
                            .clickable { if (hit.isVideo) onPlayVideo(hit) else onOpenFolder(hit) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(58.dp, 76.dp).clip(RoundedCornerShape(8.dp)).background(tagDark)) {
                            if (hit.poster != null) NetworkPoster(hit.poster, Modifier.fillMaxSize())
                            else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(if (hit.isVideo) "▶" else "▣", color = accentRed)
                            }
                        }
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(hit.title, color = primaryText, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(hit.subtitle, color = textMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                            Text(if (hit.isVideo) "Video file" else "Open folder", color = accentRed, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                        Text("›", color = textMuted, fontSize = 24.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseScreen(
    onCategory: (Category) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDownloads: () -> Unit
) {
    Column(Modifier.fillMaxSize().appSurfaceBackground().verticalScroll(rememberScrollState()).padding(bottom = 100.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 18.dp, top = 24.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Browse Categories", color = primaryText, fontSize = 24.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(cardDark)
                    .clickable { onOpenDownloads() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Download, contentDescription = "Downloads", tint = primaryText, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(cardDark)
                    .clickable { onOpenSettings() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = primaryText, modifier = Modifier.size(19.dp))
            }
        }
        categories.forEach { category ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardDark)
                    .clickable { onCategory(category) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(accentRed.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                    Text("▶", color = accentRed, fontSize = 13.sp)
                }
                Spacer(Modifier.width(14.dp))
                Text(category.name, color = primaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("›", color = textMuted, fontSize = 22.sp)
            }
        }
    }
}

@Composable
private fun TrendingOrSavedScreen(
    screenTitle: String,
    items: List<Recommendation>,
    emptyMessage: String = "লোড হচ্ছে...",
    onOpen: (Recommendation) -> Unit
) {
    Column(Modifier.fillMaxSize().appSurfaceBackground().verticalScroll(rememberScrollState()).padding(bottom = 100.dp)) {
        Text(screenTitle, Modifier.padding(start = 20.dp, top = 24.dp, bottom = 16.dp), color = primaryText, fontSize = 24.sp, fontWeight = FontWeight.Black)
        if (items.isEmpty()) {
            Text(emptyMessage, Modifier.padding(20.dp), color = textMuted, fontSize = 13.sp)
        } else {
            items.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { item ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(cardDark).clickable { onOpen(item) }) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(175.dp)
                                    .background(tagDark),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.poster != null) NetworkPoster(item.poster, Modifier.fillMaxSize())
                                else Text("BEEGEAR", color = accentRed, fontWeight = FontWeight.Black)
                            }
                            Text(
                                item.title,
                                Modifier.padding(10.dp),
                                color = primaryText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

/* ============================================================================
   4. FLOATING PILL BOTTOM NAVIGATION BAR (DYNAMIC THEME & PALETTE SYNC)
   ============================================================================ */
@Composable
private fun FloatingBottomNav(tab: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val navItems = listOf(
        "HOME" to "clapperboard",
        "SEARCH" to "radar",
        "BROWSE" to "orbit",
        "TRENDING" to "flame",
        "FAVORITES" to "layers-3"
    )
    val selectedIndex = navItems.indexOfFirst { it.first == tab }.coerceAtLeast(0)
    val isDark = appDarkMode
    val barBg = if (isDark) cardDark.copy(alpha = 0.88f) else cardDark.copy(alpha = 0.92f)
    val borderTint = if (isDark) accentRed.copy(alpha = 0.35f) else primaryText.copy(alpha = 0.14f)
    val activePillBrush = if (isDark) {
        Brush.linearGradient(
            listOf(
                accentRed.copy(alpha = 0.32f),
                primaryText.copy(alpha = 0.14f),
                accentRed.copy(alpha = 0.18f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                bgDark.copy(alpha = 0.96f),
                accentRed.copy(alpha = 0.16f),
                bgDark.copy(alpha = 0.82f)
            )
        )
    }
    val activePillBorder = accentRed.copy(alpha = if (isDark) 0.55f else 0.42f)

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(RoundedCornerShape(40.dp))
            .background(barBg)
            .border(1.dp, borderTint, RoundedCornerShape(40.dp))
    ) {
        val itemWidth = maxWidth / navItems.size
        val pillWidth = itemWidth * 0.90f
        val indicatorX by animateDpAsState(
            targetValue = itemWidth * selectedIndex + (itemWidth - pillWidth) / 2,
            animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
            label = "liquidGlassPosition"
        )
        Box(
            Modifier
                .offset(x = indicatorX, y = 5.dp)
                .width(pillWidth)
                .height(66.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(activePillBrush)
                .border(1.dp, activePillBorder, RoundedCornerShape(36.dp))
        )
        Row(Modifier.fillMaxSize().padding(horizontal = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            navItems.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                Box(Modifier.weight(1f).fillMaxHeight().clickable { onSelect(item.first) }, contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (item.second) {
                            "clapperboard" -> Icons.Filled.Movie
                            "radar" -> Icons.Filled.TravelExplore
                            "orbit" -> Icons.Filled.Explore
                            "flame" -> Icons.Filled.Whatshot
                            else -> Icons.Filled.CollectionsBookmark
                        },
                        contentDescription = item.first,
                        tint = if (selected) accentRed else primaryText.copy(alpha = 0.78f),
                        modifier = Modifier.size(if (selected) 34.dp else 31.dp)
                    )
                }
            }
        }
    }
}

/* ============================================================================
   5. IMAGE LOADER (INTERNAL STORAGE) & EXOPLAYER SCREEN
   ============================================================================ */
@Composable
private fun NetworkPoster(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(null, url) {
        value = InternalStorageStore.loadThumbnailFromInternalStorage(context, url)
    }
    if (bitmap != null) {
        Image(bitmap!!.asImageBitmap(), null, modifier, contentScale = ContentScale.Crop)
    } else {
        Box(modifier.background(tagDark), contentAlignment = Alignment.Center) {
            Text("BEEGEAR", color = accentRed, fontWeight = FontWeight.Black, fontSize = 11.sp)
        }
    }
}

@Composable
private fun VideoPlayerScreen(url: String, title: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var playerError by remember(url) { mutableStateOf("") }
    var subtitleDialog by remember { mutableStateOf(false) }
    var fullscreen by remember { mutableStateOf(false) }
    var embeddedSubtitleTracks by remember { mutableStateOf(emptyList<Pair<String, Pair<TrackGroup, Int>>>()) }
    var autoSubtitleConfigs by remember { mutableStateOf(emptyList<MediaItem.SubtitleConfiguration>()) }
    var gestureOverlay by remember { mutableStateOf<Pair<String, Float>?>(null) }
    val overlayScope = rememberCoroutineScope()

    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(url)))
            prepare()
            playWhenReady = true
        }
    }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    val gestureModifier = Modifier.pointerInput(player, fullscreen) {
        var accumulatedY = 0f
        detectDragGestures(
            onDragStart = { accumulatedY = 0f },
            onDragEnd = {
                overlayScope.launch {
                    delay(1200)
                    gestureOverlay = null
                }
            },
            onDrag = { change, dragAmount ->
                change.consume()
                accumulatedY += dragAmount.y
                if (abs(accumulatedY) >= 22f) {
                    val increase = accumulatedY < 0f
                    val isLeftHalf = change.position.x < size.width / 2f
                    if (isLeftHalf) {
                        val activity = context as? Activity
                        val window = activity?.window
                        if (window != null) {
                            val attrs = window.attributes
                            val current = if (attrs.screenBrightness < 0f) 0.5f else attrs.screenBrightness
                            val newBrightness = (current + if (increase) 0.06f else -0.06f).coerceIn(0.02f, 1f)
                            attrs.screenBrightness = newBrightness
                            window.attributes = attrs
                            gestureOverlay = "BRIGHTNESS" to newBrightness
                        }
                    } else {
                        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        val newVolume = (currentVolume + if (increase) 1 else -1).coerceIn(0, maxVolume)
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                        gestureOverlay = "VOLUME" to (newVolume.toFloat() / maxVolume.toFloat())
                    }
                    accumulatedY = 0f
                }
            }
        )
    }

    LaunchedEffect(player, url) {
        val subtitles = findSameFolderSubtitles(url)
        if (subtitles.isNotEmpty()) {
            val position = player.currentPosition
            val wasPlaying = player.playWhenReady
            val item = MediaItem.Builder().setUri(Uri.parse(url)).setSubtitleConfigurations(subtitles).build()
            player.setMediaItem(item, position)
            player.prepare()
            player.playWhenReady = wasPlaying
            autoSubtitleConfigs = subtitles
        }
    }

    DisposableEffect(player) { onDispose { player.release() } }

    val externalSubtitlePicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
            }
            val mime = subtitleMimeType(uri.lastPathSegment ?: uri.toString())
                ?: when (context.contentResolver.getType(uri)) {
                    "text/vtt" -> MimeTypes.TEXT_VTT
                    "application/x-subrip", "application/srt" -> MimeTypes.APPLICATION_SUBRIP
                    else -> null
                }
            if (mime == null) {
                playerError = "এই subtitle format সমর্থিত নয়। .srt অথবা .vtt ফাইল নির্বাচন করুন।"
            } else {
                val position = player.currentPosition
                val wasPlaying = player.playWhenReady
                val currentItem = MediaItem.Builder().setUri(Uri.parse(url))
                    .setSubtitleConfigurations(
                        buildList {
                            addAll(autoSubtitleConfigs)
                            add(
                                MediaItem.SubtitleConfiguration.Builder(uri)
                                    .setMimeType(mime)
                                    .setLanguage("und")
                                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                                    .build()
                            )
                        }
                    )
                    .build()
                player.setMediaItem(currentItem, position)
                player.prepare()
                player.playWhenReady = wasPlaying
                playerError = ""
            }
        }
    }

    fun refreshSubtitleTracks() {
        val tracks = mutableListOf<Pair<String, Pair<TrackGroup, Int>>>()
        player.currentTracks.groups.forEach { group ->
            if (group.type == C.TRACK_TYPE_TEXT) {
                for (index in 0 until group.length) {
                    if (group.isTrackSupported(index)) {
                        val format = group.getTrackFormat(index)
                        val label = format.label ?: format.language ?: "Subtitle ${tracks.size + 1}"
                        tracks.add(label to (group.mediaTrackGroup to index))
                    }
                }
            }
        }
        embeddedSubtitleTracks = tracks
    }

    LaunchedEffect(player) {
        while (true) {
            refreshSubtitleTracks()
            delay(700)
        }
    }

    DisposableEffect(fullscreen) {
        val activity = context as? Activity
        val decor = activity?.window?.decorView
        @Suppress("DEPRECATION")
        val oldSystemUi = decor?.systemUiVisibility ?: 0
        val oldOrientation = activity?.requestedOrientation
        if (fullscreen) {
            @Suppress("DEPRECATION")
            decor?.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            @Suppress("DEPRECATION")
            decor?.systemUiVisibility = oldSystemUi
            if (oldOrientation != null) activity?.requestedOrientation = oldOrientation
        }
        onDispose {
            @Suppress("DEPRECATION")
            decor?.systemUiVisibility = oldSystemUi
            if (oldOrientation != null) activity?.requestedOrientation = oldOrientation
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (!fullscreen) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("‹", color = Color.White, fontSize = 30.sp, modifier = Modifier.clickable { onBack() })
                    Spacer(Modifier.width(12.dp))
                    Text(title, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(
                        "CC",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                            .clickable { refreshSubtitleTracks(); subtitleDialog = true }
                            .padding(horizontal = 13.dp, vertical = 9.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "⛶",
                        color = Color.White,
                        fontSize = 24.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                            .clickable { fullscreen = true }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                AndroidView(
                    factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true; keepScreenOn = true } },
                    update = { it.player = player },
                    modifier = Modifier.weight(1f).fillMaxWidth().then(gestureModifier)
                )
            }
        } else {
            AndroidView(
                factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true; keepScreenOn = true } },
                update = { it.player = player },
                modifier = Modifier.fillMaxSize().then(gestureModifier)
            )
            Row(Modifier.align(Alignment.TopEnd).padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "CC",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable { refreshSubtitleTracks(); subtitleDialog = true }
                        .padding(horizontal = 13.dp, vertical = 9.dp)
                )
                Text(
                    "⛶",
                    color = Color.White,
                    fontSize = 24.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable { fullscreen = false }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
            Text(
                "‹",
                color = Color.White,
                fontSize = 30.sp,
                modifier = Modifier.align(Alignment.TopStart).padding(14.dp).clickable { fullscreen = false; onBack() }
            )
        }

        gestureOverlay?.let { (type, value) ->
            val isVolume = type == "VOLUME"
            Box(
                Modifier
                    .align(if (isVolume) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isVolume) {
                            when {
                                value <= 0f -> "🔇"
                                value < 0.4f -> "🔈"
                                value < 0.75f -> "🔉"
                                else -> "🔊"
                            }
                        } else {
                            when {
                                value < 0.3f -> "🌑"
                                value < 0.6f -> "🌤"
                                else -> "☀️"
                            }
                        },
                        fontSize = 22.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.width(4.dp).height(80.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.25f))) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(value)
                                .align(Alignment.BottomCenter)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("${(value * 100).toInt()}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (playerError.isNotBlank()) {
            Text(
                playerError,
                color = Color(0xFFFF8A80),
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
            )
        }
    }

    if (subtitleDialog) {
        AlertDialog(
            onDismissRequest = { subtitleDialog = false },
            containerColor = Color(0xFF202027),
            title = { Text("Subtitle settings", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        player.trackSelectionParameters = player.trackSelectionParameters
                            .buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                            .build()
                        subtitleDialog = false
                    }) { Text("Off", color = Color.White) }

                    if (embeddedSubtitleTracks.isEmpty()) {
                        Text("No embedded subtitle track detected.", color = Color.LightGray, fontSize = 13.sp)
                    } else {
                        embeddedSubtitleTracks.forEach { (label, target) ->
                            TextButton(onClick = {
                                val (mediaTrackGroup, trackIndex) = target
                                player.trackSelectionParameters = player.trackSelectionParameters
                                    .buildUpon()
                                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                    .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                    .addOverride(TrackSelectionOverride(mediaTrackGroup, listOf(trackIndex)))
                                    .build()
                                subtitleDialog = false
                            }) { Text(label, color = Color.White) }
                        }
                    }

                    TextButton(onClick = {
                        subtitleDialog = false
                        externalSubtitlePicker.launch(arrayOf("application/x-subrip", "text/vtt", "text/*", "*/*"))
                    }) {
                        Text("＋ Open external subtitle", color = Color(0xFFFF6B75))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { subtitleDialog = false }) {
                    Text("Close", color = Color.White)
                }
            }
        )
    }
}
