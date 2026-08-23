package net.qs.inoffice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.qs.inoffice.data.WorkDataStore
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    store: WorkDataStore,
    onBack: () -> Unit
) {
    val workData by store.workMap.collectAsState(initial = emptyMap())

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
            MonthlyStatisticsSection(workData)
            YearlyStatisticsSection(workData)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun MonthlyStatisticsSection(workData: Map<String, DayState>) {
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    var filter by remember { mutableStateOf(WorkFilter.ALL) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Monthly statistics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Month Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${
                        selectedMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }
                            .take(3)
                    } ${selectedMonth.year}",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selectedMonth = selectedMonth.minusMonths(1) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Prev"
                        )
                    }
                    IconButton(onClick = { selectedMonth = YearMonth.now() }) {
                        Icon(Icons.Default.Today, contentDescription = "Today")
                    }
                    IconButton(onClick = { selectedMonth = selectedMonth.plusMonths(1) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next"
                        )
                    }
                }
            }

            val stats = remember(selectedMonth, workData) {
                calculateDetailedStats(selectedMonth, workData)
            }

            val totalDayStr = if (stats.totalWorkingDays == 1) "day" else "days"
            val offDayStr = if (stats.daysOff == 1) "day" else "days"
            Text(
                text = "Worked ${stats.workedDays} $totalDayStr. ${stats.daysOff} $offDayStr off.",
                style = MaterialTheme.typography.bodyMedium
            )

            val inOfficeDayStr = if (stats.inOfficeDays == 1) "day" else "days"
            val teamHubDayStr = if (stats.teamHubDays == 1) "day" else "days"
            Text(
                text = "${stats.inOfficeDays} $inOfficeDayStr in an office (${stats.inOfficePercent}%). ${stats.teamHubDays} $teamHubDayStr at team hub.",
                style = MaterialTheme.typography.bodyMedium
            )

            // Days List
            val filteredDays = remember(selectedMonth, workData, filter) {
                getFilteredDays(selectedMonth, workData, filter)
            }

            Column(modifier = Modifier.fillMaxWidth()) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Working days",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    // Filter Dropdown
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            WorkFilter.entries.forEach { f ->
                                DropdownMenuItem(
                                    text = { Text(f.label) },
                                    onClick = {
                                        filter = f
                                        expanded = false
                                    },
                                    trailingIcon = {
                                        if (f == filter) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                filteredDays.forEach { day ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${day.date.format(DateTimeFormatter.ofPattern("EEE dd MMM"))} - ${day.locationLabel}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${String.format(Locale.getDefault(), "%5.1f", day.hours)}h",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "TOTAL for ${
                            selectedMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }
                                .take(3)
                        } ${selectedMonth.year}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${
                            String.format(
                                Locale.getDefault(),
                                "%5.1f",
                                filteredDays.sumOf { it.hours })
                        }h",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun YearlyStatisticsSection(workData: Map<String, DayState>) {
    var selectedYear by remember { mutableIntStateOf(LocalDate.now().year) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Yearly statistics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Year Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$selectedYear",
                    style = MaterialTheme.typography.titleMedium
                )
                Row {
                    IconButton(onClick = { selectedYear-- }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Prev"
                        )
                    }
                    IconButton(onClick = { selectedYear = LocalDate.now().year }) {
                        Icon(Icons.Default.Today, contentDescription = "Today")
                    }
                    IconButton(onClick = { selectedYear++ }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next"
                        )
                    }
                }
            }

            val stats = remember(selectedYear, workData) {
                calculateYearlyStats(selectedYear, workData)
            }

            val totalDayStr = if (stats.totalWorkingDays == 1) "day" else "days"
            val offDayStr = if (stats.daysOff == 1) "day" else "days"
            Text(
                text = "Worked ${stats.workedDays} $totalDayStr. ${stats.daysOff} $offDayStr off.",
                style = MaterialTheme.typography.bodyMedium
            )

            val inOfficeDayStr = if (stats.inOfficeDays == 1) "day" else "days"
            val teamHubDayStr = if (stats.teamHubDays == 1) "day" else "days"
            Text(
                text = "${stats.inOfficeDays} $inOfficeDayStr in an office. ${stats.teamHubDays} $teamHubDayStr at team hub.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun calculateDetailedStats(month: YearMonth, data: Map<String, DayState>): DetailedStats {
    val daysInMonth = month.lengthOfMonth()
    val today = LocalDate.now()

    // For "Worked x of y days (z days off)"
    var workedDaysCount = 0
    var totalWeekdaysCount = 0
    var daysOffCount = 0

    // For matching main app stats
    var teamHubDaysCount = 0
    var inOfficeDaysCount = 0
    var actualNonWfhWeekdays = 0
    var totalWorkingWeekdays = 0

    for (day in 1..daysInMonth) {
        val date = month.atDay(day)
        if (date.isAfter(today)) continue

        val isWeekday = date.dayOfWeek.value in 1..5
        val key = "%d-%02d-%02d".format(month.year, month.monthValue, day)
        val state = data[key] ?: DayState()

        val isOffice = state.actual == WorkLocation.BASE || state.actual == WorkLocation.OTHER

        // Worked days count (x)
        if (isOffice) {
            workedDaysCount++
            inOfficeDaysCount++
        } else if (isWeekday && state.planned != WorkLocation.LEAVE) {
            if (state.actual == WorkLocation.HOME) workedDaysCount++
        }

        // y and z
        if (isWeekday) {
            totalWeekdaysCount++
            if (state.planned == WorkLocation.LEAVE) daysOffCount++
        }

        // Main app matching stats
        if (state.actual == WorkLocation.BASE) teamHubDaysCount++

        if (isWeekday && state.planned != WorkLocation.LEAVE) {
            totalWorkingWeekdays++
            if (state.actual != WorkLocation.HOME && state.actual != WorkLocation.LEAVE) {
                actualNonWfhWeekdays++
            }
        }
    }

    val inOfficePercent = if (totalWorkingWeekdays == 0) 0
    else ((actualNonWfhWeekdays * 100.0) / totalWorkingWeekdays).roundToInt()

    return DetailedStats(
        workedDays = workedDaysCount,
        totalWorkingDays = totalWeekdaysCount,
        daysOff = daysOffCount,
        inOfficePercent = inOfficePercent,
        inOfficeDays = inOfficeDaysCount,
        teamHubDays = teamHubDaysCount
    )
}

private fun calculateYearlyStats(year: Int, data: Map<String, DayState>): YearlyStats {
    var workedDays = 0
    var totalWorkingDays = 0
    var daysOff = 0
    var teamHubDays = 0
    var inOfficeDays = 0
    val today = LocalDate.now()

    // Find the first day of the year actually in an office
    var firstOfficeDay: LocalDate? = null
    outer@ for (m in 1..12) {
        val month = YearMonth.of(year, m)
        for (d in 1..month.lengthOfMonth()) {
            val date = month.atDay(d)
            if (date.isAfter(today)) break@outer
            val key = "%d-%02d-%02d".format(year, m, d)
            val state = data[key] ?: DayState()
            if (state.actual == WorkLocation.BASE || state.actual == WorkLocation.OTHER) {
                firstOfficeDay = date
                break@outer
            }
        }
    }

    if (firstOfficeDay == null) {
        return YearlyStats(0, 0, 0, 0, 0)
    }

    for (monthValue in 1..12) {
        val month = YearMonth.of(year, monthValue)
        val daysInMonth = month.lengthOfMonth()
        for (day in 1..daysInMonth) {
            val date = month.atDay(day)

            // Assume working days only start on the first day of the year that was actually in an office
            if (date.isBefore(firstOfficeDay)) continue

            if (date.isAfter(today)) continue

            val isWeekday = date.dayOfWeek.value in 1..5
            val key = "%d-%02d-%02d".format(year, monthValue, day)
            val state = data[key] ?: DayState()

            val isOffice = state.actual == WorkLocation.BASE || state.actual == WorkLocation.OTHER

            if (isOffice) {
                workedDays++
                inOfficeDays++
                if (state.actual == WorkLocation.BASE) teamHubDays++
            } else if (isWeekday && state.planned != WorkLocation.LEAVE) {
                if (state.actual == WorkLocation.HOME) workedDays++
            }

            if (isWeekday) {
                totalWorkingDays++
                if (state.planned == WorkLocation.LEAVE) daysOff++
            }
        }
    }

    return YearlyStats(workedDays, totalWorkingDays, daysOff, inOfficeDays, teamHubDays)
}

private fun getFilteredDays(
    month: YearMonth,
    data: Map<String, DayState>,
    filter: WorkFilter
): List<WorkedDayInfo> {
    val daysInMonth = month.lengthOfMonth()
    val result = mutableListOf<WorkedDayInfo>()

    for (day in 1..daysInMonth) {
        val key = "%d-%02d-%02d".format(month.year, month.monthValue, day)
        val state = data[key] ?: DayState()

        if (state.hours <= 0) continue

        val date = month.atDay(day)
        val locationLabel = when (state.actual) {
            WorkLocation.HOME -> "Work-from-home"
            WorkLocation.BASE -> "Team hub"
            WorkLocation.OTHER -> state.locationName ?: "Other office"
            WorkLocation.LEAVE -> "Leave"
        }

        val matchesFilter = when (filter) {
            WorkFilter.ALL -> true
            WorkFilter.TEAM_HUB -> state.actual == WorkLocation.BASE
            WorkFilter.OTHER_OFFICE -> state.actual == WorkLocation.OTHER
            WorkFilter.ANY_OFFICE -> state.actual == WorkLocation.BASE || state.actual == WorkLocation.OTHER
            WorkFilter.WFH -> state.actual == WorkLocation.HOME
        }

        if (matchesFilter) {
            result.add(WorkedDayInfo(date, locationLabel, state.hours))
        }
    }
    return result
}

data class DetailedStats(
    val workedDays: Int,
    val totalWorkingDays: Int,
    val daysOff: Int,
    val inOfficePercent: Int,
    val inOfficeDays: Int,
    val teamHubDays: Int
)

data class YearlyStats(
    val workedDays: Int,
    val totalWorkingDays: Int,
    val daysOff: Int,
    val inOfficeDays: Int,
    val teamHubDays: Int
)

data class WorkedDayInfo(
    val date: LocalDate,
    val locationLabel: String,
    val hours: Double
)

enum class WorkFilter(val label: String) {
    ALL("All working days"),
    TEAM_HUB("Team hub only"),
    OTHER_OFFICE("Other office only"),
    ANY_OFFICE("Any office"),
    WFH("Work-from-home")
}
