package com.music.aurelis.ui.player

/**
 * Punto de acceso a la plataforma que hospeda el reproductor.
 *
 * En Bitchord, `LyricsShare.kt` (y otros archivos del player) leen de aquí
 * el estado del host: si el compartir de letras está disponible, cómo avisar
 * al usuario de un mensaje, y preferencias globales como `reduceAnimation`.
 *
 * En Aurelis todavía no hay un host real — se portará completo en la Fase 7
 * (Player UI). Mientras tanto, este stub expone la superficie mínima que
 * el código ya portado necesita para compilar, sin hacer nada en runtime.
 */
object AurelisPlayerPlatform {
    var host: AurelisPlayerHost = StubAurelisPlayerHost
}

interface AurelisPlayerHost {
    val lyricsShareAvailable: Boolean
    fun showMessage(message: String)
}

private object StubAurelisPlayerHost : AurelisPlayerHost {
    override val lyricsShareAvailable: Boolean = false
    override fun showMessage(message: String) {
        android.util.Log.d("AurelisPlayerPlatform", message)
    }
}
