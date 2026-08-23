package com.haset.hasetapp.utils;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.haset.hasetapp.R;
import com.haset.hasetapp.views.VoiceWaveView;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ChatVoicePlayer - Enhanced voice player for chat messages
 * Manages inline playback with wave visualization for item_chat_audio.xml
 */
public class ChatVoicePlayer {
    private static final String TAG = "ChatVoicePlayer";
    
    // Playback state
    public enum PlaybackState {
        IDLE, PREPARING, PLAYING, PAUSED, COMPLETED, ERROR
    }
    
    // Components
    private final Context context;
    private MediaPlayer mediaPlayer;
    private final Handler playbackHandler;
    
    // UI Components from item_chat_audio.xml
    private ImageView btnPlayPause;
    private TextView tvAudioDuration;
    private ProgressBar pbUpload;
    private VoiceWaveView voiceWaveView;
    private LinearLayout layoutStaticWave;
    
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
    
    public ChatVoicePlayer(Context context) {
        this.context = context;
        this.playbackHandler = new Handler(Looper.getMainLooper());
        initializeMediaPlayer();
        
        MemoryMonitor.logMemoryUsage("ChatVoicePlayer_init");
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
            
            MemoryMonitor.logMemoryUsage("ChatVoicePlayer_completed");
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
     * Bind UI components from item_chat_audio.xml
     */
    public void bindUI(ImageView btnPlayPause, TextView tvAudioDuration, 
                     ProgressBar pbUpload, VoiceWaveView voiceWaveView, 
                     LinearLayout layoutStaticWave) {
        this.btnPlayPause = btnPlayPause;
        this.tvAudioDuration = tvAudioDuration;
        this.pbUpload = pbUpload;
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
            showProgressIndicator(true);
            
            Log.d(TAG, "Preparing audio: " + audioPath);
            return true;
            
        } catch (IOException e) {
            Log.e(TAG, "Error preparing audio: " + e.getMessage(), e);
            com.haset.hasetapp.utils.ErrorLogger.log(e);
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
            showProgressIndicator(false);
            startProgressUpdates();
            
            // Show animated wave, hide static wave
            showAnimatedWave(true);
            
            if (callback != null) {
                callback.onPlaybackStarted();
            }
            
            MemoryMonitor.logMemoryUsage("ChatVoicePlayer_started");
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
            
            MemoryMonitor.logMemoryUsage("ChatVoicePlayer_paused");
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
        
        MemoryMonitor.logMemoryUsage("ChatVoicePlayer_stopped");
    }
    
    /**
     * Toggle play/pause with audio path
     */
    public void togglePlayback(String audioPath) {
        if (audioPath != null) {
            currentAudioPath = audioPath;
        }
        
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
     * Toggle play/pause (legacy - uses stored path)
     */
    public void togglePlayback() {
        togglePlayback(null);
    }
    
    /**
     * Show/hide animated wave vs static wave
     */
    private void showAnimatedWave(boolean showAnimated) {
        android.util.Log.d(TAG, "showAnimatedWave called: showAnimated=" + showAnimated + " voiceWaveView=" + (voiceWaveView != null) + " layoutStaticWave=" + (layoutStaticWave != null));
        
        if (voiceWaveView != null && layoutStaticWave != null) {
            if (showAnimated) {
                voiceWaveView.setVisibility(View.VISIBLE);
                layoutStaticWave.setVisibility(View.GONE);
            } else {
                voiceWaveView.setVisibility(View.GONE);
                layoutStaticWave.setVisibility(View.VISIBLE);
            }
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
        if (tvAudioDuration != null) {
            String currentTime = formatDuration(currentMs);
            tvAudioDuration.setText(currentTime);
        }
        
        if (callback != null) {
            callback.onProgressUpdate(currentMs, totalMs);
        }
    }
    
    /**
     * Simulate amplitude for wave visualization
     */
    private int simulateAmplitude(int position, int duration) {
        // Create a realistic wave pattern for playback
        float normalizedPosition = (float) position / duration;
        return (int) (Math.sin(normalizedPosition * Math.PI * 12) * 40 + 60);
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
     * Show/hide progress indicator
     */
    private void showProgressIndicator(boolean show) {
        if (pbUpload != null) {
            pbUpload.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
    
    /**
     * Reset UI to initial state
     */
    private void resetUI() {
        updatePlayPauseButton(false);
        showProgressIndicator(false);
        
        if (tvAudioDuration != null) {
            tvAudioDuration.setText("0:00");
        }
        
        // Show static wave, hide animated wave
        showAnimatedWave(false);
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
        Log.d(TAG, "Cleaning up chat voice player");
        
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
        tvAudioDuration = null;
        pbUpload = null;
        voiceWaveView = null;
        layoutStaticWave = null;
        callback = null;
        
        MemoryMonitor.logMemoryUsage("ChatVoicePlayer_cleanup");
        Log.d(TAG, "Chat voice player cleanup completed");
    }
}
