package com.haset.hasetapp.utils;

import android.util.Log;
import android.app.ActivityManager;
import android.content.Context;

/**
 * Memory monitoring utility for tracking app memory usage and performance.
 * Provides real-time memory statistics and leak detection.
 */
public class MemoryMonitor {
    private static final String TAG = "MemoryMonitor";
    private static final long MEMORY_LOG_INTERVAL = 30_000; // 30 seconds
    
    private static long lastLogTime = 0;
    
    /**
     * Logs current memory usage statistics
     * @param context Description of where this is called from (e.g., "PatientHomeFragment")
     */
    public static void logMemoryUsage(String context) {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        
        double usedPercentage = (double) usedMemory / maxMemory * 100;
        double totalPercentage = (double) totalMemory / maxMemory * 100;
        
        Log.d(TAG, String.format(
            "[%s] Memory: Used=%dMB (%.1f%%), Total=%dMB (%.1f%%), Max=%dMB",
            context,
            usedMemory / 1024 / 1024,
            usedPercentage,
            totalMemory / 1024 / 1024,
            totalPercentage,
            maxMemory / 1024 / 1024
        ));
        
        // Warning if memory usage is high
        if (usedPercentage > 80) {
            Log.w(TAG, "⚠️ High memory usage detected in " + context + ": " + 
                  String.format("%.1f%%", usedPercentage));
        }
    }
    
    /**
     * Logs memory usage at intervals to avoid spam
     * @param context Description of where this is called from
     */
    public static void logMemoryUsageThrottled(String context) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLogTime > MEMORY_LOG_INTERVAL) {
            logMemoryUsage(context);
            lastLogTime = currentTime;
        }
    }
    
    /**
     * Gets detailed memory information
     */
    public static MemoryInfo getMemoryInfo(Context context) {
        Runtime runtime = Runtime.getRuntime();
        ActivityManager activityManager = (ActivityManager) 
            context.getSystemService(Context.ACTIVITY_SERVICE);
        
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        
        return new MemoryInfo(
            runtime.totalMemory() - runtime.freeMemory(), // Used
            runtime.totalMemory(), // Total
            runtime.maxMemory(), // Max
            memoryInfo.availMem, // System available
            memoryInfo.totalMem // System total
        );
    }
    
    /**
     * Checks if the app is running low on memory
     */
    public static boolean isLowMemory(Context context) {
        ActivityManager activityManager = (ActivityManager) 
            context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        
        return memoryInfo.lowMemory || 
               (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) 
               > (Runtime.getRuntime().maxMemory() * 0.85);
    }
    
    /**
     * Memory information data class
     */
    public static class MemoryInfo {
        public final long usedMemory;
        public final long totalMemory;
        public final long maxMemory;
        public final long systemAvailable;
        public final long systemTotal;
        
        public MemoryInfo(long usedMemory, long totalMemory, long maxMemory, 
                         long systemAvailable, long systemTotal) {
            this.usedMemory = usedMemory;
            this.totalMemory = totalMemory;
            this.maxMemory = maxMemory;
            this.systemAvailable = systemAvailable;
            this.systemTotal = systemTotal;
        }
        
        public double getUsedPercentage() {
            return (double) usedMemory / maxMemory * 100;
        }
        
        public double getTotalPercentage() {
            return (double) totalMemory / maxMemory * 100;
        }
        
        @Override
        public String toString() {
            return String.format(
                "App Memory: %d/%dMB (%.1f%%), System: %dMB available",
                usedMemory / 1024 / 1024,
                maxMemory / 1024 / 1024,
                getUsedPercentage(),
                systemAvailable / 1024 / 1024
            );
        }
    }
}
