package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.MediaStoreScanner
import com.example.model.VideoItem
import com.example.model.formatCompactDate

/** Row colours shared by the video lists (one colour system: MaterialTheme.colorScheme). */
@Composable
internal fun videoRowColors(amoledBlack: Boolean) = ListItemDefaults.segmentedColors(
    containerColor = if (amoledBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor = MaterialTheme.colorScheme.onSurface,
    supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
    selectedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    selectedSupportingContentColor = MaterialTheme.colorScheme.onSecondaryContainer
)

/** "1080p · Oct 25, 2026" line under a video's title. */
@Composable
private fun VideoMetaLine(video: VideoItem, showResolutionTier: Boolean, small: Boolean = false) {
    val badge: String? = null
    Text(
        text = listOfNotNull(badge, formatCompactDate(video.dateStr)).joinToString(" · "),
        style = if (small) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/** The ⋮ button with the video's menu anchored to it. */
@Composable
private fun VideoMenuButton(video: VideoItem, onMenuClick: () -> Unit, menu: @Composable () -> Unit, small: Boolean = false) {
    Box {
        IconButton(
            onClick = onMenuClick,
            shapes = IconButtonDefaults.shapes(),
            modifier = Modifier
                .size(if (small) 36.dp else 48.dp)
                .testTag("video_menu_button_${video.mediaId}")
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Video actions for ${video.title}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        menu()
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VideoListItem(
    video: VideoItem,
    index: Int,
    totalCount: Int,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    amoledBlack: Boolean,
    showResolutionTier: Boolean,
    badgeOverride: String? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMenuClick: () -> Unit,
    menu: @Composable () -> Unit = {},
    showMenu: Boolean = true,
    modifier: Modifier = Modifier
) {
    SegmentedListItem(
        selected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = totalCount.coerceAtLeast(1)),
        colors = videoRowColors(amoledBlack),
        leadingContent = {
            VideoThumbnailBox(
                video = video,
                badgeOverride = badgeOverride,
                modifier = Modifier
                    .width(128.dp)
                    .aspectRatio(16f / 9f)
                    .clip(MaterialTheme.shapes.medium)
            )
        },
        supportingContent = { VideoMetaLine(video, showResolutionTier) },
        trailingContent = if (isSelectionMode || showMenu) {
            {
                if (isSelectionMode) {
                    Checkbox(checked = isSelected, onCheckedChange = { onClick() })
                } else {
                    VideoMenuButton(video, onMenuClick, menu)
                }
            }
        } else null,
        content = {
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .testTag("video_list_item_${video.mediaId}")
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VideoGridItem(
    video: VideoItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    amoledBlack: Boolean,
    showResolutionTier: Boolean,
    badgeOverride: String? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMenuClick: () -> Unit,
    menu: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val bg by animateColorAsState(
        targetValue = when {
            isSelected -> scheme.secondaryContainer
            amoledBlack -> Color.Black
            else -> scheme.surfaceContainerLow
        },
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "gridBg"
    )
    val content = if (isSelected) scheme.onSecondaryContainer else scheme.onSurface
    Surface(
        shape = MaterialTheme.shapes.large,
        color = bg,
        contentColor = content,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .testTag("video_grid_item_${video.mediaId}")
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            VideoThumbnailBox(
                video = video,
                badgeOverride = badgeOverride,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(MaterialTheme.shapes.medium)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    androidx.compose.runtime.CompositionLocalProvider(
                        androidx.compose.material3.LocalContentColor provides
                            if (isSelected) scheme.onSecondaryContainer else scheme.onSurfaceVariant
                    ) {
                        VideoMetaLine(video, showResolutionTier, small = true)
                    }
                }
                if (isSelectionMode) {
                    Checkbox(checked = isSelected, onCheckedChange = { onClick() })
                } else {
                    VideoMenuButton(video, onMenuClick, menu, small = true)
                }
            }
        }
    }
}

/**
 * A video's thumbnail with its length. While it loads there's just a calm tinted box (no spinner
 * per thumbnail); the picture fades in when ready.
 */
@Composable
fun VideoThumbnailBox(
    video: VideoItem,
    badgeOverride: String? = null,
    modifier: Modifier = Modifier
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

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        val loaded = bitmap
        if (loaded != null) {
            Image(
                bitmap = loaded.asImageBitmap(),
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

        if (badgeOverride != null) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
            ) {
                Text(
                    text = badgeOverride,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Length, on a small dark pill so it reads on any picture.
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color.Black.copy(alpha = 0.62f),
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(5.dp)
        ) {
            Text(
                text = video.durationLabel,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
            )
        }
    }
}

