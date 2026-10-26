package com.music.aura.ui.components

import androidx.compose.ui.Modifier

/**
 * En Android no hay botón secundario. El long-press ya abre
 * el menú contextual. Este modifier es un no-op en móvil.
 */
fun Modifier.contextClick(onClick: (() -> Unit)?): Modifier = this
