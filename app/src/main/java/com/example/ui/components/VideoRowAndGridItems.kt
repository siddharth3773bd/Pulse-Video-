package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.MediaStoreScanner
import com.example.model.VideoItem
import com.example.model.formatCompactDate

/** Thumbnail width in the list. */
private val ListThumbWidth = 112.dp

/** The ⋮ button with the video's menu anchored to it. Same 48 dp slot whether shown or not. */
@Composable
private fun VideoMenuSlot(
    video: VideoItem,
    visible: Boolean,
    onMenuClick: () -> Unit,
    menu: @Composable () -> Unit
) {
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        if (visible) {
            IconButton(
                onClick = onMenuClick,
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.testTag("video_menu_button_${video.mediaId}")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options for ${video.title}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            menu()
        }
    }
}

/**
 * One video in the list: official Material 3 Expressive list item.
 * - Title (2 lines) over a quieter date line; the thumbnail carries the length.
 * - Selection works like Gmail: the row is tinted and the thumbnail shows a check.
 *   Nothing else moves, so titles never reflow when selection starts.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VideoListItem(
    video: VideoItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    episodeLabel: String? = null,
    onMenuClick: (() -> Unit)? = null,
    menu: @Composable () -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    ListItem(
        selected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
        onLongClickLabel = if (onLongClick != null) "Select" else null,
        leadingContent = {
            VideoThumbnailBox(
                video = video,
                episodeLabel = episodeLabel,
                selection = when {
                    !isSelectionMode -> ThumbSelection.NONE
                    isSelected -> ThumbSelection.SELECTED
                    else -> ThumbSelection.SELECTABLE
                },
                modifier = Modifier
                    .width(ListThumbWidth)
                    .aspectRatio(16f / 9f)
            )
        },
        trailingContent = if (onMenuClick != null) {
            { VideoMenuSlot(video, visible = !isSelectionMode, onMenuClick = onMenuClick, menu = menu) }
        } else null,
        supportingContent = {
            Text(
                text = formatCompactDate(video.dateStr),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
        },
        shapes = ListItemDefaults.shapes(
            shape = MaterialTheme.shapes.large,
            selectedShape = MaterialTheme.shapes.large
        ),
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = scheme.onSurface,
            supportingContentColor = scheme.onSurfaceVariant,
            selectedContainerColor = scheme.secondaryContainer,
            selectedContentColor = scheme.onSecondaryContainer,
            selectedSupportingContentColor = scheme.onSecondaryContainer
        ),
        contentPadding = PaddingValues(start = 8.dp, end = 0.dp, top = 8.dp, bottom = 8.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("video_list_item_${video.mediaId}")
    ) {
        Text(
            text = video.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * One video in the grid: a large rounded thumbnail with the title and date under it. No card
 * around it; when selected, a tinted rounded container appears and the picture shows a check.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VideoGridItem(
    video: VideoItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    episodeLabel: String? = null,
    menu: @Composable () -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    val bg by animateColorAsState(
        targetValue = if (isSelected) scheme.secondaryContainer else Color.Transparent,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "gridBg"
    )
    val content = if (isSelected) scheme.onSecondaryContainer else scheme.onSurface
    val supporting = if (isSelected) scheme.onSecondaryContainer else scheme.onSurfaceVariant
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(bg)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = "Select"
            )
            .semantics { selected = isSelected }
            .padding(6.dp)
            .testTag("video_grid_item_${video.mediaId}")
    ) {
        VideoThumbnailBox(
            video = video,
            episodeLabel = episodeLabel,
            selection = when {
                !isSelectionMode -> ThumbSelection.NONE
                isSelected -> ThumbSelection.SELECTED
                else -> ThumbSelection.SELECTABLE
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 6.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f).padding(top = 2.dp)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = content,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatCompactDate(video.dateStr),
                    style = MaterialTheme.typography.bodySmall,
                    color = supporting,
                    maxLines = 1
                )
            }
            VideoMenuSlot(video, visible = !isSelectionMode, onMenuClick = onMenuClick, menu = menu)
        }
    }
}

/** How a thumbnail shows selection. */
enum class ThumbSelection { NONE, SELECTABLE, SELECTED }

/**
 * A video's thumbnail with its length (and episode number for a series). While it loads there's a
 * calm tinted box; the picture fades in when ready. In selection mode a check circle sits on top
 * and a selected picture shrinks a little inside its tinted row, like Google Photos.
 */
@Composable
fun VideoThumbnailBox(
    video: VideoItem,
    modifier: Modifier = Modifier,
    episodeLabel: String? = null,
    selection: ThumbSelection = ThumbSelection.NONE
) {
    val context = LocalContext.current
    val cacheKey = video.id
    var bitmap by remember(cacheKey) { mutableStateOf<Bitmap?>(MediaStoreScanner.getCachedThumbnail(cacheKey)) }
    var failed by remember(cacheKey) { mutableStateOf(false) }
    // Already-cached thumbnails show at once; new ones fade in.
    val startVisible = remember(cacheKey) { bitmap != null }

    LaunchedEffect(cacheKey) {
        if (bitmap == null) {
            bitmap = MediaStoreScanner.loadVideoThumbnail(context, video.contentUri, video.mediaId)
            failed = bitmap == null
        }
    }
    val alpha by animateFloatAsState(
        targetValue = if (bitmap != null) 1f else 0f,
        animationSpec = if (startVisible) tween(0) else tween(220),
        label = "thumbFade"
    )
    val selected = selection == ThumbSelection.SELECTED
    val inset by animateDpAsState(
        targetValue = if (selected) 6.dp else 0.dp,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "thumbInset"
    )
    val corner by animateDpAsState(
        targetValue = if (selected) 16.dp else 12.dp,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "thumbCorner"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inset)
                .clip(RoundedCornerShape(corner))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            val loaded = bitmap
            if (loaded != null) {
                val image = remember(loaded) { loaded.asImageBitmap() }
                Image(
                    bitmap = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { this.alpha = alpha }
                )
            } else if (failed) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(26.dp)
                )
            }

            if (episodeLabel != null) {
                ThumbPill(episodeLabel, Modifier.align(Alignment.BottomStart))
            }
            ThumbPill(video.durationLabel, Modifier.align(Alignment.BottomEnd))
        }

        // Selection check (top-start), only while selecting.
        AnimatedVisibility(
            visible = selection != ThumbSelection.NONE,
            enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()) +
                scaleIn(MaterialTheme.motionScheme.fastSpatialSpec(), initialScale = 0.6f),
            exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()) +
                scaleOut(MaterialTheme.motionScheme.fastSpatialSpec(), targetScale = 0.6f),
            modifier = Modifier.align(Alignment.TopStart).padding(2.dp)
        ) {
            SelectionCheck(selected = selected)
        }
    }
}

@Composable
private fun SelectionCheck(selected: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(
        targetValue = if (selected) scheme.primary else Color.Black.copy(alpha = 0.35f),
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "checkBg"
    )
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(container)
            .then(if (selected) Modifier else Modifier.border(2.dp, Color.White, CircleShape))
            .semantics {
                role = Role.Checkbox
                this.selected = selected
            },
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = scheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** Small dark pill on a thumbnail ("12:34", "EP 3"), readable on any picture. */
@Composable
private fun ThumbPill(text: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color.Black.copy(alpha = 0.62f),
        contentColor = Color.White,
        modifier = modifier.padding(5.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
        )
    }
}
