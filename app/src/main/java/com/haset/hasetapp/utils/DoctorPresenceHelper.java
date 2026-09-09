package com.haset.hasetapp.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.haset.hasetapp.HASETApplication;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Real doctor presence for Instant Chat:
 * - sticky "I want to be available" toggle still exists (persisted locally)
 * - patients only treat a doctor as online if lastSeenAt is fresh
 * - heartbeat refreshes lastSeenAt while the doctor app is in the foreground
 *   (any screen — not only Doctor Home)
 * - onDisconnect flips offline if the app/process dies
 */
public final class DoctorPresenceHelper {
    private static final String TAG = "DoctorPresence";
    /** Tolerate small device/server clock skew when comparing lastSeenAt. */
    private static final long CLOCK_SKEW_TOLERANCE_MS = 15_000L;

    private static final DoctorPresenceHelper INSTANCE = new DoctorPresenceHelper();

    @Nullable private Handler heartbeatHandler;
    private final Runnable heartbeatRunnable = this::heartbeatTick;

    @Nullable private String activeDoctorId;
    private boolean wantsOnline;
    private boolean heartbeatRunning;

    private DoctorPresenceHelper() {}

    public static DoctorPresenceHelper getInstance() {
        return INSTANCE;
    }

    private Handler handler() {
        if (heartbeatHandler == null) {
            heartbeatHandler = new Handler(Looper.getMainLooper());
        }
        return heartbeatHandler;
    }

    /** Doctor explicitly toggled available. */
    public void goOnline(String doctorId) {
        if (doctorId == null || doctorId.trim().isEmpty()) return;
        activeDoctorId = doctorId;
        wantsOnline = true;
        persistWantsOnline(true);
        DatabaseReference ref = doctorRef(doctorId);
        ref.onDisconnect().updateChildren(offlinePayload());
        ref.updateChildren(onlinePayload())
                .addOnFailureListener(e -> Log.w(TAG, "goOnline failed: " + e.getMessage()));
        startHeartbeat();
    }

    /** Doctor explicitly toggled unavailable. */
    public void goOffline(String doctorId) {
        if (doctorId == null || doctorId.trim().isEmpty()) return;
        activeDoctorId = doctorId;
        wantsOnline = false;
        persistWantsOnline(false);
        stopHeartbeat();
        DatabaseReference ref = doctorRef(doctorId);
        ref.onDisconnect().cancel();
        ref.updateChildren(offlinePayload())
                .addOnFailureListener(e -> Log.w(TAG, "goOffline failed: " + e.getMessage()));
    }

    /**
     * App entered foreground. Restores presence if the doctor left availability ON
     * (from memory or SharedPreferences).
     */
    public void onAppForeground(@Nullable String doctorId) {
        if (doctorId == null || doctorId.trim().isEmpty()) return;
        activeDoctorId = doctorId;
        if (!wantsOnline) {
            wantsOnline = readPersistedWantsOnline();
        }
        if (wantsOnline) {
            goOnline(doctorId);
        }
    }

    /** App left foreground — stop heartbeats; freshness expires after the timeout window. */
    public void onAppBackground() {
        stopHeartbeat();
        if (wantsOnline && activeDoctorId != null) {
            touchLastSeen(activeDoctorId);
        }
    }

    /** @deprecated Presence is app-scoped; fragment pause must not stop heartbeats. */
    @Deprecated
    public void onForeground(String doctorId, boolean currentlyWantsOnline) {
        if (doctorId == null || doctorId.trim().isEmpty()) return;
        activeDoctorId = doctorId;
        wantsOnline = currentlyWantsOnline;
        persistWantsOnline(currentlyWantsOnline);
        if (wantsOnline) {
            goOnline(doctorId);
        }
    }

    /** @deprecated Use {@link #onAppBackground()} — do not stop presence on fragment pause. */
    @Deprecated
    public void onBackground() {
        // No-op: fragment pause must not stop app-scoped heartbeats.
    }

    public void stop() {
        wantsOnline = false;
        persistWantsOnline(false);
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
        // Allow small negative age (server clock ahead of device).
        return age >= -CLOCK_SKEW_TOLERANCE_MS && age <= Constants.DOCTOR_PRESENCE_TIMEOUT_MS;
    }

    private void startHeartbeat() {
        stopHeartbeat();
        heartbeatRunning = true;
        handler().postDelayed(heartbeatRunnable, Constants.DOCTOR_PRESENCE_HEARTBEAT_MS);
    }

    private void stopHeartbeat() {
        heartbeatRunning = false;
        if (heartbeatHandler != null) {
            heartbeatHandler.removeCallbacks(heartbeatRunnable);
        }
    }

    private void heartbeatTick() {
        if (!heartbeatRunning || !wantsOnline || activeDoctorId == null) return;
        touchLastSeen(activeDoctorId);
        handler().postDelayed(heartbeatRunnable, Constants.DOCTOR_PRESENCE_HEARTBEAT_MS);
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

    private void persistWantsOnline(boolean value) {
        Context context = safeAppContext();
        if (context == null) return;
        new PreferenceManager(context).setDoctorWantsOnline(value);
    }

    private boolean readPersistedWantsOnline() {
        Context context = safeAppContext();
        if (context == null) return false;
        return new PreferenceManager(context).getDoctorWantsOnline();
    }

    @Nullable
    private static Context safeAppContext() {
        try {
            return HASETApplication.getAppContext();
        } catch (Throwable t) {
            return null;
        }
    }
}
