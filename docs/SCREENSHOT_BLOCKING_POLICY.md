# Screenshot Blocking Policy - HASETApp

**Version:** 1.0  
**Date:** April 5, 2026  
**Purpose:** Document screenshot blocking implementation for privacy protection

---

## Overview

HASETApp implements **selective screenshot blocking** to protect user privacy while maintaining usability. Screenshots are blocked only in sensitive areas containing private or financial data, while public content remains shareable.

---

## Implementation

### Helper Class
`SensitiveActivityHelper.java` - Utility class for managing screenshot permissions

```java
// Block screenshots for sensitive activities
SensitiveActivityHelper.blockScreenshots(activity);

// Allow screenshots for public content
SensitiveActivityHelper.allowScreenshots(activity);
```

---

## Screenshot Blocking Policy

### SENSITIVE AREAS (Screenshots Blocked)

These areas contain private/financial data and screenshots are blocked:

| Screen | Activity/Component | Reason |
|--------|-------------------|--------|
| Payment | `PaymentActivity` | Financial data, card numbers |
| Chat | `ChatActivity` | Private conversations |
| Prescriptions | `PrescriptionDetailBottomSheet` | Medical information |
| Profile | `ProfileFragment` | Personal health info |

### PUBLIC AREAS (Screenshots Allowed)

These areas contain shareable content:

| Screen | Activity | Reason |
|--------|----------|--------|
| Articles | `ArticleActivity` | Shareable health content |
| Doctor Profiles | `DoctorDetailsActivity` | Public doctor information |
| Health Tips | Part of home feed | Educational content |
| Appointment Booking | `BookAppointmentActivity` | General booking info |
| Doctor Home | `DoctorHomeFragment` | Public dashboard |

---

## How It Works

### Android FLAG_SECURE
Uses `WindowManager.LayoutParams.FLAG_SECURE` to prevent:
- ✅ Screenshot capture
- ✅ Screen recording
- ✅ Preview in recent apps (on some devices)

### Implementation Example

```java
// In sensitive activity onCreate():
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    // Block screenshots for sensitive screen
    SensitiveActivityHelper.blockScreenshots(this);
    
    setContentView(R.layout.activity_payment);
    // ...
}

// For BottomSheetDialogFragment:
@Override
public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    if (getDialog() != null && getDialog().getWindow() != null) {
        getDialog().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }
}
```

---

## Why Selective Blocking?

### Benefits
- ✅ Protects sensitive medical/financial data
- ✅ Maintains usability for public content sharing
- ✅ Users can screenshot articles to share
- ✅ Users can screenshot doctor profiles for reference
- ✅ Better UX than app-wide blocking

### Previously (App-wide blocking)
- ❌ Users couldn't screenshot any content
- ❌ Inconvenient for sharing articles
- ❌ Frustrating for support/troubleshooting

---

## Files Modified

### New Files
- `SensitiveActivityHelper.java` - Helper class for selective blocking

### Activities Updated
- `PaymentActivity.java` - Added screenshot blocking
- `ChatActivity.java` - Added screenshot blocking

### Fragments/Components Updated
- `ProfileFragment.java` - Added screenshot blocking
- `PrescriptionDetailBottomSheet.java` - Added screenshot blocking

### Removed
- Global screenshot blocking from `HASETApplication.java`

---

## Testing

To verify screenshot blocking works correctly:

1. **Sensitive screens** - Try taking screenshot in Payment/Chat/Profile
   - Should be blocked

2. **Public screens** - Try taking screenshot in Articles/Doctor Profile
   - Should work normally

3. **Screen recording** - Try recording in sensitive screens
   - Should be blocked

---

## Security Notes

- FLAG_SECURE is not foolproof - determined users can bypass with ADB
- For maximum security, consider additional measures:
  - Data encryption
  - Secure storage for sensitive data
  - Certificate pinning for API calls

---

## Changelog

| Date | Change |
|------|--------|
| Apr 5, 2026 | Implemented selective screenshot blocking |
| Apr 5, 2026 | Documented policy |

---

# Notification Badge Behavior - HASETApp

**Version:** 1.0  
**Date:** April 5, 2026  
**Purpose:** Document non-intrusive notification badge implementation

---

## Overview

The notification badge system is designed to be **non-intrusive** - it only shows NEW notifications since the user's last app session, and automatically clears when the app opens.

### User Experience Goals

| Before (Annoying) | After (Non-Intrusive) |
|-------------------|----------------------|
| Badge always shows total unread count | Badge shows only NEW notifications since last open |
| User sees old notifications every time | Clears automatically when app opens |
| "You have 47 unread messages" (weeks old) | "Fresh" feeling - only new stuff shows |

---

## How It Works

### Option 1: Only NEW Notifications

The badge tracks `new_notifications_since_last_open` - a counter that:
- Increments when new notifications arrive while app is open
- Resets to 0 when app is opened
- Shows only notifications from the current session

### Option 2: Auto-Clear on App Open

When `DashboardActivity.onCreate()` runs:
1. `badgeHelper.onAppOpened()` is called
2. `last_app_open_timestamp` is updated
3. `new_notifications_since_last_open` is reset to 0
4. Badge disappears (unless new notifications arrive after this)

---

## Implementation

### Key Methods in NotificationBadgeHelper

```java
// Called when app opens - clears badge for fresh start
public void onAppOpened() {
    long currentTime = System.currentTimeMillis();
    preferences.edit()
        .putLong(LAST_APP_OPEN_KEY, currentTime)
        .putInt(NEW_NOTIFICATIONS_COUNT_KEY, 0)
        .apply();
}

// Called when new notification arrives
public void incrementNewNotifications() {
    int current = preferences.getInt(NEW_NOTIFICATIONS_COUNT_KEY, 0);
    preferences.edit().putInt(NEW_NOTIFICATIONS_COUNT_KEY, current + 1).apply();
}

// Get only NEW notifications since last open
public int getNewNotificationsSinceLastOpen() {
    return preferences.getInt(NEW_NOTIFICATIONS_COUNT_KEY, 0);
}
```

### Where It's Called

```java
// DashboardActivity.java - onCreate()
NotificationBadgeHelper badgeHelper = new NotificationBadgeHelper(this);
badgeHelper.onAppOpened();
```

---

## Badge Behavior Matrix

| Scenario | Badge Shows | Action |
|----------|-------------|--------|
| App opens | 0 (cleared) | Auto-clear on open |
| New notification arrives (app open) | +1 | Increment counter |
| Multiple notifications arrive | Counter increases | Shows total new |
| User opens notification activity | 0 | Marked as read |
| User switches apps | Same count | Preserved |
| User returns to app later | Only new since return | Filtered by timestamp |

---

## Files Modified

### Helper Class
- `NotificationBadgeHelper.java` - Added session tracking methods:
  - `onAppOpened()`
  - `getNewNotificationsSinceLastOpen()`
  - `incrementNewNotifications()`
  - `getLastAppOpenTime()`
  - `shouldShowBadge()`

### Activities
- `DashboardActivity.java` - Calls `onAppOpened()` in `onCreate()`

### ViewModels
- `HomeViewModel.java` - Uses `getNewNotificationsSinceLastOpen()`
- `DoctorHomeViewModel.java` - Uses `getNewNotificationsSinceLastOpen()`

---

## Why This Design?

### Problems Solved
- ❌ Old notifications cluttering badge
- ❌ User confusion about "unread" count
- ❌ Annoying to see same notifications repeatedly

### Benefits
- ✅ Fresh start each app session
- ✅ Only new content gets attention
- ✅ User control over what "new" means
- ✅ Cleaner, less cluttered UI

---

## Changelog

| Date | Change |
|------|--------|
| Apr 5, 2026 | Implemented non-intrusive badge system |
| Apr 5, 2026 | Added session-based notification tracking |
| Apr 5, 2026 | Auto-clear badge on app open |

---

**End of Document**
