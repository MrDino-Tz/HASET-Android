package com.haset.hasetapp.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateTimeUtils {
    private static final SimpleDateFormat SDF_DATE = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private static final SimpleDateFormat SDF_TIME = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private static final SimpleDateFormat SDF_DATE_TIME = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
    private static final SimpleDateFormat SDF_SEPARATOR = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    
    public static String formatDate(long timestamp) {
        return SDF_DATE.format(new Date(timestamp));
    }
    
    public static String formatTime(long timestamp) {
        return SDF_TIME.format(new Date(timestamp));
    }
    
    public static String formatDateTime(long timestamp) {
        return SDF_DATE_TIME.format(new Date(timestamp));
    }

    public static boolean isSameDay(long timestamp1, long timestamp2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTimeInMillis(timestamp1);
        cal2.setTimeInMillis(timestamp2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    public static String formatDateForSeparator(long timestamp) {
        long now = System.currentTimeMillis();

        if (isSameDay(timestamp, now)) {
            return "Today";
        } else if (isSameDay(timestamp, now - (24 * 60 * 60 * 1000))) { // Yesterday
            return "Yesterday";
        } else {
            return SDF_SEPARATOR.format(new Date(timestamp));
        }
    }
    
    public static String getTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (seconds < 60) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + " min ago";
        } else if (hours < 24) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (days < 7) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else {
            return formatDate(timestamp);
        }
    }
}
