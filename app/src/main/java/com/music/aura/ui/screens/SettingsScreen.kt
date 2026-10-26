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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.aura.data.ProfileRepository
import com.music.aura.data.SettingsRepository
import com.music.aura.ui.components.RowDivider
import com.music.aura.ui.components.SegmentedControl
import com.music.aura.ui.components.SettingsGroup
import com.music.aura.ui.components.SettingsRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isVisible: Boolean,
    onDismiss: () -> Unit,
) {
    if (!isVisible) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Perfil
    var userName by remember { mutableStateOf(ProfileRepository.userName) }
    var userPhoto by remember { mutableStateOf(ProfileRepository.userPhotoUri) }
    var showNameDialog by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            userPhoto = uri.toString()
            ProfileRepository.userPhotoUri = userPhoto
        }
    }

    // Estados de Settings
    val themeMode by SettingsRepository.themeMode.collectAsState()
    val dolbyAtmos by SettingsRepository.dolbyAtmos.collectAsState()
    val liquidGlass by SettingsRepository.liquidGlass.collectAsState()
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
                        AsyncImage(model = userPhoto, contentDescription = null, modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(30.dp))
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
                icon = Icons.Filled.Edit,
                title = "Nombre",
                value = userName,
                onClick = { showNameDialog = true }
            )
        }

        // --- Audio ---
        SettingsGroup(header = "Audio Quality") {
            SettingsRow(
                icon = Icons.Filled.Speaker,
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
                icon = Icons.Filled.GraphicEq,
                title = "Lossless",
                subtitle = "Reproduce la máxima calidad local.",
                trailing = { Text("Lossless", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) }
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Filled.Shuffle,
                title = "AutoMix",
                subtitle = "Transiciones perfectas entre canciones.",
                trailing = {
                    Text("$crossfade s", color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                onClick = { /* TODO: Abrir diálogo de AutoMix */ }
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
                icon = Icons.Filled.AutoAwesome,
                title = "Liquid Glass",
                subtitle = "Cristal real en la barra de navegación. Android 12+.",
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
                icon = Icons.Filled.Lyrics,
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
                icon = Icons.Filled.BlurOn,
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
                icon = Icons.Filled.Folder,
                title = "Local music folder",
                subtitle = "Music/Aura",
                onClick = { /* TODO: Elegir carpeta SAF */ }
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Filled.FilterAlt,
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
                Text("v1.6.0", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(48.dp))
    }
}
