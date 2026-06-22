package com.haset.hasetapp.utils;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.haset.hasetapp.R;
import com.haset.hasetapp.views.VoiceWaveView;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * VoicePlayerManager - Manages voice message playback with wave visualization
 * Similar to WhatsApp's inline voice player with real-time wave updates
 */
public class VoicePlayerManager {
    private static final String TAG = "VoicePlayerManager";
    
    // Playback state
    public enum PlaybackState {
        IDLE, PREPARING, PLAYING, PAUSED, COMPLETED, ERROR
    }
    
    // Components
    private final Context context;
    private MediaPlayer mediaPlayer;
    private final Handler playbackHandler;
    
    // UI Components
    private ImageView btnPlayPause;
    private TextView tvDuration;
    private ProgressBar progressBar;
    private VoiceWaveView voiceWaveView;
    private View layoutStaticWave;
    
    // State management
    private final AtomicBoolean isPlaying = new AtomicBoolean(false);
    private PlaybackState currentState = PlaybackState.IDLE;
    private String currentAudioPath;
    private int totalDuration = 0;
    private VoicePlayerCallback callback;
    
    // Playback tracking
    private static final int UPDATE_INTERVAL = 100; // 100ms updates
    private Runnable updateRunnable;
    
    public interface VoicePlayerCallback {
        void onPlaybackStarted();
        void onPlaybackPaused();
        void onPlaybackCompleted();
        void onPlaybackError(String error);
        void onProgressUpdate(int currentMs, int totalMs);
    }
    
    public VoicePlayerManager(Context context) {
        this.context = context;
        this.playbackHandler = new Handler(Looper.getMainLooper());
        initializeMediaPlayer();
        
        MemoryMonitor.logMemoryUsage("VoicePlayerManager_init");
    }
    
    /**
     * Initialize MediaPlayer
     */
    private void initializeMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(mp -> {
            currentState = PlaybackState.PREPARING;
            totalDuration = mediaPlayer.getDuration();
            
            if (callback != null) {
                callback.onPlaybackStarted();
            }
            
            startPlayback();
        });
        
        mediaPlayer.setOnCompletionListener(mp -> {
            currentState = PlaybackState.COMPLETED;
            isPlaying.set(false);
            stopProgressUpdates();
            resetUI();
            
            if (callback != null) {
                callback.onPlaybackCompleted();
            }
            
            MemoryMonitor.logMemoryUsage("VoicePlayer_completed");
        });
        
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "MediaPlayer error: " + what + ", extra: " + extra);
            currentState = PlaybackState.ERROR;
            isPlaying.set(false);
            stopProgressUpdates();
            resetUI();
            
            if (callback != null) {
                callback.onPlaybackError("Playback error: " + what);
            }
            
            return true;
        });
    }
    
    /**
     * Bind UI components
     */
    public void bindUI(ImageView btnPlayPause, TextView tvDuration, 
                     ProgressBar progressBar, VoiceWaveView voiceWaveView, 
                     View layoutStaticWave) {
        this.btnPlayPause = btnPlayPause;
        this.tvDuration = tvDuration;
        this.progressBar = progressBar;
        this.voiceWaveView = voiceWaveView;
        this.layoutStaticWave = layoutStaticWave;
    }
    
    /**
     * Set callback for playback events
     */
    public void setCallback(VoicePlayerCallback callback) {
        this.callback = callback;
    }
    
    /**
     * Play audio file with wave visualization
     */
    public boolean playAudio(String audioPath) {
        if (audioPath == null || audioPath.isEmpty()) {
            Log.e(TAG, "Invalid audio path");
            return false;
        }
        
        try {
            // Stop current playback if any
            stopPlayback();
            
            currentAudioPath = audioPath;
            currentState = PlaybackState.PREPARING;
            
            // Prepare MediaPlayer
            mediaPlayer.reset();
            mediaPlayer.setDataSource(audioPath);
            mediaPlayer.prepareAsync();
            
            // Update UI
            updatePlayPauseButton(false);
            showProgressBar(true);
            
            Log.d(TAG, "Preparing audio: " + audioPath);
            return true;
            
        } catch (IOException e) {
            Log.e(TAG, "Error preparing audio: " + e.getMessage(), e);
            currentState = PlaybackState.ERROR;
            if (callback != null) {
                callback.onPlaybackError("Failed to prepare audio: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Start actual playback after preparation
     */
    private void startPlayback() {
        if (currentState == PlaybackState.PREPARING) {
            mediaPlayer.start();
            currentState = PlaybackState.PLAYING;
            isPlaying.set(true);
            
            // Update UI
            updatePlayPauseButton(true);
            showProgressBar(true);
            startProgressUpdates();
            
            // Show wave visualization
            if (voiceWaveView != null && layoutStaticWave != null) {
                voiceWaveView.setVisibility(View.VISIBLE);
                layoutStaticWave.setVisibility(View.GONE);
            }
            
            if (callback != null) {
                callback.onPlaybackStarted();
            }
            
            MemoryMonitor.logMemoryUsage("VoicePlayer_started");
        }
    }
    
    /**
     * Pause playback
     */
    public void pausePlayback() {
        if (currentState == PlaybackState.PLAYING && mediaPlayer != null) {
            mediaPlayer.pause();
            currentState = PlaybackState.PAUSED;
            isPlaying.set(false);
            
            // Update UI
            updatePlayPauseButton(false);
            stopProgressUpdates();
            
            if (callback != null) {
                callback.onPlaybackPaused();
            }
            
            MemoryMonitor.logMemoryUsage("VoicePlayer_paused");
        }
    }
    
    /**
     * Resume playback
     */
    public void resumePlayback() {
        if (currentState == PlaybackState.PAUSED && mediaPlayer != null) {
            mediaPlayer.start();
            currentState = PlaybackState.PLAYING;
            isPlaying.set(true);
            
            // Update UI
            updatePlayPauseButton(true);
            startProgressUpdates();
            
            if (callback != null) {
                callback.onPlaybackStarted();
            }
        }
    }
    
    /**
     * Stop playback completely
     */
    public void stopPlayback() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
        }
        
        currentState = PlaybackState.IDLE;
        isPlaying.set(false);
        stopProgressUpdates();
        resetUI();
        
        MemoryMonitor.logMemoryUsage("VoicePlayer_stopped");
    }
    
    /**
     * Toggle play/pause
     */
    public void togglePlayback() {
        switch (currentState) {
            case PLAYING:
                pausePlayback();
                break;
            case PAUSED:
                resumePlayback();
                break;
            case IDLE:
            case COMPLETED:
            case ERROR:
                if (currentAudioPath != null) {
                    playAudio(currentAudioPath);
                }
                break;
        }
    }
    
    /**
     * Start progress updates
     */
    private void startProgressUpdates() {
        stopProgressUpdates(); // Clear any existing updates
        
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPlaying.get() && mediaPlayer != null) {
                    try {
                        int currentPosition = mediaPlayer.getCurrentPosition();
                        updateProgress(currentPosition, totalDuration);
                        
                        // Update wave visualization
                        if (voiceWaveView != null) {
                            // Simulate amplitude based on playback position
                            int amplitude = simulateAmplitude(currentPosition, totalDuration);
                            voiceWaveView.updateAmplitude(amplitude);
                        }
                        
                        // Schedule next update
                        playbackHandler.postDelayed(this, UPDATE_INTERVAL);
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating progress: " + e.getMessage());
                    }
                }
            }
        };
        playbackHandler.post(updateRunnable);
    }
    
    /**
     * Stop progress updates
     */
    private void stopProgressUpdates() {
        if (updateRunnable != null) {
            playbackHandler.removeCallbacks(updateRunnable);
            updateRunnable = null;
        }
    }
    
    /**
     * Update progress UI
     */
    private void updateProgress(int currentMs, int totalMs) {
        if (tvDuration != null) {
            String currentTime = formatDuration(currentMs);
            String totalTime = formatDuration(totalMs);
            tvDuration.setText(currentTime + " / " + totalTime);
        }
        
        if (progressBar != null && totalMs > 0) {
            int progress = (int) ((currentMs * 100) / totalMs);
            progressBar.setProgress(progress);
        }
        
        if (callback != null) {
            callback.onProgressUpdate(currentMs, totalMs);
        }
    }
    
    /**
     * Simulate amplitude for wave visualization
     */
    private int simulateAmplitude(int position, int duration) {
        // Create a simple sine wave simulation
        float normalizedPosition = (float) position / duration;
        return (int) (Math.sin(normalizedPosition * Math.PI * 8) * 50 + 50);
    }
    
    /**
     * Update play/pause button
     */
    private void updatePlayPauseButton(boolean isPlaying) {
        if (btnPlayPause != null) {
            int iconRes = isPlaying ? R.drawable.ic_pause_arrow : R.drawable.ic_play_arrow;
            btnPlayPause.setImageResource(iconRes);
        }
    }
    
    /**
     * Show/hide progress bar
     */
    private void showProgressBar(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
    
    /**
     * Reset UI to initial state
     */
    private void resetUI() {
        updatePlayPauseButton(false);
        showProgressBar(false);
        
        if (tvDuration != null) {
            tvDuration.setText("0:00");
        }
        
        if (progressBar != null) {
            progressBar.setProgress(0);
        }
        
        // Show static wave
        if (voiceWaveView != null && layoutStaticWave != null) {
            voiceWaveView.setVisibility(View.GONE);
            layoutStaticWave.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Format duration in mm:ss format
     */
    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
    
    /**
     * Get current playback state
     */
    public PlaybackState getCurrentState() {
        return currentState;
    }
    
    /**
     * Check if currently playing
     */
    public boolean isPlaying() {
        return isPlaying.get();
    }
    
    /**
     * Get current audio path
     */
    public String getCurrentAudioPath() {
        return currentAudioPath;
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        Log.d(TAG, "Cleaning up voice player manager");
        
        stopPlayback();
        stopProgressUpdates();
        
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        
        if (playbackHandler != null) {
            playbackHandler.removeCallbacksAndMessages(null);
        }
        
        // Clear UI references
        btnPlayPause = null;
        tvDuration = null;
        progressBar = null;
        voiceWaveView = null;
        layoutStaticWave = null;
        callback = null;
        
        MemoryMonitor.logMemoryUsage("VoicePlayerManager_cleanup");
        Log.d(TAG, "Voice player manager cleanup completed");
    }
}
