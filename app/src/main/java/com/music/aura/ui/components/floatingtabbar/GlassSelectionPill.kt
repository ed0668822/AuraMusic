package com.music.aura.ui.components.floatingtabbar

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * La pill de selección animada de la navbar.
 *
 * Inspirada en la física de spring de la GlassSelectionPill de BitChord
 * (spring de baja rigidez, dampingRatio mediumBouncy → rebote sutil al llegar
 * al destino), pero reimplementada de forma autónoma para no depender del
 * sistema de backdrop/lens vendored (que reservamos para el Módulo 5).
 *
 * Se posiciona por debajo de la fila de tabs (con el mismo ancho), y se desliza
 * en X con animación de spring cuando cambia [selectedIndex].
 *
 * Uso típico dentro de un `BoxWithConstraints` cuyo ancho se reparte en
 * `tabs.size` columnas de `tabWidth`:
 * ```
 * BoxWithConstraints(Modifier.fillMaxWidth()) {
 *     val tabWidth = maxWidth / tabs.size
 *     AnimatedSelectionPill(selectedIndex, tabWidth, pillColor)
 *     Row { tabs.forEach { ... Box(Modifier.weight(1f)) { ... } } }
 * }
 * ```
 */
@Composable
fun AnimatedSelectionPill(
    selectedIndex: Int,
    tabWidth: Dp,
    pillColor: Color = Color.White.copy(alpha = 0.15f),
    modifier: Modifier = Modifier,
) {
    val offsetX by animateDpAsState(
        targetValue = tabWidth * selectedIndex,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "selectionPillOffset",
    )

    Box(
        modifier = modifier
            .offset(x = offsetX)
            .width(tabWidth)
            .fillMaxHeight()
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .background(pillColor, RoundedCornerShape(percent = 50)),
    )
}
