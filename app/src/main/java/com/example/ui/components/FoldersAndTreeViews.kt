@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.VideoItem
import com.example.model.formatSizeLabel
import com.example.model.resolutionLabel

// ---------------------------------------------------------------------------------------------
// Shared pieces
// ---------------------------------------------------------------------------------------------

/** Empty or "permission needed" message with an Expressive shape behind the icon. */
@Composable
internal fun LibraryMessage(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(MaterialShapes.Cookie9Sided.toShape())
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        actions()
    }
}

@Composable
fun MediaPermissionPromptCard(
    title: String,
    explanation: String,
    showOpenSettingsButton: Boolean,
    onGrantPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    LibraryMessage(
        icon = Icons.Default.LockOpen,
        title = title,
        body = explanation,
        modifier = modifier.testTag("permission_prompt_card")
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = onGrantPermission,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.testTag("grant_permission_btn")
            ) { Text("Grant permission") }
            if (showOpenSettingsButton) {
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = onOpenSettings,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.testTag("open_settings_btn")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Open settings")
                }
            }
        }
    }
}

/** "/storage/emulated/0/Movies/Netflix" -> "Movies/Netflix" (or "SD card/…"). */
internal fun friendlyFolderPath(path: String): String {
    val trimmed = path.trimEnd('/')
    return when {
        trimmed.startsWith(INTERNAL_ROOT) -> trimmed.removePrefix(INTERNAL_ROOT).trimStart('/').ifBlank { "Internal storage" }
        trimmed.startsWith("/storage/") -> {
            val rest = trimmed.removePrefix("/storage/").substringAfter('/', "")
            if (rest.isBlank()) "SD card" else "SD card/$rest"
        }
        else -> trimmed
    }
}

private const val INTERNAL_ROOT = "/storage/emulated/0"

// ---------------------------------------------------------------------------------------------
// Folders
// ---------------------------------------------------------------------------------------------

private data class FolderEntry(
    val path: String,
    val name: String,
    val videos: List<VideoItem>,
    val sizeMB: Double
)

/** One row per folder. Folders are told apart by their full path, so same-named folders don't merge. */
@Composable
fun FoldersLibraryView(
    videos: List<VideoItem>,
    amoledBlack: Boolean,
    onSelectFolder: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val folders = remember(videos) {
        videos.groupBy { it.folderPath }
            .map { (path, list) ->
                FolderEntry(
                    path = path,
                    name = path.substringAfterLast('/').ifBlank { list.first().folder },
                    videos = list,
                    sizeMB = list.sumOf { it.sizeMB }
                )
            }
            .sortedWith(compareBy<FolderEntry> { it.name.lowercase() }.thenBy { it.path })
    }

    if (folders.isEmpty()) {
        LibraryMessage(
            icon = Icons.Default.Folder,
            title = "No video folders",
            body = "Folders with videos in them will show up here.",
            modifier = modifier.testTag("folders_empty_state")
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("folders_library_view"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
    ) {
        itemsIndexed(folders, key = { _, f -> f.path }) { idx, folder ->
            SegmentedListItem(
                onClick = { onSelectFolder(folder.path) },
                shapes = ListItemDefaults.segmentedShapes(index = idx, count = folders.size),
                colors = videoRowColors(amoledBlack),
                leadingContent = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(MaterialShapes.Cookie9Sided.toShape())
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                },
                supportingContent = {
                    val count = if (folder.videos.size == 1) "1 video" else "${folder.videos.size} videos"
                    Text(
                        text = "${friendlyFolderPath(folder.path)} · $count · ${formatSizeLabel(folder.sizeMB)}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                trailingContent = {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                },
                modifier = Modifier.testTag("folder_row_${folder.path}")
            ) {
                Text(folder.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Tree
// ---------------------------------------------------------------------------------------------

/** A folder in the tree. Chains of empty single-child folders are merged ("Android/media/…"). */
private class TreeNode(
    val path: String,
    var label: String,
    val children: MutableList<TreeNode> = mutableListOf(),
    val videos: MutableList<VideoItem> = mutableListOf()
) {
    var total: Int = 0
}

private sealed interface TreeRow {
    val key: String
    data class Folder(val node: TreeNode, val depth: Int, val expanded: Boolean) : TreeRow {
        override val key: String get() = "d:${node.path}"
    }
    data class Volume(val label: String, val icon: ImageVector) : TreeRow {
        override val key: String get() = "v:$label"
    }
    data class Video(val video: VideoItem, val depth: Int) : TreeRow {
        override val key: String get() = "f:${video.id}"
    }
}

/** Builds the real folder tree from the videos' paths, one tree per storage volume. */
private fun buildTree(videos: List<VideoItem>): List<Pair<String, TreeNode>> {
    fun volumeOf(path: String): String = when {
        path.startsWith(INTERNAL_ROOT) -> INTERNAL_ROOT
        path.startsWith("/storage/") -> "/storage/" + path.removePrefix("/storage/").substringBefore('/')
        else -> ""
    }
    val volumes = linkedMapOf<String, TreeNode>()
    val nodes = HashMap<String, TreeNode>()
    for (video in videos) {
        val dir = video.folderPath
        val volume = volumeOf(dir)
        val root = volumes.getOrPut(volume) { TreeNode(volume, volume) }
        // Walk from the volume down to the video's folder, creating folders as needed.
        val relative = dir.removePrefix(volume).trim('/')
        var parent = root
        var current = volume
        if (relative.isNotEmpty()) {
            for (part in relative.split('/')) {
                current = if (current.isEmpty()) part else "$current/$part"
                val node = nodes.getOrPut(current) {
                    TreeNode(current, part).also { parent.children += it }
                }
                parent = node
            }
        }
        parent.videos += video
    }
    fun finish(node: TreeNode): Int {
        node.children.sortBy { it.label.lowercase() }
        node.videos.sortBy { it.title.lowercase() }
        // Merge "a" -> "b" -> "c" when a and b hold no videos and only one folder.
        while (node.children.size == 1 && node.videos.isEmpty()) {
            val only = node.children.single()
            node.label = "${node.label}/${only.label}"
            node.children.clear()
            node.children += only.children
            node.videos += only.videos
        }
        node.total = node.videos.size + node.children.sumOf { finish(it) }
        return node.total
    }
    volumes.values.forEach { root ->
        root.children.forEach { finish(it) }
        root.videos.sortBy { it.title.lowercase() }
        root.total = root.videos.size + root.children.sumOf { it.total }
    }
    return volumes.map { (path, node) -> path to node }
}

private fun flatten(roots: List<Pair<String, TreeNode>>, expanded: Set<String>): List<TreeRow> {
    val rows = ArrayList<TreeRow>()
    fun add(node: TreeNode, depth: Int) {
        val open = node.path in expanded
        rows += TreeRow.Folder(node, depth, open)
        if (open) {
            node.children.forEach { add(it, depth + 1) }
            node.videos.forEach { rows += TreeRow.Video(it, depth + 1) }
        }
    }
    val showVolumes = roots.size > 1
    for ((path, root) in roots) {
        if (showVolumes) {
            rows += TreeRow.Volume(
                label = if (path == INTERNAL_ROOT) "Internal storage" else if (path.isBlank()) "Other" else "SD card",
                icon = if (path == INTERNAL_ROOT) Icons.Default.PhoneAndroid else Icons.Default.SdCard
            )
        }
        root.children.forEach { add(it, 0) }
        root.videos.forEach { rows += TreeRow.Video(it, 0) }
    }
    return rows
}

/**
 * A real folder tree: tap a folder to open or close it; subfolders and videos sit indented
 * underneath. Each folder shows how many videos it holds, including subfolders.
 */
@Composable
fun TreeHierarchyView(
    videos: List<VideoItem>,
    expandedNodes: Set<String>,
    showResolutionTier: Boolean,
    onToggleNode: (String) -> Unit,
    onPlayVideo: (VideoItem) -> Unit,
    onOpenMenu: (VideoItem) -> Unit,
    menu: @Composable (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (videos.isEmpty()) {
        LibraryMessage(
            icon = Icons.Default.AccountTree,
            title = "No videos",
            body = "Folders with videos in them will show up here as a tree.",
            modifier = modifier
        )
        return
    }
    val roots = remember(videos) { buildTree(videos) }
    val rows = remember(roots, expandedNodes) { flatten(roots, expandedNodes) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("tree_hierarchy_view"),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 128.dp)
    ) {
        items(rows, key = { it.key }) { row ->
            when (row) {
                is TreeRow.Volume -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 6.dp)
                ) {
                    Icon(row.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(row.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                is TreeRow.Folder -> TreeFolderRow(row, onToggle = { onToggleNode(row.node.path) })
                is TreeRow.Video -> TreeVideoRow(
                    video = row.video,
                    depth = row.depth,
                    showResolutionTier = showResolutionTier,
                    onClick = { onPlayVideo(row.video) },
                    onMenu = { onOpenMenu(row.video) },
                    menu = { menu(row.video) }
                )
            }
        }
    }
}

@Composable
private fun TreeFolderRow(row: TreeRow.Folder, onToggle: () -> Unit) {
    val rotation by animateFloatAsState(
        targetValue = if (row.expanded) 90f else 0f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "treeChevron"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onToggle)
            .padding(start = (8 + row.depth * 20).dp, end = 12.dp)
            .testTag("tree_folder_${row.node.path}")
    ) {
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = if (row.expanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp).rotate(rotation)
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            if (row.expanded) Icons.Default.FolderOpen else Icons.Default.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            row.node.label,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Text(
                "${row.node.total}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun TreeVideoRow(
    video: VideoItem,
    depth: Int,
    showResolutionTier: Boolean,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    menu: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(start = (8 + depth * 20 + 26).dp, top = 4.dp, bottom = 4.dp)
    ) {
        VideoThumbnailBox(
            video = video,
            modifier = Modifier
                .width(76.dp)
                .aspectRatio(16f / 9f)
                .clip(MaterialTheme.shapes.small)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(video.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val badge = if (showResolutionTier) resolutionLabel(video.width, video.height) else null
            Text(
                listOfNotNull(badge, formatSizeLabel(video.sizeMB)).joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box {
            IconButton(onClick = onMenu, shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.Default.MoreVert, contentDescription = "Video actions", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            menu()
        }
    }
}
