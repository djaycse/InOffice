package net.qs.inoffice

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.qs.inoffice.data.WorkDataStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    store: WorkDataStore,
    onBack: () -> Unit,
    onNavigateToOffices: () -> Unit,
    onNavigateToWifi: () -> Unit,
    onNavigateToGoals: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val theme by store.appTheme.collectAsState(initial = "FOLLOW_SYSTEM")

    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            scope.launch {
                val data = store.exportData()
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(data.toByteArray())
                }
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val content = stream.bufferedReader().readText()
                    store.importData(content)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Theme", style = MaterialTheme.typography.titleMedium)
            
            Column {
                ThemeOption("Follow system", "FOLLOW_SYSTEM", theme) { scope.launch { store.saveAppTheme(it) } }
                ThemeOption("Light mode", "LIGHT", theme) { scope.launch { store.saveAppTheme(it) } }
                ThemeOption("Dark mode", "DARK", theme) { scope.launch { store.saveAppTheme(it) } }
            }

            HorizontalDivider()

            SettingsItem("Offices", Icons.Default.LocationOn, onNavigateToOffices)
            SettingsItem("Auto-detect", Icons.Default.Wifi, onNavigateToWifi)
            SettingsItem("Goals", Icons.Default.Settings, onNavigateToGoals)

            HorizontalDivider()

            Text("Data", style = MaterialTheme.typography.titleMedium)
            
            SettingsItem("Backup data", Icons.Default.Backup) {
                createDocumentLauncher.launch("inoffice_backup.txt")
            }
            SettingsItem("Restore data", Icons.Default.Restore) {
                openDocumentLauncher.launch(arrayOf("text/plain"))
            }
        }
    }
}

@Composable
fun ThemeOption(label: String, value: String, current: String, onSelect: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(value) }
            .padding(vertical = 8.dp)
    ) {
        RadioButton(selected = current == value, onClick = { onSelect(value) })
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
fun SettingsItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
