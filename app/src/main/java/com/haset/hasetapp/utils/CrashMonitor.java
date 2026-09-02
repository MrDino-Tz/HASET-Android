package com.haset.hasetapp.utils;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

/**
 * Centralised Crashlytics helper for breadcrumbs, custom keys and non-fatal
 * reporting so crashes in hot areas (auth, payments, appointments) can be
 * traced to the screen/flow where they occurred.
 *
 * Addition to {@link ErrorLogger}: breadcrumbs record the user journey leading
 * up to a crash, while custom keys tag the report with the subsystem in play.
 */
public final class CrashMonitor {

    private static final String SCREEN_KEY = "screen";
    private static final String FLOW_KEY = "flow";

    private CrashMonitor() {
    }

    /** Record a step in the user journey (visible on any crash in this session). */
    public static void breadcrumb(String message) {
        FirebaseCrashlytics.getInstance().log(message);
    }

    /** Tag the crash report with the subsystem being exercised. */
    public static void setFlow(String flow) {
        FirebaseCrashlytics.getInstance().setCustomKey(FLOW_KEY, flow == null ? "" : flow);
    }

    /** Tag the crash report with the screen/activity in play. */
    public static void setScreen(String screen) {
        FirebaseCrashlytics.getInstance().setCustomKey(SCREEN_KEY, screen == null ? "" : screen);
    }

    /** Convenience: set flow + screen + record a step in one call. */
    public static void step(String flow, String screen, String message) {
        setFlow(flow);
        setScreen(screen);
        breadcrumb(message);
    }

    /** Report a handled failure as a non-fatal Crashlytics issue. */
    public static void report(String flow, String screen, String message, Throwable throwable) {
        setFlow(flow);
        setScreen(screen);
        breadcrumb(message);
        if (throwable != null) {
            FirebaseCrashlytics.getInstance().recordException(throwable);
        }
    }
}
