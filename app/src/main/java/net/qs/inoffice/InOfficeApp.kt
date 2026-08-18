package net.qs.inoffice

import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import net.qs.inoffice.data.WorkDataStore
import net.qs.inoffice.ui.theme.InOfficeTheme
import net.qs.inoffice.worker.LocationWorker
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InOfficeApp() {

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var showHelp by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showOfficeLocations by remember { mutableStateOf(false) }
    var showWifiSettings by remember { mutableStateOf(false) }
    var showGoalSettings by remember { mutableStateOf(false) }
    var showGpsLog by remember { mutableStateOf(false) }
    var showStatistics by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var selectedAction by remember { mutableStateOf(DashboardAction.NONE) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { WorkDataStore(context) }
    val scope = rememberCoroutineScope()

    val savedMap by store.workMap.collectAsState(initial = emptyMap())
    val pollInterval by store.pollIntervalMinutes.collectAsState(initial = 30L)
    val goalPercent by store.goalOfficePercent.collectAsState(initial = 50)
    val goalDays by store.goalTeamHubDays.collectAsState(initial = 5)
    val themeStr by store.appTheme.collectAsState(initial = "FOLLOW_SYSTEM")

    val darkTheme = when (themeStr) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

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
        val workRequest = OneTimeWorkRequestBuilder<LocationWorker>().build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }

    val dayStates = remember(savedMap) {
        mutableStateMapOf<String, DayState>().apply {
            savedMap.forEach { (date, value) ->
                put(date, value)
            }
        }
    }

    if (showOfficeLocations) {
        BackHandler { showOfficeLocations = false }
        OfficeLocationsScreen(store = store, onBack = { showOfficeLocations = false })
        return
    }

    if (showWifiSettings) {
        BackHandler { showWifiSettings = false }
        WifiSettingsScreen(store = store, onBack = { showWifiSettings = false })
        return
    }

    if (showGoalSettings) {
        BackHandler { showGoalSettings = false }
        GoalSettingsScreen(store = store, onBack = { showGoalSettings = false })
        return
    }

    if (showGpsLog) {
        BackHandler { showGpsLog = false }
        GpsLogScreen(store = store, onBack = { showGpsLog = false })
        return
    }

    if (showStatistics) {
        BackHandler { showStatistics = false }
        StatisticsScreen(dayStates = dayStates, onBack = { showStatistics = false })
        return
    }

    if (showSettings) {
        BackHandler { showSettings = false }
        SettingsScreen(
            store = store,
            onBack = { showSettings = false },
            onNavigateToOffices = { showOfficeLocations = true },
            onNavigateToWifi = { showWifiSettings = true },
            onNavigateToGoals = { showGoalSettings = true }
        )
        return
    }

    InOfficeTheme(darkTheme = darkTheme, dynamicColor = false) {
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
                            Text("InOffice v${BuildConfig.VERSION_NAME}")
                        }
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
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
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Settings") },
                                    onClick = {
                                        menuExpanded = false
                                        showSettings = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Log") },
                                    onClick = {
                                        menuExpanded = false
                                        showGpsLog = true
                                    },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Statistics") },
                                    onClick = {
                                        menuExpanded = false
                                        showStatistics = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.BarChart, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("About") },
                                    onClick = {
                                        menuExpanded = false
                                        showAbout = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
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
                    Spacer(Modifier.height(8.dp))
                    DashboardButtons(
                        selectedAction = selectedAction,
                        onActionToggle = { action ->
                            selectedAction = if (selectedAction == action) DashboardAction.NONE else action
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    var totalDrag by remember { mutableFloatStateOf(0f) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(currentMonth) {
                                detectHorizontalDragGestures(
                                    onDragStart = { totalDrag = 0f },
                                    onDragEnd = {
                                        if (totalDrag > 100) currentMonth = currentMonth.minusMonths(1)
                                        else if (totalDrag < -100) currentMonth = currentMonth.plusMonths(1)
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
                            selectedAction = selectedAction
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }

                Column(
                    modifier = Modifier.padding(16.dp),
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
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                        )
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(12.dp).background(Color(0xFF4CAF50)))
                                Spacer(Modifier.width(4.dp))
                                Text("Team hub", style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.width(16.dp))
                                Box(Modifier.size(12.dp).background(Color(0xFFFFA500)))
                                Spacer(Modifier.width(4.dp))
                                Text("Other office", style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.width(16.dp))
                                Box(Modifier.size(12.dp).background(Color.Red))
                                Spacer(Modifier.width(4.dp))
                                Text("WFH", style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.width(16.dp))
                                val dayOffColor = if (darkTheme) net.qs.inoffice.ui.theme.DayOffDark else net.qs.inoffice.ui.theme.DayOffLight
                                Box(Modifier.size(12.dp).background(dayOffColor))
                                Spacer(Modifier.width(4.dp))
                                Text("Day off", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(12.dp).background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape))
                                Spacer(Modifier.width(4.dp))
                                Text("Plan", style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.width(16.dp))
                                Box(Modifier.size(12.dp).border(2.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape))
                                Spacer(Modifier.width(4.dp))
                                Text("Actual", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    val stats = calculateStats(currentMonth, dayStates)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("In office ($goalPercent%)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                StatRow(label = "Plan", value = stats.plannedNonWfhPercent, goal = goalPercent, isPercent = true)
                                StatRow(label = "Actual", value = stats.actualNonWfhPercent, goal = goalPercent, isPercent = true)
                            }
                        }
                        Card(modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Team hub ($goalDays days)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                StatRow(label = "Plan", value = stats.plannedBaseCount, goal = goalDays, isPercent = false)
                                StatRow(label = "Actual", value = stats.actualBaseCount, goal = goalDays, isPercent = false)
                            }
                        }
                    }
                }
            }
        }

        if (showHelp) {
            AlertDialog(
                onDismissRequest = { showHelp = false },
                title = { Text("How to use") },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("1. Set your office locations in Settings")
                        Text("2. Set auto-detect settings in Settings")
                        Text("3. Set your attendance goals in Settings")
                        Text("4. Monitor statistics on main screen.")
                        Spacer(Modifier.height(8.dp))
                        Text("--- Editing Calendar ---", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        Text("Use the buttons above the calendar to select an action, then tap a date:")
                        Text("- Set planned: Cycle through planned office types (Team hub, Other office)")
                        Text("- Set actual: Cycle through actual office types (Team hub, Other office)")
                        Text("- Set WFH: Toggle explicitly marked WFH days")
                        Text("- Set holidays: Toggle leave/holiday days")
                        Text("- Set hours worked: Set duration worked (0-12h)")
                        Text("- Eraser: Clear all type markers for a day")
                        Spacer(Modifier.height(8.dp))
                        Text("Tapping the same day with the same tool will cycle through options and eventually toggle it off.")
                    }
                },
                confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Got it") } }
            )
        }

        if (showAbout) {
            AlertDialog(
                onDismissRequest = { showAbout = false },
                confirmButton = { TextButton(onClick = { showAbout = false }) { Text("Close") } },
                title = {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        AndroidView(factory = { ctx -> ImageView(ctx).apply { setImageResource(R.drawable.inoffice_logo); clipToOutline = true } }, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)))
                        Spacer(Modifier.height(16.dp))
                        Text("InOffice", style = MaterialTheme.typography.headlineSmall)
                        Text("v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("A simple office attendance planner and tracker designed to help you stay on top of your work-from-office goals.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("Developed by djaycse.", style = MaterialTheme.typography.bodySmall)
                        Text("Copyright © 2026", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }
}

@Composable
fun DashboardButtons(selectedAction: DashboardAction, onActionToggle: (DashboardAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DashboardButton("Set planned days", DashboardAction.SET_PLANNED, selectedAction, onActionToggle, Modifier.weight(1f))
            DashboardButton("Set actual days", DashboardAction.SET_ACTUAL, selectedAction, onActionToggle, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DashboardButton("Set WFH days", DashboardAction.SET_WFH, selectedAction, onActionToggle, Modifier.weight(1f))
            DashboardButton("Set holidays", DashboardAction.SET_HOLIDAY, selectedAction, onActionToggle, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DashboardButton("Set hours worked", DashboardAction.SET_HOURS, selectedAction, onActionToggle, Modifier.weight(1f))
            DashboardButton("Eraser", DashboardAction.ERASER, selectedAction, onActionToggle, Modifier.weight(1f))
        }
    }
}

@Composable
fun DashboardButton(label: String, action: DashboardAction, selectedAction: DashboardAction, onActionToggle: (DashboardAction) -> Unit, modifier: Modifier = Modifier) {
    val isSelected = action == selectedAction
    Button(
        onClick = { onActionToggle(action) },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
        ),
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        contentPadding = PaddingValues(4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}

@Composable
fun StatRow(label: String, value: Int, goal: Int, isPercent: Boolean) {
    val progress = (value.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
    val isMet = progress >= 1f
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(imageVector = if (isMet) Icons.Default.Check else Icons.Default.Close, contentDescription = null, tint = if (isMet) Color(0xFF2E7D32) else Color.Red, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(45.dp))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.weight(1f).height(4.dp).padding(horizontal = 4.dp), strokeCap = StrokeCap.Butt, trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        Text(text = if (isPercent) "$value%" else "$value", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(30.dp))
    }
}
