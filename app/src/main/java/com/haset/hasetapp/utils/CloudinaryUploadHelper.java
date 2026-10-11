package com.haset.hasetapp.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.effect.Presentation;
import androidx.media3.transformer.Composition;
import androidx.media3.transformer.DefaultEncoderFactory;
import androidx.media3.transformer.AudioEncoderSettings;
import androidx.media3.transformer.EditedMediaItem;
import androidx.media3.transformer.EditedMediaItemSequence;
import androidx.media3.transformer.Effects;
import androidx.media3.transformer.ExportException;
import androidx.media3.transformer.ExportResult;
import androidx.media3.transformer.Transformer;
import androidx.media3.transformer.VideoEncoderSettings;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Uploads media to Cloudinary via a restricted unsigned preset.
 * PDFs upload as image resources. Public access comes from the preset (not client params).
 */
public class CloudinaryUploadHelper {
    private static final String TAG = "CloudinaryUpload";
    private static final long MAX_STANDARD_UPLOAD_BYTES = 1024L * 1024L;
    private static final long MAX_VIDEO_UPLOAD_BYTES = 5L * 1024L * 1024L;
    private static final long MAX_SOURCE_BYTES = 500L * 1024L * 1024L;
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
            if ("image".equalsIgnoreCase(fileType)) {
                safeName = safeName.replaceAll("\\.[^.]*$", "") + ".jpg";
            }
            String uniqueFileName = fileType + "_" + timestamp + "_" + safeName;

            String resourceType = getResourceType(fileType, fileName);
            // public_id relative to folder (do not also prefix folder — that doubles the path)
            String publicId = uniqueFileName;
            // Cloudinary image PDFs should not keep ".pdf" in public_id; format comes from upload.
            if ("image".equals(resourceType) || "video".equals(resourceType) || "auto".equals(resourceType)) {
                publicId = publicId.replaceAll("\\.[^.]*$", "");
            }
            final String uploadPublicId = publicId;

            long maxUploadBytes = getMaxUploadBytes(fileType);
            File source = uriToFile(context, fileUri,
                    "video".equalsIgnoreCase(fileType) || "image".equalsIgnoreCase(fileType)
                            ? MAX_SOURCE_BYTES : maxUploadBytes, fileName);
            File file = source;
            if (file == null) {
                if (listener != null) listener.onUploadError("Failed to access file");
                return;
            }
            if (source.length() > ("video".equalsIgnoreCase(fileType) || "image".equalsIgnoreCase(fileType)
                    ? MAX_SOURCE_BYTES : maxUploadBytes)) {
                deleteIfTemporary(context, source);
                if (listener != null) listener.onUploadError("Selected file exceeds the allowed processing size.");
                return;
            }

            if ("image".equalsIgnoreCase(fileType)) {
                file = compressImage(context, source);
                if (file == null || file.length() > maxUploadBytes) {
                    deleteIfTemporary(context, source);
                    deleteIfTemporary(context, file);
                    if (listener != null) listener.onUploadError("Could not reduce this image below 1 MB.");
                    return;
                }
            } else if ("video".equalsIgnoreCase(fileType)) {
                if (source.length() <= maxUploadBytes) {
                    // Small videos do not need transcoding.
                } else {
                    compressVideo(context, source, fileName, compressed -> {
                        deleteIfTemporary(context, source);
                        if (compressed == null || compressed.length() > MAX_VIDEO_UPLOAD_BYTES) {
                            deleteIfTemporary(context, compressed);
                            if (listener != null) listener.onUploadError(
                                    "This video could not be reduced to 5 MB. Choose a shorter video.");
                            return;
                        }
                        dispatchUpload(context, compressed, uniqueFileName, uploadPublicId, folder,
                                resourceType, MAX_VIDEO_UPLOAD_BYTES, listener);
                    }, error -> {
                        deleteIfTemporary(context, source);
                        if (listener != null) listener.onUploadError("Could not compress video: " + error);
                    });
                    return;
                }
            } else if (file.length() > maxUploadBytes) {
                deleteIfTemporary(context, source);
                if (listener != null) listener.onUploadError("Files must be 1 MB or smaller.");
                return;
            }

            if (file.length() > maxUploadBytes) {
                deleteIfTemporary(context, source);
                deleteIfTemporary(context, file);
                if (listener != null) listener.onUploadError("Files must be 1 MB or smaller.");
                return;
            }

            if (file != source) deleteIfTemporary(context, source);
            dispatchUpload(context, file, uniqueFileName, uploadPublicId, folder, resourceType, maxUploadBytes, listener);

        } catch (Exception e) {
            Log.e(TAG, "Error preparing file upload: " + e.getMessage(), e);
            ErrorLogger.log(e);
            if (listener != null) {
                listener.onUploadError("Error preparing upload: " + e.getMessage());
            }
        }
    }

    private static void dispatchUpload(Context context, File file, String uniqueFileName, String publicId,
                                       String folder, String resourceType, long maxUploadBytes,
                                       OnFileUploadListener listener) {
        Map<String, Object> options = new HashMap<>();
        options.put("folder", folder);
        options.put("public_id", publicId);
        options.put("resource_type", resourceType);
        if ("image".equals(resourceType)) {
            options.put("quality", "auto");
            options.put("fetch_format", "auto");
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
                            deleteIfTemporary(context, file);
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
                            deleteIfTemporary(context, file);
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                            Log.w(TAG, "Upload rescheduled: " + error.getDescription());
                        }
                    })
                    .dispatch(context);
    }

    private interface VideoResult { void complete(File file); }
    private interface VideoError { void fail(String message); }

    private static void compressVideo(Context context, File source, String name,
                                      VideoResult success, VideoError failure) {
        File output;
        try {
            MediaMetadataRetriever metadata = new MediaMetadataRetriever();
            long durationMs;
            try {
                metadata.setDataSource(source.getAbsolutePath());
                durationMs = Long.parseLong(metadata.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_DURATION));
            } finally {
                metadata.release();
            }
            if (durationMs <= 0) throw new IOException("Video duration is unavailable");
            int videoBitrate = (int) Math.max(100_000L,
                    Math.min(1_000_000L, (4L * 1024L * 1024L * 8L * 1000L / durationMs) - 64_000L));
            output = File.createTempFile("compressed_", ".mp4", context.getCacheDir());
            // Transformer creates the destination itself.
            //noinspection ResultOfMethodCallIgnored
            output.delete();
            MediaItem item = MediaItem.fromUri(Uri.fromFile(source));
            Effects effects = new Effects(Collections.emptyList(),
                    Arrays.asList(Presentation.createForHeight(360)));
            EditedMediaItem edited = new EditedMediaItem.Builder(item).setEffects(effects).build();
            EditedMediaItemSequence sequence = new EditedMediaItemSequence.Builder(edited).build();
            Composition composition = new Composition.Builder(sequence).build();
            DefaultEncoderFactory encoderFactory = new DefaultEncoderFactory.Builder(context)
                    .setRequestedAudioEncoderSettings(new AudioEncoderSettings.Builder()
                            .setBitrate(48_000).build())
                    .setRequestedVideoEncoderSettings(new VideoEncoderSettings.Builder()
                            .setBitrate(videoBitrate).build())
                    .build();
            Transformer transformer = new Transformer.Builder(context.getApplicationContext())
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .setEncoderFactory(encoderFactory)
                    .addListener(new Transformer.Listener() {
                        @Override public void onCompleted(Composition composition, ExportResult result) {
                            success.complete(output);
                        }
                        @Override public void onError(Composition composition, ExportResult result,
                                                      ExportException exception) {
                            //noinspection ResultOfMethodCallIgnored
                            output.delete();
                            failure.fail(exception.getMessage() != null ? exception.getMessage() : "transcoding failed");
                        }
                    }).build();
            transformer.start(composition, output.getAbsolutePath());
        } catch (Exception e) {
            failure.fail(e.getMessage() != null ? e.getMessage() : "transcoding failed");
        }
    }

    private static File compressImage(Context context, File source) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(source.getAbsolutePath(), bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;
        int sample = 1;
        while (Math.max(bounds.outWidth / sample, bounds.outHeight / sample) > 1800) sample *= 2;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        Bitmap bitmap = BitmapFactory.decodeFile(source.getAbsolutePath(), options);
        if (bitmap == null) return null;
        File output = File.createTempFile("image_upload_", ".jpg", context.getCacheDir());
        try (FileOutputStream stream = new FileOutputStream(output)) {
            int quality = 88;
            do {
                stream.getChannel().truncate(0);
                stream.getChannel().position(0);
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
                stream.flush();
                if (output.length() <= MAX_STANDARD_UPLOAD_BYTES) break;
                quality -= 12;
            } while (quality >= 40);
        } finally {
            bitmap.recycle();
        }
        return output;
    }

    private static void deleteIfTemporary(Context context, File file) {
        if (file != null && file.getAbsolutePath().startsWith(context.getCacheDir().getAbsolutePath())) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
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

    /**
     * Build a smaller CDN URL for avatars/list thumbnails so Glide is less likely to
     * fail with status -1 on slow or flaky mobile networks.
     */
    public static String forThumbnail(String url, int sizePx) {
        String normalized = toPublicDeliveryUrl(url);
        if (normalized == null || normalized.isEmpty()) return normalized;
        if (!normalized.contains("res.cloudinary.com") || !normalized.contains("/image/upload/")) {
            return normalized;
        }
        if (normalized.contains("/image/upload/c_") || normalized.contains("/image/upload/w_")) {
            return normalized;
        }
        int size = Math.max(64, sizePx);
        String transform = "c_fill,g_auto,w_" + size + ",h_" + size + ",q_auto:eco,f_auto/";
        return normalized.replace("/image/upload/", "/image/upload/" + transform);
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
