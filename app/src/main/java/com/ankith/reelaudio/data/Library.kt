package com.ankith.reelaudio.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.ankith.reelaudio.download.ReelDownloader
import com.ankith.reelaudio.links.IdeaExtractor
import com.ankith.reelaudio.links.LinkFetcher
import com.ankith.reelaudio.transcribe.SpeechModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Single entry point the UI talks to: saving reels, groups, deleting. */
class Library(private val context: Context) {
    val db = AppDatabase.get(context)
    val downloader = ReelDownloader(context)
    val speech = SpeechModel(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val transcribeQueue = Channel<Long>(Channel.UNLIMITED)

    init {
        // One clip at a time, so the phone stays responsive.
        scope.launch { for (id in transcribeQueue) runTranscribe(id) }
        // Pick up anything that was waiting when the app last closed.
        scope.launch {
            if (speech.isReady) {
                db.clips().idsWithTranscript(listOf("QUEUED", "WORKING")).forEach { transcribeQueue.trySend(it) }
            }
        }
    }

    /** Queues speech-to-text for one clip. */
    fun transcribe(clipId: Long) = scope.launch {
        val clip = db.clips().get(clipId) ?: return@launch
        db.clips().update(clip.copy(transcriptStatus = TranscriptStatus.QUEUED))
        transcribeQueue.trySend(clipId)
    }

    /** Queues every saved clip that has no transcript yet. Returns how many. */
    suspend fun transcribeMissing(): Int {
        val ids = db.clips().idsWithTranscript(listOf("NONE", "FAILED"))
        ids.forEach { transcribe(it) }
        return ids.size
    }

    private suspend fun runTranscribe(clipId: Long) {
        val clip = db.clips().get(clipId) ?: return
        if (clip.status != ClipStatus.READY || clip.filePath.isBlank()) return
        db.clips().update(clip.copy(transcriptStatus = TranscriptStatus.WORKING))
        val updated = try {
            val t = speech.transcribe(clip.filePath)
            (db.clips().get(clipId) ?: return).copy(
                transcript = t.text,
                transcriptLang = t.language,
                subtitles = if (t.text.isNotBlank()) t.vtt else clip.subtitles,
                transcriptStatus = TranscriptStatus.DONE,
            )
        } catch (t: Throwable) {
            (db.clips().get(clipId) ?: return).copy(transcriptStatus = TranscriptStatus.FAILED, error = t.message ?: "")
        }
        db.clips().update(updated)
        if (updated.transcriptStatus == TranscriptStatus.DONE) extractIdeas(clipId)
    }

    data class SaveResult(val reels: Int, val links: Int, val existing: Int)

    /**
     * Saves every link found in [text]: reels and videos go to the audio library, any other
     * website goes to Links.
     */
    suspend fun addFromText(text: String, groupId: Long? = null): SaveResult {
        var reels = 0
        var links = 0
        var existing = 0
        for (url in extractUrls(text)) {
            if (isMediaUrl(url)) {
                val found = db.clips().byUrl(url)
                val id = if (found != null) {
                    if (found.status == ClipStatus.FAILED) retry(found.id)
                    existing++
                    found.id
                } else {
                    reels++
                    db.clips().insert(Clip(sourceUrl = url)).also { startDownload(it, url) }
                }
                if (groupId != null && id > 0) db.clips().addToGroup(ClipGroup(id, groupId))
            } else {
                val (id, isNew) = addLink(url, sourceClipId = null)
                if (isNew) links++ else existing++
                if (groupId != null && id > 0) db.links().addToGroup(LinkGroup(id, groupId))
            }
        }
        return SaveResult(reels, links, existing)
    }

    /** Saves a website and fetches its details in the background. Returns its id and whether it's new. */
    suspend fun addLink(url: String, sourceClipId: Long?): Pair<Long, Boolean> {
        db.links().byUrl(url)?.let { found ->
            if (found.status == LinkStatus.FAILED) refreshLink(found.id)
            return found.id to false
        }
        val id = db.links().insert(SavedLink(url = url, sourceClipId = sourceClipId))
        if (id > 0) scope.launch { runFetchLink(id) }
        return id to (id > 0)
    }

    fun refreshLink(linkId: Long) = scope.launch {
        val link = db.links().get(linkId) ?: return@launch
        db.links().update(link.copy(status = LinkStatus.FETCHING, error = ""))
        runFetchLink(linkId)
    }

    private val linksDir get() = File(context.filesDir, "links").apply { mkdirs() }

    private suspend fun runFetchLink(linkId: Long) {
        val link = db.links().get(linkId) ?: return
        val updated = try {
            val page = LinkFetcher.fetch(link.url)
            val image = File(linksDir, "link_$linkId.img")
            val hasImage = page.imageUrl.isNotBlank() && LinkFetcher.downloadImage(page.imageUrl, image)
            (db.links().get(linkId) ?: return).copy(
                title = page.title, siteName = page.siteName, description = page.description,
                content = page.content, imagePath = if (hasImage) image.absolutePath else "",
                status = LinkStatus.READY, error = "",
            )
        } catch (t: Throwable) {
            (db.links().get(linkId) ?: return).copy(
                status = LinkStatus.FAILED,
                error = "Couldn't open this page. ${t.message.orEmpty()}".trim(),
            )
        }
        db.links().update(updated)
    }

    suspend fun deleteLink(link: SavedLink) = withContext(Dispatchers.IO) {
        if (link.imagePath.isNotBlank()) File(link.imagePath).delete()
        db.links().clearGroups(link.id)
        db.links().delete(link.id)
    }

    suspend fun setLinkInGroup(linkId: Long, groupId: Long, member: Boolean) =
        if (member) db.links().addToGroup(LinkGroup(linkId, groupId))
        else db.links().removeFromGroup(linkId, groupId)

    /** Saves websites mentioned in a reel's caption or spoken words into Links. */
    private suspend fun extractIdeas(clipId: Long) {
        val clip = db.clips().get(clipId) ?: return
        val text = listOf(clip.caption, clip.transcript).joinToString("\n")
        IdeaExtractor.findLinks(text, exclude = clip.sourceUrl).forEach { addLink(it, sourceClipId = clipId) }
    }

    fun retry(clipId: Long) = scope.launch {
        val clip = db.clips().get(clipId) ?: return@launch
        db.clips().update(clip.copy(status = ClipStatus.DOWNLOADING, error = ""))
        runDownload(clipId, clip.sourceUrl)
    }

    private fun startDownload(clipId: Long, url: String) = scope.launch { runDownload(clipId, url) }

    private suspend fun runDownload(clipId: Long, url: String) {
        try {
            val r = downloader.download(url, clipId)
            val clip = db.clips().get(clipId) ?: return
            db.clips().update(
                clip.copy(
                    title = r.title, author = r.author, caption = r.caption, subtitles = r.subtitles,
                    durationSec = r.durationSec, filePath = r.filePath, thumbPath = r.thumbPath,
                    status = ClipStatus.READY, error = "",
                )
            )
            extractIdeas(clipId)
            if (speech.isReady) transcribe(clipId)
        } catch (t: Throwable) {
            val clip = db.clips().get(clipId) ?: return
            db.clips().update(clip.copy(status = ClipStatus.FAILED, error = t.message ?: t.toString()))
        }
    }

    /** Imports a video/audio file shared from the gallery or a file manager. */
    suspend fun importFile(uri: Uri): Long = withContext(Dispatchers.IO) {
        val name = displayName(uri) ?: "Imported audio"
        val id = db.clips().insert(
            Clip(sourceUrl = "file:${System.currentTimeMillis()}:$uri", title = name.substringBeforeLast('.'))
        )
        try {
            val ext = name.substringAfterLast('.', "mp4").lowercase()
            val target = File(downloader.audioDir, "clip_$id.$ext")
            context.contentResolver.openInputStream(uri)!!.use { input ->
                target.outputStream().use { input.copyTo(it) }
            }
            val clip = db.clips().get(id)!!
            db.clips().update(clip.copy(filePath = target.absolutePath, status = ClipStatus.READY))
            if (speech.isReady) transcribe(id)
        } catch (t: Throwable) {
            db.clips().get(id)?.let { db.clips().update(it.copy(status = ClipStatus.FAILED, error = "Couldn't read that file.")) }
        }
        id
    }

    suspend fun update(clip: Clip) = db.clips().update(clip)

    suspend fun delete(clip: Clip) = withContext(Dispatchers.IO) {
        downloader.cancel(clip.id)
        listOf(clip.filePath, clip.thumbPath).filter { it.isNotBlank() }.forEach { File(it).delete() }
        db.clips().clearGroups(clip.id)
        db.clips().delete(clip.id)
    }

    suspend fun createGroup(name: String): Long = db.groups().insert(Group(name = name.trim()))
    suspend fun renameGroup(id: Long, name: String) = db.groups().rename(id, name.trim())
    suspend fun deleteGroup(id: Long) {
        db.groups().clearLinks(id)
        db.groups().clearLinkMembers(id)
        db.groups().delete(id)
    }

    suspend fun setInGroup(clipId: Long, groupId: Long, member: Boolean) =
        if (member) db.clips().addToGroup(ClipGroup(clipId, groupId))
        else db.clips().removeFromGroup(clipId, groupId)

    private fun displayName(uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }

    companion object {
        private val URL_REGEX = Regex("""https?://[^\s"'<>]+""")

        fun extractUrls(text: String): List<String> =
            URL_REGEX.findAll(text).map { normalize(it.value) }.distinct().toList()

        /** Drops tracking parameters so the same reel or page shared twice is saved once. */
        fun normalize(url: String): String {
            val trimmed = url.trimEnd('.', ',', ')', '!')
            if ("instagram.com" in trimmed) return trimmed.substringBefore('?').trimEnd('/') + "/"
            val query = trimmed.substringAfter('?', "")
            if (query.isEmpty()) return trimmed
            val kept = query.split('&').filterNot {
                it.startsWith("utm_") || it.startsWith("igsh=") || it.startsWith("si=") || it.startsWith("fbclid=")
            }
            return trimmed.substringBefore('?') + if (kept.isEmpty()) "" else "?" + kept.joinToString("&")
        }

        /** Reels and videos are saved as audio; every other link is saved as a website. */
        fun isMediaUrl(url: String): Boolean {
            val host = IdeaExtractor.host(url) ?: return false
            val path = url.substringAfter("://").substringAfter('/', "").lowercase()
            fun on(domain: String) = host == domain || host.endsWith(".$domain")
            return when {
                on("instagram.com") -> listOf("reel", "p/", "tv/").any { path.startsWith(it) }
                on("facebook.com") -> listOf("reel", "watch", "videos", "share/v", "share/r").any { it in path }
                on("youtube.com") || on("youtu.be") || on("tiktok.com") || on("fb.watch") ||
                    on("vimeo.com") || on("soundcloud.com") || on("dailymotion.com") -> true
                else -> false
            }
        }
    }
}
