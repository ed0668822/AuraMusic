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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.aura.data.AppUpdateChecker
import com.music.aura.data.ProfileRepository
import com.music.aura.data.SettingsRepository
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    isVisible: Boolean,
    onDismiss: () -> Unit,
) {
    if (!isVisible) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var userName by remember { mutableStateOf(ProfileRepository.userName) }
    var userPhoto by remember { mutableStateOf(ProfileRepository.userPhotoUri) }
    var showNameDialog by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            userPhoto = uri.toString()
            ProfileRepository.userPhotoUri = userPhoto
        }
    }

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
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") }
            Text("Ajustes", style = MaterialTheme.typography.titleLarge)
        }

        // Perfil
        Text("PERFIL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().clickable { photoPicker.launch(arrayOf("image/*")) }.padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                if (userPhoto != null) AsyncImage(model = userPhoto, contentDescription = null, modifier = Modifier.fillMaxSize())
                else Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(16.dp))
            Text("Foto de perfil (toca para cambiar)", style = MaterialTheme.typography.bodySmall)
        }
        Row(
            modifier = Modifier.fillMaxWidth().clickable { showNameDialog = true }.padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Nombre", modifier = Modifier.weight(1f))
            Text(userName, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Tema
        Text("APARIENCIA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))
        val themeOptions = listOf("Oscuro", "Claro", "Seguir sistema")
        themeOptions.forEachIndexed { index, label ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { SettingsRepository.themeMode = index }.padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, modifier = Modifier.weight(1f))
                RadioButton(selected = SettingsRepository.themeMode == index, onClick = { SettingsRepository.themeMode = index })
            }
        }

        // Actualizaciones
        Text("SISTEMA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().clickable {
                isCheckingUpdate = true
                scope.launch {
                    AppUpdateChecker.check()
                    isCheckingUpdate = false
                }
            }.padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Text("Buscar actualizaciones", modifier = Modifier.weight(1f))
            if (isCheckingUpdate) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }

        // Acerca de
        Text("ACERCA DE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Aura Music", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Versión 1.4.0", style = MaterialTheme.typography.bodySmall)
                Text("Reproductor 100% local. Soporte Dolby Atmos.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
