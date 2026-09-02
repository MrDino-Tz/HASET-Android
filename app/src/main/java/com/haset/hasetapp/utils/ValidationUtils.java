package com.haset.hasetapp.utils;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Patterns;

import java.util.Locale;

public class ValidationUtils {
    
    public static boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
    
    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    public static boolean isStrongPassword(String password) {
        return password != null
                && password.length() >= 12
                && password.matches(".*[a-z].*")
                && password.matches(".*[A-Z].*")
                && password.matches(".*[0-9].*");
    }
    
    public static boolean isValidPhone(String phone) {
        // Allow '+' at the beginning and then digits. Minimum length for a valid Tanzanian number with +255 prefix is 13 (e.g., +2557XXXXXXXX)
        return phone != null && phone.matches("^\\+[0-9]{12,}$");
    }
    
    public static boolean isValidName(String name) {
        return name != null && !name.trim().isEmpty() && name.length() >= 2;
    }
    
    public static boolean isValidNin(String nin) {
        if (nin == null) return false;
        String digits = nin.replaceAll("\\s", "");
        return digits.matches("\\d{20}");
    }

    public static boolean isPdfDocument(ContentResolver resolver, Uri uri) {
        if (uri == null) return false;
        String type = resolver.getType(uri);
        if (type != null && "application/pdf".equalsIgnoreCase(type)) {
            return true;
        }
        String path = uri.getLastPathSegment();
        return path != null && path.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    public static boolean passwordsMatch(String password, String confirmPassword) {
        return password != null && password.equals(confirmPassword);
    }
}
