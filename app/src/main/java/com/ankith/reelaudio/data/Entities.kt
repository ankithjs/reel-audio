package com.ankith.reelaudio.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ClipStatus { DOWNLOADING, READY, FAILED }

enum class TranscriptStatus { NONE, QUEUED, WORKING, DONE, FAILED }

@Entity(tableName = "clips", indices = [Index(value = ["sourceUrl"], unique = true)])
data class Clip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceUrl: String,
    val title: String = "",
    val author: String = "",
    /** The reel's caption text, shown under the player. */
    val caption: String = "",
    /** Timed subtitles (WebVTT): from the source, or from on-device speech-to-text. */
    val subtitles: String = "",
    /** What is said in the clip, as plain English text, from on-device speech-to-text. */
    val transcript: String = "",
    /** Language the speaker used, as Whisper detected it (e.g. "hi"). */
    val transcriptLang: String = "",
    val transcriptStatus: TranscriptStatus = TranscriptStatus.NONE,
    val durationSec: Int = 0,
    val filePath: String = "",
    val thumbPath: String = "",
    val status: ClipStatus = ClipStatus.DOWNLOADING,
    val error: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "user_groups", indices = [Index(value = ["name"], unique = true)])
data class Group(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

@Entity(tableName = "clip_groups", primaryKeys = ["clipId", "groupId"], indices = [Index("groupId")])
data class ClipGroup(val clipId: Long, val groupId: Long)

data class GroupWithCount(val id: Long, val name: String, val count: Int)

enum class LinkStatus { FETCHING, READY, FAILED }

/** A website, tool or skill page the user saved, or one mentioned in a saved reel. */
@Entity(tableName = "links", indices = [Index(value = ["url"], unique = true), Index("sourceClipId")])
data class SavedLink(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String = "",
    val siteName: String = "",
    val description: String = "",
    /** The page's readable text (headings, paragraphs, lists). */
    val content: String = "",
    val imagePath: String = "",
    /** The reel this link was found in, if it came from one. */
    val sourceClipId: Long? = null,
    val status: LinkStatus = LinkStatus.FETCHING,
    val error: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "link_groups", primaryKeys = ["linkId", "groupId"], indices = [Index("groupId")])
data class LinkGroup(val linkId: Long, val groupId: Long)
