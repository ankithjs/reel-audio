@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.ankith.reelaudio.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.ankith.reelaudio.data.Clip
import com.ankith.reelaudio.data.ClipStatus
import com.ankith.reelaudio.data.GroupWithCount
import com.ankith.reelaudio.playback.PlayerUi
import java.io.File

private val Accent = Color(0xFF9E7BFF)

/** Screenshot tests flip this to draw sheets inline, since popups aren't captured. */
val LocalInlineSheets = staticCompositionLocalOf { false }

@Composable
private fun AppSheet(
    onDismiss: () -> Unit,
    state: SheetState? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (LocalInlineSheets.current) {
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), tonalElevation = 1.dp) {
            Column(Modifier.padding(top = 24.dp), content = content)
        }
    } else if (state != null) {
        ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, content = content)
    } else {
        ModalBottomSheet(onDismissRequest = onDismiss, content = content)
    }
}

@Composable
fun AppRoot(vm: AppController, pendingShare: String?, onShareHandled: () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Accent, secondary = Accent)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            LibraryScreen(vm, pendingShare, onShareHandled)
        }
    }
}

@Composable
internal fun LibraryScreen(vm: AppController, pendingShare: String?, onShareHandled: () -> Unit) {
    val clips by vm.clips.collectAsState()
    val groups by vm.groups.collectAsState()
    val selected by vm.selectedGroup.collectAsState()
    val query by vm.query.collectAsState()
    val player by vm.playerState.collectAsState()
    val message by vm.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var showAdd by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showNowPlaying by remember { mutableStateOf(false) }
    var showNewGroup by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var editGroup by remember { mutableStateOf<GroupWithCount?>(null) }
    var sheetClip by remember { mutableStateOf<Clip?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.message.value = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        OutlinedTextField(
                            value = query, onValueChange = { vm.query.value = it },
                            placeholder = { Text("Search title, author, caption") },
                            singleLine = true, modifier = Modifier.fillMaxWidth(),
                        )
                    } else Text("Reel Audio", fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(onClick = {
                        if (showSearch) vm.query.value = ""
                        showSearch = !showSearch
                    }) { Icon(if (showSearch) Icons.Default.Close else Icons.Default.Search, "Search") }
                    IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Settings, "Settings") }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, "Add reel link") }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (player.clipId != null) MiniPlayer(player, vm, onOpen = { showNowPlaying = true })
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            GroupChips(
                groups = groups, selected = selected,
                onSelect = { vm.selectedGroup.value = it },
                onLongPress = { editGroup = it },
                onNew = { showNewGroup = true },
            )
            if (clips.isEmpty()) {
                EmptyState(filtered = selected != null || query.isNotBlank())
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${clips.size} clips", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { vm.shuffleAll() }) {
                        Icon(Icons.Default.Shuffle, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Shuffle")
                    }
                }
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(clips, key = { it.id }) { clip ->
                        ClipRow(
                            clip = clip,
                            playing = player.clipId == clip.id,
                            onClick = {
                                when (clip.status) {
                                    ClipStatus.READY -> vm.play(clip)
                                    ClipStatus.FAILED -> vm.retry(clip)
                                    ClipStatus.DOWNLOADING -> Unit
                                }
                            },
                            onMore = { sheetClip = clip },
                        )
                    }
                }
            }
        }
    }

    if (showAdd || pendingShare != null) {
        AddLinkDialog(
            initial = pendingShare.orEmpty(),
            groups = groups,
            preselected = selected,
            onDismiss = { showAdd = false; onShareHandled() },
            onSave = { text, groupId ->
                vm.addLinks(text, groupId)
                showAdd = false
                onShareHandled()
            },
            onNewGroup = { vm.createGroup(it) },
        )
    }
    if (showNewGroup) {
        TextPromptDialog("New group", "e.g. Philosophy", "", onDismiss = { showNewGroup = false }) {
            vm.createGroup(it); showNewGroup = false
        }
    }
    editGroup?.let { g ->
        GroupEditDialog(
            group = g,
            onDismiss = { editGroup = null },
            onRename = { vm.renameGroup(g.id, it); editGroup = null },
            onDelete = { vm.deleteGroup(g.id); editGroup = null },
        )
    }
    sheetClip?.let { c ->
        // Re-read from the live list so edits show immediately.
        val live = clips.firstOrNull { it.id == c.id } ?: c
        ClipSheet(live, groups, vm, onDismiss = { sheetClip = null })
    }
    if (showNowPlaying && player.clipId != null) {
        val clip = clips.firstOrNull { it.id == player.clipId }
        NowPlayingSheet(player, clip, vm, onDismiss = { showNowPlaying = false })
    }
    if (showSettings) {
        SettingsSheet(vm, onDismiss = { showSettings = false })
    }
}

@Composable
internal fun GroupChips(
    groups: List<GroupWithCount>,
    selected: Long?,
    onSelect: (Long?) -> Unit,
    onLongPress: (GroupWithCount) -> Unit,
    onNew: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("All") })
        }
        items(groups, key = { it.id }) { g ->
            // Material chips swallow long-presses, so groups use a hand-made pill: tap filters, hold edits.
            val isSel = selected == g.id
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSel) Accent.copy(alpha = 0.25f) else Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Accent else Color.Gray),
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp).height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .combinedClickable(onClick = { onSelect(g.id) }, onLongClick = { onLongPress(g) }),
            ) {
                Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                    Text("${g.name} · ${g.count}", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        item {
            FilterChip(
                selected = false, onClick = onNew,
                label = { Text("New group") },
                leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(16.dp)) },
            )
        }
    }
}

@Composable
internal fun EmptyState(filtered: Boolean) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.MusicNote, null, Modifier.size(56.dp), tint = Accent)
        Spacer(Modifier.height(16.dp))
        Text(
            if (filtered) "Nothing here yet." else "Save your first reel",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (filtered) "Use the ⋮ menu on any clip to add it to this group. Hold a group to rename or delete it."
            else "In Instagram, tap Share on a reel and pick Reel Audio. Or copy the link and tap +.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Thumb(path: String, size: Int) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF2A2440)),
        contentAlignment = Alignment.Center,
    ) {
        if (path.isNotBlank() && File(path).exists()) {
            AsyncImage(
                model = File(path), contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
            )
        } else Icon(Icons.Default.MusicNote, null, tint = Accent)
    }
}

@Composable
internal fun ClipRow(clip: Clip, playing: Boolean, onClick: () -> Unit, onMore: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumb(clip.thumbPath, 52)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                clip.title.ifBlank { if (clip.status == ClipStatus.DOWNLOADING) "Saving…" else "Untitled reel" },
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                color = if (playing) Accent else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (playing) FontWeight.SemiBold else FontWeight.Normal,
            )
            val sub = when (clip.status) {
                ClipStatus.DOWNLOADING -> clip.sourceUrl
                ClipStatus.FAILED -> clip.error.ifBlank { "Failed. Tap to retry." }
                ClipStatus.READY -> listOfNotNull(
                    clip.author.takeIf { it.isNotBlank() }?.let { "@$it" },
                    clip.durationSec.takeIf { it > 0 }?.let { formatTime(it * 1000L) },
                ).joinToString(" · ")
            }
            if (sub.isNotBlank()) Text(
                sub, maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = if (clip.status == ClipStatus.FAILED) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when (clip.status) {
            ClipStatus.DOWNLOADING -> CircularProgressIndicator(Modifier.size(22.dp).padding(2.dp), strokeWidth = 2.dp)
            ClipStatus.FAILED -> Icon(Icons.Default.Refresh, "Retry", tint = MaterialTheme.colorScheme.error)
            ClipStatus.READY -> Unit
        }
        IconButton(onClick = onMore) { Icon(Icons.Default.MoreVert, "More") }
    }
}

@Composable
internal fun MiniPlayer(p: PlayerUi, vm: AppController, onOpen: () -> Unit) {
    Surface(tonalElevation = 6.dp) {
        Column {
            if (p.durationMs > 0) LinearProgressIndicator(
                progress = { (p.positionMs.toFloat() / p.durationMs).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(2.dp),
            )
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Thumb(p.artwork, 40)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (p.author.isNotBlank()) Text(
                        "@${p.author}", maxLines = 1, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { vm.player.togglePlay() }) {
                    Icon(if (p.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/pause")
                }
                IconButton(onClick = { vm.player.next() }) { Icon(Icons.Default.SkipNext, "Next") }
            }
        }
    }
}

@Composable
internal fun NowPlayingSheet(p: PlayerUi, clip: Clip?, vm: AppController, onDismiss: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cues = remember(clip?.subtitles) { parseVtt(clip?.subtitles.orEmpty()) }
    var dragging by remember { mutableStateOf(false) }
    var dragPos by remember { mutableFloatStateOf(0f) }

    AppSheet(onDismiss, state) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth(0.7f).aspectRatio(1f)) {
                Box(
                    Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(Color(0xFF2A2440)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (p.artwork.isNotBlank() && File(p.artwork).exists()) AsyncImage(
                        model = File(p.artwork), contentDescription = null,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                    ) else Icon(Icons.Default.MusicNote, null, Modifier.size(72.dp), tint = Accent)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(p.title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, maxLines = 3)
            if (p.author.isNotBlank()) Text("@${p.author}", color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(12.dp))
            Slider(
                value = if (dragging) dragPos else p.positionMs.toFloat(),
                onValueChange = { dragging = true; dragPos = it },
                onValueChangeFinished = { vm.player.seekTo(dragPos.toLong()); dragging = false },
                valueRange = 0f..(p.durationMs.coerceAtLeast(1).toFloat()),
            )
            Row(Modifier.fillMaxWidth()) {
                Text(formatTime(p.positionMs), style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.weight(1f))
                Text(formatTime(p.durationMs), style = MaterialTheme.typography.labelSmall)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { vm.player.previous() }) { Icon(Icons.Default.SkipPrevious, "Previous") }
                IconButton(onClick = { vm.player.seekBy(-10_000) }) { Icon(Icons.Default.Replay10, "Back 10s") }
                FilledIconButton(onClick = { vm.player.togglePlay() }, modifier = Modifier.size(64.dp)) {
                    Icon(if (p.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/pause", Modifier.size(36.dp))
                }
                IconButton(onClick = { vm.player.seekBy(10_000) }) { Icon(Icons.Default.Forward10, "Forward 10s") }
                IconButton(onClick = { vm.player.next() }) { Icon(Icons.Default.SkipNext, "Next") }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { vm.player.toggleShuffle() }) {
                    Icon(Icons.Default.Shuffle, "Shuffle", tint = if (p.shuffle) Accent else Color.Gray)
                }
                IconButton(onClick = { vm.player.cycleRepeat() }) {
                    Icon(
                        if (p.repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        "Repeat",
                        tint = if (p.repeatMode == Player.REPEAT_MODE_OFF) Color.Gray else Accent,
                    )
                }
                TextButton(onClick = { vm.player.cycleSpeed() }) { Text("${p.speed}x") }
            }

            if (cues.isNotEmpty()) {
                val current = cues.lastOrNull { p.positionMs >= it.startMs }
                Spacer(Modifier.height(12.dp))
                Text(
                    current?.text.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = Accent,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                )
            }
            if (!clip?.caption.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                CaptionBlock(clip!!.caption)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
internal fun ClipSheet(clip: Clip, groups: List<GroupWithCount>, vm: AppController, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val memberOf by vm.groupIdsFor(clip.id).collectAsState(initial = emptyList())
    var renaming by remember { mutableStateOf(false) }
    var newGroup by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    AppSheet(onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Thumb(clip.thumbPath, 56)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(clip.title.ifBlank { "Untitled reel" }, style = MaterialTheme.typography.titleMedium, maxLines = 3)
                    if (clip.author.isNotBlank()) Text("@${clip.author}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Groups", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                groups.forEach { g ->
                    val member = g.id in memberOf
                    FilterChip(
                        selected = member,
                        onClick = { vm.setInGroup(clip, g.id, !member) },
                        label = { Text(g.name) },
                    )
                }
                FilterChip(
                    selected = false, onClick = { newGroup = true }, label = { Text("New group") },
                    leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(16.dp)) },
                )
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            SheetAction(Icons.Default.Edit, "Rename") { renaming = true }
            if (clip.status == ClipStatus.FAILED) SheetAction(Icons.Default.Refresh, "Retry") { vm.retry(clip); onDismiss() }
            if (clip.sourceUrl.startsWith("http")) SheetAction(Icons.Default.OpenInNew, "Open reel") {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(clip.sourceUrl)))
            }
            SheetAction(Icons.Default.Delete, "Delete") { confirmDelete = true }
            if (clip.status == ClipStatus.FAILED && clip.error.isNotBlank()) {
                Row(Modifier.padding(vertical = 8.dp)) {
                    Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(clip.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (clip.caption.isNotBlank()) {
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                CaptionBlock(clip.caption)
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (renaming) TextPromptDialog("Rename", "Title", clip.title, onDismiss = { renaming = false }) {
        vm.rename(clip, it); renaming = false
    }
    if (newGroup) TextPromptDialog("New group", "e.g. Poetry", "", onDismiss = { newGroup = false }) {
        vm.createGroup(it, thenAddClip = clip); newGroup = false
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete this clip?") },
        text = { Text("The saved audio is removed from your phone.") },
        confirmButton = { TextButton(onClick = { vm.delete(clip); confirmDelete = false; onDismiss() }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
    )
}

/** The reel's caption: tap Copy for the whole text, or long-press to select part of it. */
@Composable
private fun CaptionBlock(caption: String) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Caption", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = {
                clipboard.setText(AnnotatedString(caption))
                Toast.makeText(context, "Caption copied", Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Copy")
            }
        }
        SelectionContainer {
            Text(caption, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SheetAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null)
        Spacer(Modifier.width(16.dp))
        Text(label)
    }
}

@Composable
internal fun AddLinkDialog(
    initial: String,
    groups: List<GroupWithCount>,
    preselected: Long?,
    onDismiss: () -> Unit,
    onSave: (String, Long?) -> Unit,
    onNewGroup: (String) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var text by remember(initial) { mutableStateOf(initial) }
    var group by remember { mutableStateOf(preselected) }
    var newGroup by remember { mutableStateOf(false) }

    BackHandler(onBack = onDismiss)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save reel audio") },
        text = {
            Column {
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    label = { Text("Reel link(s), one per line") },
                    minLines = 2, maxLines = 6, modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = {
                    clipboard.getText()?.text?.let { text = if (text.isBlank()) it else text + "\n" + it }
                }) { Text("Paste") }
                Text("Save to group", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = group == null, onClick = { group = null }, label = { Text("None") })
                    groups.forEach { g ->
                        FilterChip(selected = group == g.id, onClick = { group = g.id }, label = { Text(g.name) })
                    }
                    FilterChip(
                        selected = false, onClick = { newGroup = true }, label = { Text("New") },
                        leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(16.dp)) },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(text, group) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (newGroup) TextPromptDialog("New group", "e.g. Motivation", "", onDismiss = { newGroup = false }) {
        onNewGroup(it); newGroup = false
    }
}

@Composable
private fun TextPromptDialog(
    title: String,
    hint: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = value, onValueChange = { value = it }, placeholder = { Text(hint) }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = { if (value.isNotBlank()) onConfirm(value.trim()) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun GroupEditDialog(
    group: GroupWithCount,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var value by remember { mutableStateOf(group.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit group") },
        text = {
            Column {
                OutlinedTextField(value = value, onValueChange = { value = it }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Deleting a group keeps its clips in All.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = { if (value.isNotBlank()) onRename(value) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDelete) { Text("Delete group", color = MaterialTheme.colorScheme.error) } },
    )
}

@Composable
internal fun SettingsSheet(vm: AppController, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var signedIn by remember { mutableStateOf(vm.isSignedIn()) }
    val storage = remember { vm.storageUsedMb() }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let { vm.exportBackup(it) }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.importBackup(it) }
    }

    LaunchedEffect(Unit) {
        // Refresh after returning from the sign-in screen.
        while (true) {
            signedIn = vm.isSignedIn()
            kotlinx.coroutines.delay(1000)
        }
    }

    AppSheet(onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text("Settings", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            if (signedIn) {
                SettingRow("Signed in to Instagram", "Used only on this phone to fetch reels you share.") {}
                TextButton(onClick = { vm.signOut(); signedIn = false }) { Text("Sign out") }
            } else {
                SettingRow(
                    "Sign in to Instagram",
                    "Optional. Fixes reels that fail with a login or rate-limit error.",
                ) { context.startActivity(Intent(context, LoginActivity::class.java)) }
            }
            HorizontalDivider()
            SettingRow("Update extractor", "Run this if saving suddenly stops working.") { vm.updateExtractor() }
            HorizontalDivider()
            SettingRow(
                "Back up library",
                "Save all clips, captions and groups as one file. Pick Google Drive to keep it online.",
            ) { backupLauncher.launch("ReelAudio-backup-${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())}.zip") }
            HorizontalDivider()
            SettingRow("Restore from backup", "Bring a backup file into this phone. Clips you already have are kept.") {
                restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream", "application/x-zip-compressed"))
            }
            HorizontalDivider()
            SettingRow("Storage used", "%.1f MB for all saved clips".format(storage)) {}
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text(
                "For personal listening only. Saving audio from Instagram may go against Instagram's terms.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp)) {
        Text(title)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
