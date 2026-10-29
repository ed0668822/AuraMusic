// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

internal val ALERT_WIDTH = 320.dp
internal val ALERT_CORNER = 20.dp
internal val ACTION_HEIGHT = 48.dp
internal val SCRIM_COLOR = Color.Black.copy(alpha = 0.4f)

@Composable
internal fun AlertRule(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    )
}

@Composable
internal fun AlertAction(
    label: String,
    emphasised: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp, horizontal = 16.dp),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = if (emphasised) FontWeight.W600 else FontWeight.Normal,
    )
}
