package com.ankith.reelaudio.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.ankith.reelaudio.download.ReelDownloader
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Saves every link found in [text]. Returns how many new links were queued. */
    suspend fun addFromText(text: String, groupId: Long? = null): Int {
        val urls = extractUrls(text)
        var added = 0
        for (url in urls) {
            val existing = db.clips().byUrl(url)
            val id = if (existing != null) {
                if (existing.status == ClipStatus.FAILED) retry(existing.id)
                existing.id
            } else {
                added++
                db.clips().insert(Clip(sourceUrl = url)).also { startDownload(it, url) }
            }
            if (groupId != null && id > 0) db.clips().addToGroup(ClipGroup(id, groupId))
        }
        return added
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

        /** Drops tracking parameters so the same reel shared twice is saved once. */
        fun normalize(url: String): String {
            val trimmed = url.trimEnd('.', ',', ')', '!')
            return if ("instagram.com" in trimmed) trimmed.substringBefore('?').trimEnd('/') + "/" else trimmed
        }
    }
}
