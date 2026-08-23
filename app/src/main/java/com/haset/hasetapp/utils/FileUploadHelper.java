package com.haset.hasetapp.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.haset.hasetapp.models.ChatMessage;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileUploadHelper {
    private static final String TAG = "FileUploadHelper";
    
    public interface OnFileUploadListener {
        void onUploadStart();
        void onUploadProgress(double progress);
        void onUploadSuccess(String downloadUrl, String fileName, long fileSize);
        void onUploadError(String error);
    }
    
    public static void uploadFile(Context context, Uri fileUri, String fileType, String fileName, 
                                OnFileUploadListener listener) {
        uploadFile(context, fileUri, fileType, fileName, "chat_attachments", listener);
    }
    
    /**
     * Upload file to Firebase Storage with custom storage path
     * @param context Application context
     * @param fileUri URI of the file to upload
     * @param fileType Type of file (image, video, audio, etc.)
     * @param fileName Original file name
     * @param storagePath Storage path (e.g., "chat_attachments", "articles")
     * @param listener Upload listener for callbacks
     */
    public static void uploadFile(Context context, Uri fileUri, String fileType, String fileName, 
                                String storagePath, OnFileUploadListener listener) {
        if (listener != null) {
            listener.onUploadStart();
        }
        
        // Check if user is authenticated
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "User is not authenticated. Cannot upload file.");
            if (listener != null) {
                listener.onUploadError("Please log in to upload files.");
            }
            return;
        }
        
        Log.d(TAG, "User authenticated: " + auth.getCurrentUser().getUid());
        
        try {
            // Generate unique filename with timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String uniqueFileName = fileType + "_" + timestamp + "_" + fileName;
            
            // Get Firebase Storage reference
            StorageReference storageRef = FirebaseStorage.getInstance().getReference();
            StorageReference fileRef = storageRef.child(storagePath + "/" + uniqueFileName);
            
            Log.d(TAG, "Starting upload for file: " + uniqueFileName + " to path: " + fileRef.getPath());
            
            // Start upload
            UploadTask uploadTask = fileRef.putFile(fileUri);
            
            // Listen for upload progress
            uploadTask.addOnProgressListener(taskSnapshot -> {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                Log.d(TAG, "Upload progress: " + progress + "%");
                if (listener != null) {
                    listener.onUploadProgress(progress);
                }
            });
            
            // Use addOnSuccessListener pattern (more reliable for getting download URL)
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                Log.d(TAG, "Upload task completed successfully");
                
                // Verify upload actually completed
                long bytesTransferred = taskSnapshot.getBytesTransferred();
                long totalBytes = taskSnapshot.getTotalByteCount();
                Log.d(TAG, "Upload completed - Bytes transferred: " + bytesTransferred + ", Total: " + totalBytes);
                
                if (bytesTransferred != totalBytes && totalBytes > 0) {
                    Log.w(TAG, "Upload may not have completed fully. Transferred: " + bytesTransferred + ", Total: " + totalBytes);
                }
                
                // Verify metadata exists
                if (taskSnapshot.getMetadata() == null) {
                    Log.e(TAG, "Upload metadata is null");
                    if (listener != null) {
                        listener.onUploadError("Upload completed but metadata is null");
                    }
                    return;
                }
                
                Log.d(TAG, "Upload successful, getting download URL...");
                Log.d(TAG, "Upload metadata path: " + taskSnapshot.getMetadata().getPath());
                Log.d(TAG, "Upload metadata size: " + taskSnapshot.getMetadata().getSizeBytes());
                Log.d(TAG, "Upload metadata name: " + taskSnapshot.getMetadata().getName());
                
                // Use the original fileRef (same pattern as ProfilePhotoHelper)
                // The metadata reference might point to a different location
                final StorageReference finalFileRef = fileRef;
                Log.d(TAG, "Using original fileRef for download URL: " + finalFileRef.getPath());
                
                // Try to get download URL directly first (like ProfilePhotoHelper does)
                finalFileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    if (uri != null) {
                        String downloadUrl = uri.toString();
                        long fileSize = taskSnapshot.getMetadata() != null ? taskSnapshot.getMetadata().getSizeBytes() : 0;
                        Log.d(TAG, "File uploaded successfully. Download URL: " + downloadUrl + ", Size: " + fileSize);
                        if (listener != null) {
                            listener.onUploadSuccess(downloadUrl, uniqueFileName, fileSize);
                        }
                    } else {
                        Log.e(TAG, "Download URL is null, retrying...");
                        // Retry with longer delay — guard: skip if listener was cleared by caller
                        if (listener != null) {
                            long fileSizeForRetry = taskSnapshot.getMetadata() != null ? taskSnapshot.getMetadata().getSizeBytes() : 0;
                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                if (listener != null) {
                                    getDownloadUrlWithRetry(finalFileRef, 0, 5, listener, uniqueFileName, fileSizeForRetry);
                                }
                            }, 3000);
                        }
                    }
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get download URL on first attempt: " + e.getMessage());
                    // Retry with exponential backoff — guard: skip if listener was cleared by caller
                    if (listener != null) {
                        long fileSizeOnFailure = taskSnapshot.getMetadata() != null ? taskSnapshot.getMetadata().getSizeBytes() : 0;
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            if (listener != null) {
                                getDownloadUrlWithRetry(finalFileRef, 0, 5, listener, uniqueFileName, fileSizeOnFailure);
                            }
                        }, 2000);
                    }
                });
                
            }).addOnFailureListener(exception -> {
                Log.e(TAG, "Upload failed: " + exception.getMessage(), exception);
                String errorMessage = "Upload failed";
                String errorMsg = exception.getMessage() != null ? exception.getMessage() : "";
                
                // Check for specific error types
                if (errorMsg.contains("permission") || errorMsg.contains("unauthorized") || 
                    errorMsg.contains("403") || errorMsg.contains("Forbidden")) {
                    errorMessage = "Permission denied. Please check Firebase Storage rules or log in again.";
                } else if (errorMsg.contains("network") || errorMsg.contains("timeout")) {
                    errorMessage = "Network error. Please check your internet connection.";
                } else if (errorMsg.contains("Object does not exist") || errorMsg.contains("404") || 
                          errorMsg.contains("terminated the upload session")) {
                    // This usually means Firebase Storage rules are blocking the upload
                    errorMessage = "Upload blocked. Please check Firebase Storage security rules allow writes to: " + storagePath;
                    Log.e(TAG, "Upload session terminated - likely a security rules issue");
                    Log.e(TAG, "Storage path: " + storagePath);
                    Log.e(TAG, "User authenticated: " + (auth.getCurrentUser() != null ? "Yes" : "No"));
                } else {
                    errorMessage = "Upload failed: " + errorMsg;
                }
                
                if (listener != null) {
                    listener.onUploadError(errorMessage);
                }
            });
            
            // Old continueWithTask pattern removed - using addOnSuccessListener instead
            /*
            uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    Exception exception = task.getException();
                    Log.e(TAG, "Upload task failed: " + (exception != null ? exception.getMessage() : "Unknown error"), exception);
                    throw exception != null ? exception : new Exception("Upload task failed");
                }
                
                Log.d(TAG, "Upload successful, getting download URL...");
                Log.d(TAG, "Upload snapshot metadata: " + (task.getResult().getMetadata() != null ? task.getResult().getMetadata().getPath() : "null"));
                
                // Get the reference from the upload result metadata
                StorageReference uploadedFileRef = fileRef;
                if (task.getResult().getMetadata() != null && task.getResult().getMetadata().getReference() != null) {
                    uploadedFileRef = task.getResult().getMetadata().getReference();
                    Log.d(TAG, "Using metadata reference: " + uploadedFileRef.getPath());
                }
                
                // Return the download URL task
                return uploadedFileRef.getDownloadUrl();
            }).addOnSuccessListener(uri -> {
                */
            
            
        } catch (Exception e) {
            Log.e(TAG, "Error preparing file upload: " + e.getMessage(), e);
            com.haset.hasetapp.utils.ErrorLogger.log(e);
            if (listener != null) {
                listener.onUploadError("Error preparing upload: " + e.getMessage());
            }
        }
    }
    
    /**
     * Get download URL with retry mechanism
     */
    private static void getDownloadUrlWithRetry(StorageReference fileRef, int attempt, int maxAttempts, 
                                                OnFileUploadListener listener, String fileName, long fileSize) {
        Log.d(TAG, "Attempting to get download URL (attempt " + (attempt + 1) + "/" + maxAttempts + ")");
        Log.d(TAG, "File reference path: " + fileRef.getPath());
        
        // Try to get download URL
        fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
            if (uri != null) {
                String downloadUrl = uri.toString();
                Log.d(TAG, "File uploaded successfully. Download URL: " + downloadUrl);
                if (listener != null) {
                    listener.onUploadSuccess(downloadUrl, fileName, fileSize);
                }
            } else {
                Log.e(TAG, "Download URL is null");
                if (listener != null) {
                    listener.onUploadError("Failed to get download URL: URL is null");
                }
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to get download URL (attempt " + (attempt + 1) + "): " + e.getMessage(), e);
            
            // Retry if we haven't exceeded max attempts and error suggests timing issue
            String errorMsg = e.getMessage() != null ? e.getMessage() : "";
            boolean isRetryableError = errorMsg.contains("Object location not found") || 
                                     errorMsg.contains("Object does not exist") ||
                                     errorMsg.contains("not found") ||
                                     errorMsg.contains("location") ||
                                     errorMsg.contains("404") ||
                                     errorMsg.contains("Not Found");
            
            if (attempt < maxAttempts - 1 && isRetryableError) {
                // Wait before retrying (exponential backoff with longer delays)
                long delay = (long) Math.pow(2, attempt) * 2000; // 2s, 4s, 8s, 16s, 32s
                Log.d(TAG, "Retrying download URL retrieval in " + delay + "ms...");
                Log.d(TAG, "Error was: " + errorMsg);
                Log.d(TAG, "This might be a timing issue - file may need more time to be available");
                
                // Guard: only retry if the listener is still active
                if (listener != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        if (listener != null) {
                            getDownloadUrlWithRetry(fileRef, attempt + 1, maxAttempts, listener, fileName, fileSize);
                        }
                    }, delay);
                }
            } else {
                // Max attempts reached or different error
                String errorMessage = "Failed to get download URL";
                if (e.getMessage() != null) {
                    if (e.getMessage().contains("Object location not found") || e.getMessage().contains("Object does not exist")) {
                        errorMessage = "File uploaded but not yet available. Please try again later.";
                    } else if (e.getMessage().contains("permission")) {
                        errorMessage = "Permission denied. Please check Firebase Storage rules.";
                    } else {
                        errorMessage = "Failed to get download URL: " + e.getMessage();
                    }
                }
                if (listener != null) {
                    listener.onUploadError(errorMessage);
                }
            }
        });
    }
    
    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
    
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format(Locale.getDefault(), "%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    public static boolean isValidFileType(String fileName, String expectedType) {
        String extension = getFileExtension(fileName);
        switch (expectedType.toLowerCase()) {
            case "document":
                return extension.equals("pdf") || extension.equals("doc") || extension.equals("docx") 
                        || extension.equals("txt") || extension.equals("xls") || extension.equals("xlsx");
            case "audio":
                return extension.equals("mp3") || extension.equals("wav") || extension.equals("aac") 
                        || extension.equals("m4a") || extension.equals("ogg");
            case "video":
                return extension.equals("mp4") || extension.equals("avi") || extension.equals("mov") 
                        || extension.equals("mkv") || extension.equals("wmv") || extension.equals("3gp");
            default:
                return false;
        }
    }
}
