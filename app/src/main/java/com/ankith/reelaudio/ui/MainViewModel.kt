package com.ankith.reelaudio.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ankith.reelaudio.data.Backup
import com.ankith.reelaudio.data.Clip
import com.ankith.reelaudio.data.GroupWithCount
import com.ankith.reelaudio.data.Library
import com.ankith.reelaudio.data.SavedLink
import com.ankith.reelaudio.library
import com.ankith.reelaudio.playback.PlayerConnection
import com.ankith.reelaudio.playback.PlayerUi
import com.ankith.reelaudio.transcribe.ModelState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app), AppController {
    private val library = app.library
    private val backup = Backup(app, library)
    override val player = PlayerConnection(app)
    override val playerState: StateFlow<PlayerUi> get() = player.state

    override val selectedGroup = MutableStateFlow<Long?>(null)
    override val query = MutableStateFlow("")
    override val message = MutableStateFlow<String?>(null)

    override val groups: StateFlow<List<GroupWithCount>> =
        library.db.groups().allWithCounts().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    override val clips: StateFlow<List<Clip>> = combine(
        selectedGroup.flatMapLatest { g -> if (g == null) library.db.clips().all() else library.db.clips().inGroup(g) },
        query,
    ) { list, q ->
        if (q.isBlank()) list
        else list.filter { c -> listOf(c.title, c.author, c.caption).any { it.contains(q, ignoreCase = true) } }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** True while the app is on screen; the progress bar only ticks then, to save battery. */
    val visible = MutableStateFlow(false)

    override val links: StateFlow<List<SavedLink>> = combine(
        selectedGroup.flatMapLatest { g -> if (g == null) library.db.links().all() else library.db.links().inGroup(g) },
        query,
    ) { list, q ->
        if (q.isBlank()) list
        else list.filter { l -> listOf(l.title, l.siteName, l.description, l.url).any { it.contains(q, ignoreCase = true) } }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Every clip's title, so a link can say which reel it came from. */
    private val clipTitles: StateFlow<Map<Long, String>> = library.db.clips().all()
        .map { list -> list.associate { it.id to it.title } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    override fun clipTitle(clipId: Long?): String? = clipId?.let { clipTitles.value[it] }

    override fun linksForClip(clipId: Long): Flow<List<SavedLink>> = library.db.links().forClip(clipId)
    override fun linkGroupIds(linkId: Long): Flow<List<Long>> = library.db.links().groupIdsFor(linkId)

    override fun setLinkInGroup(link: SavedLink, groupId: Long, member: Boolean) {
        viewModelScope.launch { library.setLinkInGroup(link.id, groupId, member) }
    }

    override fun refreshLink(link: SavedLink) {
        library.refreshLink(link.id)
    }

    override fun deleteLink(link: SavedLink) {
        viewModelScope.launch { library.deleteLink(link) }
    }

    init {
        viewModelScope.launch {
            runCatching { player.connect() }
            visible.collectLatest { onScreen ->
                while (onScreen) {
                    player.refresh()
                    delay(500)
                }
            }
        }
    }

    override fun groupIdsFor(clipId: Long): Flow<List<Long>> = library.db.clips().groupIdsFor(clipId)

    override fun addLinks(text: String, groupId: Long?) {
        viewModelScope.launch {
            if (Library.extractUrls(text).isEmpty()) {
                message.value = "No link found. Paste a reel link, a website like excalidraw.com, or a tool's name."
                return@launch
            }
            val r = library.addFromText(text, groupId)
            val parts = listOfNotNull(
                r.reels.takeIf { it > 0 }?.let { if (it == 1) "1 reel" else "$it reels" },
                r.links.takeIf { it > 0 }?.let { if (it == 1) "1 link" else "$it links" },
            )
            message.value = if (parts.isEmpty()) "Already in your library." else "Saving ${parts.joinToString(" and ")}…"
        }
    }

    fun importFile(uri: Uri) {
        viewModelScope.launch {
            library.importFile(uri)
            message.value = "Imported."
        }
    }

    override fun retry(clip: Clip) {
        library.retry(clip.id)
    }

    override fun delete(clip: Clip) {
        viewModelScope.launch { library.delete(clip) }
    }

    override fun rename(clip: Clip, title: String) {
        viewModelScope.launch { library.update(clip.copy(title = title)) }
    }

    override fun setInGroup(clip: Clip, groupId: Long, member: Boolean) {
        viewModelScope.launch { library.setInGroup(clip.id, groupId, member) }
    }

    override fun createGroup(name: String, thenAddClip: Clip?) {
        viewModelScope.launch {
            if (name.isBlank()) return@launch
            val id = library.createGroup(name)
            if (id > 0 && thenAddClip != null) library.setInGroup(thenAddClip.id, id, true)
        }
    }

    override fun renameGroup(id: Long, name: String) {
        viewModelScope.launch { library.renameGroup(id, name) }
    }

    override fun deleteGroup(id: Long) {
        viewModelScope.launch {
            if (selectedGroup.value == id) selectedGroup.value = null
            library.deleteGroup(id)
        }
    }

    override fun play(clip: Clip, shuffle: Boolean) =
        player.playQueue(clips.value.filter { it.filePath.isNotBlank() }, clip.id, shuffle)

    override fun shuffleAll() {
        val list = clips.value.filter { it.filePath.isNotBlank() }
        if (list.isNotEmpty()) player.playQueue(list, list.random().id, shuffle = true)
    }

    override fun updateExtractor() {
        viewModelScope.launch {
            message.value = "Updating extractor…"
            message.value = runCatching { "Extractor updated: " + library.downloader.updateExtractor() }
                .getOrElse { "Update failed: ${it.message}" }
        }
    }

    override fun storageUsedMb(): Double =
        (library.downloader.audioDir.listFiles()?.sumOf { it.length() } ?: 0L) / 1_048_576.0

    override fun isSignedIn(): Boolean = library.downloader.cookiesFile.exists()

    override fun signOut() {
        library.downloader.cookiesFile.delete()
        android.webkit.CookieManager.getInstance().removeAllCookies(null)
        message.value = "Signed out of Instagram."
    }

    override val speechModel: StateFlow<ModelState> get() = library.speech.state

    override fun downloadSpeechModel() {
        viewModelScope.launch {
            library.speech.download()
            if (library.speech.isReady) {
                val n = library.transcribeMissing()
                message.value = if (n > 0) "Speech model ready. Writing out $n clips…" else "Speech model ready."
            }
        }
    }

    override fun deleteSpeechModel() {
        viewModelScope.launch { library.speech.delete() }
    }

    override fun transcribe(clip: Clip) {
        if (!library.speech.isReady) {
            message.value = "Turn on speech to text in Settings first (one-time download)."
            return
        }
        library.transcribe(clip.id)
    }

    override fun transcribeAll() {
        viewModelScope.launch {
            val n = library.transcribeMissing()
            message.value = if (n == 0) "Every clip already has its speech written out." else "Writing out $n clips…"
        }
    }

    override fun exportBackup(target: Uri) {
        viewModelScope.launch {
            message.value = "Backing up…"
            message.value = runCatching { backup.export(target) }
                .fold({ "Backed up $it clips." }, { "Backup failed: ${it.message}" })
        }
    }

    override fun importBackup(source: Uri) {
        viewModelScope.launch {
            message.value = "Restoring…"
            message.value = runCatching { backup.import(source) }
                .fold({ if (it == 0) "Everything in that backup is already here." else "Restored $it clips and links." },
                    { "Restore failed: ${it.message}" })
        }
    }

    override fun onCleared() {
        player.release()
    }
}
