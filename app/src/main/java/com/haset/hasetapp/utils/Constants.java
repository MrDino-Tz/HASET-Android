package com.haset.hasetapp.utils;

public class Constants {
    // User Roles
    public static final String ROLE_PATIENT = "patient";
    public static final String ROLE_DOCTOR = "doctor";
//    public static final String ROLE_ADMIN = "admin";
    
    // Appointment Status
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_DECLINED = "declined";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_CANCELLED = "cancelled";
    public static final String APPOINTMENT_TYPE_ONLINE_CHAT = "Online Chat";
    
    // Firebase Database Paths
    public static final String USERS_PATH = "users";
    public static final String DOCTORS_PATH = "doctors";
    public static final String APPOINTMENTS_PATH = "appointments";
    public static final String MESSAGES_PATH = "messages";
    public static final String PRESCRIPTIONS_PATH = "prescriptions";
    public static final String NOTIFICATIONS_PATH = "notifications";
    public static final String USER_CONVERSATIONS_PATH = "user_conversations";
    public static final String ARTICLES_PATH = "articles";
    public static final String CALL_SIGNALING_PATH = "call_signaling";
    public static final String BANNERS_PATH = "promotional_banners";
    public static final String AUDIT_LOGS_PATH = "audit_logs";
    public static final String DOCTOR_RATINGS_PATH = "doctor_ratings";
    public static final String WITHDRAWAL_REQUESTS_PATH = "withdrawal_requests";
    public static final String APP_CONFIG_PATH = "app_config";
    
    // Call Status
    public static final String CALL_STATUS_RINGING = "ringing";
    public static final String CALL_STATUS_ACCEPTED = "accepted";
    public static final String CALL_STATUS_DECLINED = "declined";
    public static final String CALL_STATUS_ENDED = "ended";
    
    // SharedPreferences Keys
    public static final String PREF_NAME = "HCareAppPrefs";
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_USER_ROLE = "userRole";
    public static final String KEY_USER_NAME = "userName";
    
    // File Attachment Request Codes
    public static final int REQUEST_CODE_DOCUMENT = 1001;
    public static final int REQUEST_CODE_AUDIO = 1002;
    public static final int REQUEST_CODE_VIDEO = 1003;
    public static final int REQUEST_CODE_CAMERA = 1004;
    public static final int REQUEST_CODE_IMAGE = 1005;
    public static final String KEY_USER_EMAIL = "userEmail";
    public static final String KEY_USER_PHONE = "userPhone";
    public static final String KEY_PROFILE_PHOTO_PATH = "profilePhotoPath";
    public static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    
    // Intent Keys
    public static final String EXTRA_DOCTOR_ID = "doctorId";
    public static final String EXTRA_APPOINTMENT_ID = "appointmentId";
    public static final String EXTRA_PATIENT_ID = "patientId";
    public static final String EXTRA_CHAT_USER_ID = "chatUserId";
    public static final String EXTRA_CHAT_USER_NAME = "chatUserName";
    public static final String EXTRA_CHAT_USER_IMAGE = "chatUserImage";
    public static final String EXTRA_IS_CALLER = "isCaller";
    public static final String EXTRA_APPOINTMENT_APPROVED_AT = "appointmentApprovedAt";
    public static final String EXTRA_IS_FROM_APPOINTMENT = "isFromAppointment";
    public static final String ACTION_REPLY = "com.haset.hasetapp.ACTION_REPLY";
    public static final String KEY_TEXT_REPLY = "key_text_reply";
    
    // ========================
    // Environment Configuration
    // ========================
    // Set to false for production to use production API
    public static final boolean IS_DEBUG_MODE = true;
    
    // API Base URLs - Change to production URL for release
    // Hosted payment backend is deployed under /public/api on Hostinger.
    // PRODUCTION: public static final String PRODUCTION_API_URL = "https://payments.hasethospital.or.tz/public/api/";
    public static final String PRODUCTION_API_URL = "https://payments.hasethospital.or.tz/public/api/";
    public static final String PAYMENT_API_BASE_URL = "https://payments.hasethospital.or.tz/public/api/";
    // PRODUCTION: public static final String DEVELOPMENT_API_URL = "https://payments.hasethospital.or.tz/public/api/";
    public static final String DEVELOPMENT_API_URL = "http://192.168.1.126:8000/api/";
    
    // Use the production backend for all API traffic.
    public static final String API_BASE_URL = PRODUCTION_API_URL;
    
    // ========================
    // Payment Security
    // ========================
    // Minimum payment amount (in TZS) - API range: 50 - 5,000,000
    public static final double MIN_PAYMENT_AMOUNT = 50.0;
    public static final double MAX_PAYMENT_AMOUNT = 5000000.0;
    
    // Payment timeout in milliseconds (5 minutes)
    public static final long PAYMENT_TIMEOUT_MS = 300000;
    
    // Payment Webhook URL - Backend endpoint for payment status notifications
    // See: /from BACKEND/BACKEND_REQUIREMENTS.md
    public static final String PAYMENT_WEBHOOK_URL = PRODUCTION_API_URL + "payment/callback";
    public static final String PAYMENT_CLIENT_API_KEY = "hsk_40e850e045de6d6636d68c45c4c814aa2be41e07522d64e1";
    
    // ========================
    // Security Settings
    // ========================
    // Session timeout in milliseconds (30 minutes)
    public static final long SESSION_TIMEOUT_MS = 1800000;
    
    // Specialties
    public static final String[] SPECIALTIES = {
        "General Physician",
        "Cardiologist",
        "Dermatologist",
        "Pediatrician",
        "Orthopedic",
        "Neurologist",
        "Psychiatrist",
        "Gynecologist",
        "Dentist",
        "ENT Specialist"
    };
    
    // Time Slots
    public static final String[] TIME_SLOTS = {
        "09:00 AM", "09:30 AM", "10:00 AM", "10:30 AM",
        "11:00 AM", "11:30 AM", "12:00 PM", "12:30 PM",
        "02:00 PM", "02:30 PM", "03:00 PM", "03:30 PM",
        "04:00 PM", "04:30 PM", "05:00 PM", "05:30 PM",
        "06:00 PM", "06:30 PM", "07:00 PM", "07:30 PM"
    };

    // ========================
    // Revenue Share (Commission)
    // ========================
    // Percentage of total payment that goes to the doctor (60%)
    public static final double DOCTOR_REVENUE_SHARE = 0.6;
    // Percentage of total payment that goes to the platform (40%)
    public static final double HASET_REVENUE_SHARE = 0.4;

    // ========================
    // Legal & Support URLs
    // ========================
    public static final String PRIVACY_POLICY_URL = "https://hasethospital.or.tz/legal/privacy-policy";
    public static final String TERMS_CONDITIONS_URL = "https://hasethospital.or.tz/legal/terms";
    public static final String SUPPORT_URL = "https://hasethospital.or.tz/contact";
    
    // ========================
    // Notification Types
    public static final String NOTIF_TYPE_APPOINTMENT_REMINDER = "appointment_reminder";
    public static final String NOTIF_TYPE_APPOINTMENT_STATUS = "appointment_status";
    public static final String NOTIF_TYPE_NEW_APPOINTMENT = "new_appointment";
    public static final String NOTIF_TYPE_CHAT_MESSAGE = "chat_message";
    public static final String NOTIF_TYPE_NEW_REGISTRATION = "new_registration";
    public static final String NOTIF_TYPE_WITHDRAWAL_REQUEST = "withdrawal_request";
    public static final String NOTIF_TYPE_SYSTEM_ALERT = "system_alert";
    public static final String NOTIF_TYPE_GENERAL = "general";
    public static final String NOTIF_TYPE_ARTICLE = "article";
    public static final String NOTIF_TYPE_TRENDING_ARTICLE = "trending_article";
    
    // FCM Topics (for push notifications)
    public static final String TOPIC_ADMIN = "admin_alerts";
    public static final String TOPIC_DOCTORS = "doctors_alerts";
    public static final String TOPIC_ALL = "all_users";
    public static final String TOPIC_ARTICLES = "articles";
    public static final String TOPIC_TRENDING_ARTICLES = "trending_articles";
    
    // Trending articles threshold (minimum views to be considered trending)
    public static final int TRENDING_VIEWS_THRESHOLD = 50;

    // "New" label threshold
    public static final int NEW_DOCTOR_THRESHOLD_DAYS = 7;

}
