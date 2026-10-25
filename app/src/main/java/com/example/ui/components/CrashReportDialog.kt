@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.player.PlaybackErrorReport
import com.example.ui.icons.Symbols

/** Shown once after the app closed because of an error, so the error can be copied and sent. */
@Composable
fun CrashReportDialog(report: String, onDismiss: () -> Unit) {
    ErrorReportDialog(
        icon = Symbols.Info,
        title = "Doppler closed unexpectedly",
        message = null,
        report = report,
        clipLabel = "Doppler crash report",
        onDismiss = onDismiss
    )
}

/** A song that couldn't play: what happened, and the full reason (selectable, with Copy). */
@Composable
fun PlaybackErrorDialog(error: PlaybackErrorReport, onDismiss: () -> Unit) {
    ErrorReportDialog(
        icon = Symbols.MusicOff,
        title = "Couldn't play “${error.title}”",
        message = if (error.skipped) "Skipped to the next song. The reason is below." else "The reason is below.",
        report = error.details,
        clipLabel = "Doppler playback error",
        onDismiss = onDismiss
    )
}

/** Official alert dialog with a short message and a monospace report that can be copied. */
@Composable
fun ErrorReportDialog(
    icon: ImageVector,
    title: String,
    message: String?,
    report: String,
    clipLabel: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(icon, contentDescription = null) },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (message != null) Text(message)
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    SelectionContainer {
                        Text(
                            report,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .heightIn(max = 320.dp)
                                .verticalScroll(rememberScrollState())
                                .horizontalScroll(rememberScrollState())
                                .padding(12.dp)
                                .testTag("error_report_text")
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(ClipData.newPlainText(clipLabel, "$title\n$report"))
                    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.testTag("error_report_copy")
            ) { Text("Copy") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) { Text("Close") }
        }
    )
}
