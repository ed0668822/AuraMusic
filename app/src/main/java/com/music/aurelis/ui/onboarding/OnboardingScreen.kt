package com.music.aurelis.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.aurelis.R
import com.music.aurelis.ui.icons.AurelisIcons
import com.music.aurelis.ui.theme.MeshGradient

private const val PAGE_WELCOME = 0
private const val PAGE_PERMISSION = 1

@Composable
fun OnboardingScreen(
    permissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var page by rememberSaveable { mutableIntStateOf(PAGE_WELCOME) }
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = page == PAGE_PERMISSION) { page = PAGE_WELCOME }

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
    ) {
        MeshGradient(
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.05f),
                        0.55f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.45f),
                    ),
                ),
        )

        AnimatedContent(
            targetState = page,
            transitionSpec = {
                val forward = targetState > initialState
                val slide = tween<IntOffset>(420)
                (slideInHorizontally(slide) { if (forward) it / 3 else -it / 3 } + fadeIn(tween(320, 100)))
                    .togetherWith(
                        slideOutHorizontally(slide) { if (forward) -it / 3 else it / 3 } + fadeOut(tween(200)),
                    )
            },
            label = "onboardingPage",
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding(),
        ) { shown ->
            OnboardingPage {
                when (shown) {
                    PAGE_WELCOME -> WelcomePage(
                        onNext = { page = PAGE_PERMISSION },
                    )
                    else -> PermissionPage(
                        onBack = { page = PAGE_WELCOME },
                        permissionGranted = permissionGranted,
                        onRequestPermission = onRequestPermission,
                        onFinish = onFinish,
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPage(content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .padding(horizontal = 28.dp),
            content = content,
        )
    }
}

@Composable
private fun ColumnScope.WelcomePage(onNext: () -> Unit) {
    Spacer(Modifier.height(56.dp))
    Icon(
        imageVector = AurelisIcons.MusicNote,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(46.dp),
    )
    Spacer(Modifier.height(24.dp))
    Title(stringResource(R.string.onboarding_welcome_title))
    Spacer(Modifier.height(14.dp))
    Text(
        text = stringResource(R.string.onboarding_welcome_body),
        style = MaterialTheme.typography.bodyLarge,
        fontSize = 17.sp,
        lineHeight = 23.sp,
        color = Color.White.copy(alpha = 0.72f),
    )

    Spacer(Modifier.weight(1f))

    PillButton(
        label = stringResource(R.string.onboarding_next),
        onClick = onNext,
        primary = true,
    )
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun ColumnScope.PermissionPage(
    onBack: () -> Unit,
    permissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onFinish: () -> Unit,
) {
    Spacer(Modifier.height(16.dp))
    BackButton(onBack)
    Spacer(Modifier.height(20.dp))
    Title(stringResource(R.string.onboarding_permission_title))
    Spacer(Modifier.height(14.dp))
    Text(
        text = stringResource(R.string.onboarding_permission_body),
        style = MaterialTheme.typography.bodyLarge,
        fontSize = 17.sp,
        lineHeight = 23.sp,
        color = Color.White.copy(alpha = 0.72f),
    )

    Spacer(Modifier.weight(1f))

    if (!permissionGranted) {
        PillButton(
            label = stringResource(R.string.onboarding_grant_permission),
            onClick = onRequestPermission,
            primary = true,
        )
    } else {
        PillButton(
            label = stringResource(R.string.onboarding_start),
            onClick = onFinish,
            primary = true,
        )
    }
    Spacer(Modifier.height(12.dp))
    PillButton(
        label = stringResource(R.string.onboarding_skip),
        onClick = onFinish,
        primary = false,
    )
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun Title(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.displayLarge.copy(
            fontSize = 40.sp,
            lineHeight = 44.sp,
            letterSpacing = (-1.2).sp,
        ),
        color = Color.White,
    )
}

@Composable
private fun PillButton(
    label: String,
    onClick: () -> Unit,
    primary: Boolean,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(stiffness = 900f), label = "pillScale")
    val alpha by animateFloatAsState(if (enabled) 1f else 0.4f, tween(180), label = "pillAlpha")
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clip(CircleShape)
            .background(if (primary) Color.White else Color.White.copy(alpha = 0.16f))
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontSize = 17.sp,
            fontWeight = FontWeight.W700,
            color = if (primary) Color.Black else Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = AurelisIcons.ChevronRight,
            contentDescription = stringResource(R.string.onboarding_back),
            tint = Color.White,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer { scaleX = -1f },
        )
    }
}
