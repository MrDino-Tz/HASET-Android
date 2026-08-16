package com.haset.hasetapp.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Helper class for uploading files to Cloudinary
 * 
 * Setup Instructions:
 * 1. Sign up at https://cloudinary.com (free account)
 * 2. Create a restricted unsigned upload preset in the Cloudinary dashboard.
 * 3. Initialize in your Application class or MainActivity:
 *    Map config = new HashMap();
 *    config.put("cloud_name", "your_cloud_name");
 *    MediaManager.init(context, config);
 */
public class CloudinaryUploadHelper {
    private static final String TAG = "CloudinaryUpload";
    private static final long MAX_STANDARD_UPLOAD_BYTES = 25L * 1024L * 1024L;
    private static final long MAX_VIDEO_UPLOAD_BYTES = 100L * 1024L * 1024L;
    private static String uploadPreset;
    
    public interface OnFileUploadListener {
        void onUploadStart();
        void onUploadProgress(double progress);
        void onUploadSuccess(String downloadUrl, String fileName);
        void onUploadError(String error);
    }
    
    /**
     * Upload file to Cloudinary
     * @param context Application context
     * @param fileUri URI of the file to upload
     * @param fileType Type of file (image, video, audio, etc.)
     * @param fileName Original file name
     * @param folder Cloudinary folder path (e.g.,   "news_posts", "chat_attachments")
     * @param listener Upload listener for callbacks
     */
    public static void uploadFile(Context context, Uri fileUri, String fileType, String fileName,
                                 String folder, OnFileUploadListener listener) {
        if (listener != null) {
            listener.onUploadStart();
        }
        
        try {
            // Check if MediaManager is initialized (try-catch since isInitialized might not exist)
            try {
                MediaManager.get();
            } catch (Exception e) {
                Log.e(TAG, "Cloudinary MediaManager not initialized. Please initialize in Application class.");
                if (listener != null) {
                    listener.onUploadError("Cloudinary not initialized. Please check configuration.");
                }
                return;
            }
            
            // Generate unique filename with timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String uniqueFileName = fileType + "_" + timestamp + "_" + fileName;
            
            // Prepare upload options
            Map<String, Object> options = new HashMap<>();
            options.put("public_id", folder + "/" + uniqueFileName.replaceAll("\\.[^.]*$", "")); // Remove extension for public_id
            options.put("folder", folder);
            options.put("resource_type", getResourceType(fileType));
            
            // For images, add optimization
            if (fileType.equals("image")) {
                options.put("quality", "auto");
                options.put("fetch_format", "auto");
            }
            
            // For videos, add optimization
            if (fileType.equals("video")) {
                options.put("resource_type", "video");
                options.put("format", "mp4");
            }
            
            // Convert URI to File if needed
            long maxUploadBytes = getMaxUploadBytes(fileType);
            File file = uriToFile(context, fileUri, maxUploadBytes);
            if (file == null) {
                Log.e(TAG, "Failed to convert URI to File");
                if (listener != null) {
                    listener.onUploadError("Failed to access file");
                }
                return;
            }
            
            // Start upload
            String requestId = MediaManager.get().upload(file.getAbsolutePath())
                    .options(options)
                    .unsigned(uploadPreset)
                    .maxFileSize(maxUploadBytes)
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                            Log.d(TAG, "Upload started: " + requestId);
                        }
                        
                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                            double progress = (100.0 * bytes) / totalBytes;
                            Log.d(TAG, "Upload progress: " + progress + "%");
                            if (listener != null) {
                                listener.onUploadProgress(progress);
                            }
                        }
                        
                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            // Extract secure URL from result map
                            String secureUrl = null;
                            if (resultData != null) {
                                Object urlObj = resultData.get("secure_url");
                                if (urlObj == null) {
                                    urlObj = resultData.get("url");
                                }
                                if (urlObj != null) {
                                    secureUrl = urlObj.toString();
                                }
                                
                            }
                            
                            if (secureUrl == null || secureUrl.isEmpty()) {
                                Log.e(TAG, "❌ ERROR: Upload succeeded but no URL in result");
                                if (listener != null) {
                                    listener.onUploadError("Upload succeeded but failed to get URL");
                                }
                                return;
                            }
                            
                            if (listener != null) {
                                listener.onUploadSuccess(secureUrl, uniqueFileName);
                            }
                        }
                        
                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            Log.e(TAG, "❌ CLOUDINARY UPLOAD ERROR");
                            Log.e(TAG, "Request ID: " + requestId);
                            Log.e(TAG, "Error Code: " + error.getCode());
                            Log.e(TAG, "Error Description: " + error.getDescription());
                            Log.e(TAG, "Error Info: " + error.toString());
                            
                            String errorMessage = "Upload failed: " + error.getDescription();
                            if (error.getCode() == 401) {
                                errorMessage = "Upload authorization failed. Check the restricted upload preset.";
                            } else if (error.getCode() == 400) {
                                errorMessage = "Invalid request. Please check file format.";
                                Log.e(TAG, "❌ INVALID REQUEST: Check file format and size");
                            }
                            
                            if (listener != null) {
                                listener.onUploadError(errorMessage);
                            }
                        }
                        
                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                            Log.w(TAG, "Upload rescheduled: " + error.getDescription());
                        }
                    })
                    .dispatch(context);
            
        } catch (Exception e) {
            Log.e(TAG, "Error preparing file upload: " + e.getMessage(), e);
            if (listener != null) {
                listener.onUploadError("Error preparing upload: " + e.getMessage());
            }
        }
    }
    
    /**
     * Convert URI to File
     */
    private static File uriToFile(Context context, Uri uri, long maxBytes) {
        try {
            String scheme = uri.getScheme();
            if (scheme == null || scheme.equals("file")) {
                return new File(uri.getPath());
            } else if (scheme.equals("content")) {
                // Copy content URI to temp file
                File tempFile = File.createTempFile("upload_", ".tmp", context.getCacheDir());
                try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
                     FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                    if (inputStream == null) {
                        return null;
                    }
                    byte[] buffer = new byte[8192];
                    long totalBytes = 0;
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        totalBytes += bytesRead;
                        if (totalBytes > maxBytes) {
                            throw new IOException("Selected file exceeds the upload size limit");
                        }
                        outputStream.write(buffer, 0, bytesRead);
                    }
                } catch (Exception error) {
                    //noinspection ResultOfMethodCallIgnored
                    tempFile.delete();
                    throw error;
                }
                return tempFile;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error converting URI to File: " + e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Get Cloudinary resource type based on file type
     */
    private static String getResourceType(String fileType) {
        switch (fileType.toLowerCase()) {
            case "image":
                return "image";
            case "video":
                return "video";
            case "audio":
                return "raw"; // Cloudinary doesn't have audio type, use raw
            default:
                return "auto";
        }
    }

    private static long getMaxUploadBytes(String fileType) {
        return "video".equalsIgnoreCase(fileType)
                ? MAX_VIDEO_UPLOAD_BYTES
                : MAX_STANDARD_UPLOAD_BYTES;
    }
    
    /**
     * Initialize Cloudinary (call this in Application class)
     * @param context Application context
     * @param cloudName Your Cloudinary cloud name
     * @param preset Restricted unsigned upload preset configured in Cloudinary
     */
    public static void initialize(Context context, String cloudName, String preset) {
        if (preset == null || preset.trim().isEmpty()) {
            throw new IllegalArgumentException("Cloudinary upload preset is required");
        }
        Map<String, Object> config = new HashMap<>();
        config.put("cloud_name", cloudName);
        uploadPreset = preset;
        MediaManager.init(context, config);
    }

    public static String getUploadPreset() {
        return uploadPreset;
    }
}
