@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import com.example.ui.icons.Symbols
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
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

/**
 * The Videos search bar's input field, shared by the collapsed bar in the header
 * ([androidx.compose.material3.AppBarWithSearch]) and the full-screen search, as in the official
 * Material 3 sample. Collapsed it shows a search icon; expanded, a back arrow and a clear button.
 */
@Composable
fun VideoSearchInputField(state: SearchBarState, textFieldState: TextFieldState) {
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val expanded = state.currentValue == SearchBarValue.Expanded
    SearchBarDefaults.InputField(
        textFieldState = textFieldState,
        searchBarState = state,
        onSearch = { keyboard?.hide() },
        placeholder = { Text("Search videos", modifier = Modifier.clearAndSetSemantics {}) },
        leadingIcon = {
            if (expanded) {
                IconButton(
                    onClick = { scope.launch { state.animateToCollapsed() } },
                    shapes = IconButtonDefaults.shapes()
                ) { Icon(Symbols.ArrowBack, contentDescription = "Back") }
            } else {
                Icon(Symbols.Search, contentDescription = null)
            }
        },
        trailingIcon = {
            if (expanded && textFieldState.text.isNotEmpty()) {
                IconButton(onClick = { textFieldState.clearText() }, shapes = IconButtonDefaults.shapes()) {
                    Icon(Symbols.Close, contentDescription = "Clear search")
                }
            }
        },
        modifier = Modifier.testTag("search_input_field")
    )
}

/** Full-screen video search that grows out of the header's search bar (same component as Music). */
@Composable
fun VideoSearchView(
    state: SearchBarState,
    textFieldState: TextFieldState,
    videos: List<VideoItem>,
    onPlay: (VideoItem) -> Unit
) {
    val query = textFieldState.text.toString()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SearchBarValue.Collapsed) textFieldState.clearText()
    }
    ExpandedFullScreenContainedSearchBar(
        state = state,
        inputField = { VideoSearchInputField(state, textFieldState) },
        colors = SearchBarDefaults.containedColors(state = state)
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
                icon = Symbols.Search,
                title = "Search your videos",
                body = "Find videos by name, folder or series."
            )
            results.isEmpty() -> LibraryMessage(
                icon = Symbols.Search,
                title = "No results",
                body = "Nothing matches “${query.trim()}”."
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 8.dp, end = 8.dp, top = 8.dp,
                    bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
            ) {
                items(results, key = { it.id }) { v ->
                    VideoListItem(
                        video = v,
                        isSelected = false,
                        isSelectionMode = false,
                        onClick = { onPlay(v) },
                        onLongClick = null
                    )
                }
            }
        }
    }
}
