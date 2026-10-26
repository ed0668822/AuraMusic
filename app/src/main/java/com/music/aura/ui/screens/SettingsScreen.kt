package com.music.aura.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.aura.data.ProfileRepository
import com.music.aura.data.SettingsRepository
import com.music.aura.ui.components.*

@Composable
fun SettingsScreen(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onOpenEqualizer: () -> Unit = {},
) {
    if (!isVisible) return

    val context = LocalContext.current
    var userName by remember { mutableStateOf(ProfileRepository.userName) }
    var userPhoto by remember { mutableStateOf(ProfileRepository.userPhotoUri) }
    var showNameDialog by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
            userPhoto = uri.toString()
            ProfileRepository.userPhotoUri = userPhoto
        }
    }

    val themeMode by SettingsRepository.themeMode.collectAsState()
    val dolbyAtmos by SettingsRepository.dolbyAtmos.collectAsState()
    val liquidGlass by SettingsRepository.liquidGlass.collectAsState()
    val reduceAnimation by SettingsRepository.reduceAnimation.collectAsState()
    val reduceDynamicBlur by SettingsRepository.reduceDynamicBlur.collectAsState()
    val syncedLyrics by SettingsRepository.syncedLyrics.collectAsState()
    val blurLyrics by SettingsRepository.blurUnfocusedLyrics.collectAsState()
    val filterNonMusic by SettingsRepository.filterNonMusicAudio.collectAsState()
    val autoplay by SettingsRepository.autoplay.collectAsState()
    val crossfade by SettingsRepository.crossfadeSeconds.collectAsState()

    if (showNameDialog) {
        var tempName by remember { mutableStateOf(userName) }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Editar Nombre") },
            text = { OutlinedTextField(value = tempName, onValueChange = { tempName = it }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    userName = tempName
                    ProfileRepository.userName = tempName
                    showNameDialog = false
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showNameDialog = false }) { Text("Cancelar") } }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 8.dp, end = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
            }
            Text(
                text = "Settings",
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // --- Perfil ---
        SettingsGroup(topSpacing = 0.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { photoPicker.launch(arrayOf("image/*")) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(60.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (userPhoto != null) {
                        AsyncImage(model = userPhoto, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(30.dp))
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(text = userName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = "Toca para cambiar foto", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Edit,
                title = "Nombre",
                value = userName,
                onClick = { showNameDialog = true }
            )
        }

        // --- Audio ---
        SettingsGroup(header = "Audio Quality") {
            SettingsRow(
                icon = Icons.Rounded.Speaker,
                title = "Dolby Atmos",
                subtitle = "Reproduce la versión envolvente cuando esté disponible.",
                trailing = {
                    Switch(
                        checked = dolbyAtmos,
                        onCheckedChange = { SettingsRepository.setDolbyAtmos(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                },
                onClick = { SettingsRepository.setDolbyAtmos(!dolbyAtmos) }
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.GraphicEq,
                title = "Lossless",
                subtitle = "Reproduce la máxima calidad local.",
                trailing = { Text("Lossless", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) }
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Tune,
                title = "Equalizer",
                subtitle = "Tone, seven bands and balance",
                onClick = onOpenEqualizer
            )
        }

        // --- Playback ---
        SettingsGroup(header = "Playback") {
            SettingsRow(
                icon = Icons.Rounded.Autorenew,
                title = "Autoplay",
                subtitle = "Sigue reproduciendo canciones similares al terminar la cola.",
                trailing = {
                    Switch(
                        checked = autoplay,
                        onCheckedChange = { SettingsRepository.setAutoplay(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                },
                onClick = { SettingsRepository.setAutoplay(!autoplay) }
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Shuffle,
                title = "AutoMix",
                subtitle = "Transiciones perfectas entre canciones.",
                value = "$crossfade s",
                onClick = { }
            )
        }

        // --- Apariencia ---
        SettingsGroup(header = "Appearance") {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Theme", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                SegmentedControl(
                    options = listOf("System", "Light", "Dark"),
                    selectedIndex = themeMode,
                    onSelect = { SettingsRepository.setThemeMode(it) }
                )
            }
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.MotionPhotosOff,
                title = "Reduce animation",
                subtitle = "Congela el gradiente del reproductor en lugar de dejarlo flotar.",
                trailing = {
                    Switch(
                        checked = reduceAnimation,
                        onCheckedChange = { SettingsRepository.setReduceAnimation(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                },
                onClick = { SettingsRepository.setReduceAnimation(!reduceAnimation) }
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.BlurOff,
                title = "Reduce dynamic blur",
                subtitle = "Cambia el cristal difuminado por fondos sólidos en toda la app.",
                trailing = {
                    Switch(
                        checked = reduceDynamicBlur,
                        onCheckedChange = { SettingsRepository.setReduceDynamicBlur(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                },
                onClick = { SettingsRepository.setReduceDynamicBlur(!reduceDynamicBlur) }
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.AutoAwesome,
                title = "Liquid Glass",
                subtitle = "Cristal real en la barra de navegación.",
                trailing = {
                    Switch(
                        checked = liquidGlass,
                        onCheckedChange = { SettingsRepository.setLiquidGlass(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                },
                onClick = { SettingsRepository.setLiquidGlass(!liquidGlass) }
            )
        }

        // --- Letras ---
        SettingsGroup(header = "Lyrics") {
            SettingsRow(
                icon = Icons.Rounded.Lyrics,
                title = "Synced lyrics",
                subtitle = "Ilumina las palabras mientras se cantan.",
                trailing = {
                    Switch(
                        checked = syncedLyrics,
                        onCheckedChange = { SettingsRepository.setSyncedLyrics(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                },
                onClick = { SettingsRepository.setSyncedLyrics(!syncedLyrics) }
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.BlurOn,
                title = "Blur unfocused lyrics",
                subtitle = "Mantiene el foco en la línea actual.",
                trailing = {
                    Switch(
                        checked = blurLyrics,
                        onCheckedChange = { SettingsRepository.setBlurUnfocusedLyrics(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                },
                onClick = { SettingsRepository.setBlurUnfocusedLyrics(!blurLyrics) }
            )
        }

        // --- Local Music ---
        SettingsGroup(header = "Local Music") {
            SettingsRow(
                icon = Icons.Rounded.Folder,
                title = "Local music folder",
                subtitle = "Music/Aura",
                onClick = { }
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.FilterAlt,
                title = "Filter non-music audio",
                subtitle = "Oculta clips, notas de voz y sonidos del sistema.",
                trailing = {
                    Switch(
                        checked = filterNonMusic,
                        onCheckedChange = { SettingsRepository.setFilterNonMusicAudio(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                },
                onClick = { SettingsRepository.setFilterNonMusicAudio(!filterNonMusic) }
            )
        }

        // --- Acerca de ---
        SettingsGroup(header = "About") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Aura Music", fontWeight = FontWeight.Bold)
                Text("v1.9.0", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(48.dp))
    }
}
