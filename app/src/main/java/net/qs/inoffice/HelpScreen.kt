package net.qs.inoffice

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("How to use") },
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
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("1. Set your office locations (Team hub and Other offices).")
                Text("2. Set auto-detect settings (WiFi and GPS polling).")
                Text("3. Set your attendance goals.")
                Text("4. Monitor statistics on the main screen.")

                Spacer(Modifier.height(8.dp))
                Text("Interacting with the Calendar", style = MaterialTheme.typography.titleMedium)
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))

                Text("5. Use the 'Tap mode' selector at the bottom of the screen to choose your interaction:")
                Text("- N/A: Safe mode, tapping does nothing.")
                Text("- Team hub / Other office (Plan): Toggle your planned office location.")
                Text("- Team hub / Other office (Actual): Toggle your actual office attendance.")
                Text("- Day off: Toggle leave/non-working days.")
                Text("- WFH: Quickly reset a day to 'Work from home'.")
                Text("- Set hours: Tap a date to record the number of hours worked.")

                Spacer(Modifier.height(8.dp))
                Text("6. Tap any date on the calendar to apply the selected mode.")
            }

            Text("Legend", style = MaterialTheme.typography.titleMedium)
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendItem(
                            color = colorFor(WorkLocation.BASE),
                            label = "Team hub",
                            isCircle = false
                        )
                        LegendItem(
                            color = colorFor(WorkLocation.OTHER),
                            label = "Other office",
                            isCircle = false
                        )
                        
                        val isDark = androidx.compose.foundation.isSystemInDarkTheme()
                        val dayOffColor = if (isDark) net.qs.inoffice.ui.theme.DayOffDark else net.qs.inoffice.ui.theme.DayOffLight
                        LegendItem(
                            color = dayOffColor,
                            label = "Day off",
                            isCircle = false
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendItem(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "Planned",
                            isCircle = true,
                            isBorder = false
                        )
                        LegendItem(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "Actual",
                            isCircle = true,
                            isBorder = true
                        )
                        LegendItem(
                            color = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFFBDBDBD) else Color(0xFF424242),
                            label = "Hours set",
                            isCircle = false,
                            isTriangle = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    isCircle: Boolean,
    isBorder: Boolean = false,
    isTriangle: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isTriangle) {
            Canvas(modifier = Modifier.size(12.dp)) {
                val path = Path().apply {
                    moveTo(size.width, 0f)
                    lineTo(size.width, size.height)
                    lineTo(0f, 0f)
                    close()
                }
                drawPath(path, color = color)
            }
        } else {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .then(
                        if (isCircle) {
                            if (isBorder) Modifier.border(2.dp, color, CircleShape)
                            else Modifier.background(color, CircleShape)
                        } else {
                            Modifier.background(color)
                        }
                    )
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
