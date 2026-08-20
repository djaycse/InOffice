package net.qs.inoffice

import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.launch
import net.qs.inoffice.data.WorkDataStore
import net.qs.inoffice.worker.LocationWorker
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InOfficeApp(store: WorkDataStore) {

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var showHelp by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showOfficeLocations by remember { mutableStateOf(false) }
    var showWifiSettings by remember { mutableStateOf(false) }
    var showGoalSettings by remember { mutableStateOf(false) }
    var showGpsLog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var tapMode by remember { mutableStateOf(TapMode.DO_NOTHING) }

    var showHoursDialog by remember { mutableStateOf(false) }
    var selectedDateKey by remember { mutableStateOf("") }
    var selectedDayState by remember { mutableStateOf(DayState()) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    val savedMap by store.workMap.collectAsState(initial = emptyMap())
    val pollInterval by store.pollIntervalMinutes.collectAsState(initial = 30L)
    val goalPercent by store.goalOfficePercent.collectAsState(initial = 50)
    val goalDays by store.goalTeamHubDays.collectAsState(initial = 5)

    LaunchedEffect(pollInterval) {
        val workRequest =
            PeriodicWorkRequestBuilder<LocationWorker>(Duration.ofMinutes(pollInterval))
                .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "location_scan",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    LaunchedEffect(Unit) {
        // Trigger a one-time scan on app launch
        val workRequest = OneTimeWorkRequestBuilder<LocationWorker>().build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }

    if (showHelp) {
        BackHandler { showHelp = false }
        HelpScreen(
            onBack = { showHelp = false }
        )
        return
    }

    if (showOfficeLocations) {
        BackHandler { showOfficeLocations = false }
        OfficeLocationsScreen(
            store = store,
            onBack = { showOfficeLocations = false }
        )
        return
    }

    if (showWifiSettings) {
        BackHandler { showWifiSettings = false }
        WifiSettingsScreen(
            store = store,
            onBack = { showWifiSettings = false }
        )
        return
    }

    if (showGoalSettings) {
        BackHandler { showGoalSettings = false }
        GoalSettingsScreen(
            store = store,
            onBack = { showGoalSettings = false }
        )
        return
    }

    if (showSettings) {
        BackHandler { showSettings = false }
        SettingsScreen(
            store = store,
            onBack = { showSettings = false },
            onNavigateToOffices = { showOfficeLocations = true },
            onNavigateToAutoDetect = { showWifiSettings = true },
            onNavigateToGoals = { showGoalSettings = true }
        )
        return
    }

    if (showGpsLog) {
        BackHandler { showGpsLog = false }
        GpsLogScreen(
            store = store,
            onBack = { showGpsLog = false }
        )
        return
    }

    val dayStates = remember(savedMap) {
        mutableStateMapOf<String, DayState>().apply {
            savedMap.forEach { (date, value) ->
                put(date, value)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {

                    Row(verticalAlignment = Alignment.CenterVertically) {

                        AndroidView(
                            factory = { ctx ->
                                ImageView(ctx).apply {
                                    setImageResource(R.drawable.inoffice_logo)
                                    clipToOutline = true
                                }
                            },
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )

                        Spacer(Modifier.width(8.dp))

                        Text("InOffice")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu"
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("How to use") },
                                onClick = {
                                    menuExpanded = false
                                    showHelp = true
                                },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = {
                                    menuExpanded = false
                                    showSettings = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Settings, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Log") },
                                onClick = {
                                    menuExpanded = false
                                    showGpsLog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ListAlt,
                                        contentDescription = null
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("About") },
                                onClick = {
                                    menuExpanded = false
                                    showAbout = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Info, contentDescription = null)
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {

                Spacer(Modifier.height(16.dp))

                MonthHeader(
                    month = currentMonth,
                    onPrev = { currentMonth = currentMonth.minusMonths(1) },
                    onToday = { currentMonth = YearMonth.now() },
                    onNext = { currentMonth = currentMonth.plusMonths(1) }
                )

                Spacer(Modifier.height(4.dp))

                var totalDrag by remember { mutableFloatStateOf(0f) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(currentMonth) {
                            detectHorizontalDragGestures(
                                onDragStart = { totalDrag = 0f },
                                onDragEnd = {
                                    if (totalDrag > 100) {
                                        currentMonth = currentMonth.minusMonths(1)
                                    } else if (totalDrag < -100) {
                                        currentMonth = currentMonth.plusMonths(1)
                                    }
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDrag += dragAmount
                                }
                            )
                        }
                ) {
                    CalendarGrid(
                        month = currentMonth,
                        dayStates = dayStates,
                        scope = scope,
                        store = store,
                        tapMode = tapMode,
                        onSetHours = { key, state ->
                            selectedDateKey = key
                            selectedDayState = state
                            showHoursDialog = true
                        }
                    )
                }

                Spacer(Modifier.height(16.dp))
            }

            Column(
                modifier = Modifier
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val todayState = dayStates[todayStr] ?: DayState()

                if (!todayState.locationName.isNullOrEmpty()) {
                    Text(
                        text = "Currently at ${todayState.locationName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    )
                }

                TapModePanel(
                    selectedMode = tapMode,
                    onModeSelected = { tapMode = it }
                )

                val stats = calculateStats(currentMonth, dayStates)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Left Card: In office
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "In office ($goalPercent%)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Planned Row
                            StatRow(
                                isActual = false,
                                isBaseOnly = false,
                                value = stats.plannedNonWfhPercent,
                                goal = goalPercent,
                                isPercent = true
                            )

                            // Actual Row
                            StatRow(
                                isActual = true,
                                isBaseOnly = false,
                                value = stats.actualNonWfhPercent,
                                goal = goalPercent,
                                isPercent = true
                            )
                        }
                    }

                    // Right Card: Team hub
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "Team hub ($goalDays days)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Planned Row
                            StatRow(
                                isActual = false,
                                isBaseOnly = true,
                                value = stats.plannedBaseCount,
                                goal = goalDays,
                                isPercent = false
                            )

                            // Actual Row
                            StatRow(
                                isActual = true,
                                isBaseOnly = true,
                                value = stats.actualBaseCount,
                                goal = goalDays,
                                isPercent = false
                            )
                        }
                    }
                }
            }
        }
    }

    if (showHoursDialog) {
        WorkHoursDialog(
            date = selectedDateKey,
            initialHours = selectedDayState.hours,
            onDismiss = { showHoursDialog = false },
            onSave = { hours ->
                val newState = selectedDayState.copy(hours = hours)
                dayStates[selectedDateKey] = newState
                scope.launch {
                    store.save(selectedDateKey, newState)
                }
                showHoursDialog = false
            }
        )
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) {
                    Text("Close")
                }
            },
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AndroidView(
                        factory = { ctx ->
                            ImageView(ctx).apply {
                                setImageResource(R.drawable.inoffice_logo)
                                clipToOutline = true
                            }
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "InOffice",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        "v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "A simple office attendance planner and tracker designed to help you stay on top of your work-from-office goals.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        "Developed by djaycse.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Copyright © 2026",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}

@Composable
fun WorkHoursDialog(
    date: String,
    initialHours: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var hoursInput by remember { mutableStateOf(initialHours.toString()) }
    val formattedDate = remember(date) {
        try {
            val localDate = LocalDate.parse(date)
            localDate.format(DateTimeFormatter.ofPattern("EEE dd MMM"))
        } catch (_: Exception) {
            date
        }
    }

    val hoursValue = hoursInput.toDoubleOrNull() ?: 0.0
    val isValid = hoursValue in 0.0..12.0 &&
            (hoursInput.split(".").getOrNull(1)?.length ?: 0) <= 2

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set work hours for $formattedDate") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = hoursInput,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.toDoubleOrNull() != null || input == ".") {
                            hoursInput = input
                        }
                    },
                    label = { Text("Hours worked") },
                    suffix = { Text("hrs") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = !isValid && hoursInput.isNotEmpty(),
                    supportingText = {
                        if (!isValid && hoursInput.isNotEmpty()) {
                            Text("Must be between 0 and 12 hours (max 2 decimal places)")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Grid of buttons 0, 3 to 9.5
                val options = listOf(
                    0.0,
                    3.0,
                    3.5,
                    4.0,
                    4.5,
                    5.0,
                    5.5,
                    6.0,
                    6.5,
                    7.0,
                    7.5,
                    8.0,
                    8.5,
                    9.0,
                    9.5
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in options.indices step 3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (j in 0..2) {
                                if (i + j < options.size) {
                                    val opt = options[i + j]
                                    Button(
                                        onClick = { hoursInput = opt.toString() },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            if (opt % 1.0 == 0.0) opt.toInt()
                                                .toString() else opt.toString()
                                        )
                                    }
                                } else {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (isValid) onSave(hoursValue) },
                enabled = isValid
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TapModePanel(
    selectedMode: TapMode,
    onModeSelected: (TapMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tap:",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.width(8.dp))

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TapModeIcon(selectedMode)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = selectedMode.shortLabel,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                TapMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TapModeIcon(mode)
                                Spacer(Modifier.width(12.dp))
                                Text(mode.label)
                            }
                        },
                        onClick = {
                            onModeSelected(mode)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TapModeIcon(mode: TapMode) {
    val blue = Color(0xFF1E88E5)
    val green = Color(0xFF4CAF50)
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val dayOffColor =
        if (isDark) net.qs.inoffice.ui.theme.DayOffDark else net.qs.inoffice.ui.theme.DayOffLight

    Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) {
        when (mode) {
            TapMode.DO_NOTHING -> {
                Canvas(Modifier.fillMaxSize()) {
                    drawLine(
                        color = Color.Gray,
                        start = androidx.compose.ui.geometry.Offset(
                            x = 4.dp.toPx(),
                            y = 8.dp.toPx()
                        ),
                        end = androidx.compose.ui.geometry.Offset(
                            x = 12.dp.toPx(),
                            y = 8.dp.toPx()
                        ),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            TapMode.TEAM_HUB_PLAN -> {
                Canvas(Modifier.fillMaxSize()) { drawCircle(green) }
            }

            TapMode.OTHER_OFFICE_PLAN -> {
                Canvas(Modifier.fillMaxSize()) { drawCircle(blue) }
            }

            TapMode.TEAM_HUB_ACTUAL -> {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(color = green, style = Stroke(width = 2.dp.toPx()))
                }
            }

            TapMode.OTHER_OFFICE_ACTUAL -> {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(color = blue, style = Stroke(width = 2.dp.toPx()))
                }
            }

            TapMode.DAY_OFF -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(dayOffColor)
                )
            }

            TapMode.WFH -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .border(1.dp, MaterialTheme.colorScheme.outline)
                )
            }

            TapMode.SET_HOURS -> {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatIndicator(isActual: Boolean, isBaseOnly: Boolean) {
    val blue = Color(0xFF1E88E5)
    val green = Color(0xFF4CAF50)

    Box(Modifier.size(12.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (isBaseOnly) {
                if (isActual) {
                    drawCircle(
                        color = green,
                        style = Stroke(width = 2.dp.toPx())
                    )
                } else {
                    drawCircle(color = green)
                }
            } else {
                // Half-half
                if (isActual) {
                    drawArc(
                        color = blue,
                        startAngle = 90f,
                        sweepAngle = 180f,
                        useCenter = false,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawArc(
                        color = green,
                        startAngle = 270f,
                        sweepAngle = 180f,
                        useCenter = false,
                        style = Stroke(width = 2.dp.toPx())
                    )
                } else {
                    drawArc(
                        color = blue,
                        startAngle = 90f,
                        sweepAngle = 180f,
                        useCenter = true
                    )
                    drawArc(
                        color = green,
                        startAngle = 270f,
                        sweepAngle = 180f,
                        useCenter = true
                    )
                }
            }
        }
    }
}

@Composable
fun StatRow(
    isActual: Boolean,
    isBaseOnly: Boolean,
    value: Int,
    goal: Int,
    isPercent: Boolean
) {
    val progress = (value.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
    val isMet = progress >= 1f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        StatIndicator(isActual = isActual, isBaseOnly = isBaseOnly)

        Spacer(Modifier.width(8.dp))

        Text(
            text = if (isPercent) "$value%" else "$value days",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )

        Icon(
            imageVector = if (isMet) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (isMet) Color(0xFF2E7D32) else Color.Red,
            modifier = Modifier.size(14.dp)
        )
    }
}
