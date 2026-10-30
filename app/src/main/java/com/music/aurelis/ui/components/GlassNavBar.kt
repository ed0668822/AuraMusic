// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.music.aurelis.ui.components.floatingtabbar.GlassPillMotion
import com.music.aurelis.ui.components.floatingtabbar.pillPlacement
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

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
    val scope = rememberCoroutineScope()
    val motion = remember { GlassPillMotion(scope, selectedIndex) }
    val density = LocalDensity.current

    LaunchedEffect(selectedIndex) {
        motion.animateTo(selectedIndex)
    }

    val barTint = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) null else Color.White.copy(alpha = 0.09f)
    
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .height(64.dp)
            .clip(shape)
            .hazeChild(state = hazeState, style = HazeMaterials.regular(MaterialTheme.colorScheme.surface))
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), shape)
    ) {
        val tabWidthPx = with(density) { maxWidth.toPx() } / tabs.size
        val heightPx = with(density) { maxHeight.toPx() }
        
        motion.stepDp = maxWidth.value / tabs.size

        Box(
            modifier = Modifier
                .pillPlacement(
                    center = { Offset(tabWidthPx * motion.position + tabWidthPx / 2, heightPx / 2f) },
                    size = { Size(tabWidthPx, heightPx) }
                )
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), shape)
        )

        Row(modifier = Modifier.fillMaxSize()) {
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
                        .fillMaxHeight()
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
