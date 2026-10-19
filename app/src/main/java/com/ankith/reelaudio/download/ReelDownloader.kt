package com.ankith.reelaudio.download

import android.content.Context
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/** Fetches the audio track of a reel (or any yt-dlp supported link) into app storage. */
class ReelDownloader(private val context: Context) {

    data class Result(
        val title: String,
        val author: String,
        val caption: String,
        val subtitles: String,
        val durationSec: Int,
        val filePath: String,
        val thumbPath: String,
    )

    private val initLock = Mutex()
    private var initialized = false

    val audioDir: File get() = File(context.filesDir, "audio").apply { mkdirs() }
    val cookiesFile: File get() = File(context.filesDir, "instagram_cookies.txt")

    private suspend fun ensureInit() = initLock.withLock {
        if (!initialized) {
            withContext(Dispatchers.IO) { YoutubeDL.getInstance().init(context) }
            initialized = true
        }
    }

    /** Pulls the newest extractor so Instagram changes don't break saving. */
    suspend fun updateExtractor(): String = withContext(Dispatchers.IO) {
        ensureInit()
        val status = YoutubeDL.getInstance().updateYoutubeDL(context)
        "${status?.name ?: "DONE"} (${YoutubeDL.getInstance().versionName(context) ?: "unknown"})"
    }

    suspend fun download(url: String, clipId: Long): Result = withContext(Dispatchers.IO) {
        ensureInit()
        val base = "clip_$clipId"
        audioDir.listFiles { f -> f.name.startsWith("$base.") }?.forEach { it.delete() }

        val request = YoutubeDLRequest(url).apply {
            addOption("-f", "bestaudio/best")
            addOption("--no-playlist")
            addOption("--playlist-items", "1")
            addOption("--no-mtime")
            addOption("-o", "${audioDir.absolutePath}/$base.%(ext)s")
            addOption("--write-thumbnail")
            addOption("--write-subs")
            addOption("--write-auto-subs")
            addOption("--sub-langs", "all,-live_chat")
            addOption("--sub-format", "vtt/best")
            addOption("--ignore-errors")
            addOption(
                "--print",
                "after_move:%(.{title,uploader,uploader_id,channel,description,duration,filepath})j",
            )
            if (cookiesFile.exists()) addOption("--cookies", cookiesFile.absolutePath)
        }
        // yt-dlp exits non-zero when a side file (like subtitles) fails even though the audio
        // saved fine, so judge success by the file on disk rather than the exit code.
        val (out, err) = try {
            YoutubeDL.getInstance().execute(request, "clip-$clipId").let { it.out to it.err }
        } catch (e: YoutubeDL.CanceledException) {
            throw IllegalStateException("Cancelled")
        } catch (e: Exception) {
            "" to (e.message ?: e.toString())
        }
        val jsonLine = out.lineSequence().lastOrNull { it.trim().startsWith("{") }
        val json = jsonLine?.let { runCatching { JSONObject(it) }.getOrNull() } ?: JSONObject()

        val media = json.optString("filepath").takeIf { it.isNotBlank() && File(it).exists() }
            ?: audioDir.listFiles { f -> f.name.startsWith("$base.") && f.extension.lowercase() in MEDIA_EXT }
                ?.firstOrNull()?.absolutePath
            ?: throw IllegalStateException(friendlyError(err))
        val thumb = audioDir.listFiles { f -> f.name.startsWith("$base.") && f.extension.lowercase() in IMAGE_EXT }
            ?.firstOrNull()?.absolutePath.orEmpty()
        val subs = audioDir.listFiles { f -> f.name.startsWith("$base.") && f.extension.lowercase() == "vtt" }
            .orEmpty().sortedBy { if (it.name.contains(".en")) 0 else 1 }
        val subtitleText = subs.firstOrNull()?.readText().orEmpty()
        subs.forEach { it.delete() }

        val author = listOf("uploader", "channel", "uploader_id")
            .map { json.optString(it).takeUnless { s -> s == "null" }.orEmpty() }
            .firstOrNull { it.isNotBlank() }.orEmpty()
        val caption = json.optString("description").takeUnless { it == "null" }.orEmpty()
        val rawTitle = json.optString("title").takeUnless { it == "null" }.orEmpty()
        Result(
            title = niceTitle(rawTitle, caption, author),
            author = author,
            caption = caption,
            subtitles = subtitleText,
            durationSec = json.optDouble("duration", 0.0).toInt(),
            filePath = media,
            thumbPath = thumb,
        )
    }

    fun cancel(clipId: Long) {
        runCatching { YoutubeDL.getInstance().destroyProcessById("clip-$clipId") }
    }

    private fun niceTitle(title: String, caption: String, author: String): String {
        // Instagram titles are usually "Video by <user>"; the caption's first line reads better.
        val firstLine = caption.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }.orEmpty()
        val generic = title.isBlank() || title.startsWith("Video by", true) || title.startsWith("Post by", true)
        val chosen = if (generic && firstLine.isNotEmpty()) firstLine else title
        return chosen.ifBlank { if (author.isNotBlank()) "Reel by $author" else "Untitled reel" }.take(120)
    }

    private fun friendlyError(err: String): String {
        Log.w("ReelDownloader", err)
        val e = err.lowercase()
        return when {
            "login" in e || "cookies" in e || "rate-limit" in e || "rate limit" in e ->
                "Instagram wants you signed in. Open Settings > Sign in to Instagram, then retry."
            "private" in e -> "This reel is private."
            "unsupported url" in e -> "That link isn't a supported reel or video link."
            else -> err.lineSequence().lastOrNull { it.contains("ERROR") }?.substringAfter("ERROR:")?.trim()
                ?: "Couldn't save this reel. Try Settings > Update extractor, then retry."
        }
    }

    companion object {
        val MEDIA_EXT = setOf("m4a", "mp4", "webm", "mp3", "aac", "opus", "ogg", "mkv", "mov")
        val IMAGE_EXT = setOf("jpg", "jpeg", "png", "webp")
    }
}
