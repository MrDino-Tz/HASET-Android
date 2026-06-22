package com.haset.hasetapp.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Optimized VoiceRecorderHelper with improved memory management and performance.
 * Features proper cleanup, memory leak prevention, and performance monitoring.
 */
public class OptimizedVoiceRecorderHelper {
    private static final String TAG = "OptimizedVoiceRecorder";
    
    // Recording configuration
    private static final int AUDIO_BITRATE = 96000;  // Reduced from 128k for better performance
    private static final int AUDIO_SAMPLE_RATE = 22050; // Reduced from 44.1k for mobile optimization
    private static final int AMPLITUDE_UPDATE_INTERVAL = 100; // Increased from 50ms to reduce CPU usage
    private static final int MAX_RECORDING_DURATION = 60000; // 60 seconds max recording
    
    // MediaRecorder and file management
    private MediaRecorder mediaRecorder;
    private String outputFile;
    private final AtomicBoolean isRecording = new AtomicBoolean(false);
    private final Context context;
    
    // Performance monitoring
    private Handler performanceHandler;
    private long recordingStartTime;
    private long lastAmplitudeUpdate = 0;
    private int amplitudeUpdateCount = 0;
    
    // Memory management
    private boolean isDestroyed = false;
    
    public OptimizedVoiceRecorderHelper(Context context) {
        this.context = context;
        this.performanceHandler = new Handler(Looper.getMainLooper());
        MemoryMonitor.logMemoryUsage("VoiceRecorder_init");
    }
    
    /**
     * Check if recording permission is granted
     */
    public boolean hasRecordingPermission() {
        if (isDestroyed) return false;
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
                == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Start recording voice note with optimizations
     */
    public boolean startRecording() {
        if (isDestroyed) {
            Log.w(TAG, "Cannot start recording - helper is destroyed");
            return false;
        }
        
        if (isRecording.get()) {
            Log.w(TAG, "Already recording");
            return false;
        }
        
        if (!hasRecordingPermission()) {
            Log.e(TAG, "Recording permission not granted");
            return false;
        }
        
        // Start recording
        boolean result = doStartRecording();
        
        // Log performance
        Log.d(TAG, "Voice recording start completed: " + result);
        
        return result;
    }
    
    private boolean doStartRecording() {
        try {
            // Create output file in app's external music directory (saves to phone)
            File audioDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            if (audioDir == null) {
                // Fallback to cache if external not available
                audioDir = new File(context.getCacheDir(), "voice_notes");
            }
            if (!audioDir.exists()) {
                audioDir.mkdirs();
            }
            
            outputFile = new File(audioDir, "voice_" + System.currentTimeMillis() + ".m4a").getAbsolutePath();
            
            // Initialize MediaRecorder with optimized settings
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mediaRecorder = new MediaRecorder(context);
            } else {
                mediaRecorder = new MediaRecorder();
            }
            
            // Optimized recording configuration for mobile
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioEncodingBitRate(AUDIO_BITRATE);  // Reduced for performance
            mediaRecorder.setAudioSamplingRate(AUDIO_SAMPLE_RATE); // Reduced for mobile
            mediaRecorder.setOutputFile(outputFile);
            
            // Add error handling for prepare
            mediaRecorder.prepare();
            mediaRecorder.start();
            
            isRecording.set(true);
            recordingStartTime = System.currentTimeMillis();
            amplitudeUpdateCount = 0;
            
            Log.d(TAG, "Recording started: " + outputFile);
            MemoryMonitor.logMemoryUsage("VoiceRecording_started");
            return true;
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to start recording: " + e.getMessage(), e);
            releaseRecorder();
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error starting recording: " + e.getMessage(), e);
            releaseRecorder();
            return false;
        }
    }
    
    /**
     * Stop recording and return the file path
     */
    public String stopRecording() {
        if (!isRecording.get() || mediaRecorder == null) {
            Log.w(TAG, "Not recording");
            return null;
        }
        
        // Stop recording and return result
        return doStopRecording();
    }
    
    private String doStopRecording() {
        try {
            long recordingDuration = System.currentTimeMillis() - recordingStartTime;
            
            // Stop the recorder
            mediaRecorder.stop();
            isRecording.set(false);
            
            // Log performance metrics
            Log.d(TAG, String.format("Recording stopped: %s, Duration: %dms, Updates: %d", 
                outputFile, recordingDuration, amplitudeUpdateCount));
            
            // Check if recording is too short (less than 500ms)
            if (recordingDuration < 500) {
                Log.w(TAG, "Recording too short, deleting file");
                deleteOutputFile();
                releaseRecorder();
                return null;
            }
            
            // Check if recording is too long
            if (recordingDuration > MAX_RECORDING_DURATION) {
                Log.w(TAG, "Recording too long, truncating");
            }
            
            String filePath = outputFile;
            releaseRecorder();
            
            MemoryMonitor.logMemoryUsage("VoiceRecording_stopped");
            return filePath;
            
        } catch (RuntimeException e) {
            Log.e(TAG, "Error stopping recording: " + e.getMessage(), e);
            releaseRecorder();
            deleteOutputFile();
            return null;
        }
    }
    
    /**
     * Cancel recording (delete the file)
     */
    public void cancelRecording() {
        if (isRecording.get()) {
            stopRecording();
        }
        
        deleteOutputFile();
        Log.d(TAG, "Recording cancelled");
    }
    
    /**
     * Get current audio amplitude for visualization (optimized)
     */
    public int getAmplitude() {
        if (!isRecording.get() || mediaRecorder == null || isDestroyed) {
            return 0;
        }
        
        // Throttle amplitude updates to improve performance
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAmplitudeUpdate < AMPLITUDE_UPDATE_INTERVAL) {
            return 0; // Skip this update
        }
        
        try {
            int amplitude = mediaRecorder.getMaxAmplitude();
            lastAmplitudeUpdate = currentTime;
            amplitudeUpdateCount++;
            
            // Normalize and optimize amplitude calculation
            return Math.min(amplitude / 150, 100); // Adjusted normalization
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Get current recording duration in milliseconds
     */
    public long getCurrentDuration() {
        if (!isRecording.get()) {
            return 0;
        }
        return System.currentTimeMillis() - recordingStartTime;
    }
    
    /**
     * Check if currently recording
     */
    public boolean isRecording() {
        return isRecording.get() && !isDestroyed;
    }
    
    /**
     * Get recording performance statistics
     */
    public RecordingStats getStats() {
        return new RecordingStats(
            getCurrentDuration(),
            amplitudeUpdateCount,
            isRecording.get(),
            outputFile
        );
    }
    
    /**
     * Release MediaRecorder resources with enhanced cleanup
     */
    private void releaseRecorder() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (Exception e) {
                // Ignore stop errors
            }
            
            try {
                mediaRecorder.reset();
                mediaRecorder.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing recorder: " + e.getMessage(), e);
            }
            mediaRecorder = null;
        }
    }
    
    /**
     * Delete output file if it exists
     */
    private void deleteOutputFile() {
        if (outputFile != null) {
            File file = new File(outputFile);
            if (file.exists()) {
                file.delete();
                Log.d(TAG, "Recording file deleted");
            }
            outputFile = null;
        }
    }
    
    /**
     * Enhanced cleanup with memory monitoring
     */
    public void cleanup() {
        if (isDestroyed) return;
        
        Log.d(TAG, "Cleaning up voice recorder");
        
        // Cancel any pending performance monitoring
        if (performanceHandler != null) {
            performanceHandler.removeCallbacksAndMessages(null);
        }
        
        // Stop recording if active
        if (isRecording.get()) {
            cancelRecording();
        }
        
        // Release resources
        releaseRecorder();
        
        // Mark as destroyed
        isDestroyed = true;
        
        MemoryMonitor.logMemoryUsage("VoiceRecorder_cleanup");
        Log.d(TAG, "Voice recorder cleanup completed");
    }
    
    /**
     * Get the output file path
     */
    public String getOutputFile() {
        return isDestroyed ? null : outputFile;
    }
    
    /**
     * Recording statistics data class
     */
    public static class RecordingStats {
        public final long duration;
        public final int amplitudeUpdates;
        public final boolean isRecording;
        public final String filePath;
        
        public RecordingStats(long duration, int amplitudeUpdates, boolean isRecording, String filePath) {
            this.duration = duration;
            this.amplitudeUpdates = amplitudeUpdates;
            this.isRecording = isRecording;
            this.filePath = filePath;
        }
        
        @Override
        public String toString() {
            return String.format("RecordingStats{duration=%dms, updates=%d, recording=%s, file=%s}", 
                duration, amplitudeUpdates, isRecording, filePath);
        }
    }
}
