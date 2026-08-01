package com.haset.hasetapp.utils;

import android.util.Patterns;

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
    
    public static boolean passwordsMatch(String password, String confirmPassword) {
        return password != null && password.equals(confirmPassword);
    }
}
