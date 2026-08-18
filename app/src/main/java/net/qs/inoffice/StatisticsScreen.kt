package net.qs.inoffice

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    dayStates: Map<String, DayState>,
    onBack: () -> Unit
) {
    var selectedMonth by remember { mutableIntStateOf(LocalDate.now().monthValue) }
    var selectedYear by remember { mutableIntStateOf(LocalDate.now().year) }
    var selectedType by remember { mutableStateOf<WorkLocation?>(null) }
    
    val today = LocalDate.now()
    val initialFY = if (today.monthValue >= 7) {
        LocalDate.of(today.year, 7, 1) to LocalDate.of(today.year + 1, 6, 30)
    } else {
        LocalDate.of(today.year - 1, 7, 1) to LocalDate.of(today.year, 6, 30)
    }
    
    var startDate by remember { mutableStateOf(initialFY.first) }
    var endDate by remember { mutableStateOf(initialFY.second) }
    var rangeSelectedType by remember { mutableStateOf<WorkLocation?>(null) }
    
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
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
            // Filter Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Monthly Filter", style = MaterialTheme.typography.titleMedium)
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MonthDropdown(selectedMonth, onSelect = { selectedMonth = it }, Modifier.weight(1f))
                        YearDropdown(selectedYear, onSelect = { selectedYear = it }, Modifier.weight(1f))
                    }
                    
                    TypeDropdown(selectedType, onSelect = { selectedType = it })
                }
            }

            // Monthly Log Section
            val filteredEntries = remember(dayStates, selectedMonth, selectedYear, selectedType) {
                dayStates.entries.asSequence().filter { entry ->
                    val date = try { LocalDate.parse(entry.key) } catch(_: Exception) { return@filter false }
                    (date.monthValue == selectedMonth) && (date.year == selectedYear) &&
                            (selectedType == null || entry.value.planned == selectedType || entry.value.actual == selectedType) &&
                            (entry.value.workHours != null)
                }.sortedBy { it.key }.toList()
            }
            
            val monthlyTotalHours = filteredEntries.sumOf { it.value.workHours ?: 0.0 }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Log for ${Month.of(selectedMonth).getDisplayName(TextStyle.FULL, Locale.getDefault())} $selectedYear", style = MaterialTheme.typography.titleSmall)
                        Text("${monthlyTotalHours}h", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    if (filteredEntries.isEmpty()) {
                        Text("No entries for selected criteria", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        filteredEntries.forEach { entry ->
                            val date = LocalDate.parse(entry.key)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} (${entry.value.actual})")
                                Text("${entry.value.workHours}h")
                            }
                        }
                    }
                }
            }

            // Range Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Date Range Summary", style = MaterialTheme.typography.titleMedium)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { showStartDatePicker = true }) {
                            Text(startDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
                        }
                        Text(" to ")
                        TextButton(onClick = { showEndDatePicker = true }) {
                            Text(endDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
                        }
                    }

                    TypeDropdown(rangeSelectedType, onSelect = { rangeSelectedType = it })
                    
                    val rangeTotalHours = remember(dayStates, startDate, endDate, rangeSelectedType) {
                        dayStates.entries.asSequence().sumOf { entry ->
                            val date = try { LocalDate.parse(entry.key) } catch(_: Exception) { return@sumOf 0.0 }
                            if ((date.isAfter(startDate) || date.isEqual(startDate)) && 
                                (date.isBefore(endDate) || date.isEqual(endDate)) &&
                                (rangeSelectedType == null || entry.value.planned == rangeSelectedType || entry.value.actual == rangeSelectedType)) {
                                entry.value.workHours ?: 0.0
                            } else 0.0
                        }
                    }
                    
                    Text("Total for range: ${rangeTotalHours}h", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        startDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        endDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}

@Composable
fun MonthDropdown(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = Month.of(selected).getDisplayName(TextStyle.FULL, Locale.getDefault()),
            onValueChange = {},
            readOnly = true,
            label = { Text("Month") },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.clickable { expanded = true }) },
            modifier = Modifier.fillMaxWidth().clickable { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            (1..12).forEach { m ->
                DropdownMenuItem(
                    text = { Text(Month.of(m).getDisplayName(TextStyle.FULL, Locale.getDefault())) },
                    onClick = { onSelect(m); expanded = false }
                )
            }
        }
    }
}

@Composable
fun YearDropdown(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val years = (2020..2030).toList()
    Box(modifier) {
        OutlinedTextField(
            value = selected.toString(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Year") },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.clickable { expanded = true }) },
            modifier = Modifier.fillMaxWidth().clickable { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            years.forEach { y ->
                DropdownMenuItem(
                    text = { Text(y.toString()) },
                    onClick = { onSelect(y); expanded = false }
                )
            }
        }
    }
}

@Composable
fun TypeDropdown(selected: WorkLocation?, onSelect: (WorkLocation?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val types = listOf(null, WorkLocation.BASE, WorkLocation.OTHER, WorkLocation.WFH)
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = when(selected) {
                WorkLocation.BASE -> "Team hub"
                WorkLocation.OTHER -> "Other office"
                WorkLocation.WFH -> "WFH"
                else -> "All types"
            },
            onValueChange = {},
            readOnly = true,
            label = { Text("Type of day") },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.clickable { expanded = true }) },
            modifier = Modifier.fillMaxWidth().clickable { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            types.forEach { t ->
                DropdownMenuItem(
                    text = {
                        Text(when(t) {
                            WorkLocation.BASE -> "Team hub"
                            WorkLocation.OTHER -> "Other office"
                            WorkLocation.WFH -> "WFH"
                            else -> "All types"
                        })
                    },
                    onClick = { onSelect(t); expanded = false }
                )
            }
        }
    }
}
