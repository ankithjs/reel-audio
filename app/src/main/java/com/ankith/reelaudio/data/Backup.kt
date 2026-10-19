package com.ankith.reelaudio.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Packs the whole library (audio, covers, captions, groups) into one .zip the user can save
 * anywhere, such as Google Drive, and restores it on another phone. The Instagram sign-in is
 * never included.
 */
class Backup(private val context: Context, private val library: Library) {
    private val db get() = library.db

    /** Returns how many clips were written. */
    suspend fun export(target: Uri): Int = withContext(Dispatchers.IO) {
        val clips = db.clips().allOnce().filter { it.status == ClipStatus.READY && File(it.filePath).exists() }
        val groups = db.groups().allOnce().associateBy { it.id }
        val links = db.clips().allLinks().groupBy({ it.clipId }, { it.groupId })

        val json = JSONObject().apply {
            put("version", 1)
            put("groups", JSONArray(groups.values.map { it.name }))
            put("clips", JSONArray(clips.map { c ->
                JSONObject().apply {
                    put("sourceUrl", c.sourceUrl)
                    put("title", c.title)
                    put("author", c.author)
                    put("caption", c.caption)
                    put("subtitles", c.subtitles)
                    put("transcript", c.transcript)
                    put("transcriptLang", c.transcriptLang)
                    put("durationSec", c.durationSec)
                    put("createdAt", c.createdAt)
                    put("media", "media/" + File(c.filePath).name)
                    if (c.thumbPath.isNotBlank() && File(c.thumbPath).exists()) put("thumb", "thumbs/" + File(c.thumbPath).name)
                    put("groups", JSONArray(links[c.id].orEmpty().mapNotNull { groups[it]?.name }))
                }
            }))
        }

        context.contentResolver.openOutputStream(target, "w")!!.use { raw ->
            ZipOutputStream(raw.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry("library.json"))
                zip.write(json.toString().toByteArray())
                zip.closeEntry()
                for (c in clips) {
                    zip.addFile("media/", File(c.filePath))
                    if (c.thumbPath.isNotBlank() && File(c.thumbPath).exists()) zip.addFile("thumbs/", File(c.thumbPath))
                }
            }
        }
        clips.size
    }

    /** Adds everything from a backup to this phone's library. Returns how many clips were new. */
    suspend fun import(source: Uri): Int = withContext(Dispatchers.IO) {
        val temp = File(context.cacheDir, "restore").apply { deleteRecursively(); mkdirs() }
        try {
            context.contentResolver.openInputStream(source)!!.use { raw ->
                ZipInputStream(raw.buffered()).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        val out = File(temp, entry.name).canonicalFile
                        // Ignore entries that try to escape the temp folder.
                        if (entry.isDirectory || !out.path.startsWith(temp.canonicalPath + File.separator)) continue
                        out.parentFile?.mkdirs()
                        out.outputStream().use { zip.copyTo(it) }
                    }
                }
            }
            val manifest = File(temp, "library.json")
            require(manifest.exists()) { "That file isn't a Reel Audio backup." }
            val json = JSONObject(manifest.readText())

            val groupIds = mutableMapOf<String, Long>()
            suspend fun groupId(name: String): Long = groupIds.getOrPut(name) {
                db.groups().byName(name)?.id ?: db.groups().insert(Group(name = name))
            }
            json.optJSONArray("groups")?.let { arr -> for (i in 0 until arr.length()) groupId(arr.getString(i)) }

            var added = 0
            val clips = json.getJSONArray("clips")
            for (i in 0 until clips.length()) {
                val c = clips.getJSONObject(i)
                val url = c.getString("sourceUrl")
                val existing = db.clips().byUrl(url)
                val id = if (existing != null && existing.status == ClipStatus.READY) existing.id else {
                    existing?.let { library.delete(it) }
                    val media = File(temp, c.getString("media"))
                    if (!media.exists()) continue
                    val newId = db.clips().insert(
                        Clip(
                            sourceUrl = url,
                            title = c.optString("title"),
                            author = c.optString("author"),
                            caption = c.optString("caption"),
                            subtitles = c.optString("subtitles"),
                            transcript = c.optString("transcript"),
                            transcriptLang = c.optString("transcriptLang"),
                            transcriptStatus = if (c.optString("transcript").isNotBlank()) TranscriptStatus.DONE
                            else TranscriptStatus.NONE,
                            durationSec = c.optInt("durationSec"),
                            createdAt = c.optLong("createdAt", System.currentTimeMillis()),
                            status = ClipStatus.READY,
                        )
                    )
                    val dir = library.downloader.audioDir
                    val audio = File(dir, "clip_$newId.${media.extension}")
                    media.copyTo(audio, overwrite = true)
                    val thumb = c.optString("thumb").takeIf { it.isNotBlank() }?.let { File(temp, it) }
                        ?.takeIf { it.exists() }
                        ?.let { t -> File(dir, "clip_$newId.${t.extension}").also { t.copyTo(it, overwrite = true) } }
                    db.clips().get(newId)?.let {
                        db.clips().update(it.copy(filePath = audio.absolutePath, thumbPath = thumb?.absolutePath.orEmpty()))
                    }
                    added++
                    newId
                }
                c.optJSONArray("groups")?.let { arr ->
                    for (j in 0 until arr.length()) db.clips().addToGroup(ClipGroup(id, groupId(arr.getString(j))))
                }
            }
            added
        } finally {
            temp.deleteRecursively()
        }
    }

    private fun ZipOutputStream.addFile(folder: String, file: File) {
        putNextEntry(ZipEntry(folder + file.name))
        file.inputStream().use { it.copyTo(this) }
        closeEntry()
    }
}
