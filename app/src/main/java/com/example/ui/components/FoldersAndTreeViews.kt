package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioTrackItem
import com.example.model.PlaylistItem
import com.example.model.VideoItem
import com.example.model.formatSizeLabel
import com.example.model.resolutionLabel
import com.example.ui.theme.LocalM3Palette
import com.example.ui.theme.PlusJakartaSansFontFamily

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MediaPermissionPromptCard(
    title: String,
    explanation: String,
    showOpenSettingsButton: Boolean,
    onGrantPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalM3Palette.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(28.dp)
            .testTag("permission_prompt_card"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(MaterialShapes.Cookie9Sided.toShape())
                .background(palette.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LockOpen,
                contentDescription = null,
                tint = palette.primary,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            color = palette.onSurface,
            fontFamily = PlusJakartaSansFontFamily,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = explanation,
            color = palette.onSurfaceVariant,
            fontFamily = PlusJakartaSansFontFamily,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(22.dp))

        Button(
            onClick = onGrantPermission,
            shapes = ButtonDefaults.shapes(),
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.primary,
                contentColor = palette.onPrimary
            ),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            modifier = Modifier.testTag("grant_permission_btn")
        ) {
            Text(
                text = "Grant permission",
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (showOpenSettingsButton) {
            Spacer(modifier = Modifier.height(12.dp))
            FilledTonalButton(
                onClick = onOpenSettings,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = palette.surfaceContainerHigh,
                    contentColor = palette.primary
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 11.dp),
                modifier = Modifier.testTag("open_settings_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Open settings",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FoldersLibraryView(
    videos: List<VideoItem>,
    playlists: List<PlaylistItem>,
    amoledBlack: Boolean,
    onSelectFolder: (String) -> Unit,
    onSelectPlaylist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalM3Palette.current
    val existingIds = videos.map { it.id }.toSet()
    val foldersMap = videos.groupBy { it.folder }
    val folderEntries = foldersMap.entries.toList()
    val cardBg = if (amoledBlack) palette.surfaceContainerLowest else palette.surfaceContainerLow

    if (folderEntries.isEmpty() && playlists.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp)
                .testTag("folders_empty_state"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(MaterialShapes.SoftBurst.toShape())
                    .background(palette.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "No video folders found",
                color = palette.onSurface,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Videos stored on your device will automatically appear here grouped by folder.",
                color = palette.onSurfaceVariant,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("folders_library_view"),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (playlists.isNotEmpty()) {
            item {
                Text(
                    text = "Playlists",
                    color = palette.onSurfaceVariant,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                ) {
                    items(playlists, key = { it.id }) { pl ->
                        val count = pl.mediaUris.count { it in existingIds }
                        ElevatedCard(
                            onClick = { onSelectPlaylist(pl.id) },
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = palette.surfaceContainerHigh
                            ),
                            modifier = Modifier.testTag("playlist_chip_${pl.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(MaterialShapes.Sunny.toShape())
                                        .background(palette.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlaylistPlay,
                                        contentDescription = pl.name,
                                        tint = palette.onSecondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = pl.name,
                                        color = palette.onSurface,
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "$count video${if (count == 1) "" else "s"}",
                                        color = palette.onSurfaceVariant,
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Device Storage Folders",
                    color = palette.onSurfaceVariant,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                )
            }
        }

        itemsIndexed(folderEntries, key = { _, entry -> entry.key }) { idx, (folderName, folderVideos) ->
            val totalMB = folderVideos.sumOf { it.sizeMB }
            val samplePath = folderVideos.firstOrNull()?.path?.substringBeforeLast("/") ?: folderName

            SegmentedListItem(
                onClick = { onSelectFolder(folderName) },
                shapes = ListItemDefaults.segmentedShapes(
                    index = idx,
                    count = folderEntries.size.coerceAtLeast(1)
                ),
                colors = ListItemDefaults.segmentedColors(
                    containerColor = cardBg,
                    contentColor = palette.onSurface,
                    supportingContentColor = palette.onSurfaceVariant
                ),
                leadingContent = {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(MaterialShapes.Cookie9Sided.toShape())
                            .background(palette.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = folderName,
                            tint = palette.onSecondaryContainer,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                },
                supportingContent = {
                    Text(
                        text = "$samplePath · ${formatSizeLabel(totalMB)}",
                        color = palette.onSurfaceVariant,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Open $folderName",
                        tint = palette.onSurfaceVariant
                    )
                },
                content = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = folderName,
                            color = palette.onSurface,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            shape = CircleShape,
                            color = palette.surfaceContainerHighest
                        ) {
                            Text(
                                text = "${folderVideos.size}",
                                color = palette.primary,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("folder_row_$folderName")
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TreeHierarchyView(
    videos: List<VideoItem>,
    expandedNodes: Set<String>,
    showResolutionTier: Boolean,
    onToggleNode: (String) -> Unit,
    onPlayVideo: (VideoItem) -> Unit,
    onOpenMenu: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalM3Palette.current

    if (videos.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountTree,
                contentDescription = null,
                tint = palette.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No video files on device",
                color = palette.onSurface,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
        return
    }

    val dirMap = videos.groupBy { it.path.substringBeforeLast("/", it.folder) }
    val allDirs = dirMap.keys.sorted()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("tree_hierarchy_view"),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(allDirs, key = { it }) { dirPath ->
            val isExpanded = dirPath in expandedNodes
            val dirVideos = dirMap[dirPath].orEmpty()
            val depth = dirPath.count { it == '/' }.coerceAtMost(3)

            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = palette.surfaceContainerLow),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = (depth * 4).dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleNode(dirPath) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = palette.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = dirPath,
                        color = palette.onSurface,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${dirVideos.size}",
                        color = palette.onSurfaceVariant,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 8.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        dirVideos.forEachIndexed { idx, video ->
                            val badge = resolutionLabel(video.width, video.height)
                            SegmentedListItem(
                                onClick = { onPlayVideo(video) },
                                shapes = ListItemDefaults.segmentedShapes(
                                    index = idx,
                                    count = dirVideos.size.coerceAtLeast(1)
                                ),
                                colors = ListItemDefaults.segmentedColors(
                                    containerColor = palette.surfaceContainer,
                                    contentColor = palette.onSurface,
                                    supportingContentColor = palette.onSurfaceVariant
                                ),
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Default.VideoFile,
                                        contentDescription = null,
                                        tint = palette.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                supportingContent = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (showResolutionTier && badge != null) {
                                            Text(
                                                text = badge,
                                                color = palette.primary,
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = "${video.durationLabel} · ${formatSizeLabel(video.sizeMB)}",
                                            color = palette.onSurfaceVariant,
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 11.5.sp
                                        )
                                    }
                                },
                                trailingContent = {
                                    IconButton(
                                        onClick = { onOpenMenu(video) },
                                        shapes = IconButtonDefaults.shapes(),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Actions",
                                            tint = palette.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                content = {
                                    Text(
                                        text = video.displayName,
                                        color = palette.onSurface,
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
