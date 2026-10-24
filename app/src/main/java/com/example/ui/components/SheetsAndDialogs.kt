package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.FilledTonalToggleButton
import androidx.compose.material3.FilledTonalToggleButtonDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LayoutMode
import com.example.model.PlaylistItem
import com.example.model.ResumePromptTarget
import com.example.model.SortBy
import com.example.model.SortDir
import com.example.model.VideoItem
import com.example.model.ViewMode
import com.example.model.formatDurationMs
import com.example.model.formatSizeLabel
import com.example.model.resolutionLabel
import com.example.ui.theme.AllM3Palettes
import com.example.ui.theme.LocalM3Palette
import com.example.ui.theme.PlusJakartaSansFontFamily

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SortAndViewBottomSheet(
    smartGroups: Boolean,
    viewMode: ViewMode,
    layoutMode: LayoutMode,
    sortBy: SortBy,
    sortDir: SortDir,
    showResolutionTier: Boolean,
    onToggleSmartGroups: () -> Unit,
    onSelectViewMode: (ViewMode) -> Unit,
    onSelectLayoutMode: (LayoutMode) -> Unit,
    onToggleShowResolutionTier: () -> Unit,
    onSelectSort: (SortBy) -> Unit,
    onDismiss: () -> Unit,
    isAudioTab: Boolean = false
) {
    val palette = LocalM3Palette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surfaceContainerLow,
        contentColor = palette.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = palette.DragHandleColor) },
        modifier = Modifier.testTag("sort_and_view_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            if (!isAudioTab) {
                // Smart Groups SegmentedListItem with official Switch
                SegmentedListItem(
                    checked = smartGroups,
                    onCheckedChange = { onToggleSmartGroups() },
                    shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
                    colors = ListItemDefaults.segmentedColors(
                        containerColor = palette.surfaceContainer,
                        contentColor = palette.onSurface,
                        supportingContentColor = palette.onSurfaceVariant,
                        selectedContainerColor = palette.surfaceContainer,
                        selectedContentColor = palette.onSurface,
                        selectedSupportingContentColor = palette.onSurfaceVariant
                    ),
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(MaterialShapes.Sunny.toShape())
                                .background(if (smartGroups) palette.primary else palette.surfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (smartGroups) palette.onPrimary else palette.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    supportingContent = {
                        Text(
                            text = "Auto-bundle series & sort episodes",
                            color = palette.onSurfaceVariant,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.5.sp
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = smartGroups,
                            onCheckedChange = null,
                            thumbContent = if (smartGroups) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            } else null
                        )
                    },
                    content = {
                        Text(
                            text = "Smart Groups",
                            color = palette.onSurface,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("smart_groups_toggle_card")
                )

                Spacer(modifier = Modifier.height(22.dp))
            }

            // View Mode Connected Button Group
            Text(
                text = "View mode",
                color = palette.onSurfaceVariant,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                val viewItems = listOf(
                    if (isAudioTab) {
                        Triple(ViewMode.VIDEOS, "Music", Icons.Default.MusicNote)
                    } else {
                        Triple(ViewMode.VIDEOS, "Videos", Icons.Default.Movie)
                    },
                    Triple(ViewMode.FOLDERS, "Folders", Icons.Default.Folder),
                    Triple(ViewMode.TREE, "Tree", Icons.Default.AccountTree)
                )
                viewItems.forEachIndexed { idx, (mode, label, icon) ->
                    val active = viewMode == mode
                    val shapes = when (idx) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        viewItems.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    }
                    ToggleButton(
                        checked = active,
                        onCheckedChange = { onSelectViewMode(mode) },
                        shapes = shapes,
                        colors = ToggleButtonDefaults.colors(
                            containerColor = palette.surfaceContainerHigh,
                            contentColor = palette.onSurfaceVariant,
                            checkedContainerColor = palette.secondaryContainer,
                            checkedContentColor = palette.onSecondaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("view_mode_btn_${mode.id}")
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Layout Mode & Resolution Tier Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Layout",
                    color = palette.onSurfaceVariant,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                if (!isAudioTab) {
                    FilledTonalToggleButton(
                        checked = showResolutionTier,
                        onCheckedChange = { onToggleShowResolutionTier() },
                        shapes = FilledTonalToggleButtonDefaults.shapesFor(buttonHeight = 32.dp),
                        colors = FilledTonalToggleButtonDefaults.colors(
                            containerColor = palette.surfaceContainerHigh,
                            contentColor = palette.onSurfaceVariant,
                            checkedContainerColor = palette.secondaryContainer,
                            checkedContentColor = palette.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("toggle_resolution_tier_btn")
                    ) {
                        Icon(
                            imageVector = if (showResolutionTier) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Resolution badges: ${if (showResolutionTier) "Shown" else "Hidden"}",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                val layoutItems = listOf(
                    Triple(LayoutMode.LIST, "List", Icons.Default.ViewList),
                    Triple(LayoutMode.GRID, "Grid", Icons.Default.GridView)
                )
                layoutItems.forEachIndexed { idx, (mode, label, icon) ->
                    val active = layoutMode == mode
                    val shapes = when (idx) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        else -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    }
                    ToggleButton(
                        checked = active,
                        onCheckedChange = { onSelectLayoutMode(mode) },
                        shapes = shapes,
                        colors = ToggleButtonDefaults.colors(
                            containerColor = palette.surfaceContainerHigh,
                            contentColor = palette.onSurfaceVariant,
                            checkedContainerColor = palette.secondaryContainer,
                            checkedContentColor = palette.onSecondaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("layout_mode_btn_${mode.id}")
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2x2 Sort Grid using Expressive ToggleButtons
            Text(
                text = "Sort",
                color = palette.onSurfaceVariant,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            val sortPairs = listOf(
                listOf(SortBy.NAME to Icons.Default.Title, SortBy.DATE to Icons.Default.CalendarToday),
                listOf(SortBy.DURATION to Icons.Default.Schedule, SortBy.SIZE to Icons.Default.Storage)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sortPairs.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { (option, icon) ->
                            val active = sortBy == option
                            val dirBadge = if (sortDir == SortDir.ASC) option.ascShort else option.descShort

                            ToggleButton(
                                checked = active,
                                onCheckedChange = { onSelectSort(option) },
                                shapes = ToggleButtonDefaults.shapesFor(buttonHeight = 48.dp),
                                colors = ToggleButtonDefaults.colors(
                                    containerColor = palette.surfaceContainerHigh,
                                    contentColor = palette.onSurfaceVariant,
                                    checkedContainerColor = palette.secondaryContainer,
                                    checkedContentColor = palette.onSecondaryContainer
                                ),
                                contentPadding = PaddingValues(
                                    start = if (active) 14.dp else 16.dp,
                                    end = if (active) 8.dp else 16.dp
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("sort_option_btn_${option.id}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = option.label,
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (active) {
                                        Surface(
                                            shape = CircleShape,
                                            color = palette.surfaceContainerLowest
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .height(28.dp)
                                                    .padding(horizontal = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Text(
                                                    text = dirBadge,
                                                    color = palette.primary,
                                                    fontFamily = PlusJakartaSansFontFamily,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Icon(
                                                    imageVector = if (sortDir == SortDir.ASC) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                                    contentDescription = dirBadge,
                                                    tint = palette.primary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemeAndDisplayBottomSheet(
    currentThemeId: String,
    amoledBlack: Boolean,
    showResolutionTier: Boolean,
    onSelectTheme: (String) -> Unit,
    onToggleAmoledBlack: () -> Unit,
    onToggleShowResolution: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalM3Palette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surfaceContainerLow,
        contentColor = palette.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = palette.DragHandleColor) },
        modifier = Modifier.testTag("theme_and_display_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Dynamic Color Seed",
                color = palette.onSurfaceVariant,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(AllM3Palettes, key = { it.id }) { p ->
                    val active = currentThemeId == p.id
                    Card(
                        onClick = { onSelectTheme(p.id) },
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = if (active) p.secondaryContainer else palette.surfaceContainer
                        ),
                        modifier = Modifier
                            .width(72.dp)
                            .border(
                                width = 2.dp,
                                color = if (active) p.primary else Color.Transparent,
                                shape = MaterialTheme.shapes.large
                            )
                            .testTag("theme_seed_${p.id}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(MaterialShapes.Sunny.toShape())
                                    .background(p.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                if (active) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Active theme",
                                        tint = p.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = p.name.substringBefore(" "),
                                color = palette.onSurface,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                SegmentedListItem(
                    checked = amoledBlack,
                    onCheckedChange = { onToggleAmoledBlack() },
                    shapes = ListItemDefaults.segmentedShapes(index = 0, count = 2),
                    colors = ListItemDefaults.segmentedColors(
                        containerColor = palette.surfaceContainer,
                        contentColor = palette.onSurface,
                        supportingContentColor = palette.onSurfaceVariant,
                        selectedContainerColor = palette.surfaceContainer,
                        selectedContentColor = palette.onSurface,
                        selectedSupportingContentColor = palette.onSurfaceVariant
                    ),
                    supportingContent = {
                        Text(
                            text = "Remove gray card containers",
                            color = palette.onSurfaceVariant,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = amoledBlack,
                            onCheckedChange = null
                        )
                    },
                    content = {
                        Text(
                            text = "AMOLED Pure Black",
                            color = palette.onSurface,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("amoled_black_toggle_row")
                )

                SegmentedListItem(
                    checked = showResolutionTier,
                    onCheckedChange = { onToggleShowResolution() },
                    shapes = ListItemDefaults.segmentedShapes(index = 1, count = 2),
                    colors = ListItemDefaults.segmentedColors(
                        containerColor = palette.surfaceContainer,
                        contentColor = palette.onSurface,
                        supportingContentColor = palette.onSurfaceVariant,
                        selectedContainerColor = palette.surfaceContainer,
                        selectedContentColor = palette.onSurface,
                        selectedSupportingContentColor = palette.onSurfaceVariant
                    ),
                    supportingContent = {
                        Text(
                            text = "Show 8K, 4K, 1440p, 1080p, 720p, SD badge",
                            color = palette.onSurfaceVariant,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = showResolutionTier,
                            onCheckedChange = null
                        )
                    },
                    content = {
                        Text(
                            text = "Show resolution badges",
                            color = palette.onSurface,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("show_resolution_toggle_row")
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ColorOsFloatingMenuOverlay(
    video: VideoItem,
    onAddToPlaylist: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit,
    onDetails: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalM3Palette.current
    val menuItems: List<Triple<String, ImageVector, () -> Unit>> = listOf(
        Triple("Add to playlist", Icons.Default.CreateNewFolder, onAddToPlaylist),
        Triple("Rename", Icons.Default.Edit, onRename),
        Triple("Share", Icons.Default.Share, onShare),
        Triple("Delete", Icons.Default.DeleteOutline, onDelete),
        Triple("Select", Icons.Default.Check, onSelect),
        Triple("Details", Icons.Default.Info, onDetails)
    )

    DropdownMenuPopup(
        expanded = true,
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("floating_video_menu_overlay")
    ) {
        DropdownMenuGroup(
            shapes = MenuDefaults.groupShape(index = 0, count = 1),
            containerColor = palette.surfaceContainerHigh
        ) {
            menuItems.forEachIndexed { index, (label, icon, action) ->
                val isError = label == "Delete"
                val textColor = if (isError) palette.error else palette.onSurface
                val iconColor = if (isError) palette.error else palette.onSurfaceVariant

                SelectableDropdownMenuItem(
                    selected = false,
                    onClick = action,
                    shapes = MenuDefaults.itemShape(index = index, count = menuItems.size),
                    text = {
                        Text(
                            text = label,
                            color = textColor,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    trailingContent = {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = iconColor,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("menu_action_${label.lowercase().replace(" ", "_")}")
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ColorOsDetailsSheet(
    video: VideoItem,
    onCopyPath: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalM3Palette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val badge = resolutionLabel(video.width, video.height)
    val detailRows = buildList {
        add(Triple("Name", video.displayName, false))
        add(Triple("Format", video.ext, false))
        add(Triple("Size", formatSizeLabel(video.sizeMB), false))
        if (badge != null) {
            add(Triple("Resolution", badge, false))
        }
        add(Triple("Length", video.durationLabel, false))
        add(Triple("Path", video.path, true))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surfaceContainerLow,
        contentColor = palette.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = palette.DragHandleColor) },
        modifier = Modifier.testTag("video_details_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .testTag("details_close_btn")
                ) {
                    Text(
                        text = "Close",
                        color = palette.primary,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "Details",
                    color = palette.onSurface,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                detailRows.forEachIndexed { idx, (label, value, copyable) ->
                    SegmentedListItem(
                        onClick = { if (copyable) onCopyPath(value) },
                        shapes = ListItemDefaults.segmentedShapes(
                            index = idx,
                            count = detailRows.size.coerceAtLeast(1)
                        ),
                        colors = ListItemDefaults.segmentedColors(
                            containerColor = palette.surfaceContainer,
                            contentColor = palette.onSurface,
                            overlineContentColor = palette.onSurfaceVariant
                        ),
                        overlineContent = {
                            Text(
                                text = label,
                                color = palette.onSurfaceVariant,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 12.5.sp
                            )
                        },
                        trailingContent = if (copyable) {
                            {
                                FilledTonalButton(
                                    onClick = { onCopyPath(value) },
                                    shapes = ButtonDefaults.shapes(),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("copy_path_btn")
                                ) {
                                    Text(
                                        text = "Copy",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        } else null,
                        content = {
                            Text(
                                text = value,
                                color = palette.onSurface,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 20.sp
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ColorOsRenameDialog(
    video: VideoItem,
    renameValue: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalM3Palette.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surfaceContainerHigh,
        titleContentColor = palette.onSurface,
        textContentColor = palette.onSurfaceVariant,
        title = {
            Text(
                text = "Rename video",
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            OutlinedTextField(
                value = renameValue,
                onValueChange = onValueChange,
                singleLine = true,
                label = {
                    Text(
                        text = "File name (.${video.ext})",
                        fontFamily = PlusJakartaSansFontFamily
                    )
                },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rename_input_field")
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.testTag("rename_save_btn")
            ) {
                Text(
                    text = "Save",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shapes = ButtonDefaults.shapes()
            ) {
                Text(
                    text = "Cancel",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        modifier = Modifier.testTag("rename_video_dialog")
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AddToPlaylistDialog(
    playlists: List<PlaylistItem>,
    onSelectPlaylistName: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalM3Palette.current
    var customPlaylistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surfaceContainerHigh,
        titleContentColor = palette.onSurface,
        textContentColor = palette.onSurfaceVariant,
        title = {
            Text(
                text = "Add to playlist",
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (playlists.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        playlists.forEachIndexed { idx, pl ->
                            SegmentedListItem(
                                onClick = { onSelectPlaylistName(pl.name) },
                                shapes = ListItemDefaults.segmentedShapes(
                                    index = idx,
                                    count = playlists.size.coerceAtLeast(1)
                                ),
                                colors = ListItemDefaults.segmentedColors(
                                    containerColor = palette.surfaceContainer,
                                    contentColor = palette.onSurface
                                ),
                                trailingContent = {
                                    Icon(
                                        imageVector = Icons.Default.CreateNewFolder,
                                        contentDescription = null,
                                        tint = palette.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                content = {
                                    Text(
                                        text = pl.name,
                                        color = palette.onSurface,
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("playlist_option_${pl.name}")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = customPlaylistName,
                    onValueChange = { customPlaylistName = it },
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = "New playlist name...",
                            fontFamily = PlusJakartaSansFontFamily
                        )
                    },
                    trailingIcon = if (customPlaylistName.isNotBlank()) {
                        {
                            TextButton(
                                onClick = { onSelectPlaylistName(customPlaylistName.trim()) },
                                shapes = ButtonDefaults.shapes()
                            ) {
                                Text(
                                    text = "Create",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else null,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            if (customPlaylistName.isNotBlank()) {
                Button(
                    onClick = { onSelectPlaylistName(customPlaylistName.trim()) },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(
                        text = "Create",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shapes = ButtonDefaults.shapes()
            ) {
                Text(
                    text = "Cancel",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        modifier = Modifier.testTag("add_to_playlist_dialog")
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ResumePlaybackBottomSheet(
    target: ResumePromptTarget,
    onResumeFromSaved: () -> Unit,
    onStartFromBeginning: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalM3Palette.current
    val resumeTimeLabel = formatDurationMs(target.resumePositionMs)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surfaceContainerLow,
        contentColor = palette.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = palette.DragHandleColor) },
        modifier = Modifier.testTag("resume_playback_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = target.video.title,
                color = palette.onSurface,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Continue where you left off or play from the start",
                color = palette.onSurfaceVariant,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onResumeFromSaved,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primary,
                    contentColor = palette.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("resume_from_saved_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Resume from $resumeTimeLabel",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            FilledTonalButton(
                onClick = onStartFromBeginning,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = palette.surfaceContainerHigh,
                    contentColor = palette.onSurface
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("start_from_beginning_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Start from beginning",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
