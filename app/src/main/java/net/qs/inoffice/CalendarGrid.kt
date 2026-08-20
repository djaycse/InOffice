package net.qs.inoffice

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.qs.inoffice.data.WorkDataStore
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarGrid(
    month: YearMonth,
    dayStates: MutableMap<String, DayState>,
    scope: CoroutineScope,
    store: WorkDataStore,
    tapMode: TapMode,
    onSetHours: (String, DayState) -> Unit
) {

    val firstDay = month.atDay(1)
    val daysInMonth = month.lengthOfMonth()
    val startOffset = firstDay.dayOfWeek.value - 1

    val today = LocalDate.now()

    Column {

        val days = listOf("M", "T", "W", "T", "F", "S", "S")

        Row(Modifier.fillMaxWidth()) {
            days.forEach {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(2.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(4.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        val totalCells = startOffset + daysInMonth
        val rowsNeeded = kotlin.math.ceil(totalCells / 7.0).toInt()

        for (row in 0 until rowsNeeded) {
            Row(Modifier.fillMaxWidth()) {

                for (col in 0..6) {

                    val index = row * 7 + col
                    val dayNumber = index - startOffset + 1

                    val valid = dayNumber in 1..daysInMonth

                    val dateKey = if (valid)
                        "%d-%02d-%02d".format(month.year, month.monthValue, dayNumber)
                    else ""

                    val dayState = dayStates[dateKey] ?: DayState()

                    val isToday =
                        valid &&
                                dayNumber == today.dayOfMonth &&
                                month.year == today.year &&
                                month.monthValue == today.monthValue

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                    ) {

                        if (valid) {
                            val isWeekend = col == 5 || col == 6
                            val isLeave = dayState.planned == WorkLocation.LEAVE
                            val isDayOff = isWeekend || isLeave
                            val isDark = androidx.compose.foundation.isSystemInDarkTheme()
                            val dayOffColor =
                                if (isDark) net.qs.inoffice.ui.theme.DayOffDark else net.qs.inoffice.ui.theme.DayOffLight
                            val onDayOffColor = if (isDark) Color.White else Color.Black

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (isDayOff) dayOffColor
                                        else Color.Transparent
                                    )
                                    // Today highlight: full size themed border
                                    .border(
                                        width = if (isToday) 2.dp else 0.dp,
                                        color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                    .combinedClickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            when (tapMode) {
                                                TapMode.DO_NOTHING -> {}
                                                TapMode.TEAM_HUB_PLAN -> {
                                                    val next =
                                                        if (dayState.planned == WorkLocation.BASE) WorkLocation.HOME else WorkLocation.BASE
                                                    updateState(
                                                        dateKey,
                                                        dayState.copy(planned = next),
                                                        dayStates,
                                                        scope,
                                                        store
                                                    )
                                                }

                                                TapMode.OTHER_OFFICE_PLAN -> {
                                                    val next =
                                                        if (dayState.planned == WorkLocation.OTHER) WorkLocation.HOME else WorkLocation.OTHER
                                                    updateState(
                                                        dateKey,
                                                        dayState.copy(planned = next),
                                                        dayStates,
                                                        scope,
                                                        store
                                                    )
                                                }

                                                TapMode.TEAM_HUB_ACTUAL -> {
                                                    val next =
                                                        if (dayState.actual == WorkLocation.BASE) WorkLocation.HOME else WorkLocation.BASE
                                                    updateState(
                                                        dateKey,
                                                        dayState.copy(actual = next),
                                                        dayStates,
                                                        scope,
                                                        store
                                                    )
                                                }

                                                TapMode.OTHER_OFFICE_ACTUAL -> {
                                                    val next =
                                                        if (dayState.actual == WorkLocation.OTHER) WorkLocation.HOME else WorkLocation.OTHER
                                                    updateState(
                                                        dateKey,
                                                        dayState.copy(actual = next),
                                                        dayStates,
                                                        scope,
                                                        store
                                                    )
                                                }

                                                TapMode.DAY_OFF -> {
                                                    val next =
                                                        if (dayState.planned == WorkLocation.LEAVE) WorkLocation.HOME else WorkLocation.LEAVE
                                                    updateState(
                                                        dateKey,
                                                        dayState.copy(planned = next),
                                                        dayStates,
                                                        scope,
                                                        store
                                                    )
                                                }

                                                TapMode.WFH -> {
                                                    updateState(
                                                        dateKey,
                                                        dayState.copy(
                                                            planned = WorkLocation.HOME,
                                                            actual = WorkLocation.HOME
                                                        ),
                                                        dayStates,
                                                        scope,
                                                        store
                                                    )
                                                }

                                                TapMode.SET_HOURS -> {
                                                    onSetHours(dateKey, dayState)
                                                }
                                            }
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                // Planned state: Filled circle (Inner)
                                if (dayState.planned != WorkLocation.HOME && dayState.planned != WorkLocation.LEAVE) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(if (isToday) 10.dp else 8.dp) // Smaller inner circle
                                            .background(colorFor(dayState.planned), CircleShape)
                                    )
                                }

                                // Actual state: Circle border (Outer)
                                if (dayState.actual != WorkLocation.HOME && dayState.actual != WorkLocation.LEAVE) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(if (isToday) 4.dp else 2.dp) // Outer circle
                                            .border(3.dp, colorFor(dayState.actual), CircleShape)
                                    )
                                }

                                Text(
                                    text = dayNumber.toString(),
                                    color = if (isDayOff) onDayOffColor
                                    else if (dayState.planned == WorkLocation.HOME) MaterialTheme.colorScheme.onSurface
                                    else Color.Black
                                )

                                if (dayState.hours > 0) {
                                    val notchColor =
                                        if (isDark) Color(0xFFBDBDBD) else Color(0xFF424242)
                                    Canvas(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(8.dp)
                                    ) {
                                        val path = Path().apply {
                                            moveTo(size.width, 0f)
                                            lineTo(size.width, size.height)
                                            lineTo(0f, 0f)
                                            close()
                                        }
                                        drawPath(path, color = notchColor)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun updateState(
    dateKey: String,
    newState: DayState,
    dayStates: MutableMap<String, DayState>,
    scope: CoroutineScope,
    store: WorkDataStore
) {
    if (newState.planned == WorkLocation.HOME && newState.actual == WorkLocation.HOME) {
        dayStates.remove(dateKey)
    } else {
        dayStates[dateKey] = newState
    }

    scope.launch {
        if (newState.planned == WorkLocation.HOME && newState.actual == WorkLocation.HOME) {
            store.delete(dateKey)
        } else {
            store.save(dateKey, newState)
        }
    }
}
