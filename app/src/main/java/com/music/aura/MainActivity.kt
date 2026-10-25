package com.music.aura

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.music.aura.data.AudioPermission
import com.music.aura.data.LibraryState

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // La respuesta se maneja en la UI observando el estado.
        // El ViewModel se recargará al volver a entrar en composición.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Solicitar permiso si no está concedido
        if (!AudioPermission.isGranted(this)) {
            permissionLauncher.launch(AudioPermission.requiredPermission())
        }

        setContent {
            AuraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    LibraryScreen()
                }
            }
        }
    }
}

@Composable
private fun LibraryScreen(viewModel: LibraryViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    // Cargar al entrar si el permiso está concedido
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (AudioPermission.isGranted(viewModel.getApplication())) {
            viewModel.loadLibrary()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        when (val current = state) {
            is LibraryState.Loading -> {
                Text(
                    text = "Cargando biblioteca…",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            is LibraryState.Success -> {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Aura Music",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "${current.songs.size} canciones encontradas",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            is LibraryState.Error -> {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Error",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = current.message,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Button(
                        onClick = { viewModel.loadLibrary() },
                        modifier = Modifier.padding(top = 16.dp),
                    ) {
                        Text("Reintentar")
                    }
                }
            }
        }
    }
}

@Composable
private fun AuraTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
