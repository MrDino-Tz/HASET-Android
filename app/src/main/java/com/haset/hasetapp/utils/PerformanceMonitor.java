package com.haset.hasetapp.utils;

import android.util.Log;
import android.view.Choreographer;
import android.view.FrameMetrics;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Performance monitoring utility for tracking frame rates and animation performance.
 * Provides real-time FPS monitoring and performance metrics.
 */
public class PerformanceMonitor {
    private static final String TAG = "PerformanceMonitor";
    private static final long MONITOR_INTERVAL = 2000; // 2 seconds
    private static final int FPS_HISTORY_SIZE = 30; // Keep last 30 measurements
    
    private static long lastFrameTime = 0;
    private static int frameCount = 0;
    private static final List<Long> fpsHistory = new ArrayList<>();
    private static boolean isMonitoring = false;
    
    // Frame callback for FPS monitoring
    private static final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            if (lastFrameTime > 0) {
                long frameDuration = frameTimeNanos - lastFrameTime;
                double fps = 1_000_000_000.0 / frameDuration;
                
                frameCount++;
                
                // Log FPS every 60 frames to avoid spam
                if (frameCount % 60 == 0) {
                    fpsHistory.add((long) fps);
                    if (fpsHistory.size() > FPS_HISTORY_SIZE) {
                        fpsHistory.remove(0);
                    }
                    
                    double avgFps = fpsHistory.stream()
                            .mapToLong(Long::longValue)
                            .average()
                            .orElse(60.0);
                    
                    Log.d(TAG, String.format("FPS: %.1f (Avg: %.1f)", fps, avgFps));
                    
                    // Warn if FPS is low
                    if (fps < 30) {
                        Log.w(TAG, "⚠️ Low FPS detected: " + String.format("%.1f", fps));
                    }
                }
            }
            lastFrameTime = frameTimeNanos;
            
            if (isMonitoring) {
                Choreographer.getInstance().postFrameCallback(this);
            }
        }
    };
    
    /**
     * Starts FPS monitoring
     */
    public static void startMonitoring() {
        if (!isMonitoring) {
            isMonitoring = true;
            frameCount = 0;
            lastFrameTime = 0;
            fpsHistory.clear();
            Choreographer.getInstance().postFrameCallback(frameCallback);
            Log.d(TAG, "Performance monitoring started");
        }
    }
    
    /**
     * Stops FPS monitoring
     */
    public static void stopMonitoring() {
        if (isMonitoring) {
            isMonitoring = false;
            Choreographer.getInstance().removeFrameCallback(frameCallback);
            Log.d(TAG, "Performance monitoring stopped");
            
            // Log final statistics
            if (!fpsHistory.isEmpty()) {
                double avgFps = fpsHistory.stream()
                        .mapToLong(Long::longValue)
                        .average()
                        .orElse(60.0);
                long minFps = fpsHistory.stream().mapToLong(Long::longValue).min().orElse(60);
                long maxFps = fpsHistory.stream().mapToLong(Long::longValue).max().orElse(60);
                
                Log.i(TAG, String.format("Performance Summary - Avg: %.1f FPS, Min: %d, Max: %d", 
                    avgFps, minFps, maxFps));
            }
        }
    }
    
    /**
     * Sets up frame metrics tracking for an activity
     */
    public static void setupFrameMetrics(@NonNull AppCompatActivity activity) {
        Window window = activity.getWindow();
        if (window != null) {
            window.addOnFrameMetricsAvailableListener(new Window.OnFrameMetricsAvailableListener() {
                @Override
                public void onFrameMetricsAvailable(Window window, FrameMetrics frameMetrics, int dropCountSinceLastInvocation) {
                    long totalDuration = frameMetrics.getMetric(FrameMetrics.TOTAL_DURATION);
                    long gpuDuration = frameMetrics.getMetric(FrameMetrics.GPU_DURATION);
                    long layoutDuration = frameMetrics.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION);
                    long drawDuration = frameMetrics.getMetric(FrameMetrics.DRAW_DURATION);
                    
                    // Convert nanoseconds to milliseconds
                    double totalMs = totalDuration / 1_000_000.0;
                    double gpuMs = gpuDuration / 1_000_000.0;
                    double layoutMs = layoutDuration / 1_000_000.0;
                    double drawMs = drawDuration / 1_000_000.0;
                    
                    // Log if frame takes too long (>16ms for 60fps)
                    if (totalMs > 16.0) {
                        Log.w(TAG, String.format(
                            "Slow frame detected - Total: %.2fms, GPU: %.2fms, Layout: %.2fms, Draw: %.2fms",
                            totalMs, gpuMs, layoutMs, drawMs
                        ));
                    }
                    
                    // Log dropped frames
                    if (dropCountSinceLastInvocation > 0) {
                        Log.w(TAG, "Dropped frames: " + dropCountSinceLastInvocation);
                    }
                }
            }, new android.os.Handler());
            
            Log.d(TAG, "Frame metrics tracking enabled for " + activity.getClass().getSimpleName());
        }
    }
    
    /**
     * Gets current performance statistics
     */
    public static PerformanceStats getStats() {
        if (fpsHistory.isEmpty()) {
            return new PerformanceStats(0, 0, 0, 0);
        }
        
        double avgFps = fpsHistory.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(60.0);
        long minFps = fpsHistory.stream().mapToLong(Long::longValue).min().orElse(60);
        long maxFps = fpsHistory.stream().mapToLong(Long::longValue).max().orElse(60);
        
        return new PerformanceStats(avgFps, minFps, maxFps, fpsHistory.size());
    }
    
    /**
     * Checks if performance is good
     */
    public static boolean isPerformanceGood() {
        PerformanceStats stats = getStats();
        return stats.averageFps >= 45 && stats.minFps >= 30;
    }
    
    /**
     * Performance statistics data class
     */
    public static class PerformanceStats {
        public final double averageFps;
        public final long minFps;
        public final long maxFps;
        public final int sampleCount;
        
        public PerformanceStats(double averageFps, long minFps, long maxFps, int sampleCount) {
            this.averageFps = averageFps;
            this.minFps = minFps;
            this.maxFps = maxFps;
            this.sampleCount = sampleCount;
        }
        
        @Override
        public String toString() {
            return String.format("FPS: Avg=%.1f, Min=%d, Max=%d, Samples=%d", 
                averageFps, minFps, maxFps, sampleCount);
        }
    }
    
    /**
     * Logs animation performance
     */
    public static void logAnimationPerformance(String animationName, long durationMs) {
        Log.d(TAG, String.format("Animation '%s' completed in %dms", animationName, durationMs));
        
        // Warn if animation is too slow
        if (durationMs > 500) {
            Log.w(TAG, "⚠️ Slow animation detected: " + animationName + " took " + durationMs + "ms");
        }
    }
    
    /**
     * Measures and logs the performance of a runnable operation
     */
    public static void measurePerformance(String operationName, Runnable operation) {
        long startTime = System.nanoTime();
        operation.run();
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;
        
        Log.d(TAG, String.format("Operation '%s' completed in %dms", operationName, durationMs));
        
        if (durationMs > 100) {
            Log.w(TAG, "⚠️ Slow operation detected: " + operationName + " took " + durationMs + "ms");
        }
    }
}
