package com.ankith.reelaudio

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
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
import com.ankith.reelaudio.transcribe.ModelState
import com.ankith.reelaudio.data.TranscriptStatus
import com.ankith.reelaudio.ui.AppController
import com.ankith.reelaudio.ui.AppRoot
import com.ankith.reelaudio.ui.ReelTheme
import com.ankith.reelaudio.ui.ClipSheet
import com.ankith.reelaudio.ui.LinkSheet
import com.ankith.reelaudio.data.LinkStatus
import com.ankith.reelaudio.data.SavedLink
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
        ReelTheme {
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

    @Test fun links() = shot("7_links") { AppRoot(FakeApp(), null, initialTab = 1) {} }

    @Test fun linkDetails() = shot("8_link_details") {
        Sheet { LinkSheet(LINKS[0], GROUPS, FakeApp()) {} }
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
    GroupWithCount(5, "AI tools", 9),
)

private val SAMPLE = listOf(
    Clip(
        id = 1, sourceUrl = "https://www.instagram.com/reel/a/", status = ClipStatus.READY,
        title = "The obstacle is the way", author = "daily.stoic.notes", durationSec = 72, filePath = "x",
        caption = "The impediment to action advances action. What stands in the way becomes the way.\n\n— Marcus Aurelius, Meditations 5.20\n\n#stoicism #philosophy #marcusaurelius",
        transcriptLang = "hi", transcriptStatus = TranscriptStatus.DONE,
        transcript = "",
        subtitles = "WEBVTT\n\n" +
            "00:00:00.000 --> 00:00:09.000\nWhen something blocks your path, don't run from it.\n\n" +
            "00:00:09.000 --> 00:00:21.000\nThe obstacle itself becomes the way forward.\n\n" +
            "00:00:21.000 --> 00:00:33.000\nWhatever stands in front of you is your teacher.\n\n" +
            "00:00:33.000 --> 00:00:45.000\nSo stop complaining, and start walking through it.\n\n" +
            "00:00:45.000 --> 00:00:58.000\nThat is how a person truly grows.\n",
    ),
    Clip(
        id = 2, sourceUrl = "https://www.instagram.com/reel/b/", status = ClipStatus.READY,
        title = "Do not go gentle into that good night", author = "spoken.verse", durationSec = 95, filePath = "x",
        transcriptStatus = TranscriptStatus.WORKING,
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

private val LINKS = listOf(
    SavedLink(
        id = 1, url = "https://excalidraw.com/", title = "Excalidraw | Hand-drawn look & feel • Collaborative • Secure",
        siteName = "Excalidraw", status = LinkStatus.READY, sourceClipId = 4,
        description = "Excalidraw is a virtual collaborative whiteboard tool that lets you easily sketch diagrams that have a hand-drawn feel to them.",
        content = "Excalidraw\nFree, open source whiteboard\nSketch diagrams with a hand-drawn feel.\nEnd-to-end encrypted collaboration.\nExport to PNG and SVG.",
    ),
    SavedLink(
        id = 2, url = "https://www.perplexity.ai/", title = "Perplexity", siteName = "Perplexity AI", status = LinkStatus.READY,
        description = "Perplexity is a free AI-powered answer engine that provides accurate, trusted, and real-time answers to any question.",
    ),
    SavedLink(
        id = 5, url = "https://github.com/anthropics/skills/tree/main/skills/frontend-design", title = "frontend-design",
        siteName = "Claude skill", status = LinkStatus.READY, sourceClipId = 2,
        description = "Create distinctive, production-grade frontend interfaces with high design quality.",
    ),
    SavedLink(id = 3, url = "https://roadmap.sh/frontend", status = LinkStatus.FETCHING),
    SavedLink(
        id = 4, url = "https://www.coursera.org/learn/learning-how-to-learn", title = "Learning How to Learn",
        siteName = "Coursera", status = LinkStatus.READY,
        description = "Powerful mental tools to help you master tough subjects. Barbara Oakley and Terrence Sejnowski.",
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
    override val links = MutableStateFlow(LINKS)
    override fun linksForClip(clipId: Long): Flow<List<SavedLink>> = flowOf(LINKS.filter { it.sourceClipId == clipId })
    override fun linkGroupIds(linkId: Long): Flow<List<Long>> = flowOf(listOf(5L))
    override fun setLinkInGroup(link: SavedLink, groupId: Long, member: Boolean) {}
    override fun refreshLink(link: SavedLink) {}
    override fun deleteLink(link: SavedLink) {}
    override fun clipTitle(clipId: Long?): String? = clipId?.let { id -> SAMPLE.firstOrNull { it.id == id }?.title }
    override val speechModel = MutableStateFlow<ModelState>(ModelState.Ready)
    override fun downloadSpeechModel() {}
    override fun deleteSpeechModel() {}
    override fun transcribe(clip: Clip) {}
    override fun transcribeAll() {}
    override fun exportBackup(target: android.net.Uri) {}
    override fun importBackup(source: android.net.Uri) {}
}
