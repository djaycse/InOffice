package net.qs.inoffice

enum class WorkLocation {
    HOME,
    BASE,
    OTHER,
    LEAVE,
    WFH
}

data class DayState(
    val planned: WorkLocation = WorkLocation.HOME,
    val actual: WorkLocation = WorkLocation.HOME,
    val locationName: String? = null,
    val workHours: Double? = null,
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

enum class DashboardAction {
    NONE,
    SET_PLANNED,
    SET_ACTUAL,
    SET_WFH,
    SET_HOLIDAY,
    SET_HOURS,
    ERASER
}
