# InOffice - Office Attendance Planner and Tracker

InOffice is a lightweight Android application designed to help employees track their office
attendance and ensure they meet organizational requirements. It provides a simple calendar interface
to log planned and actual work locations, automatically calculating key statistics such as in-office
percentage and "Team Hub" days.

## Key Features

- **Manual Logging**: Tap to set planned work locations and long-press to set actual attendance.
- **Visual Statistics**: Real-time tracking of office attendance vs. WFH requirements.
- **Auto-Detection**: Optional background scanning that detects office Wi-Fi networks and uses GPS
  to automatically log your attendance when you are at a configured office location.
- **Customizable Locations**: Configure up to 10 office locations with custom names and GPS
  coordinates.
- **Flexible Settings**: Adjust polling intervals and target Wi-Fi networks to suit your workplace
  environment.

---

## Changelog

### Version 1.2.0

#### New features

* Monthly/yearly statistics now available via new Statistics menu
* Import/Export of settings and calendar data now available via Settings menu

### Version 1.1.0

#### New features

* New tap 'mode' selection decides what tapping on a date will do. Tap again to restore previous
  state.
* Track hours worked per day, set via a new tap mode, and shown visually as a triangle notch on a
  date.
* Set exclusive light/dark/default application-wide theme

#### Changes

* Tap and hold on a date no longer sets actual office location. Use new tap 'mode' instead.
* Aligned Team hub and Other office colours with application icon colours:
    * Team hub changed from orange to green
    * Other office changed from green to blue
* UI tweaks to better handling of Android's larger display scaling and font settings:
    * Shortened calendar title and days of week
    * Removed legend from main screen and moved into How to use screen
    * Removed progress bars for plan and actual statistics
* Combined Offices, Auto-detect, and Goals menu items into a single Settings menu

#### Fixes

* Clock and other items on top of screen were being hidden with white background colour in light
  theme.

### Version 1.0.0

* First release