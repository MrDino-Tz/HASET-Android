package com.haset.hasetapp.utils;

import java.util.Locale;

/**
 * File helpers for chat attachments. Uploads go through {@link CloudinaryUploadHelper}.
 */
public final class FileUploadHelper {
    private FileUploadHelper() {}

    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.US);
    }

    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format(Locale.getDefault(), "%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    public static boolean isValidFileType(String fileName, String expectedType) {
        String extension = getFileExtension(fileName);
        switch (expectedType.toLowerCase(Locale.US)) {
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
