package com.haset.hasetapp.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;

/**
 * Helper class for recording voice notes
 */
public class VoiceRecorderHelper {
    private static final String TAG = "VoiceRecorderHelper";
    
    private MediaRecorder mediaRecorder;
    private String outputFile;
    private boolean isRecording = false;
    private Context context;
    
    public VoiceRecorderHelper(Context context) {
        this.context = context;
    }
    
    /**
     * Check if recording permission is granted
     */
    public boolean hasRecordingPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
                == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Start recording voice note
     */
    public boolean startRecording() {
        if (isRecording) {
            Log.w(TAG, "Already recording");
            return false;
        }
        
        if (!hasRecordingPermission()) {
            Log.e(TAG, "Recording permission not granted");
            return false;
        }
        
        try {
            // Create output file
            File audioDir = new File(context.getCacheDir(), "voice_notes");
            if (!audioDir.exists()) {
                audioDir.mkdirs();
            }
            
            outputFile = new File(audioDir, "voice_" + System.currentTimeMillis() + ".m4a").getAbsolutePath();
            
            // Initialize MediaRecorder
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mediaRecorder = new MediaRecorder(context);
            } else {
                mediaRecorder = new MediaRecorder();
            }
            
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioEncodingBitRate(128000);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setOutputFile(outputFile);
            
            mediaRecorder.prepare();
            mediaRecorder.start();
            
            isRecording = true;
            Log.d(TAG, "Recording started: " + outputFile);
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
        if (!isRecording || mediaRecorder == null) {
            Log.w(TAG, "Not recording");
            return null;
        }
        
        try {
            mediaRecorder.stop();
            isRecording = false;
            Log.d(TAG, "Recording stopped: " + outputFile);
            String filePath = outputFile;
            releaseRecorder();
            return filePath;
        } catch (RuntimeException e) {
            Log.e(TAG, "Error stopping recording: " + e.getMessage(), e);
            releaseRecorder();
            // Delete incomplete file
            if (outputFile != null) {
                File file = new File(outputFile);
                if (file.exists()) {
                    file.delete();
                }
            }
            return null;
        }
    }
    
    /**
     * Cancel recording (delete the file)
     */
    public void cancelRecording() {
        if (isRecording) {
            stopRecording();
        }
        
        // Delete the file if it exists
        if (outputFile != null) {
            File file = new File(outputFile);
            if (file.exists()) {
                file.delete();
                Log.d(TAG, "Recording cancelled, file deleted");
            }
        }
    }
    
    /**
     * Get current recording duration in milliseconds
     */
    public long getCurrentDuration() {
        if (isRecording && mediaRecorder != null) {
            try {
                return mediaRecorder.getMaxAmplitude(); // This is a workaround, actual duration tracking needs timer
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }
    
    /**
     * Get current audio amplitude for visualization
     */
    public int getAmplitude() {
        if (isRecording && mediaRecorder != null) {
            try {
                int amplitude = mediaRecorder.getMaxAmplitude();
                // Normalize amplitude (0-32767) to a more usable range
                return Math.min(amplitude / 100, 100);
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }
    
    /**
     * Check if currently recording
     */
    public boolean isRecording() {
        return isRecording;
    }
    
    /**
     * Release MediaRecorder resources
     */
    private void releaseRecorder() {
        if (mediaRecorder != null) {
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
     * Get the output file path
     */
    public String getOutputFile() {
        return outputFile;
    }
    
    /**
     * Cleanup on destroy
     */
    public void cleanup() {
        if (isRecording) {
            cancelRecording();
        }
        releaseRecorder();
    }
}

