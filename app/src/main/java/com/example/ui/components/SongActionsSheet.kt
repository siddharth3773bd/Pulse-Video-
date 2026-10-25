@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AudioTrackItem
import com.example.ui.theme.LocalM3Palette

/** One row in the song actions sheet. */
private data class SongAction(
    val label: String,
    val icon: ImageVector,
    val tag: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

/**
 * The song's ⋮ menu: an official ModalBottomSheet with the song at the top and grouped
 * actions shown as Material 3 Expressive segmented lists.
 */
@Composable
fun SongActionsSheet(
    track: AudioTrackItem,
    liked: Boolean,
    canGoToAlbum: Boolean,
    canGoToArtist: Boolean,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleLike: () -> Unit,
    onGoToAlbum: () -> Unit,
    onGoToArtist: () -> Unit,
    onShare: () -> Unit,
    onSongInfo: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalM3Palette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Every action closes the sheet first.
    fun act(block: () -> Unit): () -> Unit = {
        onDismiss()
        block()
    }

    val groups = listOf(
        listOf(
            SongAction("Play next", Icons.AutoMirrored.Filled.PlaylistPlay, "song_action_play_next", onClick = act(onPlayNext)),
            SongAction("Add to queue", Icons.AutoMirrored.Filled.QueueMusic, "song_action_add_queue", onClick = act(onAddToQueue))
        ),
        listOf(
            SongAction(
                if (liked) "Remove from liked songs" else "Add to liked songs",
                if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                "song_action_like",
                onClick = act(onToggleLike)
            )
        ),
        listOf(
            SongAction("Go to album", Icons.Default.Album, "song_action_album", enabled = canGoToAlbum, onClick = act(onGoToAlbum)),
            SongAction("Go to artist", Icons.Default.Person, "song_action_artist", enabled = canGoToArtist, onClick = act(onGoToArtist))
        ),
        listOf(
            SongAction("Share", Icons.Default.Share, "song_action_share", onClick = act(onShare)),
            SongAction("Song info", Icons.Default.Info, "song_action_info", onClick = act(onSongInfo)),
            SongAction("Delete from device", Icons.Default.Delete, "song_action_delete", onClick = act(onDelete))
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("song_actions_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // The song this menu is for.
            Row(verticalAlignment = Alignment.CenterVertically) {
                AudioArt(
                    uri = track.contentUri,
                    placeholderBackground = palette.surfaceContainerHighest,
                    placeholderTint = palette.primary,
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            groups.forEach { group ->
                Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                    group.forEachIndexed { index, action ->
                        val isDelete = action.tag == "song_action_delete"
                        SegmentedListItem(
                            onClick = action.onClick,
                            enabled = action.enabled,
                            shapes = ListItemDefaults.segmentedShapes(index = index, count = group.size),
                            leadingContent = {
                                Icon(
                                    imageVector = action.icon,
                                    contentDescription = null,
                                    tint = if (isDelete) MaterialTheme.colorScheme.error else LocalContentColor.current
                                )
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            modifier = Modifier.testTag(action.tag)
                        ) {
                            Text(
                                text = action.label,
                                color = if (isDelete) MaterialTheme.colorScheme.error else Color.Unspecified
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
