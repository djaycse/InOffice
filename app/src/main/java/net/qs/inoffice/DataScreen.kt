package net.qs.inoffice

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.qs.inoffice.data.WorkDataStore
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(
    store: WorkDataStore,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showRestoreWarning by remember { mutableStateOf<RestoreType?>(null) }
    var showErrorPopup by remember { mutableStateOf<String?>(null) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    val saveSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-yaml")
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val locations = store.officeLocations.first()
                    val wifi = store.wifiSsid.first()
                    val pollInterval = store.pollIntervalMinutes.first()
                    val goalPercent = store.goalOfficePercent.first()
                    val goalDays = store.goalTeamHubDays.first()
                    val theme = store.appTheme.first()

                    val yaml = YamlUtils.exportSettings(
                        locations,
                        wifi,
                        pollInterval,
                        goalPercent,
                        goalDays,
                        theme
                    )
                    context.contentResolver.openOutputStream(it)?.use { out ->
                        out.write(yaml.toByteArray())
                    }
                } catch (e: Exception) {
                    showErrorPopup = "Error saving settings: ${e.message}"
                }
            }
        }
    }

    val restoreSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            pendingUri = it
            showRestoreWarning = RestoreType.SETTINGS
        }
    }

    val saveDataLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-yaml")
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val data = store.workMap.first()
                    val yaml = YamlUtils.exportWorkData(data)
                    context.contentResolver.openOutputStream(it)?.use { out ->
                        out.write(yaml.toByteArray())
                    }
                } catch (e: Exception) {
                    showErrorPopup = "Error saving data: ${e.message}"
                }
            }
        }
    }

    val restoreDataLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            pendingUri = it
            showRestoreWarning = RestoreType.DATA
        }
    }

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import/Export") },
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
            // Settings Section
            DataSection(
                title = "Settings",
                subtitle = "Export your configured office locations, Wi-Fi, GPS and goal settings to a file, in case you need to restore it later or on another device",
                onSave = {
                    val timestamp =
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                    saveSettingsLauncher.launch("inoffice_settings_$timestamp.yml")
                },
                onRestore = { restoreSettingsLauncher.launch(arrayOf("*/*")) }
            )

            // Calendar Data Section
            DataSection(
                title = "Calendar data",
                subtitle = "Export your entered calendar data, such as planned and actual office days, leave days, hours worked, etc.",
                onSave = {
                    val timestamp =
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                    saveDataLauncher.launch("inoffice_data_$timestamp.yml")
                },
                onRestore = { restoreDataLauncher.launch(arrayOf("*/*")) }
            )
        }
    }

    if (showRestoreWarning != null) {
        AlertDialog(
            onDismissRequest = { showRestoreWarning = null },
            title = { Text("Warning") },
            text = { Text("Existing configuration/data may be changed as a result of this restoration. Do you want to proceed?") },
            confirmButton = {
                TextButton(onClick = {
                    val type = showRestoreWarning
                    val uri = pendingUri
                    showRestoreWarning = null
                    if (uri != null && type != null) {
                        scope.launch {
                            try {
                                val content =
                                    context.contentResolver.openInputStream(uri)?.use { input ->
                                        BufferedReader(InputStreamReader(input)).readText()
                                    } ?: throw Exception("Could not read file")

                                if (type == RestoreType.SETTINGS) {
                                    val settings = YamlUtils.parseSettings(content)
                                    store.saveOfficeLocations(settings.locations)
                                    store.saveWifiSsid(settings.wifi)
                                    store.savePollInterval(settings.pollInterval)
                                    store.saveGoalOfficePercent(settings.goalPercent)
                                    store.saveGoalTeamHubDays(settings.goalDays)
                                    store.saveAppTheme(settings.theme)
                                } else {
                                    val data = YamlUtils.parseWorkData(content)
                                    data.forEach { (date, state) ->
                                        store.save(date, state)
                                    }
                                }
                            } catch (_: Exception) {
                                showErrorPopup =
                                    "File is not in a valid format for restoring ${if (type == RestoreType.SETTINGS) "configuration" else "this data"}."
                            }
                        }
                    }
                }) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreWarning = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showErrorPopup != null) {
        AlertDialog(
            onDismissRequest = { showErrorPopup = null },
            title = { Text("Error") },
            text = { Text(showErrorPopup ?: "") },
            confirmButton = {
                TextButton(onClick = { showErrorPopup = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun DataSection(
    title: String,
    subtitle: String,
    onSave: () -> Unit,
    onRestore: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onSave) {
                    Text("Export")
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onRestore) {
                    Text("Import")
                }
            }
        }
    }
}

private enum class RestoreType {
    SETTINGS, DATA
}

private object YamlUtils {
    fun exportSettings(
        locations: List<OfficeLocation>,
        wifi: String,
        pollInterval: Long,
        goalPercent: Int,
        goalDays: Int,
        theme: AppTheme
    ): String {
        val sb = StringBuilder()
        sb.append("office_locations:\n")
        locations.forEach {
            sb.append("  - name: \"${it.name}\"\n")
            sb.append("    lat: ${it.lat}\n")
            sb.append("    lng: ${it.lng}\n")
            sb.append("    type: ${it.type.name}\n")
        }
        sb.append("wifi_ssid: \"$wifi\"\n")
        sb.append("poll_interval: $pollInterval\n")
        sb.append("goal_office_percent: $goalPercent\n")
        sb.append("goal_team_hub_days: $goalDays\n")
        sb.append("app_theme: ${theme.name}\n")
        return sb.toString()
    }

    fun exportWorkData(map: Map<String, DayState>): String {
        val sb = StringBuilder()
        sb.append("work_data:\n")
        map.toSortedMap().forEach { (date, state) ->
            sb.append("  - date: $date\n")
            sb.append("    planned: ${state.planned.name}\n")
            sb.append("    actual: ${state.actual.name}\n")
            sb.append("    location_name: \"${state.locationName ?: ""}\"\n")
            sb.append("    hours: ${state.hours}\n")
        }
        return sb.toString()
    }

    fun parseSettings(content: String): SettingsBackup {
        val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }
        var wifi = ""
        var pollInterval = 30L
        var goalPercent = 50
        var goalDays = 5
        var theme = AppTheme.DEFAULT
        val locations = mutableListOf<OfficeLocation>()

        var currentLocName = ""
        var currentLocLat = 0.0
        var currentLocLng = 0.0
        var currentLocType = WorkLocation.BASE

        var inLocations = false

        lines.forEach { line ->
            when {
                line.startsWith("office_locations:") -> inLocations = true
                line.startsWith("- name:") -> {
                    if (currentLocName.isNotEmpty()) {
                        locations.add(
                            OfficeLocation(
                                currentLocName,
                                currentLocLat,
                                currentLocLng,
                                currentLocType
                            )
                        )
                    }
                    currentLocName = line.substringAfter(":").trim().removeSurrounding("\"")
                    inLocations = true
                }

                line.startsWith("lat:") -> currentLocLat =
                    line.substringAfter(":").trim().toDoubleOrNull() ?: 0.0

                line.startsWith("lng:") -> currentLocLng =
                    line.substringAfter(":").trim().toDoubleOrNull() ?: 0.0

                line.startsWith("type:") -> currentLocType =
                    WorkLocation.valueOf(line.substringAfter(":").trim())

                line.startsWith("wifi_ssid:") -> {
                    if (inLocations && currentLocName.isNotEmpty()) {
                        locations.add(
                            OfficeLocation(
                                currentLocName,
                                currentLocLat,
                                currentLocLng,
                                currentLocType
                            )
                        )
                        currentLocName = ""
                    }
                    inLocations = false
                    wifi = line.substringAfter(":").trim().removeSurrounding("\"")
                }

                line.startsWith("poll_interval:") -> pollInterval =
                    line.substringAfter(":").trim().toLongOrNull() ?: 30L

                line.startsWith("goal_office_percent:") -> goalPercent =
                    line.substringAfter(":").trim().toIntOrNull() ?: 50

                line.startsWith("goal_team_hub_days:") -> goalDays =
                    line.substringAfter(":").trim().toIntOrNull() ?: 5

                line.startsWith("app_theme:") -> theme =
                    AppTheme.valueOf(line.substringAfter(":").trim())
            }
        }
        if (inLocations && currentLocName.isNotEmpty()) {
            locations.add(
                OfficeLocation(
                    currentLocName,
                    currentLocLat,
                    currentLocLng,
                    currentLocType
                )
            )
        }

        return SettingsBackup(locations, wifi, pollInterval, goalPercent, goalDays, theme)
    }

    fun parseWorkData(content: String): Map<String, DayState> {
        val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val map = mutableMapOf<String, DayState>()

        var currentDate = ""
        var planned = WorkLocation.HOME
        var actual = WorkLocation.HOME
        var locName = ""
        var hours = 0.0

        lines.forEach { line ->
            when {
                line.startsWith("- date:") -> {
                    if (currentDate.isNotEmpty()) {
                        map[currentDate] =
                            DayState(planned, actual, locName.ifEmpty { null }, hours)
                    }
                    currentDate = line.substringAfter(":").trim()
                }

                line.startsWith("planned:") -> planned =
                    WorkLocation.valueOf(line.substringAfter(":").trim())

                line.startsWith("actual:") -> actual =
                    WorkLocation.valueOf(line.substringAfter(":").trim())

                line.startsWith("location_name:") -> locName =
                    line.substringAfter(":").trim().removeSurrounding("\"")

                line.startsWith("hours:") -> hours =
                    line.substringAfter(":").trim().toDoubleOrNull() ?: 0.0
            }
        }
        if (currentDate.isNotEmpty()) {
            map[currentDate] = DayState(planned, actual, locName.ifEmpty { null }, hours)
        }
        return map
    }
}

private data class SettingsBackup(
    val locations: List<OfficeLocation>,
    val wifi: String,
    val pollInterval: Long,
    val goalPercent: Int,
    val goalDays: Int,
    val theme: AppTheme
)
