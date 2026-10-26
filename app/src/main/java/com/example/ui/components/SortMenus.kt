@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CheckableDropdownMenuItem
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.LayoutMode
import com.example.model.LibrarySongSort
import com.example.model.SortBy
import com.example.model.SortDir
import com.example.model.ViewMode
import com.example.ui.icons.Symbols

/*
 * Sorting, the Material 3 Expressive way:
 * - At the top of a list, a chip shows how it's sorted ("Date added ▾"); it opens the grouped
 *   menu of sort fields. Picking a new field starts it in its usual order (newest, longest,
 *   A to Z); picking the selected field again flips the order, which its second line spells out.
 * - Videos' header button keeps the view options (what to show, list or grid, series groups).
 * Changes apply straight away.
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

/** The sort chip at the top of a list ("Date added ▾"); it opens [menu], which also flips the order. */
@Composable
internal fun SortControls(
    fieldLabel: String,
    menu: @Composable (expanded: Boolean, onDismiss: () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        FilterChip(
            selected = true,
            onClick = { open = true },
            label = { Text(fieldLabel) },
            trailingIcon = {
                Icon(
                    Symbols.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            },
            modifier = Modifier.testTag("sort_chip")
        )
        menu(open) { open = false }
    }
}

/** Videos' header menu: what to show, list or grid, and series grouping. */
@Composable
fun VideoViewMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    viewMode: ViewMode,
    layoutMode: LayoutMode,
    smartGroups: Boolean,
    onSelectViewMode: (ViewMode) -> Unit,
    onSelectLayoutMode: (LayoutMode) -> Unit,
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
            count = 3,
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
            count = 3,
            choices = listOf(
                MenuChoice(LayoutMode.LIST, "List", Symbols.ViewAgenda),
                MenuChoice(LayoutMode.GRID, "Grid", Symbols.GridView)
            ),
            selected = layoutMode,
            onSelect = onSelectLayoutMode,
            tag = { "layout_mode_btn_${it.id}" }
        )
        Spacer(Modifier.height(MenuDefaults.GroupSpacing))
        DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 2, count = 3)) {
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

/** Videos: the sort chip and arrow at the top of the list, with its menu of fields. */
@Composable
fun VideoSortControls(
    sortBy: SortBy,
    sortDir: SortDir,
    onSelectSortField: (SortBy) -> Unit,
    onSelectSortDir: (SortDir) -> Unit,
    modifier: Modifier = Modifier
) {
    val flip = { onSelectSortDir(if (sortDir == SortDir.ASC) SortDir.DESC else SortDir.ASC) }
    SortControls(
        fieldLabel = sortBy.label,
        menu = { expanded, onDismiss ->
            DropdownMenuPopup(
                expanded = expanded,
                onDismissRequest = onDismiss,
                modifier = Modifier.testTag("video_sort_menu")
            ) {
                ChoiceGroup(
                    label = "Sort by",
                    index = 0,
                    count = 1,
                    choices = SortBy.entries.map { MenuChoice(it, it.label, it.icon) },
                    selected = sortBy,
                    onSelect = { field -> if (field == sortBy) flip() else onSelectSortField(field) },
                    tag = { "sort_option_btn_${it.id}" },
                    selectedLine = if (sortDir == SortDir.ASC) sortBy.ascFull else sortBy.descFull,
                    flippable = true
                )
            }
        },
        modifier = modifier
    )
}

private val SortBy.icon: ImageVector
    get() = when (this) {
        SortBy.NAME -> Symbols.SortByAlpha
        SortBy.DATE -> Symbols.CalendarToday
        SortBy.DURATION -> Symbols.Timer
        SortBy.SIZE -> Symbols.Storage
    }

/**
 * Music: songs get the field chip and the arrow; albums and artists ([field] null) only go
 * A to Z or Z to A, so they get just the arrow.
 */
@Composable
fun LibrarySortControls(
    field: LibrarySongSort?,
    ascending: Boolean,
    onSelectField: (LibrarySongSort) -> Unit,
    onSelectAscending: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val orderLabel = when {
        field == null -> if (ascending) "A to Z" else "Z to A"
        ascending -> field.ascendingLabel
        else -> field.descendingLabel
    }
    SortControls(
        fieldLabel = field?.label ?: orderLabel,
        menu = { expanded, onDismiss ->
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
                        selectedLine = orderLabel,
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
                        onSelect = { onSelectAscending(it); onDismiss() },
                        tag = { "sort_order_${if (it) "asc" else "desc"}" }
                    )
                }
            }
        },
        modifier = modifier
    )
}

private val LibrarySongSort.icon: ImageVector
    get() = when (this) {
        LibrarySongSort.DATE_ADDED -> Symbols.CalendarToday
        LibrarySongSort.TITLE -> Symbols.SortByAlpha
        LibrarySongSort.ARTIST -> Symbols.Person
        LibrarySongSort.ALBUM -> Symbols.Album
        LibrarySongSort.DURATION -> Symbols.Timer
    }
