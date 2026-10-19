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
