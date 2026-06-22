package com.haset.hasetapp.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.haset.hasetapp.R;
import com.haset.hasetapp.views.VoiceWaveView;

import java.util.concurrent.atomic.AtomicBoolean;

public class VoiceRecordingBottomSheet {
    private static final String TAG = "VoiceRecordingBottomSheet";
    
    private BottomSheetDialog bottomSheetDialog;
    private VoiceWaveView voiceWaveView;
    private TextView tvRecordingDuration;
    private ImageView ivRecordingIcon;
    private ImageView btnClose;
    private ImageView btnRecord;
    
    private final AtomicBoolean isRecording = new AtomicBoolean(false);
    private final Context context;
    private VoiceRecordingCallback callback;
    private OptimizedVoiceRecorderHelper voiceRecorderHelper;
    private Handler recordingHandler;
    private long recordingStartTime;
    private Runnable timerRunnable;
    
    public interface VoiceRecordingCallback {
        void onRecordingStarted();
        void onRecordingStopped(String audioFilePath, long duration);
        void onRecordingCancelled();
        void onRecordingError(String error);
    }
    
    public VoiceRecordingBottomSheet(Context context, VoiceRecordingCallback callback) {
        this.context = context;
        this.callback = callback;
        this.voiceRecorderHelper = new OptimizedVoiceRecorderHelper(context);
        this.recordingHandler = new Handler(Looper.getMainLooper());
    }
    
    public void show() {
        if (bottomSheetDialog != null && bottomSheetDialog.isShowing()) {
            return;
        }
        
        bottomSheetDialog = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
        View sheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_voice_recording, null);
        
        initializeViews(sheetView);
        setupClickListeners();
        
        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.setCancelable(false);
        
        bottomSheetDialog.setOnDismissListener(dialog -> stopTimerUpdates());
        
        bottomSheetDialog.show();
        
        updateUIForIdleState();
    }
    
    private void initializeViews(View sheetView) {
        voiceWaveView = sheetView.findViewById(R.id.voiceWaveView);
        tvRecordingDuration = sheetView.findViewById(R.id.tvRecordingDuration);
        ivRecordingIcon = sheetView.findViewById(R.id.ivRecordingIcon);
        btnClose = sheetView.findViewById(R.id.btnClose);
        btnRecord = sheetView.findViewById(R.id.btnRecord);
    }
    
    private void setupClickListeners() {
        btnClose.setOnClickListener(v -> {
            if (isRecording.get()) {
                cancelRecording();
            } else {
                dismiss();
            }
        });
        
        btnRecord.setOnClickListener(v -> {
            if (isRecording.get()) {
                stopRecording();
            } else {
                startRecording();
            }
        });
    }
    
    private void startRecording() {
        if (!voiceRecorderHelper.hasRecordingPermission()) {
            if (callback != null) {
                callback.onRecordingError("Recording permission not granted");
            }
            return;
        }
        
        // Try to start recording
        boolean started = voiceRecorderHelper.startRecording();
        if (!started) {
            android.util.Log.e("VoiceRecording", "Failed to start recording");
            if (callback != null) {
                callback.onRecordingError("Failed to start recording");
            }
            return;
        }
        
        // Recording started successfully
        isRecording.set(true);
        recordingStartTime = System.currentTimeMillis();
        
        android.util.Log.d("VoiceRecording", "Recording started successfully");
        
        updateUIForRecordingState();
        startTimerUpdates();
        
        if (callback != null) {
            callback.onRecordingStarted();
        }
    }
    
    public void stopRecording() {
        if (!isRecording.get()) {
            return;
        }
        
        // Stop the recording and get file path
        String audioFilePath = voiceRecorderHelper.stopRecording();
        long duration = voiceRecorderHelper.getCurrentDuration();
        
        android.util.Log.d("VoiceRecording", "Stop recording - file: " + audioFilePath + ", duration: " + duration);
        
        isRecording.set(false);
        stopTimerUpdates();
        
        updateUIForStoppedState();
        
        if (callback != null) {
            callback.onRecordingStopped(audioFilePath, duration);
        }
        
        // Auto dismiss after short delay to show check mark
        new Handler(Looper.getMainLooper()).postDelayed(this::dismiss, 500);
    }
    
    private void cancelRecording() {
        if (isRecording.get()) {
            isRecording.set(false);
            stopTimerUpdates();
            voiceRecorderHelper.cancelRecording();
            
            if (callback != null) {
                callback.onRecordingCancelled();
            }
        }
        dismiss();
    }

    public String getLastRecordedFilePath() {
        return voiceRecorderHelper != null ? voiceRecorderHelper.getOutputFile() : null;
    }
    
    private void startTimerUpdates() {
        stopTimerUpdates();
        
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRecording.get()) {
                    updateRecordingDuration();
                    recordingHandler.postDelayed(this, 100);
                }
            }
        };
        recordingHandler.post(timerRunnable);
    }
    
    private void stopTimerUpdates() {
        if (timerRunnable != null) {
            recordingHandler.removeCallbacks(timerRunnable);
            timerRunnable = null;
        }
    }
    
    private void updateRecordingDuration() {
        if (tvRecordingDuration != null && voiceWaveView != null) {
            long duration = System.currentTimeMillis() - recordingStartTime;
            tvRecordingDuration.setText(formatDuration(duration));
            voiceWaveView.updateAmplitude(simulateAmplitude(duration));
        }
    }
    
    private int simulateAmplitude(long duration) {
        long seconds = duration / 1000;
        float amplitude = (float) (Math.sin(seconds * 0.5) * 0.3 + 
                               Math.sin(seconds * 2) * 0.2 + 
                               Math.sin(seconds * 5) * 0.1 + 0.4);
        return (int) (amplitude * 100);
    }
    
    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    private void updateUIForIdleState() {
        if (btnRecord != null) {
            btnRecord.setImageResource(R.drawable.ic_mic);
            btnRecord.setColorFilter(context.getColor(R.color.green_primary));
        }
        
        if (ivRecordingIcon != null) {
            ivRecordingIcon.setImageResource(R.drawable.ic_mic);
            ivRecordingIcon.setColorFilter(context.getColor(R.color.text_secondary));
        }
        
        if (tvRecordingDuration != null) {
            tvRecordingDuration.setText("00:00");
        }
    }
    
    private void updateUIForRecordingState() {
        if (btnRecord != null) {
            btnRecord.setImageResource(R.drawable.ic_stop);
            btnRecord.setColorFilter(context.getColor(R.color.red_primary));
        }
        
        if (ivRecordingIcon != null) {
            ivRecordingIcon.setImageResource(R.drawable.ic_mic);
            ivRecordingIcon.setColorFilter(context.getColor(R.color.red_primary));
        }
        
        if (tvRecordingDuration != null) {
            tvRecordingDuration.setText("00:00");
        }
    }
    
    private void updateUIForStoppedState() {
        if (btnRecord != null) {
            btnRecord.setImageResource(R.drawable.ic_mic);
            btnRecord.setColorFilter(context.getColor(R.color.green_primary));
        }
        
        if (ivRecordingIcon != null) {
            ivRecordingIcon.setImageResource(R.drawable.ic_check);
            ivRecordingIcon.setColorFilter(context.getColor(R.color.green_primary));
        }
    }
    
    public boolean isRecording() {
        return isRecording.get();
    }
    
    public boolean isShowing() {
        return bottomSheetDialog != null && bottomSheetDialog.isShowing();
    }
    
    public void dismiss() {
        if (bottomSheetDialog != null && bottomSheetDialog.isShowing()) {
            stopTimerUpdates();
            bottomSheetDialog.dismiss();
            bottomSheetDialog = null;
        }
    }
    
    public void cleanup() {
        dismiss();
        voiceRecorderHelper = null;
        callback = null;
    }
}
