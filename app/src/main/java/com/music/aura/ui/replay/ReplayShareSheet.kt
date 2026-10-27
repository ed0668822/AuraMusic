package com.music.aura.ui.replay

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.music.aura.R
import com.music.aura.data.stats.ReplaySummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

@Composable
fun ReplayShareSheet(
    summary: ReplaySummary,
    holder: String,
    memberSince: String?,
    page: ReplayStoryPage?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var poster by remember { mutableStateOf<Bitmap?>(null) }
    var failed by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    LaunchedEffect(summary, page) {
        poster = runCatching { renderReplayPoster(context, summary, holder, memberSince, page) }
            .onFailure { failed = true }
            .getOrNull()
    }

    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp),
    ) {
        Text(
            text = stringResource(R.string.share_my_replay),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.W800,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(
            text = if (page == null) {
                stringResource(R.string.replay_share_summary_description)
            } else {
                stringResource(R.string.replay_share_card_description)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        Box(
            Modifier
                .fillMaxWidth(0.42f)
                .align(Alignment.CenterHorizontally)
                .aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            val image = poster
            when {
                image != null -> Image(
                    bitmap = image.asImageBitmap(),
                    contentDescription = stringResource(R.string.your_replay),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth(),
                )
                failed -> Text(
                    text = stringResource(R.string.couldnt_draw_picture),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                else -> CircularProgressIndicator()
            }
        }

        Spacer(Modifier.height(22.dp))

        val ready = poster != null
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShareAction(
                label = if (saved) stringResource(R.string.saved) else stringResource(R.string.save),
                icon = Icons.Rounded.Download,
                accent = false,
                enabled = ready && !saved,
                modifier = Modifier.weight(1f),
            ) {
                val image = poster ?: return@ShareAction
                scope.launch { saved = saveToGallery(context, image, summary.label) }
            }
            ShareAction(
                label = stringResource(R.string.share),
                icon = Icons.Rounded.IosShare,
                accent = true,
                enabled = ready,
                modifier = Modifier.weight(1f),
            ) {
                val image = poster ?: return@ShareAction
                scope.launch {
                    val uri = cacheForSharing(context, image) ?: return@launch
                    context.startActivity(
                        Intent.createChooser(sendIntent(uri), context.getString(R.string.share_your_replay)),
                    )
                    onDismiss()
                }
            }
        }
    }
}

@Composable
internal fun ShareAction(
    label: String,
    icon: ImageVector,
    accent: Boolean,
    enabled: Boolean,
    saved: Boolean = false,
    savedLabel: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val background = when {
        accent -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val foreground = when {
        accent -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(background.copy(alpha = if (enabled) 1f else 0.4f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Crossfade(targetState = saved, label = "shareAction") { isSaved ->
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Rounded.Check else icon,
                    contentDescription = null,
                    tint = foreground,
                    modifier = Modifier.size(19.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (isSaved) (savedLabel ?: label) else label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.W700,
                    color = foreground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

internal fun sendIntent(uri: Uri) = Intent(Intent.ACTION_SEND)
    .setType(MIME)
    .putExtra(Intent.EXTRA_STREAM, uri)
    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

internal suspend fun cacheForSharing(
    context: Context,
    bitmap: Bitmap,
    fileName: String = "replay.png",
): Uri? = withContext(Dispatchers.IO) {
    runCatching {
        val folder = File(context.cacheDir, SHARE_FOLDER).apply { mkdirs() }
        val file = File(folder, fileName)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull()
}

internal suspend fun saveToGallery(
    context: Context,
    bitmap: Bitmap,
    label: String,
    prefix: String = "aura-replay",
): Boolean = withContext(Dispatchers.IO) {
    val name = "$prefix-${label.replace(' ', '-').lowercase(Locale.ROOT)}.png"
    runCatching {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, MIME)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/AuraMusic",
                )
            }
        }
        val uri = context.contentResolver
            .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("no row")
        context.contentResolver.openOutputStream(uri)?.use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        } ?: error("no stream")
        true
    }.getOrDefault(false)
}

private const val MIME = "image/png"
private const val SHARE_FOLDER = "shared"
