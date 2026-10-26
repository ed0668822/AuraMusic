package com.music.aura.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.aura.data.AppUpdateChecker
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun UpdateAvailableDialog(
    version: String,
    hazeState: HazeState,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onInstall: () -> Unit,
    onOpenReleasePage: () -> Unit,
) {
    val state by AppUpdateChecker.download.collectAsStateWithLifecycle()
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .clip(shape)
                .hazeChild(state = hazeState, style = HazeMaterials.thick(MaterialTheme.colorScheme.surface))
                .clickable(enabled = false) {},
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Actualización Disponible",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "La versión $version está lista.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                if (state is AppUpdateChecker.DownloadState.Downloading) {
                    Spacer(Modifier.height(16.dp))
                    val fraction = (state as AppUpdateChecker.DownloadState.Downloading).fraction
                    LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 0.5.dp)

            when (state) {
                is AppUpdateChecker.DownloadState.Downloading -> {
                    TextButton(onClick = onCancelDownload, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Text("Cancelar descarga", color = MaterialTheme.colorScheme.error)
                    }
                }
                is AppUpdateChecker.DownloadState.Ready -> {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = onDismiss, modifier = Modifier.weight(1f).padding(8.dp)) {
                            Text("Después")
                        }
                        TextButton(onClick = onInstall, modifier = Modifier.weight(1f).padding(8.dp)) {
                            Text("Instalar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                else -> {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = onDismiss, modifier = Modifier.weight(1f).padding(8.dp)) {
                            Text("Recordar después")
                        }
                        TextButton(onClick = onDownload, modifier = Modifier.weight(1f).padding(8.dp)) {
                            Text("Descargar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
