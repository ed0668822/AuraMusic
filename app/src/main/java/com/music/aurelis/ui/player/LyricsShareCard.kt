// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.ui.player

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun renderLyricsShareCard(
    context: Context,
    card: LyricsShareRequest,
    story: Boolean = false,
): Bitmap = withContext(Dispatchers.Default) {
    // TODO: Implementación real pendiente de copiar el módulo ui/replay de BitChord.
    // Por ahora devolvemos un bitmap vacío para que el proyecto compile.
    Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
}
