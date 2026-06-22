package com.haset.hasetapp.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.haset.hasetapp.R;
import com.haset.hasetapp.views.VoiceWaveView;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * VoiceRecordingManager - Manages voice recording with optimized memory usage and performance.
 * Handles recording lifecycle, UI updates, and proper cleanup.
 */
public class VoiceRecordingManager {
    private static final String TAG = "VoiceRecordingManager";
    
    // Recording configuration
    private static final long MAX_RECORDING_DURATION = 60000; // 60 seconds
    private static final long WARNING_DURATION = 50000;       // 50 seconds warning
    private static final int TIMER_UPDATE_INTERVAL = 100;     // 100ms timer updates
    
    // Components
    private final Context context;
    private final OptimizedVoiceRecorderHelper voiceRecorder;
    private final VoiceRecordingCallback callback;
    
    // UI components
    private AlertDialog recordingDialog;
    private VoiceWaveView voiceWaveView;
    private TextView tvRecordingDuration;
    private Button btnCancel;
    private Button btnStop;
    
    // State management
    private final AtomicBoolean isRecording = new AtomicBoolean(false);
    private final Handler recordingHandler = new Handler(Looper.getMainLooper());
    private long recordingStartTime;
    
    // Runnables for cleanup
    private Runnable timerRunnable;
    private Runnable amplitudeRunnable;
    
    public interface VoiceRecordingCallback {
        void onRecordingStarted();
        void onRecordingCompleted(String audioFilePath, long duration);
        void onRecordingCancelled();
        void onRecordingError(String error);
    }
    
    public VoiceRecordingManager(Context context, VoiceRecordingCallback callback) {
        this.context = context;
        this.callback = callback;
        this.voiceRecorder = new OptimizedVoiceRecorderHelper(context);
        
        MemoryMonitor.logMemoryUsage("VoiceRecordingManager_init");
    }
    
    /**
     * Start voice recording with permission check
     */
    public boolean startRecording() {
        if (isRecording.get()) {
            Log.w(TAG, "Already recording");
            return false;
        }
        
        // Check permission
        if (!voiceRecorder.hasRecordingPermission()) {
            callback.onRecordingError("Recording permission not granted");
            return false;
        }
        
        // Start recording
        if (voiceRecorder.startRecording()) {
            isRecording.set(true);
            recordingStartTime = System.currentTimeMillis();
            
            // Show recording dialog
            showRecordingDialog();
            
            // Start timer and amplitude updates
            startTimerUpdates();
            startAmplitudeUpdates();
            
            callback.onRecordingStarted();
            MemoryMonitor.logMemoryUsage("VoiceRecording_started");
            
            Log.d(TAG, "Voice recording started");
            return true;
        } else {
            callback.onRecordingError("Failed to start recording");
            return false;
        }
    }
    
    /**
     * Stop recording and handle completion
     */
    public void stopRecording() {
        if (!isRecording.get()) {
            Log.w(TAG, "Not recording");
            return;
        }
        
        // Stop updates
        stopTimerUpdates();
        stopAmplitudeUpdates();
        
        // Stop recording
        String audioFilePath = voiceRecorder.stopRecording();
        long duration = System.currentTimeMillis() - recordingStartTime;
        
        // Hide dialog
        hideRecordingDialog();
        
        // Update state
        isRecording.set(false);
        
        // Log performance
        OptimizedVoiceRecorderHelper.RecordingStats stats = voiceRecorder.getStats();
        Log.d(TAG, String.format("Recording completed: %s, Stats: %s", audioFilePath, stats));
        
        if (audioFilePath != null && duration > 500) { // Minimum 500ms
            callback.onRecordingCompleted(audioFilePath, duration);
        } else {
            callback.onRecordingCancelled();
        }
        
        MemoryMonitor.logMemoryUsage("VoiceRecording_stopped");
    }
    
    /**
     * Cancel recording
     */
    public void cancelRecording() {
        if (!isRecording.get()) {
            return;
        }
        
        // Stop updates
        stopTimerUpdates();
        stopAmplitudeUpdates();
        
        // Cancel recording
        voiceRecorder.cancelRecording();
        
        // Hide dialog
        hideRecordingDialog();
        
        // Update state
        isRecording.set(false);
        
        callback.onRecordingCancelled();
        MemoryMonitor.logMemoryUsage("VoiceRecording_cancelled");
        
        Log.d(TAG, "Voice recording cancelled");
    }
    
    /**
     * Show recording dialog with optimized UI
     */
    private void showRecordingDialog() {
        // Create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_voice_recording, null);
        
        // Find views
        voiceWaveView = dialogView.findViewById(R.id.voiceWaveView);
        tvRecordingDuration = dialogView.findViewById(R.id.tvRecordingDuration);
        btnCancel = dialogView.findViewById(R.id.btnCancel);
        btnStop = dialogView.findViewById(R.id.btnStop);
        
        // Setup dialog
        builder.setView(dialogView)
               .setCancelable(false);
        
        recordingDialog = builder.create();
        recordingDialog.show();
        
        // Setup click listeners
        btnCancel.setOnClickListener(v -> cancelRecording());
        btnStop.setOnClickListener(v -> stopRecording());
        
        // Initialize UI
        tvRecordingDuration.setText("00:00");
        
        Log.d(TAG, "Recording dialog shown");
    }
    
    /**
     * Hide recording dialog
     */
    private void hideRecordingDialog() {
        if (recordingDialog != null && recordingDialog.isShowing()) {
            recordingDialog.dismiss();
            recordingDialog = null;
            voiceWaveView = null;
            tvRecordingDuration = null;
            btnCancel = null;
            btnStop = null;
        }
    }
    
    /**
     * Start timer updates with performance optimization
     */
    private void startTimerUpdates() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRecording.get() && tvRecordingDuration != null) {
                    long duration = System.currentTimeMillis() - recordingStartTime;
                    
                    // Format duration
                    String durationText = formatDuration(duration);
                    tvRecordingDuration.setText(durationText);
                    
                    // Check for maximum duration
                    if (duration >= MAX_RECORDING_DURATION) {
                        Log.w(TAG, "Maximum recording duration reached");
                        stopRecording();
                        return;
                    }
                    
                    // Show warning at 50 seconds
                    if (duration >= WARNING_DURATION && duration < WARNING_DURATION + TIMER_UPDATE_INTERVAL) {
                        tvRecordingDuration.setTextColor(0xFFFF0000); // Red color
                    }
                    
                    // Schedule next update
                    recordingHandler.postDelayed(this, TIMER_UPDATE_INTERVAL);
                }
            }
        };
        recordingHandler.post(timerRunnable);
    }
    
    /**
     * Stop timer updates
     */
    private void stopTimerUpdates() {
        if (timerRunnable != null) {
            recordingHandler.removeCallbacks(timerRunnable);
            timerRunnable = null;
        }
    }
    
    /**
     * Start amplitude updates with performance optimization
     */
    private void startAmplitudeUpdates() {
        amplitudeRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRecording.get() && voiceWaveView != null) {
                    // Get amplitude from optimized recorder
                    int amplitude = voiceRecorder.getAmplitude();
                    voiceWaveView.updateAmplitude(amplitude);
                    
                    // Schedule next update
                    recordingHandler.postDelayed(this, 100); // 100ms for better performance
                }
            }
        };
        recordingHandler.post(amplitudeRunnable);
    }
    
    /**
     * Stop amplitude updates
     */
    private void stopAmplitudeUpdates() {
        if (amplitudeRunnable != null) {
            recordingHandler.removeCallbacks(amplitudeRunnable);
            amplitudeRunnable = null;
        }
    }
    
    /**
     * Format duration in milliseconds to MM:SS format
     */
    private String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    /**
     * Check if currently recording
     */
    public boolean isRecording() {
        return isRecording.get();
    }
    
    /**
     * Get current recording duration
     */
    public long getCurrentDuration() {
        if (isRecording.get()) {
            return System.currentTimeMillis() - recordingStartTime;
        }
        return 0;
    }
    
    /**
     * Get recording statistics
     */
    public OptimizedVoiceRecorderHelper.RecordingStats getStats() {
        return voiceRecorder.getStats();
    }
    
    /**
     * Cleanup resources with memory monitoring
     */
    public void cleanup() {
        Log.d(TAG, "Cleaning up voice recording manager");
        
        // Stop recording if active
        if (isRecording.get()) {
            cancelRecording();
        }
        
        // Hide dialog
        hideRecordingDialog();
        
        // Stop all handlers
        stopTimerUpdates();
        stopAmplitudeUpdates();
        
        // Cleanup recorder
        voiceRecorder.cleanup();
        
        // Clear handler
        if (recordingHandler != null) {
            recordingHandler.removeCallbacksAndMessages(null);
        }
        
        MemoryMonitor.logMemoryUsage("VoiceRecordingManager_cleanup");
        Log.d(TAG, "Voice recording manager cleanup completed");
    }
    
    /**
     * Check if manager is properly initialized
     */
    public boolean isInitialized() {
        return voiceRecorder != null && !context.equals(null);
    }
}
