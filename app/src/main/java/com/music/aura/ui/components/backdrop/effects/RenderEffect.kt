package com.music.aura.ui.components.backdrop.effects

import androidx.compose.ui.graphics.RenderEffect
import com.music.aura.ui.components.backdrop.BackdropEffectScope
import com.music.aura.ui.components.backdrop.RuntimeShader
import com.music.aura.ui.components.backdrop.internal.RuntimeShaderEffect
import com.music.aura.ui.components.backdrop.internal.chain
import com.music.aura.ui.components.backdrop.isRenderEffectSupported
import com.music.aura.ui.components.backdrop.isRuntimeShaderSupported
import org.intellij.lang.annotations.Language
import kotlin.contracts.ExperimentalContracts

fun BackdropEffectScope.effect(effect: RenderEffect) {
    if (!isRenderEffectSupported()) return

    renderEffect = renderEffect.chain(effect)
}

@OptIn(ExperimentalContracts::class)
fun BackdropEffectScope.runtimeShaderEffect(
    key: String,
    @Language("AGSL") shaderString: String,
    uniformShaderName: String,
    block: RuntimeShader.() -> Unit
) {
    if (!isRuntimeShaderSupported()) return

    val effect =
        RuntimeShaderEffect(
            runtimeShader = obtainRuntimeShader(key, shaderString).apply(block),
            uniformShaderName = uniformShaderName
        )
    renderEffect = renderEffect.chain(effect)
}
