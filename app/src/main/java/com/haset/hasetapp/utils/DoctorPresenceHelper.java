package com.haset.hasetapp.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Real doctor presence for Instant Chat:
 * - sticky "I want to be available" toggle still exists
 * - patients only treat a doctor as online if lastSeenAt is fresh
 * - heartbeat refreshes lastSeenAt while the doctor app is in foreground
 * - onDisconnect flips offline if the app/process dies
 */
public final class DoctorPresenceHelper {
    private static final String TAG = "DoctorPresence";

    private static final DoctorPresenceHelper INSTANCE = new DoctorPresenceHelper();

    private final Handler heartbeatHandler = new Handler(Looper.getMainLooper());
    private final Runnable heartbeatRunnable = this::heartbeatTick;

    @Nullable private String activeDoctorId;
    private boolean wantsOnline;
    private boolean heartbeatRunning;

    private DoctorPresenceHelper() {}

    public static DoctorPresenceHelper getInstance() {
        return INSTANCE;
    }

    /** Doctor explicitly toggled available. */
    public void goOnline(String doctorId) {
        if (doctorId == null || doctorId.trim().isEmpty()) return;
        activeDoctorId = doctorId;
        wantsOnline = true;
        DatabaseReference ref = doctorRef(doctorId);
        Map<String, Object> offlineOnDisconnect = offlinePayload();
        ref.onDisconnect().updateChildren(offlineOnDisconnect);
        ref.updateChildren(onlinePayload())
                .addOnFailureListener(e -> Log.w(TAG, "goOnline failed: " + e.getMessage()));
        startHeartbeat();
    }

    /** Doctor explicitly toggled unavailable. */
    public void goOffline(String doctorId) {
        if (doctorId == null || doctorId.trim().isEmpty()) return;
        activeDoctorId = doctorId;
        wantsOnline = false;
        stopHeartbeat();
        DatabaseReference ref = doctorRef(doctorId);
        ref.onDisconnect().cancel();
        ref.updateChildren(offlinePayload())
                .addOnFailureListener(e -> Log.w(TAG, "goOffline failed: " + e.getMessage()));
    }

    /** Call from DoctorHomeFragment.onResume when the doctor intends to stay available. */
    public void onForeground(String doctorId, boolean currentlyWantsOnline) {
        if (doctorId == null || doctorId.trim().isEmpty()) return;
        activeDoctorId = doctorId;
        wantsOnline = currentlyWantsOnline;
        if (wantsOnline) {
            goOnline(doctorId);
        }
    }

    /** Call from DoctorHomeFragment.onPause — stop heartbeats; freshness will expire. */
    public void onBackground() {
        stopHeartbeat();
        // Keep onDisconnect armed so force-kill still marks offline.
        if (wantsOnline && activeDoctorId != null) {
            touchLastSeen(activeDoctorId);
        }
    }

    public void stop() {
        wantsOnline = false;
        stopHeartbeat();
        if (activeDoctorId != null) {
            doctorRef(activeDoctorId).onDisconnect().cancel();
        }
        activeDoctorId = null;
    }

    public static boolean isEffectivelyOnline(boolean onlineFlag,
                                              @Nullable String onlineStatus,
                                              long lastSeenAtMs) {
        if (!onlineFlag) return false;
        if (onlineStatus != null
                && !"online".equalsIgnoreCase(onlineStatus)
                && !"busy".equalsIgnoreCase(onlineStatus)) {
            return false;
        }
        if (lastSeenAtMs <= 0L) return false;
        long age = System.currentTimeMillis() - lastSeenAtMs;
        return age >= 0 && age <= Constants.DOCTOR_PRESENCE_TIMEOUT_MS;
    }

    private void startHeartbeat() {
        stopHeartbeat();
        heartbeatRunning = true;
        heartbeatHandler.postDelayed(heartbeatRunnable, Constants.DOCTOR_PRESENCE_HEARTBEAT_MS);
    }

    private void stopHeartbeat() {
        heartbeatRunning = false;
        heartbeatHandler.removeCallbacks(heartbeatRunnable);
    }

    private void heartbeatTick() {
        if (!heartbeatRunning || !wantsOnline || activeDoctorId == null) return;
        touchLastSeen(activeDoctorId);
        heartbeatHandler.postDelayed(heartbeatRunnable, Constants.DOCTOR_PRESENCE_HEARTBEAT_MS);
    }

    private void touchLastSeen(String doctorId) {
        Map<String, Object> touch = new HashMap<>();
        touch.put("lastSeenAt", ServerValue.TIMESTAMP);
        touch.put("online", true);
        touch.put("onlineStatus", "online");
        doctorRef(doctorId).updateChildren(touch)
                .addOnFailureListener(e -> Log.w(TAG, "heartbeat failed: " + e.getMessage()));
    }

    private static DatabaseReference doctorRef(String doctorId) {
        return FirebaseHelper.getDoctorsNodeRef().child(doctorId);
    }

    private static Map<String, Object> onlinePayload() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("online", true);
        updates.put("onlineStatus", "online");
        updates.put("lastSeenAt", ServerValue.TIMESTAMP);
        updates.put("lastUpdated", ServerValue.TIMESTAMP);
        return updates;
    }

    private static Map<String, Object> offlinePayload() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("online", false);
        updates.put("onlineStatus", "offline");
        updates.put("lastSeenAt", ServerValue.TIMESTAMP);
        updates.put("lastUpdated", ServerValue.TIMESTAMP);
        return updates;
    }
}
