package net.qs.inoffice

enum class WorkLocation {
    HOME,
    BASE,
    OTHER,
    LEAVE
}

enum class AppTheme(val label: String) {
    DEFAULT("Android default"),
    LIGHT("Light"),
    DARK("Dark");
    
    override fun toString(): String = label
}

enum class TapMode(val label: String, val shortLabel: String) {
    DO_NOTHING("Do nothing", "N/A"),
    TEAM_HUB_PLAN("Team hub (plan)", "Team hub"),
    OTHER_OFFICE_PLAN("Other office (plan)", "Other office"),
    TEAM_HUB_ACTUAL("Team hub (actual)", "Team hub"),
    OTHER_OFFICE_ACTUAL("Other office (actual)", "Other office"),
    DAY_OFF("Day off", "Day off"),
    WFH("Work-from-home", "WFH"),
    SET_HOURS("Set work hours", "Set hours")
}

data class DayState(
    val planned: WorkLocation = WorkLocation.HOME,
    val actual: WorkLocation = WorkLocation.HOME,
    val locationName: String? = null,
    val hours: Double = 0.0
)

data class MonthStats(
    val actualNonWfhPercent: Int,
    val actualBaseCount: Int,
    val plannedNonWfhPercent: Int,
    val plannedBaseCount: Int,
    val totalDays: Int
)

data class MonthInsights(
    val longestStreak: Int,
    val currentStreak: Int,
    val warningText: String?
)

data class OfficeLocation(
    val name: String,
    val lat: Double,
    val lng: Double,
    val type: WorkLocation
)
