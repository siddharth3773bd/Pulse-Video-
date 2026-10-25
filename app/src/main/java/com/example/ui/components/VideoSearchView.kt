@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExpandedFullScreenContainedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.example.model.VideoItem
import kotlinx.coroutines.launch

/** Full-screen video search that grows out of the search icon (same component as Music). */
@Composable
fun VideoSearchView(
    state: SearchBarState,
    textFieldState: TextFieldState,
    videos: List<VideoItem>,
    showResolutionTier: Boolean,
    effectiveAmoled: Boolean,
    onPlay: (VideoItem) -> Unit
) {
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val query = textFieldState.text.toString()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SearchBarValue.Collapsed) textFieldState.clearText()
    }
    val colors = SearchBarDefaults.containedColors(state = state)
    ExpandedFullScreenContainedSearchBar(
        state = state,
        inputField = {
            SearchBarDefaults.InputField(
                textFieldState = textFieldState,
                searchBarState = state,
                colors = colors.inputFieldColors,
                onSearch = { keyboard?.hide() },
                placeholder = { Text("Search videos", modifier = Modifier.clearAndSetSemantics {}) },
                leadingIcon = {
                    IconButton(
                        onClick = { scope.launch { state.animateToCollapsed() } },
                        shapes = IconButtonDefaults.shapes()
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { textFieldState.clearText() }, shapes = IconButtonDefaults.shapes()) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier.testTag("search_input_field")
            )
        },
        colors = colors
    ) {
        val q = query.trim().lowercase()
        val results = remember(videos, q) {
            if (q.isEmpty()) emptyList()
            else videos.filter {
                it.title.lowercase().contains(q) ||
                    it.folder.lowercase().contains(q) ||
                    (it.series?.lowercase()?.contains(q) == true)
            }
        }
        when {
            q.isEmpty() -> LibraryMessage(
                icon = Icons.Default.Search,
                title = "Search your videos",
                body = "Find videos by name, folder or series."
            )
            results.isEmpty() -> LibraryMessage(
                icon = Icons.Default.Search,
                title = "No results",
                body = "Nothing matches “${query.trim()}”."
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp, end = 12.dp, top = 8.dp,
                    bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
            ) {
                itemsIndexed(results, key = { _, v -> v.id }) { idx, v ->
                    VideoListItem(
                        video = v,
                        index = idx,
                        totalCount = results.size,
                        isSelected = false,
                        isSelectionMode = false,
                        amoledBlack = effectiveAmoled,
                        showResolutionTier = showResolutionTier,
                        onClick = { onPlay(v) },
                        onLongClick = {},
                        onMenuClick = {},
                        showMenu = false
                    )
                }
            }
        }
    }
}
