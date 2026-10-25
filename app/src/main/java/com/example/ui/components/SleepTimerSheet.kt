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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.formatDurationMs

/** "45 min", "1 h", "1 h 30 min". */
internal fun minutesLabel(m: Int): String =
    if (m < 60) "$m min" else if (m % 60 == 0) "${m / 60} h" else "${m / 60} h ${m % 60} min"

/**
 * Sleep timer, the same for music and videos: pick a time (official Expressive standard button
 * group) or "at the end of this song/video", then Start. A running timer shows what's left and
 * can be turned off here.
 *
 * @param endLabel the "at the end" choice, e.g. "At the end of this song".
 * @param endShortLabel what the header says while that choice is running, e.g. "End of song".
 */
@Composable
fun SleepTimerSheet(
    active: Boolean,
    atEndActive: Boolean,
    remainingMs: Long,
    endLabel: String,
    endShortLabel: String,
    endIcon: ImageVector,
    onStart: (minutes: Int) -> Unit,
    onAtEnd: () -> Unit,
    onTurnOff: () -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var minutes by remember { mutableIntStateOf(30) }
    var atEnd by remember { mutableStateOf(atEndActive) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("sleep_timer_sheet")
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sleep timer", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (active) {
                    Text(
                        if (atEndActive) endShortLabel else "${formatDurationMs(remainingMs + 999L)} left",
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.primary,
                        modifier = Modifier.testTag("sleep_timer_status")
                    )
                }
            }
            CompactRow("Stop after") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(listOf(15, 30, 45), listOf(60, 90, 120)).forEach { row ->
                        ChoiceButtonGroup(
                            options = row.map { it to (if (it == 90) "1.5 h" else minutesLabel(it)) },
                            selected = if (atEnd) null else minutes.takeIf { it in row },
                            onSelect = { v ->
                                if (v != null) {
                                    minutes = v
                                    atEnd = false
                                }
                            },
                            tag = { "sleep_preset_$it" }
                        )
                    }
                }
            }
            FilledTonalToggleButton(
                checked = atEnd,
                onCheckedChange = { atEnd = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sleep_at_end")
            ) {
                Icon(endIcon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(endLabel)
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (active) {
                    OutlinedButton(
                        onClick = onTurnOff,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sleep_turn_off")
                    ) { Text("Turn off") }
                }
                Button(
                    onClick = { if (atEnd) onAtEnd() else onStart(minutes) },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sleep_start")
                ) { Text(if (atEnd) "Start" else "Start · ${minutesLabel(minutes)}") }
            }
        }
    }
}
