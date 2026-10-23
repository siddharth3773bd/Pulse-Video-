package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MediaStoreScanner
import com.example.model.VideoItem
import com.example.model.formatCompactDate
import com.example.model.resolutionLabel
import com.example.ui.theme.LocalM3Palette
import com.example.ui.theme.PlusJakartaSansFontFamily

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
    modifier: Modifier = Modifier
) {
    val palette = LocalM3Palette.current
    val badge = resolutionLabel(video.width, video.height)
    val shortDate = formatCompactDate(video.dateStr)
    val rowCardBg = if (amoledBlack) palette.PureBlack else palette.surfaceContainerLow

    SegmentedListItem(
        selected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
        shapes = ListItemDefaults.segmentedShapes(
            index = index,
            count = totalCount.coerceAtLeast(1)
        ),
        colors = ListItemDefaults.segmentedColors(
            containerColor = rowCardBg,
            contentColor = palette.onSurface,
            supportingContentColor = palette.onSurfaceVariant,
            selectedContainerColor = palette.secondaryContainer,
            selectedContentColor = palette.onSecondaryContainer,
            selectedSupportingContentColor = palette.onSecondaryContainer.copy(alpha = 0.8f)
        ),
        leadingContent = {
            VideoThumbnailBox(
                video = video,
                badgeOverride = badgeOverride,
                modifier = Modifier
                    .width(124.dp)
                    .height(70.dp)
                    .clip(MaterialTheme.shapes.medium)
            )
        },
        supportingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                if (showResolutionTier && badge != null) {
                    Text(
                        text = badge,
                        color = palette.onSurfaceVariant,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.2).sp
                    )
                }
                Text(
                    text = shortDate,
                    color = palette.onSurfaceVariant,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        trailingContent = {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = palette.primary,
                        uncheckedColor = palette.outline,
                        checkmarkColor = palette.onPrimary
                    ),
                    modifier = Modifier.minimumInteractiveComponentSize()
                )
            } else {
                IconButton(
                    onClick = onMenuClick,
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier
                        .size(48.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("video_menu_button_${video.mediaId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Video actions for ${video.title}",
                        tint = palette.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        content = {
            Text(
                text = video.title,
                color = palette.onSurface,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 19.8.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = (-0.2).sp
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
    modifier: Modifier = Modifier
) {
    val palette = LocalM3Palette.current
    val motionScheme = MaterialTheme.motionScheme
    val badge = resolutionLabel(video.width, video.height)
    val shortDate = formatCompactDate(video.dateStr)

    val cardBg = if (amoledBlack) palette.PureBlack else palette.surfaceContainerLow
    val targetBg = if (isSelected) palette.secondaryContainer else cardBg
    val bgColor by animateColorAsState(
        targetValue = targetBg,
        animationSpec = motionScheme.defaultEffectsSpec(),
        label = "gridBg"
    )

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        color = palette.onSurface,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 17.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (showResolutionTier && badge != null) {
                            Text(
                                text = badge,
                                color = palette.onSurfaceVariant,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.2).sp
                            )
                        }
                        Text(
                            text = shortDate,
                            color = palette.onSurfaceVariant,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClick() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = palette.primary,
                            uncheckedColor = palette.outline,
                            checkmarkColor = palette.onPrimary
                        ),
                        modifier = Modifier
                            .padding(top = 2.dp, start = 4.dp)
                            .size(24.dp)
                    )
                } else {
                    IconButton(
                        onClick = onMenuClick,
                        shapes = IconButtonDefaults.shapes(),
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("video_grid_menu_button_${video.mediaId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Video actions for ${video.title}",
                            tint = palette.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VideoThumbnailBox(
    video: VideoItem,
    badgeOverride: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalM3Palette.current
    val cacheKey = video.id
    var bitmap by remember(cacheKey) {
        mutableStateOf<Bitmap?>(MediaStoreScanner.getCachedThumbnail(cacheKey))
    }
    var isLoading by remember(cacheKey) {
        mutableStateOf(bitmap == null)
    }

    LaunchedEffect(cacheKey) {
        if (bitmap == null) {
            isLoading = true
            bitmap = MediaStoreScanner.loadVideoThumbnail(
                context = context,
                videoUri = video.contentUri,
                videoId = video.mediaId
            )
            isLoading = false
        }
    }

    Box(
        modifier = modifier.background(palette.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        val loadedBitmap = bitmap
        if (loadedBitmap != null) {
            Image(
                bitmap = loadedBitmap.asImageBitmap(),
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (isLoading) {
            LoadingIndicator(
                color = palette.primary,
                modifier = Modifier.size(28.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Movie,
                contentDescription = null,
                tint = palette.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.size(26.dp)
            )
        }

        if (badgeOverride != null) {
            Surface(
                shape = CircleShape,
                color = palette.secondaryContainer,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
            ) {
                Text(
                    text = badgeOverride,
                    color = palette.onSecondaryContainer,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Bottom dark scrim gradient
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(32.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Duration label at bottom-right
        Text(
            text = video.durationLabel,
            color = Color.White,
            fontFamily = PlusJakartaSansFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.3.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 6.dp)
        )
    }
}
