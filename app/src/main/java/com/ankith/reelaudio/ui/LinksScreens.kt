@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.ankith.reelaudio.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ankith.reelaudio.data.Clip
import com.ankith.reelaudio.data.GroupWithCount
import com.ankith.reelaudio.data.LinkStatus
import com.ankith.reelaudio.data.SavedLink

@Composable
internal fun LinksList(links: List<SavedLink>, vm: AppController, filtered: Boolean, onOpen: (SavedLink) -> Unit) {
    if (links.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Default.Link, null, Modifier.size(56.dp), tint = Accent)
            Spacer(Modifier.height(16.dp))
            Text(if (filtered) "No links here yet." else "Save your first link", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Share any website, tool or skill page to Reel Audio, or tap +. Websites mentioned in your saved reels show up here too.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp)) {
        items(links, key = { it.id }) { link -> LinkRow(link, vm.clipTitle(link.sourceClipId)) { onOpen(link) } }
    }
}

@Composable
private fun LinkRow(link: SavedLink, fromClip: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Thumb(link.imagePath, 52, Icons.Default.Link)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                link.title.ifBlank { if (link.status == LinkStatus.FETCHING) "Reading page…" else link.url },
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Text(
                link.siteName.ifBlank { link.url.substringAfter("://").substringBefore('/') },
                style = MaterialTheme.typography.labelMedium, color = Accent,
            )
            val sub = when (link.status) {
                LinkStatus.FAILED -> link.error
                else -> link.description
            }
            if (sub.isNotBlank()) Text(
                sub, maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = if (link.status == LinkStatus.FAILED) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (fromClip != null) Text(
                "From reel: ${fromClip.ifBlank { "untitled" }}",
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (link.status == LinkStatus.FETCHING) {
            Spacer(Modifier.width(8.dp))
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}

/** Everything saved about a link, as plain text to copy or share. */
internal fun SavedLink.asText(): String = buildString {
    appendLine(title.ifBlank { url })
    appendLine(url)
    if (description.isNotBlank()) appendLine().appendLine(description)
    if (content.isNotBlank()) appendLine().appendLine(content)
}.trim()

/** Everything saved about a reel, as plain text to copy or share. */
internal fun Clip.asText(links: List<SavedLink>): String = buildString {
    appendLine(title.ifBlank { "Untitled reel" })
    if (author.isNotBlank()) appendLine("@$author")
    if (sourceUrl.startsWith("http")) appendLine(sourceUrl)
    if (transcript.isNotBlank()) appendLine().appendLine("What they say:").appendLine(transcript)
    if (caption.isNotBlank()) appendLine().appendLine("Caption:").appendLine(caption)
    if (links.isNotEmpty()) {
        appendLine().appendLine("Links mentioned:")
        links.forEach { appendLine("- ${it.title.ifBlank { it.url }}: ${it.url}") }
    }
}.trim()

internal fun copyText(context: Context, clipboard: androidx.compose.ui.platform.ClipboardManager, text: String) {
    clipboard.setText(AnnotatedString(text))
    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
}

internal fun shareText(context: Context, title: String, text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, title)
        .putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(send, title))
}

@Composable
internal fun LinkSheet(link: SavedLink, groups: List<GroupWithCount>, vm: AppController, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val memberOf by vm.linkGroupIds(link.id).collectAsState(initial = emptyList())
    var newGroup by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    AppSheet(onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Thumb(link.imagePath, 56, Icons.Default.Link)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(link.title.ifBlank { link.url }, style = MaterialTheme.typography.titleMedium, maxLines = 3)
                    Text(link.siteName, color = Accent, style = MaterialTheme.typography.labelLarge)
                }
            }
            vm.clipTitle(link.sourceClipId)?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Mentioned in your reel \"${it.ifBlank { "untitled" }}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("Groups", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                groups.forEach { g ->
                    val member = g.id in memberOf
                    FilterChip(selected = member, onClick = { vm.setLinkInGroup(link, g.id, !member) }, label = { Text(g.name) })
                }
                FilterChip(
                    selected = false, onClick = { newGroup = true }, label = { Text("New group") },
                    leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(16.dp)) },
                )
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            SheetAction(Icons.Default.OpenInNew, "Open website") {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.url)))
            }
            SheetAction(Icons.Default.ContentCopy, "Copy all as text") { copyText(context, clipboard, link.asText()) }
            SheetAction(Icons.Default.Share, "Share as text") { shareText(context, link.title.ifBlank { link.url }, link.asText()) }
            if (link.status != LinkStatus.FETCHING) SheetAction(Icons.Default.Refresh, "Read the page again") { vm.refreshLink(link) }
            SheetAction(Icons.Default.Delete, "Delete") { confirmDelete = true }
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            if (link.status == LinkStatus.FAILED) {
                Text(link.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }
            if (link.description.isNotBlank()) {
                Text("About", style = MaterialTheme.typography.labelLarge)
                SelectionContainer { Text(link.description, style = MaterialTheme.typography.bodyMedium) }
                Spacer(Modifier.height(12.dp))
            }
            if (link.content.isNotBlank()) {
                Text("From the page", style = MaterialTheme.typography.labelLarge)
                SelectionContainer {
                    Text(
                        link.content,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = if (expanded) Int.MAX_VALUE else 12,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (link.content.lines().size > 12 || link.content.length > 900) {
                    TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Show less" else "Show more") }
                }
            }
            Text(link.url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))
        }
    }

    if (newGroup) TextPromptDialog("New group", "e.g. AI tools", "", onDismiss = { newGroup = false }) {
        vm.createGroup(it); newGroup = false
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete this link?") },
        confirmButton = { TextButton(onClick = { vm.deleteLink(link); confirmDelete = false; onDismiss() }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
    )
}

/** Websites a reel mentioned, shown inside the reel's menu. */
@Composable
internal fun MentionedLinks(clip: Clip, vm: AppController) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val links by vm.linksForClip(clip.id).collectAsState(initial = emptyList())
    if (links.isNotEmpty()) {
        Text("Links mentioned", style = MaterialTheme.typography.labelLarge)
        links.forEach { l ->
            Row(
                Modifier.fillMaxWidth().clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(l.url))) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Link, null, tint = Accent)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(l.title.ifBlank { l.url }, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                    if (l.description.isNotBlank()) Text(
                        l.description, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
    TextButton(onClick = { copyText(context, clipboard, clip.asText(links)) }) {
        Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Copy everything about this reel as text")
    }
}
