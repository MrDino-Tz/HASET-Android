package com.haset.hasetapp.database;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.haset.hasetapp.database.dao.AuditLogDao;
import com.haset.hasetapp.database.dao.DoctorDao;
import com.haset.hasetapp.database.dao.DoctorRatingDao;
import com.haset.hasetapp.database.dao.DoctorWalletDao;
import com.haset.hasetapp.database.dao.ArticlePostDao;
import com.haset.hasetapp.database.dao.UserDao;
import com.haset.hasetapp.database.entities.AuditLogEntity;
import com.haset.hasetapp.database.entities.DoctorEntity;
import com.haset.hasetapp.database.entities.DoctorRatingEntity;
import com.haset.hasetapp.database.entities.DoctorWalletEntity;
import com.haset.hasetapp.database.entities.ArticlePostEntity;
import com.haset.hasetapp.database.entities.UserEntity;
import com.haset.hasetapp.models.AuditLog;
import com.haset.hasetapp.models.Doctor;
import com.haset.hasetapp.utils.Constants;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Local Storage Helper - Manages local database operations
 * Replaces Firebase for offline/local-only functionality
 */
public class LocalStorageHelper {
    
    private static LocalStorageHelper instance;
    private final AppDatabase database;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    
    private LocalStorageHelper(Context context) {
        database = AppDatabase.getInstance(context);
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public static synchronized LocalStorageHelper getInstance(Context context) {
        if (instance == null) {
            instance = new LocalStorageHelper(context.getApplicationContext());
        }
        return instance;
    }
    
    // User Operations
    
    public void registerUser(String email, String password, String fullName, String phone, 
                            String role, OnCompleteListener<UserEntity> listener) {
        executorService.execute(() -> {
            try {
                // Check if user already exists
                UserEntity existing = database.userDao().getUserByEmail(email);
                if (existing != null) {
                    postError(listener, "User with this email already exists");
                    return;
                }
                
                // Create new user
                String userId = UUID.randomUUID().toString();
                String hashedPassword = hashPassword(password);
                UserEntity user = new UserEntity(userId, email, hashedPassword, fullName, phone, role);
                
                database.userDao().insert(user);
                postSuccess(listener, user);
            } catch (Exception e) {
                postError(listener, e.getMessage());
            }
        });
    }
    
    public void createUser(UserEntity user, OnCompleteListener<UserEntity> listener) {
        executorService.execute(() -> {
            try {
                // Check if user already exists
                UserEntity existing = database.userDao().getUserByEmail(user.getEmail());
                if (existing != null) {
                    postError(listener, "User with this email already exists");
                    return;
                }
                
                // Insert the user (password should already be hashed)
                database.userDao().insert(user);
                postSuccess(listener, user);
            } catch (Exception e) {
                postError(listener, e.getMessage());
            }
        });
    }
    
    public void loginUser(String email, String password, OnCompleteListener<UserEntity> listener) {
        executorService.execute(() -> {
            try {
                String hashedPassword = hashPassword(password);
                UserEntity user = database.userDao().login(email, hashedPassword);
                
                if (user != null) {
                    postSuccess(listener, user);
                } else {
                    postError(listener, "Invalid email or password");
                }
            } catch (Exception e) {
                postError(listener, e.getMessage());
            }
        });
    }
    
    public void getUserById(String userId, OnCompleteListener<UserEntity> listener) {
        executorService.execute(() -> {
            try {
                UserEntity user = database.userDao().getUserById(userId);
                postSuccess(listener, user);
            } catch (Exception e) {
                postError(listener, e.getMessage());
            }
        });
    }
    
    public void getDoctors(OnCompleteListener<List<UserEntity>> listener) {
        executorService.execute(() -> {
            try {
                List<UserEntity> doctors = database.userDao().getUsersByRole("doctor");
                postSuccess(listener, doctors);
            } catch (Exception e) {
                postError(listener, e.getMessage());
            }
        });
    }
    
    // Removed Appointment Operations
    // public void createAppointment(String patientId, String doctorId, String patientName,
    //                              String doctorName, String date, String time, String reason,
    //                              OnCompleteListener<AppointmentEntity> listener) { ... }
    // public void getPatientAppointments(String patientId, OnCompleteListener<List<AppointmentEntity>> listener) { ... }
    // public void getDoctorAppointments(String doctorId, OnCompleteListener<List<AppointmentEntity>> listener) { ... }
    // public void getDoctorAppointmentsByStatus(String doctorId, String status,
    //                                           OnCompleteListener<List<AppointmentEntity>> listener) { ... }
    // public void updateAppointmentStatus(String appointmentId, String status,
    //                                    OnCompleteListener<Boolean> listener) { ... }
    // public void updateAppointmentStatus(String appointmentId, String status, Context context,
    //                                    OnCompleteListener<Boolean> listener) { ... }
    private void logAppointmentStatusUpdate(Context context, String appointmentId, String oldStatus, String newStatus, String doctorName) {
        try {
            com.haset.hasetapp.utils.PreferenceManager prefManager = new com.haset.hasetapp.utils.PreferenceManager(context);
            String userId = prefManager.getUserId();
            String userName = prefManager.getUserName();
            String userRole = prefManager.getUserRole();
            
            if (userId != null && userName != null && userRole != null) {
                AuditLogEntity entity = new AuditLogEntity();
                entity.setLogId(UUID.randomUUID().toString());
                entity.setUserId(userId);
                entity.setUserName(userName);
                entity.setUserRole(userRole);
                entity.setAction("UPDATE_APPOINTMENT");
                entity.setDescription("Appointment status changed from " + oldStatus + " to " + newStatus + " with " + doctorName);
                entity.setEntityType("APPOINTMENT");
                entity.setEntityId(appointmentId);
                entity.setTimestamp(System.currentTimeMillis());
                
                database.auditLogDao().insert(entity);
            }
        } catch (Exception e) {
            // Silently fail - don't interrupt the main operation
        }
    }
    
    // Utility Methods
    
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return password; // Fallback (not secure, but prevents crash)
        }
    }
    
    private <T> void postSuccess(OnCompleteListener<T> listener, T result) {
        mainHandler.post(() -> {
            if (listener != null) {
                listener.onSuccess(result);
            }
        });
    }
    
    private <T> void postError(OnCompleteListener<T> listener, String error) {
        mainHandler.post(() -> {
            if (listener != null) {
                listener.onError(error);
            }
        });
    }
    
    // Admin methods
    public void getAllUsers(OnCompleteListener<List<UserEntity>> listener) {
        executorService.execute(() -> {
            try {
                List<UserEntity> users = database.userDao().getAllUsers();
                postSuccess(listener, users);
            } catch (Exception e) {
                postError(listener, e.getMessage());
            }
        });
    }
    
    // Doctor methods
    public void getDoctorsForPatients(OnCompleteListener<List<Doctor>> listener) {
        executorService.execute(() -> {
            try {
                List<UserEntity> doctorUsers = database.userDao().getUsersByRole("doctor");
                List<Doctor> doctors = new ArrayList<>();
                
                for (UserEntity user : doctorUsers) {
                    Doctor doctor = new Doctor();
                    doctor.setDoctorId(user.getUserId());
                    doctor.setUserId(user.getUserId());
                    doctor.setFullName(user.getFullName());
                    doctor.setEmail(user.getEmail());
                    doctor.setPhone(user.getPhone());
                    
                    // Get doctor-specific info from DoctorEntity
                    DoctorEntity doctorEntity = database.doctorDao().getDoctorById(user.getUserId());
                    if (doctorEntity != null && doctorEntity.isApproved()) {
                        // Only include approved doctors
                        doctor.setSpecialty(doctorEntity.getSpecialty() != null ? doctorEntity.getSpecialty() : "General Physician");
                        doctor.setConsultationFee(doctorEntity.getConsultationFee() > 0 ? doctorEntity.getConsultationFee() : 0.0);
                        
                        // Parse available times (format: "HH:mm-HH:mm" or comma-separated for backward compatibility)
                        if (doctorEntity.getAvailableTimes() != null && !doctorEntity.getAvailableTimes().isEmpty()) {
                            String timesStr = doctorEntity.getAvailableTimes();
                            List<String> timeList = new ArrayList<>();
                            
                            if (timesStr.contains("-")) {
                                // New format: "HH:mm-HH:mm" - generate time slots
                                String[] range = timesStr.split("-");
                                if (range.length == 2) {
                                    try {
                                        String[] fromParts = range[0].trim().split(":");
                                        String[] toParts = range[1].trim().split(":");
                                        if (fromParts.length == 2 && toParts.length == 2) {
                                            int fromHour = Integer.parseInt(fromParts[0]);
                                            int fromMinute = Integer.parseInt(fromParts[1]);
                                            int toHour = Integer.parseInt(toParts[0]);
                                            int toMinute = Integer.parseInt(toParts[1]);
                                            
                                            // Generate 30-minute intervals
                                            java.util.Calendar cal = java.util.Calendar.getInstance();
                                            cal.set(java.util.Calendar.HOUR_OF_DAY, fromHour);
                                            cal.set(java.util.Calendar.MINUTE, fromMinute);
                                            
                                            java.util.Calendar endCal = java.util.Calendar.getInstance();
                                            endCal.set(java.util.Calendar.HOUR_OF_DAY, toHour);
                                            endCal.set(java.util.Calendar.MINUTE, toMinute);
                                            
                                            while (!cal.after(endCal)) {
                                                int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
                                                int minute = cal.get(java.util.Calendar.MINUTE);
                                                timeList.add(String.format(java.util.Locale.getDefault(), "%02d:%02d", hour, minute));
                                                cal.add(java.util.Calendar.MINUTE, 30);
                                            }
                                        }
                                    } catch (Exception e) {
                                        // Fallback: use the range as-is
                                        timeList.add(range[0].trim());
                                        timeList.add(range[1].trim());
                                    }
                                }
                            } else {
                                // Old format: comma-separated times
                                String[] times = timesStr.split(",");
                                for (String time : times) {
                                    timeList.add(time.trim());
                                }
                            }
                            
                            doctor.setAvailableTimes(timeList);
                        }
                        
                        // Get average rating for doctor
                        Double avgRating = database.doctorRatingDao().getAverageRating(user.getUserId());
                        doctor.setRating(avgRating != null && avgRating > 0 ? avgRating.floatValue() : 0.0f);
                        
                        // Get total patients treated (using rating count as proxy for now)
                        int ratingCount = database.doctorRatingDao().getRatingCount(user.getUserId());
                        doctor.setPatientsTreated(ratingCount);
                        
                        doctor.setExperience(doctorEntity.getExperience() > 0 ? doctorEntity.getExperience() : 5); // Use real experience if available
                        doctors.add(doctor);
                    }
                    // Skip doctors without DoctorEntity or not approved
                }
                
                postSuccess(listener, doctors);
            } catch (Exception e) {
                postError(listener, e.getMessage());
            }
        });
    }
    
    // Audit Log methods
    public void saveAuditLog(AuditLog auditLog, OnCompleteListener<Void> listener) {
        executorService.execute(() -> {
            try {
                AuditLogEntity entity = new AuditLogEntity();
                entity.setLogId(auditLog.getLogId() != null ? auditLog.getLogId() : UUID.randomUUID().toString());
                entity.setUserId(auditLog.getUserId());
                entity.setUserName(auditLog.getUserName());
                entity.setUserRole(auditLog.getUserRole());
                entity.setAction(auditLog.getAction());
                entity.setDescription(auditLog.getDescription());
                entity.setEntityType(auditLog.getEntityType());
                entity.setEntityId(auditLog.getEntityId());
                entity.setTimestamp(auditLog.getTimestamp());
                entity.setIpAddress(auditLog.getIpAddress());
                entity.setDeviceInfo(auditLog.getDeviceInfo());
                
                database.auditLogDao().insert(entity);
                postSuccess(listener, null);
            } catch (Exception e) {
                postError(listener, e.getMessage());
            }
        });
    }
    
    public void getAllAuditLogs(OnCompleteListener<List<AuditLog>> listener) {
        executorService.execute(() -> {
            try {
                List<AuditLogEntity> entities = database.auditLogDao().getAllAuditLogs();
                
                List<AuditLog> auditLogs = new ArrayList<>();
                for (AuditLogEntity entity : entities) {
                    AuditLog log = new AuditLog();
                    log.setLogId(entity.getLogId());
                    log.setUserId(entity.getUserId());
                    log.setUserName(entity.getUserName());
                    log.setUserRole(entity.getUserRole());
                    log.setAction(entity.getAction());
                    log.setDescription(entity.getDescription());
                    log.setEntityType(entity.getEntityType());
                    log.setEntityId(entity.getEntityId());
                    log.setTimestamp(entity.getTimestamp());
                    log.setIpAddress(entity.getIpAddress());
                    log.setDeviceInfo(entity.getDeviceInfo());
                    auditLogs.add(log);
                }
                
                postSuccess(listener, auditLogs);
            } catch (Exception e) {
                postError(listener, e.getMessage());
            }
        });
    }
    
    public void clearAppointmentsForUser(String userId, String role, OnCompleteListener<Void> listener) {
        executorService.execute(() -> {
            try {
                // The original logic for clearing appointments involved Room database access,
                // which has been deprecated. This method should now be handled by FirebaseHelper if needed.
                // For now, we will simply return success.
                postSuccess(listener, null);
            } catch (Exception e) {
                postError(listener, e.getMessage());
            }
        });
    }
    
    // public void updateAppointment(Appointment appointment, OnCompleteListener<Void> listener) { ... } // Removed
    
    public void getDoctorById(String doctorId, OnCompleteListener<Doctor> listener) {
        executorService.execute(() -> {
            try {
                UserEntity user = database.userDao().getUserById(doctorId);
                if (user != null && "doctor".equals(user.getRole())) {
                    Doctor doctor = new Doctor();
                    doctor.setDoctorId(user.getUserId());
                    doctor.setUserId(user.getUserId());
                    doctor.setFullName(user.getFullName());
                    doctor.setEmail(user.getEmail());
                    doctor.setPhone(user.getPhone());
                    
                    // Get doctor-specific info from DoctorEntity
                    DoctorEntity doctorEntity = database.doctorDao().getDoctorById(doctorId);
                    if (doctorEntity != null && doctorEntity.isApproved()) {
                        // Only return approved doctors
                        doctor.setSpecialty(doctorEntity.getSpecialty() != null ? doctorEntity.getSpecialty() : "General Physician");
                        doctor.setConsultationFee(doctorEntity.getConsultationFee() > 0 ? doctorEntity.getConsultationFee() : 0.0);
                        
                        // Parse available times (format: "HH:mm-HH:mm" or comma-separated for backward compatibility)
                        if (doctorEntity.getAvailableTimes() != null && !doctorEntity.getAvailableTimes().isEmpty()) {
                            String timesStr = doctorEntity.getAvailableTimes();
                            List<String> timeList = new ArrayList<>();
                            
                            if (timesStr.contains("-")) {
                                // New format: "HH:mm-HH:mm" - generate time slots
                                String[] range = timesStr.split("-");
                                if (range.length == 2) {
                                    try {
                                        String[] fromParts = range[0].trim().split(":");
                                        String[] toParts = range[1].trim().split(":");
                                        if (fromParts.length == 2 && toParts.length == 2) {
                                            int fromHour = Integer.parseInt(fromParts[0]);
                                            int fromMinute = Integer.parseInt(fromParts[1]);
                                            int toHour = Integer.parseInt(toParts[0]);
                                            int toMinute = Integer.parseInt(toParts[1]);
                                            
                                            // Generate 30-minute intervals
                                            java.util.Calendar cal = java.util.Calendar.getInstance();
                                            cal.set(java.util.Calendar.HOUR_OF_DAY, fromHour);
                                            cal.set(java.util.Calendar.MINUTE, fromMinute);
                                            
                                            java.util.Calendar endCal = java.util.Calendar.getInstance();
                                            endCal.set(java.util.Calendar.HOUR_OF_DAY, toHour);
                                            endCal.set(java.util.Calendar.MINUTE, toMinute);
                                            
                                            while (!cal.after(endCal)) {
                                                int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
                                                int minute = cal.get(java.util.Calendar.MINUTE);
                                                timeList.add(String.format(java.util.Locale.getDefault(), "%02d:%02d", hour, minute));
                                                cal.add(java.util.Calendar.MINUTE, 30);
                                            }
                                        }
                                    } catch (Exception e) {
                                        // Fallback: use the range as-is
                                        timeList.add(range[0].trim());
                                        timeList.add(range[1].trim());
                                    }
                                }
                            } else {
                                // Old format: comma-separated times
                                String[] times = timesStr.split(",");
                                for (String time : times) {
                                    timeList.add(time.trim());
                                }
                            }
                            
                            doctor.setAvailableTimes(timeList);
                        }
                        
                        // Get average rating for doctor
                        Double avgRating = database.doctorRatingDao().getAverageRating(user.getUserId());
                        doctor.setRating(avgRating != null && avgRating > 0 ? avgRating.floatValue() : 0.0f);
                        
                        // Get total patients treated
                        int ratingCount = database.doctorRatingDao().getRatingCount(user.getUserId());
                        doctor.setPatientsTreated(ratingCount);
                        
                        doctor.setExperience(doctorEntity.getExperience() > 0 ? doctorEntity.getExperience() : 5);
                        postSuccess(listener, doctor);
                    } else {
                        // Doctor not approved or no DoctorEntity - return error
                        postError(listener, "Doctor not found or not approved");
                    }
                } else {
                    postError(listener, "Doctor not found");
                }
            } catch (Exception e) {
                postError(listener, e.getMessage());
            }
        });
    }
    
    // DoctorEntity methods
    public void getDoctorEntity(String doctorId, OnCompleteListener<DoctorEntity> listener) {
        executorService.execute(() -> {
            try {
                DoctorEntity doctorEntity = database.doctorDao().getDoctorById(doctorId);
                postSuccess(listener, doctorEntity);
            } catch (Exception e) {
                postError(listener, e.getMessage());
            }
        });
    }
    
    public void saveOrUpdateDoctor(DoctorEntity doctorEntity, OnCompleteListener<Boolean> listener) {
        executorService.execute(() -> {
            try {
                doctorEntity.setLastUpdated(System.currentTimeMillis());
                database.doctorDao().insertOrUpdate(doctorEntity);
                postSuccess(listener, true);
            } catch (Exception e) {
                postError(listener, e.getMessage());
            }
        });
    }
    
    public void updateUser(UserEntity user, OnCompleteListener<Void> listener) {
        executorService.execute(() -> {
            try {
                database.userDao().update(user);
                postSuccess(listener, null);
            } catch (Exception e) {
                postError(listener, e.getMessage());
            }
        });
    }
    
    public void deleteUser(String userId, OnCompleteListener<Void> listener) {
        executorService.execute(() -> {
            try {
                // Delete the user
                UserEntity user = database.userDao().getUserById(userId);
                if (user != null) {
                    database.userDao().delete(user);
                }
                
                postSuccess(listener, null);
            } catch (Exception e) {
                postError(listener, e.getMessage());
            }
        });
    }
    
    // News Post Operations
    
    public void createArticlePost(ArticlePostEntity post, OnCompleteListener<ArticlePostEntity> listener) {
        executorService.execute(() -> {
            try {
                // Ensure post ID is set
                if (post.getPostId() == null || post.getPostId().isEmpty()) {
                    post.setPostId(UUID.randomUUID().toString());
                }
                
                // Set timestamps
                long now = System.currentTimeMillis();
                if (post.getCreatedAt() == 0) {
                    post.setCreatedAt(now);
                }
                post.setUpdatedAt(now);
                
                // Ensure required fields have default values
                if (post.getType() == null || post.getType().isEmpty()) {
                    post.setType("image");
                }
                if (post.getStatus() == null || post.getStatus().isEmpty()) {
                    post.setStatus("draft");
                }
                if (post.getTitle() == null) {
                    post.setTitle("");
                }
                if (post.getDescription() == null) {
                    post.setDescription("");
                }
                if (post.getProfileName() == null) {
                    post.setProfileName("HASET Admin");
                }
                if (post.getTags() == null) {
                    post.setTags("");
                }
                
                // Insert into database
                database.articlePostDao().insert(post);
                postSuccess(listener, post);
            } catch (Exception e) {
                postError(listener, "Failed to create post: " + e.getMessage());
            }
        });
    }
    
    public void updateArticlePost(ArticlePostEntity post, OnCompleteListener<ArticlePostEntity> listener) {
        executorService.execute(() -> {
            try {
                // Ensure post ID exists
                if (post.getPostId() == null || post.getPostId().isEmpty()) {
                    postError(listener, "Post ID is required for update");
                    return;
                }
                
                // Update timestamp
                post.setUpdatedAt(System.currentTimeMillis());
                
                // Ensure required fields have default values
                if (post.getType() == null || post.getType().isEmpty()) {
                    post.setType("image");
                }
                if (post.getStatus() == null || post.getStatus().isEmpty()) {
                    post.setStatus("draft");
                }
                if (post.getTitle() == null) {
                    post.setTitle("");
                }
                if (post.getDescription() == null) {
                    post.setDescription("");
                }
                if (post.getProfileName() == null) {
                    post.setProfileName("HASET Admin");
                }
                if (post.getTags() == null) {
                    post.setTags("");
                }
                
                // Update in database
                database.articlePostDao().update(post);
                postSuccess(listener, post);
            } catch (Exception e) {
                postError(listener, "Failed to update post: " + e.getMessage());
            }
        });
    }
    
    public void deleteArticlePost(String postId, OnCompleteListener<Boolean> listener) {
        executorService.execute(() -> {
            try {
                database.articlePostDao().deletePostById(postId);
                postSuccess(listener, true);
            } catch (Exception e) {
                postError(listener, e.getMessage());
            }
        });
    }
    
    public void getArticlePostById(String postId, OnCompleteListener<ArticlePostEntity> listener) {
        executorService.execute(() -> {
            try {
                ArticlePostEntity post = database.articlePostDao().getPostById(postId);
                postSuccess(listener, post);
            } catch (Exception e) {
                postError(listener, e.getMessage());
            }
        });
    }
    
    public void getAllArticlePosts(OnCompleteListener<List<ArticlePostEntity>> listener) {
        executorService.execute(() -> {
            try {
                List<ArticlePostEntity> posts = database.articlePostDao().getAllPosts();
                postSuccess(listener, posts);
            } catch (Exception e) {
                postError(listener, e.getMessage());
            }
        });
    }
    
    public void getPublishedArticlePosts(OnCompleteListener<List<ArticlePostEntity>> listener) {
        executorService.execute(() -> {
            try {
                List<ArticlePostEntity> posts = database.articlePostDao().getPublishedPosts();
                postSuccess(listener, posts);
            } catch (Exception e) {
                postError(listener, e.getMessage());
            }
        });
    }
    
    public void getDraftArticlePosts(OnCompleteListener<List<ArticlePostEntity>> listener) {
        executorService.execute(() -> {
            try {
                List<ArticlePostEntity> posts = database.articlePostDao().getDraftPosts();
                postSuccess(listener, posts);
            } catch (Exception e) {
                postError(listener, e.getMessage());
            }
        });
    }
    
    public void getArticlePostsByType(String type, OnCompleteListener<List<ArticlePostEntity>> listener) {
        executorService.execute(() -> {
            try {
                List<ArticlePostEntity> posts = database.articlePostDao().getPostsByType(type);
                postSuccess(listener, posts);
            } catch (Exception e) {
                postError(listener, e.getMessage());
            }
        });
    }
    
    // Doctor Wallet Operations
    
    public void getDoctorWallet(String doctorId, OnCompleteListener<DoctorWalletEntity> listener) {
        executorService.execute(() -> {
            try {
                DoctorWalletEntity wallet = database.doctorWalletDao().getWalletByDoctorId(doctorId);
                postSuccess(listener, wallet);
            } catch (Exception e) {
                postError(listener, e.getMessage());
            }
        });
    }
    
    public void addToDoctorWallet(String doctorId, double amount, OnCompleteListener<Boolean> listener) {
        executorService.execute(() -> {
            try {
                // Get or create wallet
                DoctorWalletEntity wallet = database.doctorWalletDao().getWalletByDoctorId(doctorId);
                if (wallet == null) {
                    // Create new wallet
                    wallet = new DoctorWalletEntity(doctorId, 0);
                    database.doctorWalletDao().insertOrUpdate(wallet);
                }
                
                // Add to balance
                long timestamp = System.currentTimeMillis();
                database.doctorWalletDao().addToBalance(doctorId, amount, timestamp);
                postSuccess(listener, true);
            } catch (Exception e) {
                postError(listener, e.getMessage());
            }
        });
    }
    
    public void createOrUpdateWallet(String doctorId, double balance, OnCompleteListener<DoctorWalletEntity> listener) {
        executorService.execute(() -> {
            try {
                DoctorWalletEntity wallet = new DoctorWalletEntity(doctorId, balance);
                wallet.setLastUpdated(System.currentTimeMillis());
                database.doctorWalletDao().insertOrUpdate(wallet);
                postSuccess(listener, wallet);
            } catch (Exception e) {
                postError(listener, e.getMessage());
            }
        });
    }
    
    public void deductFromDoctorWallet(String doctorId, double amount, OnCompleteListener<Boolean> listener) {
        executorService.execute(() -> {
            try {
                // Check if wallet exists and has sufficient balance
                DoctorWalletEntity wallet = database.doctorWalletDao().getWalletByDoctorId(doctorId);
                if (wallet == null || wallet.getBalance() < amount) {
                    postSuccess(listener, false);
                    return;
                }
                
                // Deduct from balance
                long timestamp = System.currentTimeMillis();
                int rowsAffected = database.doctorWalletDao().deductFromBalance(doctorId, amount, timestamp);
                
                if (rowsAffected > 0) {
                    postSuccess(listener, true);
                } else {
                    postSuccess(listener, false);
                }
            } catch (Exception e) {
                postError(listener, e.getMessage());
            }
        });
    }
    
    // Doctor Rating Operations
    
    public void createOrUpdateRating(DoctorRatingEntity rating, OnCompleteListener<DoctorRatingEntity> listener) {
        executorService.execute(() -> {
            try {
                rating.setCreatedAt(System.currentTimeMillis());
                database.doctorRatingDao().insert(rating);
                postSuccess(listener, rating);
            } catch (Exception e) {
                postError(listener, e.getMessage());
            }
        });
    }
    
    public void getAverageRating(String doctorId, OnCompleteListener<Double> listener) {
        executorService.execute(() -> {
            try {
                Double average = database.doctorRatingDao().getAverageRating(doctorId);
                postSuccess(listener, average != null ? average : 0.0);
            } catch (Exception e) {
                postError(listener, e.getMessage());
            }
        });
    }
    
    public void getRatingCount(String doctorId, OnCompleteListener<Integer> listener) {
        executorService.execute(() -> {
            try {
                int count = database.doctorRatingDao().getRatingCount(doctorId);
                postSuccess(listener, count);
            } catch (Exception e) {
                postError(listener, e.getMessage());
            }
        });
    }
    
    public void getRatingsByDoctorId(String doctorId, OnCompleteListener<List<DoctorRatingEntity>> listener) {
        executorService.execute(() -> {
            try {
                List<DoctorRatingEntity> ratings = database.doctorRatingDao().getRatingsByDoctorId(doctorId);
                postSuccess(listener, ratings);
            } catch (Exception e) {
                postError(listener, e.getMessage());
            }
        });
    }
    
    public void getRatingByDoctorAndPatient(String doctorId, String patientId, OnCompleteListener<DoctorRatingEntity> listener) {
        executorService.execute(() -> {
            try {
                DoctorRatingEntity rating = database.doctorRatingDao().getRatingByDoctorAndPatient(doctorId, patientId);
                postSuccess(listener, rating);
            } catch (Exception e) {
                postError(listener, e.getMessage());
            }
        });
    }
    
    public void getRatingByAppointmentId(String appointmentId, OnCompleteListener<DoctorRatingEntity> listener) {
        executorService.execute(() -> {
            try {
                DoctorRatingEntity rating = database.doctorRatingDao().getRatingByAppointmentId(appointmentId);
                postSuccess(listener, rating);
            } catch (Exception e) {
                postError(listener, e.getMessage());
            }
        });
    }

    public void clearAllData(OnCompleteListener<Void> listener) {
        executorService.execute(() -> {
            try {
                database.clearAllTables();
                postSuccess(listener, null);
            } catch (Exception e) {
                postError(listener, e.getMessage());
            }
        });
    }

    // Callback Interface
    public interface OnCompleteListener<T> {
        void onSuccess(T result);
        void onError(String error);
    }
}
