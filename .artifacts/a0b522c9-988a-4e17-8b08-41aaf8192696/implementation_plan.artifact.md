# Custom Statistics Section and Icon Update

Change the Yearly statistics section to a "Custom statistics" section with a date range picker and filterable working hours total. Also update the Statistics menu icon.

## User Review Required

> [!NOTE]
> The "Custom statistics" section will default to the current financial year (July 1st to June 30th).

## Proposed Changes

### App Navigation

#### [MODIFY] [InOfficeApp.kt](file:///C:/Users/kcbkm/AndroidStudioProjects/InOffice/app/src/main/java/net/qs/inoffice/InOfficeApp.kt)
- Change the "Statistics" menu item icon from `TrendingUp` to `TableChart`.

### Statistics Screen

#### [MODIFY] [StatisticsScreen.kt](file:///C:/Users/kcbkm/AndroidStudioProjects/InOffice/app/src/main/java/net/qs/inoffice/StatisticsScreen.kt)
- Rename "Yearly statistics" title to "Custom statistics".
- Implement date range selection using "From" and "To" dates with date pickers.
- Default the range to the current financial year.
- Update statistics calculation to work with the selected date range.
- Add "Working days" filter and total hours summary (without the list of individual days).
- Ensure date formatting respects the current locale.

## Verification Plan

### Automated Tests
- None planned as this is primarily a UI and logic update in Composables.

### Manual Verification
1. Open the app and verify the Statistics menu icon is now a table.
2. Navigate to Statistics.
3. Verify "Custom statistics" section exists.
4. Check that the default range is the current financial year.
5. Tap on "From" or "To" dates and verify the date picker opens and updates the date.
6. Verify the statistics (Worked days, Office days, etc.) update when the date range changes.
7. Use the "Working days" filter and verify the "TOTAL" hours value updates accordingly.
