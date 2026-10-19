package com.ankith.reelaudio.ui

import com.ankith.reelaudio.data.Clip
import com.ankith.reelaudio.data.GroupWithCount
import com.ankith.reelaudio.playback.PlayerControls
import com.ankith.reelaudio.playback.PlayerUi
import com.ankith.reelaudio.transcribe.ModelState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** What the screens need from the app. [MainViewModel] is the real one; tests use a fake for screenshots. */
interface AppController {
    val clips: StateFlow<List<Clip>>
    val groups: StateFlow<List<GroupWithCount>>
    val selectedGroup: MutableStateFlow<Long?>
    val query: MutableStateFlow<String>
    val message: MutableStateFlow<String?>
    val playerState: StateFlow<PlayerUi>
    val player: PlayerControls

    fun groupIdsFor(clipId: Long): Flow<List<Long>>
    fun addLinks(text: String, groupId: Long?)
    fun retry(clip: Clip)
    fun delete(clip: Clip)
    fun rename(clip: Clip, title: String)
    fun setInGroup(clip: Clip, groupId: Long, member: Boolean)
    fun createGroup(name: String, thenAddClip: Clip? = null)
    fun renameGroup(id: Long, name: String)
    fun deleteGroup(id: Long)
    fun play(clip: Clip, shuffle: Boolean = false)
    fun shuffleAll()
    fun updateExtractor()
    fun storageUsedMb(): Double
    fun isSignedIn(): Boolean
    fun signOut()
    val speechModel: StateFlow<ModelState>
    fun downloadSpeechModel()
    fun deleteSpeechModel()
    fun transcribe(clip: Clip)
    fun transcribeAll()
    fun exportBackup(target: android.net.Uri)
    fun importBackup(source: android.net.Uri)
}
