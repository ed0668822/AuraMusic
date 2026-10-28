package com.music.aura.ui.components

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shared tokens for iOS-style alert cards in Aura (AppLanguageDialog, and
 * later SongInfoDialog once it's migrated). Defined here so callers don't
 * repeat magic numbers.
 */

/** Fixed width of an alert-style card. */
internal val ALERT_WIDTH = 320.dp

/** Corner radius of an alert-style card. */
internal val ALERT_CORNER = 20.dp

/** Height of a single full-width action row at the bottom of an alert. */
internal val ACTION_HEIGHT = 48.dp

/** Dimmed scrim behind an alert-style card. */
internal val SCRIM_COLOR = Color.Black.copy(alpha = 0.4f)

/** A hairline rule between rows in an alert-style card. */
@Composable
internal fun AlertRule(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    )
}
