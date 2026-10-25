package com.music.aura.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.music.aura.ui.components.floatingtabbar.AnimatedSelectionPill
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * Barra de navegación de cristal inferior.
 *
 * Portada de BitChord con las siguientes mejoras premium:
 * - Haze "regular" (frosted sólido) en vez de "thin".
 * - Glass edge hairline de 0.5dp blanco.
 * - Pill seleccionada con animación de spring (mediumBouncy) deslizante,
 *   inspirada en la GlassSelectionPill de BitChord (sin el lens/backdrop
 *   vendored, que reservamos para el Módulo 5).
 *
 * El MiniPlayer se renderiza por separado en MainActivity (arriba de esta barra),
 * por lo que GlassNavBar ya no recibe playbackState ni transport actions.
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun GlassNavBar(
    tabs: List<BottomTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(percent = 50)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .height(64.dp)
            .clip(shape)
            .hazeChild(
                state = hazeState,
                style = HazeMaterials.regular(MaterialTheme.colorScheme.surface),
            )
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), shape),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val tabWidth = maxWidth / tabs.size
            val pillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)

            // Pill animada (spring) deslizante.
            AnimatedSelectionPill(
                selectedIndex = selectedIndex,
                tabWidth = tabWidth,
                pillColor = pillColor,
            )

            // Tabs clicables encima de la pill.
            Row(modifier = Modifier.fillMaxWidth()) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = index == selectedIndex
                    val tint = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clickable { onTabSelected(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = tint,
                                modifier = Modifier.size(24.dp),
                            )
                            if (isSelected) {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tint,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
