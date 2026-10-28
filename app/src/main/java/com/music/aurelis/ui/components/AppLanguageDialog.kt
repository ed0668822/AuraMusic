package com.music.aurelis.ui.components

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.music.aurelis.R
import com.music.aurelis.data.LocaleTags
import com.music.aurelis.data.SettingsRepository
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import java.util.Locale

/** Language tag paired with its display-name string. */
data class AppLanguage(val tag: String, val nameRes: Int)

/**
 * Los 6 idiomas "principales" que el diálogo ofrece hoy.
 *
 * Al final del proyecto se expandirá al resto y se crearán los
 * `values-xx/strings.xml` con la traducción real. Mientras tanto,
 * `values/strings.xml` (base) está en español, así que cualquier selección
 * cae al español hasta que exista el `values-xx/` correspondiente; el
 * checkmark sí se mueve, y la elección persiste.
 */
val SUPPORTED_LANGUAGES = listOf(
    AppLanguage("en", R.string.english),
    AppLanguage("es", R.string.spanish),
    AppLanguage("pt", R.string.portuguese),
    AppLanguage("fr", R.string.french),
    AppLanguage("de", R.string.german),
    AppLanguage("ja", R.string.japanese),
)

fun languageDisplayNameRes(languageTag: String): Int {
    val norm = LocaleTags.normalizeAppTag(languageTag)
    return SUPPORTED_LANGUAGES.firstOrNull {
        LocaleTags.normalizeAppTag(it.tag) == norm
    }?.nameRes ?: R.string.english
}

// ─── iOS-alert style constants (mismas dimensiones que Bitchord) ─────────────
private val LANGUAGE_CARD_CORNER = 14.dp
private val LANGUAGE_CARD_WIDTH = 270.dp
private val LANGUAGE_ROW_HEIGHT = 44.dp
private val LANGUAGE_SCRIM_COLOR = Color.Black.copy(alpha = 0.4f)
private val LANGUAGE_LIST_MAX_HEIGHT = 340.dp

/**
 * Frosted iOS alert with a single-select list. Tapping a row applies the
 * language and dismisses immediately — there is no separate Done action.
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun AppLanguageDialog(
    hazeState: HazeState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceDynamicBlur by SettingsRepository.reduceDynamicBlur.collectAsState()
    val shape = RoundedCornerShape(LANGUAGE_CARD_CORNER)
    // Full script tag (zh-Hant / zh-Hans preserved); legacy bare "zh" reads as Simplified.
    val currentLanguage = LocaleTags.normalizeAppTag(
        AppCompatDelegate.getApplicationLocales().get(0)?.toLanguageTag()
            ?: Locale.getDefault().toLanguageTag(),
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LANGUAGE_SCRIM_COLOR)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(LANGUAGE_CARD_WIDTH)
                .clip(shape)
                .then(
                    if (reduceDynamicBlur) {
                        Modifier.background(MaterialTheme.colorScheme.surface)
                    } else {
                        Modifier.optimizedHazeEffect(
                            state = hazeState,
                            style = HazeMaterials.regular(MaterialTheme.colorScheme.surface),
                        )
                    },
                )
                // Swallows the tap before it reaches the scrim behind.
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 19.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.app_language),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.W600,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.app_language_description),
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }

            LazyColumn(modifier = Modifier.heightIn(max = LANGUAGE_LIST_MAX_HEIGHT)) {
                items(SUPPORTED_LANGUAGES, key = { it.tag }) { language ->
                    LanguageRule()
                    LanguageRow(
                        language = language,
                        selected = LocaleTags.normalizeAppTag(language.tag) == currentLanguage,
                        onClick = {
                            AppCompatDelegate.setApplicationLocales(
                                LocaleListCompat.forLanguageTags(language.tag),
                            )
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageRule() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
    )
}

@Composable
private fun LanguageRow(language: AppLanguage, selected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = LANGUAGE_ROW_HEIGHT)
            .background(
                if (pressed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f) else Color.Transparent,
            )
            .clickable(
                indication = null,
                interactionSource = interactionSource,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(language.nameRes),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}
