package com.ankith.reelaudio

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.media3.common.Player
import com.ankith.reelaudio.data.Clip
import com.ankith.reelaudio.data.ClipStatus
import com.ankith.reelaudio.data.GroupWithCount
import com.ankith.reelaudio.playback.PlayerControls
import com.ankith.reelaudio.playback.PlayerUi
import com.ankith.reelaudio.ui.AppController
import com.ankith.reelaudio.ui.AppRoot
import com.ankith.reelaudio.ui.ClipSheet
import com.ankith.reelaudio.ui.LocalInlineSheets
import com.ankith.reelaudio.ui.NowPlayingSheet
import com.ankith.reelaudio.ui.SettingsSheet
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders the app's real screens with sample data. Run `./gradlew recordRoborazziDebug`. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7, application = Application::class)
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Before fun setUp() {
        // Spinners and timers never settle; draw a single frame instead of waiting for idle.
        compose.mainClock.autoAdvance = false
    }

    private fun shot(name: String, content: @Composable () -> Unit) {
        compose.setContent(content)
        compose.mainClock.advanceTimeBy(500)
        compose.onRoot().captureRoboImage("build/screenshots/$name.png")
    }

    @Composable
    private fun Sheet(content: @Composable () -> Unit) {
        MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF9E7BFF))) {
            CompositionLocalProvider(LocalInlineSheets provides true) {
                Box(Modifier.fillMaxSize().background(Color(0xFF0B0B0F)), contentAlignment = Alignment.BottomCenter) {
                    content()
                }
            }
        }
    }

    @Test fun library() = shot("1_library") { AppRoot(FakeApp(), null) {} }

    @Test fun libraryGroup() = shot("2_group_filter") {
        AppRoot(FakeApp(selected = 1, clips = SAMPLE.filter { it.id in setOf(1L, 4L) }), null) {}
    }

    @Test fun empty() = shot("6_first_launch") {
        AppRoot(FakeApp(clips = emptyList(), groups = emptyList(), playing = PlayerUi()), null) {}
    }

    @Test fun nowPlaying() = shot("3_now_playing") {
        Sheet { NowPlayingSheet(PLAYING, SAMPLE[0], FakeApp()) {} }
    }

    @Test fun clipMenu() = shot("4_clip_menu") {
        Sheet { ClipSheet(SAMPLE[1], GROUPS, FakeApp()) {} }
    }

    @Test fun settings() = shot("5_settings") {
        Sheet { SettingsSheet(FakeApp()) {} }
    }
}

private val GROUPS = listOf(
    GroupWithCount(1, "Philosophy", 12),
    GroupWithCount(2, "Poetry", 8),
    GroupWithCount(3, "Motivation", 15),
    GroupWithCount(4, "Stoicism", 5),
)

private val SAMPLE = listOf(
    Clip(
        id = 1, sourceUrl = "https://www.instagram.com/reel/a/", status = ClipStatus.READY,
        title = "The obstacle is the way", author = "daily.stoic.notes", durationSec = 72, filePath = "x",
        caption = "The impediment to action advances action. What stands in the way becomes the way.\n\n— Marcus Aurelius, Meditations 5.20\n\n#stoicism #philosophy #marcusaurelius",
        subtitles = "WEBVTT\n\n00:00:30.000 --> 00:00:40.000\nwhat stands in the way becomes the way\n",
    ),
    Clip(
        id = 2, sourceUrl = "https://www.instagram.com/reel/b/", status = ClipStatus.READY,
        title = "Do not go gentle into that good night", author = "spoken.verse", durationSec = 95, filePath = "x",
        caption = "Dylan Thomas, read aloud. Save this for the nights you need it. #poetry #spokenword",
    ),
    Clip(
        id = 3, sourceUrl = "https://www.instagram.com/reel/c/", status = ClipStatus.DOWNLOADING,
    ),
    Clip(
        id = 4, sourceUrl = "https://www.instagram.com/reel/d/", status = ClipStatus.READY,
        title = "Why we fear silence", author = "think.slowly", durationSec = 58, filePath = "x",
    ),
    Clip(
        id = 5, sourceUrl = "https://www.instagram.com/reel/e/", status = ClipStatus.READY,
        title = "Discipline is choosing what you want most", author = "morning.fuel", durationSec = 41, filePath = "x",
    ),
    Clip(
        id = 6, sourceUrl = "https://www.instagram.com/reel/f/", status = ClipStatus.FAILED,
        error = "Instagram wants you signed in. Open Settings > Sign in to Instagram, then retry.",
    ),
    Clip(
        id = 7, sourceUrl = "https://www.instagram.com/reel/g/", status = ClipStatus.READY,
        title = "Rumi: what you seek is seeking you", author = "spoken.verse", durationSec = 64, filePath = "x",
    ),
)

private val PLAYING = PlayerUi(
    clipId = 1, title = SAMPLE[0].title, author = SAMPLE[0].author, isPlaying = true,
    positionMs = 34_000, durationMs = 72_000, shuffle = true, repeatMode = Player.REPEAT_MODE_ALL, speed = 1.25f,
)

private object NoopPlayer : PlayerControls {
    override fun togglePlay() {}
    override fun next() {}
    override fun previous() {}
    override fun seekTo(ms: Long) {}
    override fun seekBy(ms: Long) {}
    override fun toggleShuffle() {}
    override fun cycleRepeat() {}
    override fun cycleSpeed() {}
}

private class FakeApp(
    clips: List<Clip> = SAMPLE,
    groups: List<GroupWithCount> = GROUPS,
    selected: Long? = null,
    playing: PlayerUi = PLAYING,
) : AppController {
    override val clips = MutableStateFlow(clips)
    override val groups = MutableStateFlow(groups)
    override val selectedGroup = MutableStateFlow(selected)
    override val query = MutableStateFlow("")
    override val message = MutableStateFlow<String?>(null)
    override val playerState = MutableStateFlow(playing)
    override val player: PlayerControls = NoopPlayer
    override fun groupIdsFor(clipId: Long): Flow<List<Long>> = flowOf(listOf(2L))
    override fun addLinks(text: String, groupId: Long?) {}
    override fun retry(clip: Clip) {}
    override fun delete(clip: Clip) {}
    override fun rename(clip: Clip, title: String) {}
    override fun setInGroup(clip: Clip, groupId: Long, member: Boolean) {}
    override fun createGroup(name: String, thenAddClip: Clip?) {}
    override fun renameGroup(id: Long, name: String) {}
    override fun deleteGroup(id: Long) {}
    override fun play(clip: Clip, shuffle: Boolean) {}
    override fun shuffleAll() {}
    override fun updateExtractor() {}
    override fun storageUsedMb() = 46.3
    override fun isSignedIn() = true
    override fun signOut() {}
}
