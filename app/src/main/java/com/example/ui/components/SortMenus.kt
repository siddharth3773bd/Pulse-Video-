@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CheckableDropdownMenuItem
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import com.example.model.LayoutMode
import com.example.model.LibrarySongSort
import com.example.model.SortBy
import com.example.model.SortDir
import com.example.model.ViewMode
import com.example.ui.icons.Symbols

/*
 * Sort menus: the official Material 3 Expressive grouped menu, opened right under the sort button.
 * Each part is its own rounded group with a small label. Picking a sort field selects it in its
 * usual order (newest, longest, A to Z); picking the selected field again flips the order, which
 * its second line spells out. Changes apply straight away and the menu stays open until tapped
 * outside, so a couple of things can be changed in one go.
 */

private class MenuChoice<T>(val value: T, val label: String, val icon: ImageVector)

/** One labelled group of choices where exactly one is picked. */
@Composable
private fun <T> ChoiceGroup(
    label: String,
    index: Int,
    count: Int,
    choices: List<MenuChoice<T>>,
    selected: T?,
    onSelect: (T) -> Unit,
    tag: (T) -> String,
    selectedLine: String? = null,
    flippable: Boolean = false
) {
    if (index > 0) Spacer(Modifier.height(MenuDefaults.GroupSpacing))
    DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = index, count = count)) {
        MenuDefaults.DropdownMenuGroupLabel { Text(label) }
        choices.forEachIndexed { i, choice ->
            val isSelected = choice.value == selected
            SelectableDropdownMenuItem(
                selected = isSelected,
                onClick = { onSelect(choice.value) },
                text = { Text(choice.label) },
                shapes = MenuDefaults.itemShape(index = i, count = choices.size),
                leadingIcon = { Icon(choice.icon, contentDescription = null) },
                selectedLeadingIcon = { Icon(Symbols.Check, contentDescription = null) },
                supportingText = if (isSelected && selectedLine != null) {
                    { Text(selectedLine) }
                } else null,
                trailingContent = if (isSelected && flippable) {
                    { Icon(Symbols.SwapVert, contentDescription = "Tap to flip the order") }
                } else null,
                modifier = Modifier.testTag(tag(choice.value))
            )
        }
    }
}

/** Videos: what to show, list or grid, the order, and series grouping. */
@Composable
fun VideoSortMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    viewMode: ViewMode,
    layoutMode: LayoutMode,
    sortBy: SortBy,
    sortDir: SortDir,
    smartGroups: Boolean,
    onSelectViewMode: (ViewMode) -> Unit,
    onSelectLayoutMode: (LayoutMode) -> Unit,
    onSelectSortField: (SortBy) -> Unit,
    onSelectSortDir: (SortDir) -> Unit,
    onToggleSmartGroups: () -> Unit
) {
    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("sort_and_view_sheet")
    ) {
        ChoiceGroup(
            label = "Show",
            index = 0,
            count = 4,
            choices = listOf(
                MenuChoice(ViewMode.VIDEOS, "Videos", Symbols.VideoLibrary),
                MenuChoice(ViewMode.FOLDERS, "Folders", Symbols.Folder),
                MenuChoice(ViewMode.TREE, "Tree", Symbols.AccountTree)
            ),
            selected = viewMode,
            onSelect = onSelectViewMode,
            tag = { "view_mode_btn_${it.id}" }
        )
        ChoiceGroup(
            label = "Layout",
            index = 1,
            count = 4,
            choices = listOf(
                MenuChoice(LayoutMode.LIST, "List", Symbols.ViewAgenda),
                MenuChoice(LayoutMode.GRID, "Grid", Symbols.GridView)
            ),
            selected = layoutMode,
            onSelect = onSelectLayoutMode,
            tag = { "layout_mode_btn_${it.id}" }
        )
        ChoiceGroup(
            label = "Sort by",
            index = 2,
            count = 4,
            choices = SortBy.entries.map { MenuChoice(it, it.label, it.icon) },
            selected = sortBy,
            onSelect = { field ->
                if (field == sortBy) onSelectSortDir(if (sortDir == SortDir.ASC) SortDir.DESC else SortDir.ASC)
                else onSelectSortField(field)
            },
            tag = { "sort_option_btn_${it.id}" },
            selectedLine = if (sortDir == SortDir.ASC) sortBy.ascFull else sortBy.descFull,
            flippable = true
        )
        Spacer(Modifier.height(MenuDefaults.GroupSpacing))
        DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 3, count = 4)) {
            CheckableDropdownMenuItem(
                checked = smartGroups,
                onCheckedChange = { onToggleSmartGroups() },
                text = { Text("Group series together") },
                shapes = MenuDefaults.itemShape(index = 0, count = 1),
                leadingIcon = { Icon(Symbols.AutoAwesome, contentDescription = null) },
                checkedLeadingIcon = { Icon(Symbols.Check, contentDescription = null) },
                modifier = Modifier.testTag("smart_groups_toggle_card")
            )
        }
    }
}

private val SortBy.icon: ImageVector
    get() = when (this) {
        SortBy.NAME -> Symbols.SortByAlpha
        SortBy.DATE -> Symbols.CalendarToday
        SortBy.DURATION -> Symbols.Timer
        SortBy.SIZE -> Symbols.Storage
    }

/**
 * Music: songs sort by a field (pick it again to flip the order); albums and artists only go
 * A to Z or Z to A. [field] is null for those.
 */
@Composable
fun LibrarySortMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    field: LibrarySongSort?,
    ascending: Boolean,
    onSelectField: (LibrarySongSort) -> Unit,
    onSelectAscending: (Boolean) -> Unit
) {
    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("library_sort_menu")
    ) {
        if (field != null) {
            ChoiceGroup(
                label = "Sort by",
                index = 0,
                count = 1,
                choices = LibrarySongSort.entries.map { MenuChoice(it, it.label, it.icon) },
                selected = field,
                onSelect = { picked ->
                    if (picked == field) onSelectAscending(!ascending) else onSelectField(picked)
                },
                tag = { "sort_field_${it.id}" },
                selectedLine = if (ascending) field.ascendingLabel else field.descendingLabel,
                flippable = true
            )
        } else {
            ChoiceGroup(
                label = "Order",
                index = 0,
                count = 1,
                choices = listOf(
                    MenuChoice(true, "A to Z", Symbols.ArrowDownward),
                    MenuChoice(false, "Z to A", Symbols.ArrowUpward)
                ),
                selected = ascending,
                onSelect = onSelectAscending,
                tag = { if (it) "sort_order_asc" else "sort_order_desc" }
            )
        }
    }
}

private val LibrarySongSort.icon: ImageVector
    get() = when (this) {
        LibrarySongSort.DATE_ADDED -> Symbols.CalendarToday
        LibrarySongSort.TITLE -> Symbols.SortByAlpha
        LibrarySongSort.ARTIST -> Symbols.Person
        LibrarySongSort.ALBUM -> Symbols.Album
        LibrarySongSort.DURATION -> Symbols.Timer
    }
