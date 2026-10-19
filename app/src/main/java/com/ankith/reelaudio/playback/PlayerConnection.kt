package com.ankith.reelaudio.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.ankith.reelaudio.data.Clip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.guava.await
import java.io.File

data class PlayerUi(
    val clipId: Long? = null,
    val title: String = "",
    val author: String = "",
    val artwork: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val speed: Float = 1f,
)

interface PlayerControls {
    fun togglePlay()
    fun next()
    fun previous()
    fun seekTo(ms: Long)
    fun seekBy(ms: Long)
    fun toggleShuffle()
    fun cycleRepeat()
    fun cycleSpeed()
}

/** Thin wrapper around a MediaController bound to [PlaybackService]. */
class PlayerConnection(private val context: Context) : PlayerControls {
    private var controller: MediaController? = null
    private val _state = MutableStateFlow(PlayerUi())
    val state: StateFlow<PlayerUi> = _state

    suspend fun connect() {
        if (controller != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val c = MediaController.Builder(context, token).buildAsync().await()
        controller = c
        c.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) = refresh()
        })
        refresh()
    }

    fun release() {
        controller?.release()
        controller = null
    }

    /** Called on a timer by the UI to move the progress bar. */
    fun refresh() {
        val c = controller ?: return
        val item = c.currentMediaItem
        _state.value = PlayerUi(
            clipId = item?.mediaId?.toLongOrNull(),
            title = item?.mediaMetadata?.title?.toString().orEmpty(),
            author = item?.mediaMetadata?.artist?.toString().orEmpty(),
            artwork = item?.mediaMetadata?.artworkUri?.path.orEmpty(),
            isPlaying = c.isPlaying,
            positionMs = c.currentPosition.coerceAtLeast(0),
            durationMs = c.duration.takeIf { it > 0 } ?: 0,
            shuffle = c.shuffleModeEnabled,
            repeatMode = c.repeatMode,
            speed = c.playbackParameters.speed,
        )
    }

    fun playQueue(clips: List<Clip>, startClipId: Long, shuffle: Boolean = false) {
        val c = controller ?: return
        val playable = clips.filter { it.filePath.isNotBlank() && File(it.filePath).exists() }
        if (playable.isEmpty()) return
        val start = playable.indexOfFirst { it.id == startClipId }.coerceAtLeast(0)
        c.setMediaItems(playable.map { it.toMediaItem() }, start, 0)
        c.shuffleModeEnabled = shuffle
        c.prepare()
        c.play()
    }

    override fun togglePlay() {
        controller?.run { if (isPlaying) pause() else play() }
    }
    override fun next() { controller?.seekToNextMediaItem() }
    override fun previous() { controller?.seekToPrevious() }
    override fun seekTo(ms: Long) { controller?.seekTo(ms) }
    override fun seekBy(ms: Long) { controller?.run { seekTo((currentPosition + ms).coerceAtLeast(0)) } }
    override fun toggleShuffle() { controller?.run { shuffleModeEnabled = !shuffleModeEnabled } }
    override fun cycleRepeat() {
        controller?.run {
            repeatMode = when (repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }
    override fun cycleSpeed() {
        controller?.run {
            val speeds = listOf(1f, 1.25f, 1.5f, 2f, 0.75f)
            val i = speeds.indexOfFirst { it == playbackParameters.speed }
            setPlaybackSpeed(speeds[(i + 1) % speeds.size])
        }
    }

    private fun Clip.toMediaItem(): MediaItem {
        val meta = MediaMetadata.Builder()
            .setTitle(title.ifBlank { "Untitled reel" })
            .setArtist(author)
            .setExtras(Bundle().apply { putLong("clipId", id) })
            .apply { if (thumbPath.isNotBlank()) setArtworkUri(Uri.fromFile(File(thumbPath))) }
            .build()
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(File(filePath).toUri())
            .setMediaMetadata(meta)
            .build()
    }
}
