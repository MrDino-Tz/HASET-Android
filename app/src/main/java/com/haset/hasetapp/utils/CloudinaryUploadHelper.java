package com.haset.hasetapp.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import java.io.File;
import java.io.FileOutputStream;
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
 * 2. Get your Cloud Name, API Key, and API Secret from Dashboard
 * 3. Initialize in your Application class or MainActivity:
 *    Map config = new HashMap();
 *    config.put("cloud_name", "your_cloud_name");
 *    config.put("api_key", "your_api_key");
 *    config.put("api_secret", "your_api_secret");
 *    MediaManager.init(context, config);
 */
public class CloudinaryUploadHelper {
    private static final String TAG = "CloudinaryUpload";
    
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
            
            Log.d(TAG, "Starting upload for file: " + uniqueFileName + " to folder: " + folder);
            
            // Convert URI to File if needed
            File file = uriToFile(context, fileUri);
            if (file == null) {
                Log.e(TAG, "Failed to convert URI to File");
                if (listener != null) {
                    listener.onUploadError("Failed to access file");
                }
                return;
            }
            
            Log.d(TAG, "=== STARTING CLOUDINARY UPLOAD ===");
            Log.d(TAG, "File path: " + file.getAbsolutePath());
            Log.d(TAG, "File exists: " + file.exists());
            Log.d(TAG, "File size: " + (file.exists() ? file.length() + " bytes" : "N/A"));
            Log.d(TAG, "Upload options: " + options.toString());
            
            // Start upload
            String requestId = MediaManager.get().upload(file.getAbsolutePath())
                    .options(options)
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
                            Log.d(TAG, "=== CLOUDINARY UPLOAD SUCCESS ===");
                            Log.d(TAG, "Request ID: " + requestId);
                            Log.d(TAG, "Result Data: " + (resultData != null ? resultData.toString() : "null"));
                            
                            // Extract secure URL from result map
                            String secureUrl = null;
                            if (resultData != null) {
                                Object urlObj = resultData.get("secure_url");
                                Log.d(TAG, "secure_url from result: " + (urlObj != null ? urlObj.toString() : "null"));
                                
                                if (urlObj == null) {
                                    urlObj = resultData.get("url");
                                    Log.d(TAG, "url from result: " + (urlObj != null ? urlObj.toString() : "null"));
                                }
                                if (urlObj != null) {
                                    secureUrl = urlObj.toString();
                                }
                                
                                // Log all keys in result for debugging
                                Log.d(TAG, "All result keys: " + resultData.keySet());
                            }
                            
                            if (secureUrl == null || secureUrl.isEmpty()) {
                                Log.e(TAG, "❌ ERROR: Upload succeeded but no URL in result");
                                Log.e(TAG, "Result data was: " + (resultData != null ? resultData.toString() : "null"));
                                if (listener != null) {
                                    listener.onUploadError("Upload succeeded but failed to get URL");
                                }
                                return;
                            }
                            
                            Log.d(TAG, "✅ Upload successful!");
                            Log.d(TAG, "✅ Cloudinary URL: " + secureUrl);
                            Log.d(TAG, "✅ File name: " + uniqueFileName);
                            
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
                                errorMessage = "Authentication failed. Please check Cloudinary credentials.";
                                Log.e(TAG, "❌ AUTHENTICATION ERROR: Check cloud_name, api_key, and api_secret in strings.xml");
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
                    .dispatch();
            
            Log.d(TAG, "Upload request ID: " + requestId);
            
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
    private static File uriToFile(Context context, Uri uri) {
        try {
            String scheme = uri.getScheme();
            if (scheme == null || scheme.equals("file")) {
                return new File(uri.getPath());
            } else if (scheme.equals("content")) {
                // Copy content URI to temp file
                InputStream inputStream = context.getContentResolver().openInputStream(uri);
                if (inputStream == null) {
                    return null;
                }
                
                File tempFile = File.createTempFile("upload_", ".tmp", context.getCacheDir());
                FileOutputStream outputStream = new FileOutputStream(tempFile);
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                
                inputStream.close();
                outputStream.close();
                
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
    
    /**
     * Initialize Cloudinary (call this in Application class)
     * @param context Application context
     * @param cloudName Your Cloudinary cloud name
     * @param apiKey Your Cloudinary API key
     * @param apiSecret Your Cloudinary API secret
     */
    public static void initialize(Context context, String cloudName, String apiKey, String apiSecret) {
        Map<String, Object> config = new HashMap<>();
        config.put("cloud_name", cloudName);
        config.put("api_key", apiKey);
        config.put("api_secret", apiSecret);
        MediaManager.init(context, config);
        Log.d(TAG, "Cloudinary initialized with cloud name: " + cloudName);
    }
}

