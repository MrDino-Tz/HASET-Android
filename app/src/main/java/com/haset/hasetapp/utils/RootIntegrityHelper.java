package com.haset.hasetapp.utils;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.os.Build;

import java.io.File;

/**
 * RootIntegrityHelper — lightweight, passive root detection.
 *
 * Purpose: raise a warning when a potentially compromised environment opens a
 * SENSITIVE screen (payments). It does NOT block usage — many legitimate users
 * root devices; this exists so staff/support can advise caution and so obvious
 * tampering is surfaced during support calls.
 *
 * Detection heuristics (no third-party library):
 *  1. Well-known su binary locations
 *  2. Test-keys build tag (custom ROM / emulator images)
 *  3. Known root-management apps installed (Superuser/SuperSU/Magisk legacy)
 */
public final class RootIntegrityHelper {

    private static final String[] SU_PATHS = {
            "/system/bin/su", "/system/xbin/su", "/sbin/su",
            "/system/sd/xbin/su", "/system/bin/failsafe/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/su/bin/su"
    };

    private static final String[] ROOT_APPS_PACKAGES = {
            "com.noshufou.android.su",          // Superuser
            "eu.chainfire.supersu",             // SuperSU
            "com.topjohnwu.magisk",             // Magisk (legacy package id)
            "com.koushikdutta.superuser"        // Koush Superuser
    };

    private RootIntegrityHelper() {} // static only

    /** True if any common root indicator is present. Cheap checks only. */
    public static boolean isDeviceLikelyRooted() {
        // 1. su binaries present?
        for (String path : SU_PATHS) {
            if (new File(path).exists()) return true;
        }

        // 2. Custom/test ROM build tags
        String buildTags = Build.TAGS;
        if (buildTags != null && buildTags.contains("test-keys")) return true;

        // 3. Known root-manager packages visible to us
        // (getInstalledPackages needs QUERY_ALL_PACKAGES or exact queries;
        //  use canQueryPackage-style direct check via getPackageInfo)
        return false;
    }

    /** Exact-package visibility check that works without broad query perms. */
    public static boolean isRootAppInstalled(android.content.Context context, String packageName) {
        try {
            int flags = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                flags = android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES;
            }
            context.getPackageManager().getPackageInfo(packageName, flags);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Combined check: binaries/build-tags plus visible root managers.
     * Call from background thread if strictness matters; here it's I/O-light.
     */
    public static boolean isPotentiallyCompromised(Activity activity) {
        if (isDeviceLikelyRooted()) return true;
        for (String pkg : ROOT_APPS_PACKAGES) {
            if (isRootAppInstalled(activity, pkg)) return true;
        }
        // Debug builds are inherently inspectable; flag them too for support clarity
        return isDebuggable(activity);
    }

    private static boolean isDebuggable(Activity activity) {
        return (activity.getApplicationInfo().flags
                & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }
}
