package com.music.aura.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Utilidad para verificar el permiso de lectura de audio local.
 *
 * En Android 13+ (API 33): READ_MEDIA_AUDIO
 * En Android 12 y anteriores: READ_EXTERNAL_STORAGE
 */
object AudioPermission {

    /**
     * Devuelve el permiso necesario según la versión de Android.
     */
    fun requiredPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    /**
     * Comprueba si el permiso ya está concedido.
     */
    fun isGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            requiredPermission()
        ) == PackageManager.PERMISSION_GRANTED
    }
}
