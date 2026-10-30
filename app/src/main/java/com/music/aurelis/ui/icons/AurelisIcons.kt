// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Iconos de trazo grueso con extremos redondeados, al estilo de Apple Music.
 * Todos se dibujan como strokes a 2.2f para que compartan familia visual.
 */
object AurelisIcons {

    private const val STROKE = 2.2f
    private val stroke = SolidColor(Color.Black)

    val Home: ImageVector by lazy {
        ImageVector.Builder(
            name = "aurelis_home", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                fill = stroke, stroke = stroke, strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 4.05f)
                lineTo(19.75f, 11.35f)
                lineTo(19.75f, 19.65f)
                lineTo(14.95f, 19.65f)
                lineTo(14.95f, 15.6f)
                lineTo(9.05f, 15.6f)
                lineTo(9.05f, 19.65f)
                lineTo(4.25f, 19.65f)
                lineTo(4.25f, 11.35f)
                close()
            }
        }.build()
    }

    val Search: ImageVector by lazy {
        ImageVector.Builder(
            name = "aurelis_search", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(stroke = stroke, strokeLineWidth = STROKE, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(4.6f, 11f)
                arcToRelative(6.4f, 6.4f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12.8f, 0f)
                arcToRelative(6.4f, 6.4f, 0f, isMoreThanHalf = true, isPositiveArc = true, -12.8f, 0f)
                moveTo(15.9f, 15.9f); lineTo(20.4f, 20.4f)
            }
        }.build()
    }

    val Explore: ImageVector by lazy {
        ImageVector.Builder(
            name = "aurelis_explore", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(stroke = stroke, strokeLineWidth = STROKE, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(3.4f, 12f)
                arcToRelative(8.6f, 8.6f, 0f, isMoreThanHalf = true, isPositiveArc = true, 17.2f, 0f)
                arcToRelative(8.6f, 8.6f, 0f, isMoreThanHalf = true, isPositiveArc = true, -17.2f, 0f)
                moveTo(15.4f, 8.6f); lineTo(13.6f, 13.6f); lineTo(8.6f, 15.4f); lineTo(10.4f, 10.4f); close()
            }
        }.build()
    }

    val Library: ImageVector by lazy {
        ImageVector.Builder(
            name = "aurelis_library", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(stroke = stroke, strokeLineWidth = STROKE, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(4.6f, 4.8f); lineTo(4.6f, 19.2f)
                moveTo(9.2f, 4.8f); lineTo(9.2f, 19.2f)
                moveTo(13.8f, 4.8f); lineTo(13.8f, 19.2f)
                moveTo(17.2f, 5.6f); lineTo(20.6f, 18.9f)
            }
        }.build()
    }

    val ChevronRight: ImageVector by lazy {
        ImageVector.Builder(
            name = "aurelis_chevron_right", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(stroke = stroke, strokeLineWidth = STROKE, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(9.5f, 6.2f); lineTo(15.3f, 12f); lineTo(9.5f, 17.8f)
            }
        }.build()
    }

    val Play: ImageVector by lazy {
        ImageVector.Builder(
            name = "aurelis_play", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(stroke = stroke, strokeLineWidth = STROKE, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, fill = stroke) {
                moveTo(6.8f, 4.8f); lineTo(19.2f, 12f); lineTo(6.8f, 19.2f); close()
            }
        }.build()
    }

    val Queue: ImageVector by lazy {
        ImageVector.Builder(
            name = "aurelis_queue", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
                for (y in listOf(6f, 12f, 18f)) { moveTo(8f, y); lineTo(21f, y) }
            }
            path(fill = stroke) {
                for (y in listOf(6f, 12f, 18f)) {
                    moveTo(4.35f, y)
                    arcToRelative(1.25f, 1.25f, 0f, true, true, -2.5f, 0f)
                    arcToRelative(1.25f, 1.25f, 0f, true, true, 2.5f, 0f)
                    close()
                }
            }
        }.build()
    }

    val Shuffle: ImageVector by lazy {
        ImageVector.Builder(
            name = "aurelis_shuffle", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(stroke = stroke, strokeLineWidth = STROKE, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(3.4f, 7.4f); lineTo(7f, 7.4f); lineTo(16.6f, 16.6f); lineTo(20.6f, 16.6f)
                moveTo(18.1f, 14.1f); lineTo(20.6f, 16.6f); lineTo(18.1f, 19.1f)
                moveTo(3.4f, 16.6f); lineTo(7f, 16.6f); lineTo(9.8f, 13.9f)
                moveTo(13.9f, 10.1f); lineTo(16.6f, 7.4f); lineTo(20.6f, 7.4f)
                moveTo(18.1f, 4.9f); lineTo(20.6f, 7.4f); lineTo(18.1f, 9.9f)
            }
        }.build()
    }

    val Repeat: ImageVector by lazy {
        ImageVector.Builder(
            name = "aurelis_repeat", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(stroke = stroke, strokeLineWidth = STROKE, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(8.6f, 7.6f)
                lineTo(15.4f, 7.6f)
                arcToRelative(4.4f, 4.4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0f, 8.8f)
                lineTo(8.6f, 16.4f)
                arcToRelative(4.4f, 4.4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0f, -8.8f)
                close()
                moveTo(13.5f, 5.7f); lineTo(15.4f, 7.6f); lineTo(13.5f, 9.5f)
                moveTo(10.5f, 14.5f); lineTo(8.6f, 16.4f); lineTo(10.5f, 18.3f)
            }
        }.build()
    }

    val MusicNote: ImageVector by lazy {
        ImageVector.Builder(
            name = "aurelis_music_note", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(fill = stroke) {
                moveTo(4.2f, 17.7f); arcToRelative(2.9f, 2.5f, 0f, true, true, 5.8f, 0f); arcToRelative(2.9f, 2.5f, 0f, true, true, -5.8f, 0f); close()
                moveTo(14.2f, 15.9f); arcToRelative(2.9f, 2.5f, 0f, true, true, 5.8f, 0f); arcToRelative(2.9f, 2.5f, 0f, true, true, -5.8f, 0f); close()
            }
            path(stroke = stroke, strokeLineWidth = STROKE, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(10f, 17.7f); lineTo(10f, 6.7f)
                moveTo(20f, 15.9f); lineTo(20f, 4.9f)
                moveTo(10f, 6.7f); lineTo(20f, 4.9f)
            }
        }.build()
    }

    val Instrumental: ImageVector by lazy {
        ImageVector.Builder(
            name = "aurelis_instrumental", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).addPath(
            pathData = addPathNodes(
                "M19.952 1.651a.75.75 0 0 1 .298.599V16.303a3 3 0 0 1-2.176 2.884l-1.32.377" +
                    "a2.553 2.553 0 1 1-1.403-4.909l2.311-.66a1.5 1.5 0 0 0 1.088-1.442V6.994" +
                    "l-9 2.572v9.737a3 3 0 0 1-2.176 2.884l-1.32.377a2.553 2.553 0 1 1-1.402-4.909" +
                    "l2.31-.66a1.5 1.5 0 0 0 1.088-1.442V5.25a.75.75 0 0 1 .544-.721l10.5-3a.75.75 0 0 1 .658.122Z",
            ),
            pathFillType = PathFillType.EvenOdd,
            fill = stroke,
        ).build()
    }

    val Lyrics: ImageVector by lazy {
        ImageVector.Builder(
            name = "aurelis_lyrics", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(stroke = stroke, strokeLineWidth = STROKE, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(6.2f, 4.6f)
                lineTo(17.8f, 4.6f)
                arcToRelative(2.8f, 2.8f, 0f, false, true, 2.8f, 2.8f)
                lineTo(20.6f, 13.6f)
                arcToRelative(2.8f, 2.8f, 0f, false, true, -2.8f, 2.8f)
                lineTo(10.6f, 16.4f)
                lineTo(6.8f, 19.6f)
                lineTo(6.8f, 16.4f)
                lineTo(6.2f, 16.4f)
                arcToRelative(2.8f, 2.8f, 0f, false, true, -2.8f, -2.8f)
                lineTo(3.4f, 7.4f)
                arcToRelative(2.8f, 2.8f, 0f, false, true, 2.8f, -2.8f)
                close()
                moveTo(7.6f, 9f); lineTo(16.4f, 9f)
                moveTo(7.6f, 12.1f); lineTo(13.2f, 12.1f)
            }
        }.build()
    }

    val Heart: ImageVector by lazy { heart("aurelis_heart", filled = false) }
    val HeartFilled: ImageVector by lazy { heart("aurelis_heart_filled", filled = true) }

    private fun heart(name: String, filled: Boolean): ImageVector =
        ImageVector.Builder(
            name = name, defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(stroke = stroke, strokeLineWidth = STROKE, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, fill = if (filled) stroke else null) {
                moveTo(12f, 20f)
                curveTo(12f, 20f, 3.2f, 14.6f, 3.2f, 8.9f)
                arcToRelative(4.5f, 4.5f, 0f, false, true, 8.8f, -1.5f)
                arcToRelative(4.5f, 4.5f, 0f, false, true, 8.8f, 1.5f)
                curveTo(20.8f, 14.6f, 12f, 20f, 12f, 20f)
                close()
            }
        }.build()

    val Star: ImageVector by lazy { star("aurelis_star", filled = false) }
    val StarFilled: ImageVector by lazy { star("aurelis_star_filled", filled = true) }

    private fun star(name: String, filled: Boolean): ImageVector =
        ImageVector.Builder(
            name = name, defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(stroke = stroke, strokeLineWidth = STROKE, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, fill = if (filled) stroke else null) {
                moveTo(12f, 3.86f); lineTo(14.33f, 9.66f); lineTo(20.56f, 10.08f)
                lineTo(15.77f, 14.08f); lineTo(17.29f, 20.14f); lineTo(12f, 16.82f)
                lineTo(6.71f, 20.14f); lineTo(8.23f, 14.08f); lineTo(3.44f, 10.08f); lineTo(9.67f, 9.66f)
                close()
            }
        }.build()

    val Plus: ImageVector by lazy {
        ImageVector.Builder(
            name = "aurelis_plus", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(stroke = stroke, strokeLineWidth = STROKE, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 5f); lineTo(12f, 19f)
                moveTo(5f, 12f); lineTo(19f, 12f)
            }
        }.build()
    }

    val Check: ImageVector by lazy {
        ImageVector.Builder(
            name = "aurelis_check", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(stroke = stroke, strokeLineWidth = STROKE, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(5f, 12.8f); lineTo(9.6f, 17.4f); lineTo(19f, 6.9f)
            }
        }.build()
    }

    val Download: ImageVector by lazy {
        ImageVector.Builder(
            name = "aurelis_download", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(stroke = stroke, strokeLineWidth = STROKE, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 4.5f); lineTo(12f, 15.5f)
                moveTo(7.5f, 11f); lineTo(12f, 15.5f); lineTo(16.5f, 11f)
                moveTo(4.5f, 18f); lineTo(19.5f, 18f)
            }
        }.build()
    }

    val Clock: ImageVector by lazy {
        ImageVector.Builder(
            name = "aurelis_clock", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(stroke = stroke, strokeLineWidth = STROKE, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 3.4f)
                arcToRelative(8.6f, 8.6f, 0f, true, true, 0f, 17.2f)
                arcToRelative(8.6f, 8.6f, 0f, true, true, 0f, -17.2f)
                close()
                moveTo(12f, 7.4f); lineTo(12f, 12f); lineTo(15f, 14.4f)
            }
        }.build()
    }

    val Pin: ImageVector by lazy {
        ImageVector.Builder(
            name = "aurelis_pin", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(stroke = stroke, strokeLineWidth = STROKE, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(9.6f, 8.4f)
                arcToRelative(3.4f, 3.4f, 0f, true, true, 6.8f, 0f)
                arcToRelative(3.4f, 3.4f, 0f, true, true, -6.8f, 0f)
                moveTo(10.8f, 10.8f); lineTo(5.6f, 19.6f)
            }
        }.build()
    }

    val Infinity: ImageVector by lazy {
        ImageVector.Builder(
            name = "aurelis_infinity", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(stroke = stroke, strokeLineWidth = STROKE, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 12f)
                curveTo(10.1f, 9.1f, 8.7f, 8f, 7.1f, 8f)
                arcToRelative(4f, 4f, 0f, false, false, 0f, 8f)
                curveTo(8.7f, 16f, 10.1f, 14.9f, 12f, 12f)
                curveTo(13.9f, 9.1f, 15.3f, 8f, 16.9f, 8f)
                arcToRelative(4f, 4f, 0f, false, true, 0f, 8f)
                curveTo(15.3f, 16f, 13.9f, 14.9f, 12f, 12f)
            }
        }.build()
    }

    val GridView: ImageVector by lazy {
        ImageVector.Builder(
            name = "aurelis_grid_view", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(stroke = stroke, strokeLineWidth = STROKE, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(6.5f, 4.5f); lineTo(8.5f, 4.5f); arcToRelative(2f, 2f, 0f, false, true, 2f, 2f); lineTo(10.5f, 8.5f); arcToRelative(2f, 2f, 0f, false, true, -2f, 2f); lineTo(6.5f, 10.5f); arcToRelative(2f, 2f, 0f, false, true, -2f, -2f); lineTo(4.5f, 6.5f); arcToRelative(2f, 2f, 0f, false, true, 2f, -2f); close()
                moveTo(15.5f, 4.5f); lineTo(17.5f, 4.5f); arcToRelative(2f, 2f, 0f, false, true, 2f, 2f); lineTo(19.5f, 8.5f); arcToRelative(2f, 2f, 0f, false, true, -2f, 2f); lineTo(15.5f, 10.5f); arcToRelative(2f, 2f, 0f, false, true, -2f, -2f); lineTo(13.5f, 6.5f); arcToRelative(2f, 2f, 0f, false, true, 2f, -2f); close()
                moveTo(6.5f, 13.5f); lineTo(8.5f, 13.5f); arcToRelative(2f, 2f, 0f, false, true, 2f, 2f); lineTo(10.5f, 17.5f); arcToRelative(2f, 2f, 0f, false, true, -2f, 2f); lineTo(6.5f, 19.5f); arcToRelative(2f, 2f, 0f, false, true, -2f, -2f); lineTo(4.5f, 15.5f); arcToRelative(2f, 2f, 0f, false, true, 2f, -2f); close()
                moveTo(15.5f, 13.5f); lineTo(17.5f, 13.5f); arcToRelative(2f, 2f, 0f, false, true, 2f, 2f); lineTo(19.5f, 17.5f); arcToRelative(2f, 2f, 0f, false, true, -2f, 2f); lineTo(15.5f, 19.5f); arcToRelative(2f, 2f, 0f, false, true, -2f, -2f); lineTo(13.5f, 15.5f); arcToRelative(2f, 2f, 0f, false, true, 2f, -2f); close()
            }
        }.build()
    }

    val ListView: ImageVector by lazy {
        ImageVector.Builder(
            name = "aurelis_list_view", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(stroke = stroke, strokeLineWidth = STROKE, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(4.5f, 6.5f); lineTo(5.5f, 6.5f); moveTo(9.5f, 6.5f); lineTo(19.5f, 6.5f)
                moveTo(4.5f, 12f); lineTo(5.5f, 12f); moveTo(9.5f, 12f); lineTo(19.5f, 12f)
                moveTo(4.5f, 17.5f); lineTo(5.5f, 17.5f); moveTo(9.5f, 17.5f); lineTo(19.5f, 17.5f)
            }
        }.build()
    }
}
