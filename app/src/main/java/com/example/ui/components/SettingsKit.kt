@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.icons.Symbols

// ---------------------------------------------------------------------------------------------
// Building blocks for the settings pages (Me tab): official Material 3 Expressive pieces only.
// - A page: the medium flexible top app bar that shrinks as you scroll, then a list.
// - Sections: a small primary-coloured title and a group of joined (segmented) list items.
// - One shape for icon badges everywhere: Material 3 Expressive's cookie.
// ---------------------------------------------------------------------------------------------

/** The badge behind icons in the Me tab: one shape everywhere (Material 3 Expressive cookie). */
@Composable
internal fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialShapes.Cookie9Sided.toShape())
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(size * 0.5f))
    }
}

/**
 * A settings page: back arrow and title in the official medium flexible top app bar (it shrinks to
 * a small bar as you scroll and gets a tint once content is under it), then the [content] list.
 * Sideways, where height is short, the small top app bar is used from the start, and the list
 * keeps a comfortable reading width in the middle.
 */
@Composable
internal fun SettingsPage(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: (@Composable () -> Unit)? = null,
    /** Room at the end of the list, e.g. for a floating toolbar the page draws itself. */
    contentBottomPadding: Dp? = null,
    content: LazyListScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val compactHeight = currentWindowLayout().compactHeight
    val scrollBehavior = if (compactHeight) {
        TopAppBarDefaults.pinnedScrollBehavior()
    } else {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    }
    val titleText: @Composable () -> Unit = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    val subtitleText: (@Composable () -> Unit)? = if (subtitle != null) {
        { Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    } else null
    val backButton: @Composable () -> Unit = {
        TooltipIconButton(
            label = "Back",
            icon = Symbols.ArrowBack,
            onClick = onBack,
            modifier = Modifier.testTag("me_subpage_back_btn")
        )
    }
    val barColors = TopAppBarDefaults.topAppBarColors(
        containerColor = scheme.background,
        scrolledContainerColor = scheme.surfaceContainer
    )
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            if (compactHeight && subtitleText != null) {
                TopAppBar(
                    title = titleText,
                    subtitle = subtitleText,
                    navigationIcon = backButton,
                    actions = actions,
                    colors = barColors,
                    scrollBehavior = scrollBehavior
                )
            } else if (compactHeight) {
                TopAppBar(
                    title = titleText,
                    navigationIcon = backButton,
                    actions = actions,
                    colors = barColors,
                    scrollBehavior = scrollBehavior
                )
            } else {
                MediumFlexibleTopAppBar(
                    title = titleText,
                    subtitle = subtitleText,
                    navigationIcon = backButton,
                    actions = actions,
                    colors = barColors,
                    scrollBehavior = scrollBehavior
                )
            }
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = 720.dp)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = contentBottomPadding ?: if (floatingActionButton != null) 104.dp else 24.dp
                    ),
                    content = content
                )
            }
        }
        if (floatingActionButton != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) { floatingActionButton() }
        }
    }
}

/** A section title above a group: small, in the primary colour (Material list subheader). */
@Composable
internal fun SettingsSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
    )
}

/** One row of a settings group. */
internal sealed class SettingRow(val key: String) {
    /** Opens something (a page, a sheet). [badge] puts the icon on the cookie badge. */
    class Link(
        key: String,
        val icon: ImageVector,
        val title: String,
        val summary: String? = null,
        val badge: Boolean = false,
        val onClick: () -> Unit
    ) : SettingRow(key)

    /** A choice: the current value is shown under the title; a tap opens the options. */
    class Choice(
        key: String,
        val icon: ImageVector,
        val title: String,
        val value: String,
        val onClick: () -> Unit
    ) : SettingRow(key)

    /** On/off: the whole row toggles the switch. */
    class Toggle(
        key: String,
        val icon: ImageVector,
        val title: String,
        val summary: String? = null,
        val checked: Boolean,
        val enabled: Boolean = true,
        val onCheckedChange: (Boolean) -> Unit
    ) : SettingRow(key)

    /** Anything else (a button group, colour swatches), on the same joined background. */
    class Custom(key: String, val content: @Composable () -> Unit) : SettingRow(key)
}

/** A titled group of joined rows, as list items. [title] null = no title. */
internal fun LazyListScope.settingsGroup(key: String, title: String?, rows: List<SettingRow>) {
    if (title != null) item(key = "$key:title") { SettingsSectionTitle(title, Modifier.animateItem()) }
    itemsIndexed(rows, key = { _, row -> "$key:${row.key}" }) { index, row ->
        // Rows that come and go (like Skip time with double tap) fade in and out, and the rows
        // below slide to make room.
        SettingRowItem(
            row = row,
            index = index,
            count = rows.size,
            modifier = Modifier
                .animateItem()
                .padding(bottom = if (index < rows.lastIndex) ListItemDefaults.SegmentedGap else 0.dp)
        )
    }
    item(key = "$key:end") { Spacer(Modifier.animateItem().height(12.dp)) }
}


@Composable
private fun SettingRowItem(row: SettingRow, index: Int, count: Int, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val shapes = ListItemDefaults.segmentedShapes(index = index, count = count)
    val colors = ListItemDefaults.segmentedColors(
        containerColor = scheme.surfaceContainer,
        selectedContainerColor = scheme.surfaceContainer,
        supportingContentColor = scheme.onSurfaceVariant,
        selectedSupportingContentColor = scheme.onSurfaceVariant
    )
    val rowModifier = modifier
        .fillMaxWidth()
        .testTag("setting_${row.key}")
    when (row) {
        is SettingRow.Link -> SegmentedListItem(
            onClick = row.onClick,
            shapes = shapes,
            colors = colors,
            leadingContent = {
                if (row.badge) IconBadge(row.icon) else Icon(row.icon, contentDescription = null)
            },
            supportingContent = if (row.summary != null) {
                { Text(row.summary, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            } else null,
            trailingContent = { Icon(Symbols.ChevronRight, contentDescription = null) },
            modifier = rowModifier
        ) { Text(row.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }

        is SettingRow.Choice -> SegmentedListItem(
            onClick = row.onClick,
            shapes = shapes,
            colors = colors,
            leadingContent = { Icon(row.icon, contentDescription = null) },
            supportingContent = { Text(row.value, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            modifier = rowModifier
        ) { Text(row.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }

        is SettingRow.Toggle -> SegmentedListItem(
            checked = row.checked,
            onCheckedChange = row.onCheckedChange,
            enabled = row.enabled,
            shapes = shapes,
            colors = colors,
            leadingContent = { Icon(row.icon, contentDescription = null) },
            supportingContent = if (row.summary != null) {
                { Text(row.summary, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            } else null,
            trailingContent = {
                Switch(
                    checked = row.checked,
                    onCheckedChange = null,
                    enabled = row.enabled,
                    thumbContent = if (row.checked) {
                        { Icon(Symbols.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                    } else null
                )
            },
            modifier = rowModifier
        ) { Text(row.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }

        is SettingRow.Custom -> Surface(
            shape = shapes.shape,
            color = scheme.surfaceContainer,
            modifier = rowModifier
        ) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) { row.content() }
        }
    }
}

/** An option in a [ChoiceSheet]. */
internal class ChoiceOption<T>(val value: T, val label: String, val description: String? = null)

/** Pick one: a bottom sheet with a joined list of radio rows. Picking closes it. */
@Composable
internal fun <T> ChoiceSheet(
    title: String,
    options: List<ChoiceOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("choice_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
            )
            options.forEachIndexed { index, option ->
                val isSelected = option.value == selected
                SegmentedListItem(
                    selected = isSelected,
                    onClick = { onSelect(option.value) },
                    shapes = ListItemDefaults.segmentedShapes(index = index, count = options.size),
                    leadingContent = { RadioButton(selected = isSelected, onClick = null) },
                    supportingContent = if (option.description != null) {
                        { Text(option.description) }
                    } else null,
                    colors = ListItemDefaults.segmentedColors(
                        containerColor = scheme.surfaceContainerHigh,
                        selectedContainerColor = scheme.secondaryContainer
                    ),
                    modifier = Modifier.testTag("choice_option_$index")
                ) { Text(option.label) }
            }
        }
    }
}

/** A calm empty state in the middle of a page: badge, title, one line of help. */
@Composable
internal fun SettingsEmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconBadge(icon = icon, size = 72.dp)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
