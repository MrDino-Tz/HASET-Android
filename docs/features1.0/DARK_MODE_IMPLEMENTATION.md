# Dark Mode Implementation

## Overview
This document describes the dark mode implementation for the HASETApp. The implementation follows Material Design guidelines and provides users with three theme options:
1. Light Mode
2. Dark Mode
3. System Default (follows device settings)

## Implementation Details

### 1. Theme Resources
- **Light Theme**: Defined in `res/values/colors.xml` and `res/values/themes.xml`
- **Dark Theme**: Defined in `res/values-night/colors.xml` and `res/values-night/themes.xml`
- **Theme Attributes**: Custom attributes defined in `res/values/attrs.xml` for consistent styling across themes

### 2. Key Components

#### ThemeHelper Utility Class
Located at `utils/ThemeHelper.java`, this class provides:
- Theme application methods
- Theme detection methods
- Current theme retrieval

#### PreferenceManager Updates
The `PreferenceManager` class was extended to:
- Store theme preferences
- Provide theme constants (LIGHT, DARK, SYSTEM)
- Handle theme retrieval and storage

#### UI Updates
Layout files were updated to use theme attributes instead of hardcoded colors:
- Text colors use `?attr/colorPrimaryText` and `?attr/colorSecondaryText`
- Background colors use `?attr/colorBackground` and `?attr/colorCardBackground`
- Icon colors use `?attr/colorPrimary`

### 3. Theme Switching

#### Profile Fragment
Users can change themes through the Profile screen:
1. Navigate to Profile tab
2. Tap on "Theme" setting
3. Select preferred theme option
4. The app will automatically restart with the new theme

#### Automatic Theme Application
Themes are automatically applied at app startup in:
- SplashActivity
- LoginActivity
- DashboardActivity

### 4. Color Palette

#### Light Theme Colors
- Primary Text: `#1F2937`
- Secondary Text: `#6B7280`
- Background: `#F8F9FA`
- Card Background: `#FFFFFF`

#### Dark Theme Colors
- Primary Text: `#E0E0E0`
- Secondary Text: `#B0B0B0`
- Background: `#121212`
- Card Background: `#1E1E1E`

## Testing Dark Mode

### Manual Testing
1. Open the app
2. Navigate to Profile → Theme
3. Select "Dark" theme
4. Observe UI changes
5. Test different screens (Login, Dashboard, Appointments, etc.)

### System Theme Testing
1. Set device to dark mode
2. Select "System Default" in app
3. Verify app follows system theme
4. Toggle device theme and verify app updates

## Future Improvements

### Dynamic Theme Changes
Currently, theme changes require an app restart. Future improvements could include:
- Real-time theme switching without restart
- Smooth theme transition animations

### Additional Themes
Consider adding:
- True black theme for OLED screens
- Custom color themes based on user preferences

## Files Modified

### Java Files
- `utils/PreferenceManager.java` - Added theme preference handling
- `utils/ThemeHelper.java` - Created new utility class
- `HASETApplication.java` - Added theme initialization
- `activities/SplashActivity.java` - Added theme application
- `activities/LoginActivity.java` - Added theme application
- `activities/DashboardActivity.java` - Added theme application
- `fragments/ProfileFragment.java` - Added theme selection UI

### Resource Files
- `res/values/colors.xml` - Added light theme colors and attributes
- `res/values-night/colors.xml` - Added dark theme colors
- `res/values/themes.xml` - Updated light theme with attributes
- `res/values-night/themes.xml` - Updated dark theme with attributes
- `res/values/attrs.xml` - Created custom theme attributes
- Layout files updated to use theme attributes:
  - `layout/activity_login.xml`
  - `layout/activity_dashboard.xml`
  - `layout/item_appointment.xml`

## Usage Instructions

### For Developers
1. When creating new layouts, use theme attributes instead of hardcoded colors
2. Use `?attr/colorPrimaryText` for primary text
3. Use `?attr/colorSecondaryText` for secondary text
4. Use `?attr/colorBackground` for activity backgrounds
5. Use `?attr/colorCardBackground` for card backgrounds

### For Users
1. Open the app
2. Go to the Profile tab
3. Tap on "Theme" in the Settings section
4. Choose from Light, Dark, or System Default
5. The app will restart with the selected theme