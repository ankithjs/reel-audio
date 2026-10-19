package com.ankith.reelaudio.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ankith.reelaudio.data.Clip
import com.ankith.reelaudio.data.GroupWithCount
import com.ankith.reelaudio.data.Library
import com.ankith.reelaudio.library
import com.ankith.reelaudio.playback.PlayerConnection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val library = app.library
    val player = PlayerConnection(app)

    val selectedGroup = MutableStateFlow<Long?>(null)
    val query = MutableStateFlow("")
    val message = MutableStateFlow<String?>(null)

    val groups: StateFlow<List<GroupWithCount>> =
        library.db.groups().allWithCounts().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val clips: StateFlow<List<Clip>> = combine(
        selectedGroup.flatMapLatest { g -> if (g == null) library.db.clips().all() else library.db.clips().inGroup(g) },
        query,
    ) { list, q ->
        if (q.isBlank()) list
        else list.filter { c -> listOf(c.title, c.author, c.caption).any { it.contains(q, ignoreCase = true) } }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            runCatching { player.connect() }
            while (true) {
                player.refresh()
                delay(500)
            }
        }
    }

    fun groupIdsFor(clipId: Long): Flow<List<Long>> = library.db.clips().groupIdsFor(clipId)

    fun addLinks(text: String, groupId: Long?) = viewModelScope.launch {
        if (Library.extractUrls(text).isEmpty()) {
            message.value = "No link found. Copy the reel link from Instagram and paste it here."
            return@launch
        }
        val added = library.addFromText(text, groupId)
        message.value = when (added) {
            0 -> "Already in your library."
            1 -> "Saving audio…"
            else -> "Saving $added reels…"
        }
    }

    fun importFile(uri: Uri) = viewModelScope.launch {
        library.importFile(uri)
        message.value = "Imported."
    }

    fun retry(clip: Clip) = library.retry(clip.id)
    fun delete(clip: Clip) = viewModelScope.launch { library.delete(clip) }
    fun rename(clip: Clip, title: String) = viewModelScope.launch { library.update(clip.copy(title = title)) }
    fun setInGroup(clip: Clip, groupId: Long, member: Boolean) =
        viewModelScope.launch { library.setInGroup(clip.id, groupId, member) }

    fun createGroup(name: String, thenAddClip: Clip? = null) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        val id = library.createGroup(name)
        if (id > 0 && thenAddClip != null) library.setInGroup(thenAddClip.id, id, true)
    }

    fun renameGroup(id: Long, name: String) = viewModelScope.launch { library.renameGroup(id, name) }
    fun deleteGroup(id: Long) = viewModelScope.launch {
        if (selectedGroup.value == id) selectedGroup.value = null
        library.deleteGroup(id)
    }

    fun play(clip: Clip, shuffle: Boolean = false) =
        player.playQueue(clips.value.filter { it.filePath.isNotBlank() }, clip.id, shuffle)

    fun shuffleAll() {
        val list = clips.value.filter { it.filePath.isNotBlank() }
        if (list.isNotEmpty()) player.playQueue(list, list.random().id, shuffle = true)
    }

    fun updateExtractor() = viewModelScope.launch {
        message.value = "Updating extractor…"
        message.value = runCatching { "Extractor updated: " + library.downloader.updateExtractor() }
            .getOrElse { "Update failed: ${it.message}" }
    }

    fun storageUsedMb(): Double =
        (library.downloader.audioDir.listFiles()?.sumOf { it.length() } ?: 0L) / 1_048_576.0

    fun isSignedIn(): Boolean = library.downloader.cookiesFile.exists()
    fun signOut() {
        library.downloader.cookiesFile.delete()
        android.webkit.CookieManager.getInstance().removeAllCookies(null)
        message.value = "Signed out of Instagram."
    }

    override fun onCleared() {
        player.release()
    }
}
