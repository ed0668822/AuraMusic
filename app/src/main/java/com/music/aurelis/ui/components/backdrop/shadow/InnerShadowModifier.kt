package com.music.aurelis.ui.components.backdrop.shadow

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import com.music.aurelis.ui.components.backdrop.internal.ShapeProvider
import com.music.aurelis.ui.components.backdrop.internal.blur
import kotlin.math.ceil

internal class InnerShadowElement(
    val shapeProvider: ShapeProvider,
    val shadow: () -> InnerShadow?
) : ModifierNodeElement<InnerShadowNode>() {

    override fun create(): InnerShadowNode {
        return InnerShadowNode(shapeProvider, shadow)
    }

    override fun update(node: InnerShadowNode) {
        node.shapeProvider = shapeProvider
        node.shadow = shadow
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "innerShadow"
        properties["shapeProvider"] = shapeProvider
        properties["shadow"] = shadow
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InnerShadowElement) return false

        if (shapeProvider != other.shapeProvider) return false
        if (shadow != other.shadow) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shapeProvider.hashCode()
        result = 31 * result + shadow.hashCode()
        return result
    }
}

internal class InnerShadowNode(
    var shapeProvider: ShapeProvider,
    var shadow: () -> InnerShadow?
) : DrawModifierNode, Modifier.Node() {

    override val shouldAutoInvalidate: Boolean = false

    private var innerShadowLayer: GraphicsLayer? = null
    private val paint = Paint()

    override fun ContentDrawScope.draw() {
        val shadow = shadow() ?: return drawContent()

        val innerShadowLayer = innerShadowLayer
        if (innerShadowLayer != null) {
            val size = size
            val density: Density = this
            val layoutDirection = layoutDirection

            val radius = shadow.radius.toPx()
            val offsetX = shadow.offset.x.toPx()
            val offsetY = shadow.offset.y.toPx()
            val shadowSize = IntSize(
                ceil(size.width + radius * 4f + offsetX).toInt(),
                ceil(size.height + radius * 4f + offsetY).toInt()
            )
            val outline = shapeProvider.shape.createOutline(size, layoutDirection, density)

            configurePaint(shadow)

            innerShadowLayer.alpha = shadow.alpha
            innerShadowLayer.blendMode = shadow.blendMode
            innerShadowLayer.record(shadowSize) {
                translate(radius * 2f + offsetX, radius * 2f + offsetY) {
                    val canvas = drawContext.canvas
                    // To simulate an inner shadow, we draw the inverted shadow and clip to the shape
                    canvas.drawOutline(outline, paint)
                    canvas.translate(-offsetX, -offsetY)
                    canvas.drawOutline(outline, InnerShadowMaskPaint)
                    canvas.translate(offsetX, offsetY)
                }
            }

            translate(-radius * 2f, -radius * 2f) {
                drawLayer(innerShadowLayer)
            }
        }

        drawContent()
    }

    override fun onAttach() {
        val graphicsContext = requireGraphicsContext()
        innerShadowLayer =
            graphicsContext.createGraphicsLayer().apply {
                compositingStrategy = CompositingStrategy.Offscreen
            }
    }

    override fun onDetach() {
        val graphicsContext = requireGraphicsContext()
        innerShadowLayer?.let { layer ->
            graphicsContext.releaseGraphicsLayer(layer)
            innerShadowLayer = null
        }
    }

    private fun DrawScope.configurePaint(shadow: InnerShadow) {
        paint.color = shadow.color
        paint.blur(shadow.radius.toPx())
    }
}

private val InnerShadowMaskPaint = Paint().apply {
    blendMode = BlendMode.Clear
}
