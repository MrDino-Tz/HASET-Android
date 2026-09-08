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
 * Uploads media to Cloudinary via a restricted unsigned preset.
 * PDFs upload as image resources. Public access comes from the preset (not client params).
 */
public class CloudinaryUploadHelper {
    private static final String TAG = "CloudinaryUpload";
    private static final long MAX_STANDARD_UPLOAD_BYTES = 25L * 1024L * 1024L;
    private static final long MAX_VIDEO_UPLOAD_BYTES = 100L * 1024L * 1024L;
    private static String uploadPreset;
    private static String cloudName;

    public interface OnFileUploadListener {
        void onUploadStart();
        void onUploadProgress(double progress);
        void onUploadSuccess(String downloadUrl, String fileName);
        void onUploadError(String error);
    }

    public static void uploadFile(Context context, Uri fileUri, String fileType, String fileName,
                                 String folder, OnFileUploadListener listener) {
        if (listener != null) {
            listener.onUploadStart();
        }

        try {
            try {
                MediaManager.get();
            } catch (Exception e) {
                Log.e(TAG, "Cloudinary MediaManager not initialized.");
                if (listener != null) {
                    listener.onUploadError("Cloudinary not initialized. Please check configuration.");
                }
                return;
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String safeName = fileName != null ? fileName.replaceAll("[^A-Za-z0-9._-]", "_") : "file";
            String uniqueFileName = fileType + "_" + timestamp + "_" + safeName;

            String resourceType = getResourceType(fileType, fileName);
            // public_id relative to folder (do not also prefix folder — that doubles the path)
            String publicId = uniqueFileName;
            // Cloudinary image PDFs should not keep ".pdf" in public_id; format comes from upload.
            if ("image".equals(resourceType) || "video".equals(resourceType) || "auto".equals(resourceType)) {
                publicId = publicId.replaceAll("\\.[^.]*$", "");
            }

            Map<String, Object> options = new HashMap<>();
            options.put("folder", folder);
            options.put("public_id", publicId);
            options.put("resource_type", resourceType);
            // Do NOT pass access_mode/type here — unsigned presets reject them.
            // Public delivery is already configured on preset haset_mobile_unsigned.

            if ("image".equalsIgnoreCase(fileType)) {
                options.put("quality", "auto");
                options.put("fetch_format", "auto");
            }
            if ("video".equalsIgnoreCase(fileType)) {
                options.put("format", "mp4");
            }

            long maxUploadBytes = getMaxUploadBytes(fileType);
            File file = uriToFile(context, fileUri, maxUploadBytes, fileName);
            if (file == null) {
                if (listener != null) listener.onUploadError("Failed to access file");
                return;
            }

            MediaManager.get().upload(file.getAbsolutePath())
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
                            if (listener != null && totalBytes > 0) {
                                listener.onUploadProgress((100.0 * bytes) / totalBytes);
                            }
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String deliveryUrl = buildPublicDeliveryUrl(resultData);
                            if (deliveryUrl == null || deliveryUrl.isEmpty()) {
                                if (listener != null) {
                                    listener.onUploadError("Upload succeeded but failed to get URL");
                                }
                                return;
                            }
                            Log.d(TAG, "Upload OK url=" + deliveryUrl);
                            if (listener != null) {
                                listener.onUploadSuccess(deliveryUrl, uniqueFileName);
                            }
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            Log.e(TAG, "Upload error: " + error.getDescription());
                            String errorMessage = "Upload failed: " + error.getDescription();
                            if (error.getCode() == 401) {
                                errorMessage = "Upload authorization failed. Check the Cloudinary upload preset.";
                            } else if (error.getCode() == 400) {
                                errorMessage = "Invalid request. Please check file format.";
                            }
                            if (listener != null) listener.onUploadError(errorMessage);
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                            Log.w(TAG, "Upload rescheduled: " + error.getDescription());
                        }
                    })
                    .dispatch(context);

        } catch (Exception e) {
            Log.e(TAG, "Error preparing file upload: " + e.getMessage(), e);
            ErrorLogger.log(e);
            if (listener != null) {
                listener.onUploadError("Error preparing upload: " + e.getMessage());
            }
        }
    }

    /**
     * Prefer a public /upload/ delivery URL. Authenticated Cloudinary URLs return HTTP 401 in browser.
     */
    static String buildPublicDeliveryUrl(Map resultData) {
        if (resultData == null) return null;

        Object publicIdObj = resultData.get("public_id");
        Object resourceTypeObj = resultData.get("resource_type");
        Object versionObj = resultData.get("version");
        Object formatObj = resultData.get("format");

        String secureUrl = null;
        Object urlObj = resultData.get("secure_url");
        if (urlObj == null) urlObj = resultData.get("url");
        if (urlObj != null) secureUrl = urlObj.toString();

        if (publicIdObj != null && cloudName != null && !cloudName.isEmpty()) {
            String resourceType = resourceTypeObj != null ? resourceTypeObj.toString() : "raw";
            String publicId = publicIdObj.toString();
            // Raw public_ids already include the extension; image/video may need format suffix.
            if (!"raw".equals(resourceType) && formatObj != null
                    && !publicId.toLowerCase(Locale.US).endsWith("." + formatObj.toString().toLowerCase(Locale.US))) {
                publicId = publicId + "." + formatObj;
            }
            String versionPart = versionObj != null ? ("v" + versionObj + "/") : "";
            String built = "https://res.cloudinary.com/" + cloudName + "/"
                    + resourceType + "/upload/" + versionPart + publicId;
            return built;
        }

        if (secureUrl == null) return null;
        // Force public delivery path if the SDK returned an authenticated URL.
        return secureUrl
                .replace("/authenticated/", "/upload/")
                .replace("/private/", "/upload/");
    }

    /** Normalize any stored Cloudinary URL before download/open. */
    public static String toPublicDeliveryUrl(String url) {
        if (url == null || url.isEmpty()) return url;
        if (!url.contains("res.cloudinary.com")) return url;
        return url.replace("/authenticated/", "/upload/").replace("/private/", "/upload/");
    }

    private static File uriToFile(Context context, Uri uri, long maxBytes, String fileName) {
        try {
            String scheme = uri.getScheme();
            if (scheme == null || scheme.equals("file")) {
                return new File(uri.getPath());
            } else if (scheme.equals("content")) {
                File tempFile = File.createTempFile("upload_", getTempFileSuffix(fileName), context.getCacheDir());
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

    private static String getResourceType(String fileType, String fileName) {
        String type = fileType != null ? fileType.toLowerCase(Locale.US) : "";
        String ext = "";
        if (fileName != null) {
            int dot = fileName.lastIndexOf('.');
            if (dot >= 0 && dot < fileName.length() - 1) {
                ext = fileName.substring(dot + 1).toLowerCase(Locale.US);
            }
        }
        switch (type) {
            case "image":
                return "image";
            case "video":
                return "video";
            case "audio":
                return "raw";
            case "document":
            case "raw":
                // PDFs must be uploaded as image for Cloudinary delivery/transforms.
                // (Account must also allow "PDF and ZIP files delivery" in Security settings.)
                if ("pdf".equals(ext) || ext.isEmpty()) {
                    return "image";
                }
                return "raw";
            default:
                return "auto";
        }
    }

    private static String getTempFileSuffix(String fileName) {
        if (fileName == null) return ".tmp";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) return ".tmp";
        String extension = fileName.substring(dotIndex + 1).toLowerCase(Locale.US);
        if (!extension.matches("[a-z0-9]{1,10}")) return ".tmp";
        return "." + extension;
    }

    private static long getMaxUploadBytes(String fileType) {
        return "video".equalsIgnoreCase(fileType)
                ? MAX_VIDEO_UPLOAD_BYTES
                : MAX_STANDARD_UPLOAD_BYTES;
    }

    public static void initialize(Context context, String cloudNameValue, String preset) {
        if (preset == null || preset.trim().isEmpty()) {
            throw new IllegalArgumentException("Cloudinary upload preset is required");
        }
        cloudName = cloudNameValue;
        uploadPreset = preset;
        Map<String, Object> config = new HashMap<>();
        config.put("cloud_name", cloudNameValue);
        MediaManager.init(context, config);
    }

    public static String getUploadPreset() {
        return uploadPreset;
    }

    public static String getCloudName() {
        return cloudName;
    }
}
