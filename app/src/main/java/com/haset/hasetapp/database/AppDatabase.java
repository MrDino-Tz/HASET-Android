package com.haset.hasetapp.database;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.haset.hasetapp.database.dao.AuditLogDao;
import com.haset.hasetapp.database.dao.DoctorDao;
import com.haset.hasetapp.database.dao.DoctorRatingDao;
import com.haset.hasetapp.database.dao.DoctorWalletDao;
import com.haset.hasetapp.database.dao.ArticlePostDao;
import com.haset.hasetapp.database.dao.UserDao;
import com.haset.hasetapp.database.dao.PrescriptionDao;
import com.haset.hasetapp.database.dao.WithdrawalRequestDao;
import com.haset.hasetapp.database.entities.AuditLogEntity;
import com.haset.hasetapp.database.entities.DoctorEntity;
import com.haset.hasetapp.database.entities.DoctorRatingEntity;
import com.haset.hasetapp.database.entities.DoctorWalletEntity;
import com.haset.hasetapp.database.entities.ArticlePostEntity;
import com.haset.hasetapp.database.entities.UserEntity;
import com.haset.hasetapp.database.entities.PrescriptionEntity;
import com.haset.hasetapp.database.entities.WithdrawalRequest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main Room Database for HASETApp
 * 
 * Database Versions:
 * - Version 1: Users and Appointments tables
 * - Version 2: Added AuditLogs table
 * - Version 3: Added NewsPosts table
 * - Version 4: Added DoctorWallets table
 * - Version 5: Added Doctors table
 * - Version 6: Added profileImage column to DoctorEntity
 * - Version 7: Added profileImage column to UserEntity
 * - Version 8: Added averageRating and experience to DoctorEntity
 * - Version 9: Added appointmentType to AppointmentEntity
 * - Version 10: Removed Appointments related entities and DAOs due to Firebase migration
 * - Version 11: Added authorId column to NewsPostEntity
 * - Version 12: (Previous update)
 * - Version 13: (Previous update)
 * - Version 14: Added isOnline and onlineStatus to DoctorEntity
 * - Version 15: Added chat duration tracking to AppointmentEntity
 * - Version 16: Added WithdrawalRequest entity for wallet withdrawal system
 * - Version 18: Added imageUrl, views, and authorId to ArticlePostEntity
 * - Version 19: Added isDemo field to DoctorEntity
 * - Version 20: Removed the obsolete local password column; authentication is Firebase-only
 */
@Database(entities = {UserEntity.class, DoctorEntity.class, DoctorRatingEntity.class, DoctorWalletEntity.class, ArticlePostEntity.class, AuditLogEntity.class, PrescriptionEntity.class, WithdrawalRequest.class}, version = 20, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    
    private static final String TAG = "AppDatabase";
    private static AppDatabase instance;
    
    public abstract UserDao userDao();
    public abstract DoctorDao doctorDao();
    public abstract DoctorRatingDao doctorRatingDao();
    public abstract DoctorWalletDao doctorWalletDao();
    public abstract ArticlePostDao articlePostDao();
    public abstract WithdrawalRequestDao withdrawalRequestDao();
    public abstract AuditLogDao auditLogDao();
    public abstract PrescriptionDao prescriptionDao();
    
    /**
     * Migration from version 1 to 2
     * Adds audit_logs table and profileImage column to users table
     */
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 1 to 2");
            
            try {
                database.beginTransaction();
                
                // Create audit_logs table
                database.execSQL("CREATE TABLE IF NOT EXISTS audit_logs (" +
                    "logId TEXT NOT NULL PRIMARY KEY, " +
                    "userId TEXT, " +
                    "userName TEXT, " +
                    "userRole TEXT, " +
                    "action TEXT, " +
                    "description TEXT, " +
                    "entityType TEXT, " +
                    "entityId TEXT, " +
                    "timestamp INTEGER NOT NULL, " +
                    "ipAddress TEXT, " +
                    "deviceInfo TEXT" +
                    ")");
                
                // Add profileImage column to users table if it doesn't exist
                // SQLite doesn't support IF NOT EXISTS for ALTER TABLE ADD COLUMN
                // So we use a try-catch approach or check first
                try {
                    database.execSQL("ALTER TABLE users ADD COLUMN profileImage TEXT");
                    Log.d(TAG, "Added profileImage column to users table");
                } catch (Exception e) {
                    // Column might already exist, which is fine
                    Log.d(TAG, "profileImage column may already exist: " + e.getMessage());
                }
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 1→2 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 1→2: " + e.getMessage(), e);
                throw e; // Re-throw to let Room handle it
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 2 to 3
     * Adds news_posts table
     */
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 2 to 3");
            
            try {
                database.beginTransaction();
                
                // Create news_posts table
                database.execSQL("CREATE TABLE IF NOT EXISTS news_posts (" +
                    "postId TEXT NOT NULL PRIMARY KEY, " +
                    "type TEXT, " +
                    "title TEXT, " +
                    "description TEXT, " +
                    "profileName TEXT, " +
                    "tags TEXT, " +
                    "music TEXT, " +
                    "videoPath TEXT, " +
                    "imagePath TEXT, " +
                    "thumbnailPath TEXT, " +
                    "status TEXT, " +
                    "likes INTEGER NOT NULL DEFAULT 0, " +
                    "comments INTEGER NOT NULL DEFAULT 0, " +
                    "shares INTEGER NOT NULL DEFAULT 0, " +
                    "createdAt INTEGER NOT NULL, " +
                    "updatedAt INTEGER NOT NULL" +
                    ")");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 2→3 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 2→3: " + e.getMessage(), e);
                throw e; // Re-throw to let Room handle it
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 1 to 3 (for users who skip version 2)
     * Combines both migrations
     */
    static final Migration MIGRATION_1_3 = new Migration(1, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 1 to 3 (skipping version 2)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Create audit_logs table (from migration 1→2)
                database.execSQL("CREATE TABLE IF NOT EXISTS audit_logs (" +
                    "logId TEXT NOT NULL PRIMARY KEY, " +
                    "userId TEXT, " +
                    "userName TEXT, " +
                    "userRole TEXT, " +
                    "action TEXT, " +
                    "description TEXT, " +
                    "entityType TEXT, " +
                    "entityId TEXT, " +
                    "timestamp INTEGER NOT NULL, " +
                    "ipAddress TEXT, " +
                    "deviceInfo TEXT" +
                    ")");
                
                // Step 2: Add profileImage column to users table
                try {
                    database.execSQL("ALTER TABLE users ADD COLUMN profileImage TEXT");
                    Log.d(TAG, "Added profileImage column to users table");
                } catch (Exception e) {
                    Log.d(TAG, "profileImage column may already exist: " + e.getMessage());
                }
                
                // Step 3: Create news_posts table (from migration 2→3)
                database.execSQL("CREATE TABLE IF NOT EXISTS news_posts (" +
                    "postId TEXT NOT NULL PRIMARY KEY, " +
                    "type TEXT, " +
                    "title TEXT, " +
                    "description TEXT, " +
                    "profileName TEXT, " +
                    "tags TEXT, " +
                    "music TEXT, " +
                    "videoPath TEXT, " +
                    "imagePath TEXT, " +
                    "thumbnailPath TEXT, " +
                    "status TEXT, " +
                    "likes INTEGER NOT NULL DEFAULT 0, " +
                    "comments INTEGER NOT NULL DEFAULT 0, " +
                    "shares INTEGER NOT NULL DEFAULT 0, " +
                    "createdAt INTEGER NOT NULL, " +
                    "updatedAt INTEGER NOT NULL" +
                    ")");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 1→3 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 1→3: " + e.getMessage(), e);
                throw e; // Re-throw to let Room handle it
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 3 to 4
     * Adds doctor_wallets table
     */
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 3 to 4");
            
            try {
                database.beginTransaction();
                
                // Create doctor_wallets table
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_wallets (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "balance REAL NOT NULL DEFAULT 0, " +
                    "totalEarnings REAL NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 3→4 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 3→4: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 2 to 4 (for users who skip version 3)
     */
    static final Migration MIGRATION_2_4 = new Migration(2, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 2 to 4 (skipping version 3)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Create news_posts table (from migration 2→3)
                database.execSQL("CREATE TABLE IF NOT EXISTS news_posts (" +
                    "postId TEXT NOT NULL PRIMARY KEY, " +
                    "type TEXT, " +
                    "title TEXT, " +
                    "description TEXT, " +
                    "profileName TEXT, " +
                    "tags TEXT, " +
                    "music TEXT, " +
                    "videoPath TEXT, " +
                    "imagePath TEXT, " +
                    "thumbnailPath TEXT, " +
                    "status TEXT, " +
                    "likes INTEGER NOT NULL DEFAULT 0, " +
                    "comments INTEGER NOT NULL DEFAULT 0, " +
                    "shares INTEGER NOT NULL DEFAULT 0, " +
                    "createdAt INTEGER NOT NULL, " +
                    "updatedAt INTEGER NOT NULL" +
                    ")");
                
                // Step 2: Create doctor_wallets table (from migration 3→4)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_wallets (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "balance REAL NOT NULL DEFAULT 0, " +
                    "totalEarnings REAL NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 2→4 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 2→4: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 1 to 4 (for users who skip versions 2 and 3)
     */
    static final Migration MIGRATION_1_4 = new Migration(1, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 1 to 4 (skipping versions 2 and 3)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Create audit_logs table (from migration 1→2)
                database.execSQL("CREATE TABLE IF NOT EXISTS audit_logs (" +
                    "logId TEXT NOT NULL PRIMARY KEY, " +
                    "userId TEXT, " +
                    "userName TEXT, " +
                    "userRole TEXT, " +
                    "action TEXT, " +
                    "description TEXT, " +
                    "entityType TEXT, " +
                    "entityId TEXT, " +
                    "timestamp INTEGER NOT NULL, " +
                    "ipAddress TEXT, " +
                    "deviceInfo TEXT" +
                    ")");
                
                // Step 2: Add profileImage column to users table
                try {
                    database.execSQL("ALTER TABLE users ADD COLUMN profileImage TEXT");
                    Log.d(TAG, "Added profileImage column to users table");
                } catch (Exception e) {
                    Log.d(TAG, "profileImage column may already exist: " + e.getMessage());
                }
                
                // Step 3: Create news_posts table (from migration 2→3)
                database.execSQL("CREATE TABLE IF NOT EXISTS news_posts (" +
                    "postId TEXT NOT NULL PRIMARY KEY, " +
                    "type TEXT, " +
                    "title TEXT, " +
                    "description TEXT, " +
                    "profileName TEXT, " +
                    "tags TEXT, " +
                    "music TEXT, " +
                    "videoPath TEXT, " +
                    "imagePath TEXT, " +
                    "thumbnailPath TEXT, " +
                    "status TEXT, " +
                    "likes INTEGER NOT NULL DEFAULT 0, " +
                    "comments INTEGER NOT NULL DEFAULT 0, " +
                    "shares INTEGER NOT NULL DEFAULT 0, " +
                    "createdAt INTEGER NOT NULL, " +
                    "updatedAt INTEGER NOT NULL" +
                    ")");
                
                // Step 4: Create doctor_wallets table (from migration 3→4)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_wallets (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "balance REAL NOT NULL DEFAULT 0, " +
                    "totalEarnings REAL NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 1→4 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 1→4: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 4 to 5
     * Adds doctors table
     */
    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 4 to 5");
            
            try {
                database.beginTransaction();
                
                // Create doctors table
                database.execSQL("CREATE TABLE IF NOT EXISTS doctors (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "specialty TEXT, " +
                    "consultationFee REAL NOT NULL DEFAULT 0, " +
                    "availableTimes TEXT, " +
                    "isApproved INTEGER NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 4→5 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 4→5: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 3 to 5 (for users who skip version 4)
     */
    static final Migration MIGRATION_3_5 = new Migration(3, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 3 to 5 (skipping version 4)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Create doctor_wallets table (from migration 3→4)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_wallets (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "balance REAL NOT NULL DEFAULT 0, " +
                    "totalEarnings REAL NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 2: Create doctors table (from migration 4→5)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctors (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "specialty TEXT, " +
                    "consultationFee REAL NOT NULL DEFAULT 0, " +
                    "availableTimes TEXT, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 3→5 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 3→5: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 2 to 5 (for users who skip versions 3 and 4)
     */
    static final Migration MIGRATION_2_5 = new Migration(2, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 2 to 5 (skipping versions 3 and 4)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Create news_posts table (from migration 2→3)
                database.execSQL("CREATE TABLE IF NOT EXISTS news_posts (" +
                    "postId TEXT NOT NULL PRIMARY KEY, " +
                    "type TEXT, " +
                    "title TEXT, " +
                    "description TEXT, " +
                    "profileName TEXT, " +
                    "tags TEXT, " +
                    "music TEXT, " +
                    "videoPath TEXT, " +
                    "imagePath TEXT, " +
                    "thumbnailPath TEXT, " +
                    "status TEXT, " +
                    "likes INTEGER NOT NULL DEFAULT 0, " +
                    "comments INTEGER NOT NULL DEFAULT 0, " +
                    "shares INTEGER NOT NULL DEFAULT 0, " +
                    "createdAt INTEGER NOT NULL, " +
                    "updatedAt INTEGER NOT NULL" +
                    ")");
                
                // Step 2: Create doctor_wallets table (from migration 3→4)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_wallets (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "balance REAL NOT NULL DEFAULT 0, " +
                    "totalEarnings REAL NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 3: Create doctors table (from migration 4→5)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctors (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "specialty TEXT, " +
                    "consultationFee REAL NOT NULL DEFAULT 0, " +
                    "availableTimes TEXT, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 2→5 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 2→5: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 1 to 5 (for users who skip versions 2, 3, and 4)
     */
    static final Migration MIGRATION_1_5 = new Migration(1, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 1 to 5 (skipping versions 2, 3, and 4)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Create audit_logs table (from migration 1→2)
                database.execSQL("CREATE TABLE IF NOT EXISTS audit_logs (" +
                    "logId TEXT NOT NULL PRIMARY KEY, " +
                    "userId TEXT, " +
                    "userName TEXT, " +
                    "userRole TEXT, " +
                    "action TEXT, " +
                    "description TEXT, " +
                    "entityType TEXT, " +
                    "entityId TEXT, " +
                    "timestamp INTEGER NOT NULL, " +
                    "ipAddress TEXT, " +
                    "deviceInfo TEXT" +
                    ")");
                
                // Step 2: Add profileImage column to users table
                try {
                    database.execSQL("ALTER TABLE users ADD COLUMN profileImage TEXT");
                    Log.d(TAG, "Added profileImage column to users table");
                } catch (Exception e) {
                    Log.d(TAG, "profileImage column may already exist: " + e.getMessage());
                }
                
                // Step 3: Create news_posts table (from migration 2→3)
                database.execSQL("CREATE TABLE IF NOT EXISTS news_posts (" +
                    "postId TEXT NOT NULL PRIMARY KEY, " +
                    "type TEXT, " +
                    "title TEXT, " +
                    "description TEXT, " +
                    "profileName TEXT, " +
                    "tags TEXT, " +
                    "music TEXT, " +
                    "videoPath TEXT, " +
                    "imagePath TEXT, " +
                    "thumbnailPath TEXT, " +
                    "status TEXT, " +
                    "likes INTEGER NOT NULL DEFAULT 0, " +
                    "comments INTEGER NOT NULL DEFAULT 0, " +
                    "shares INTEGER NOT NULL DEFAULT 0, " +
                    "createdAt INTEGER NOT NULL, " +
                    "updatedAt INTEGER NOT NULL" +
                    ")");
                
                // Step 4: Create doctor_wallets table (from migration 3→4)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_wallets (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "balance REAL NOT NULL DEFAULT 0, " +
                    "totalEarnings REAL NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 5: Create doctors table with isApproved (from migration 4→5)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctors (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "specialty TEXT, " +
                    "consultationFee REAL NOT NULL DEFAULT 0, " +
                    "availableTimes TEXT, " +
                    "isApproved INTEGER NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 1→5 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 1→5: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 5 to 6
     * Adds isApproved field to doctors table
     */
    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 5 to 6");
            
            try {
                database.beginTransaction();
                
                // Add isApproved column to doctors table
                database.execSQL("ALTER TABLE doctors ADD COLUMN isApproved INTEGER NOT NULL DEFAULT 0");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 5→6 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 5→6: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 4 to 6 (for users who skip version 5)
     */
    static final Migration MIGRATION_4_6 = new Migration(4, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 4 to 6 (skipping version 5)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Create doctors table with isApproved (from migration 4→5→6)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctors (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "specialty TEXT, " +
                    "consultationFee REAL NOT NULL DEFAULT 0, " +
                    "availableTimes TEXT, " +
                    "isApproved INTEGER NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 4→6 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 4→6: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 3 to 6 (for users who skip versions 4 and 5)
     */
    static final Migration MIGRATION_3_6 = new Migration(3, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 3 to 6 (skipping versions 4 and 5)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Create doctor_wallets table (from migration 3→4)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_wallets (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "balance REAL NOT NULL DEFAULT 0, " +
                    "totalEarnings REAL NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 2: Create doctors table with isApproved (from migration 4→5→6)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctors (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "specialty TEXT, " +
                    "consultationFee REAL NOT NULL DEFAULT 0, " +
                    "availableTimes TEXT, " +
                    "isApproved INTEGER NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 3→6 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 3→6: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 2 to 6 (for users who skip versions 3, 4, and 5)
     */
    static final Migration MIGRATION_2_6 = new Migration(2, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 2 to 6 (skipping versions 3, 4, and 5)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Create news_posts table (from migration 2→3)
                database.execSQL("CREATE TABLE IF NOT EXISTS news_posts (" +
                    "postId TEXT NOT NULL PRIMARY KEY, " +
                    "type TEXT, " +
                    "title TEXT, " +
                    "description TEXT, " +
                    "profileName TEXT, " +
                    "tags TEXT, " +
                    "music TEXT, " +
                    "videoPath TEXT, " +
                    "imagePath TEXT, " +
                    "thumbnailPath TEXT, " +
                    "status TEXT, " +
                    "likes INTEGER NOT NULL DEFAULT 0, " +
                    "comments INTEGER NOT NULL DEFAULT 0, " +
                    "shares INTEGER NOT NULL DEFAULT 0, " +
                    "createdAt INTEGER NOT NULL, " +
                    "updatedAt INTEGER NOT NULL" +
                    ")");
                
                // Step 2: Create doctor_wallets table (from migration 3→4)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_wallets (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "balance REAL NOT NULL DEFAULT 0, " +
                    "totalEarnings REAL NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 3: Create doctors table with isApproved (from migration 4→5→6)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctors (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "specialty TEXT, " +
                    "consultationFee REAL NOT NULL DEFAULT 0, " +
                    "availableTimes TEXT, " +
                    "isApproved INTEGER NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 2→6 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 2→6: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 1 to 6 (for users who skip versions 2, 3, 4, and 5)
     */
    static final Migration MIGRATION_1_6 = new Migration(1, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 1 to 6 (skipping versions 2, 3, 4, and 5)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Create audit_logs table (from migration 1→2)
                database.execSQL("CREATE TABLE IF NOT EXISTS audit_logs (" +
                    "logId TEXT NOT NULL PRIMARY KEY, " +
                    "userId TEXT, " +
                    "userName TEXT, " +
                    "userRole TEXT, " +
                    "action TEXT, " +
                    "description TEXT, " +
                    "entityType TEXT, " +
                    "entityId TEXT, " +
                    "timestamp INTEGER NOT NULL, " +
                    "ipAddress TEXT, " +
                    "deviceInfo TEXT" +
                    ")");
                
                // Step 2: Add profileImage column to users table
                try {
                    database.execSQL("ALTER TABLE users ADD COLUMN profileImage TEXT");
                    Log.d(TAG, "Added profileImage column to users table");
                } catch (Exception e) {
                    Log.d(TAG, "profileImage column may already exist: " + e.getMessage());
                }
                
                // Step 3: Create news_posts table (from migration 2→3)
                database.execSQL("CREATE TABLE IF NOT EXISTS news_posts (" +
                    "postId TEXT NOT NULL PRIMARY KEY, " +
                    "type TEXT, " +
                    "title TEXT, " +
                    "description TEXT, " +
                    "profileName TEXT, " +
                    "tags TEXT, " +
                    "music TEXT, " +
                    "videoPath TEXT, " +
                    "imagePath TEXT, " +
                    "thumbnailPath TEXT, " +
                    "status TEXT, " +
                    "likes INTEGER NOT NULL DEFAULT 0, " +
                    "comments INTEGER NOT NULL DEFAULT 0, " +
                    "shares INTEGER NOT NULL DEFAULT 0, " +
                    "createdAt INTEGER NOT NULL, " +
                    "updatedAt INTEGER NOT NULL" +
                    ")");
                
                // Step 4: Create doctor_wallets table (from migration 3→4)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_wallets (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "balance REAL NOT NULL DEFAULT 0, " +
                    "totalEarnings REAL NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 5: Create doctors table with isApproved (from migration 4→5→6)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctors (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "specialty TEXT, " +
                    "consultationFee REAL NOT NULL DEFAULT 0, " +
                    "availableTimes TEXT, " +
                    "isApproved INTEGER NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 1→6 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 1→6: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 6 to 7
     * Adds doctor_ratings table
     */
    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 6 to 7");
            
            try {
                database.beginTransaction();
                
                // Create doctor_ratings table
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_ratings (" +
                    "ratingId TEXT NOT NULL PRIMARY KEY, " +
                    "doctorId TEXT NOT NULL, " +
                    "patientId TEXT NOT NULL, " +
                    "patientName TEXT, " +
                    "rating REAL NOT NULL, " +
                    "comment TEXT, " +
                    "appointmentId TEXT, " +
                    "createdAt INTEGER NOT NULL" +
                    ")");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 6→7 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 6→7: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 5 to 7 (for users who skip version 6)
     */
    static final Migration MIGRATION_5_7 = new Migration(5, 7) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 5 to 7 (skipping version 6)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Add isApproved column to doctors table (from migration 5→6)
                database.execSQL("ALTER TABLE doctors ADD COLUMN isApproved INTEGER NOT NULL DEFAULT 0");
                
                // Step 2: Create doctor_ratings table (from migration 6→7)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_ratings (" +
                    "ratingId TEXT NOT NULL PRIMARY KEY, " +
                    "doctorId TEXT NOT NULL, " +
                    "patientId TEXT NOT NULL, " +
                    "patientName TEXT, " +
                    "rating REAL NOT NULL, " +
                    "comment TEXT, " +
                    "appointmentId TEXT, " +
                    "createdAt INTEGER NOT NULL" +
                    ")");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 5→7 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 5→7: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 4 to 7 (for users who skip versions 5 and 6)
     */
    static final Migration MIGRATION_4_7 = new Migration(4, 7) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 4 to 7 (skipping versions 5 and 6)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Create doctors table with isApproved (from migration 4→5→6)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctors (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "specialty TEXT, " +
                    "consultationFee REAL NOT NULL DEFAULT 0, " +
                    "availableTimes TEXT, " +
                    "isApproved INTEGER NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 2: Create doctor_ratings table (from migration 6→7)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_ratings (" +
                    "ratingId TEXT NOT NULL PRIMARY KEY, " +
                    "doctorId TEXT NOT NULL, " +
                    "patientId TEXT NOT NULL, " +
                    "patientName TEXT, " +
                    "rating REAL NOT NULL, " +
                    "comment TEXT, " +
                    "appointmentId TEXT, " +
                    "createdAt INTEGER NOT NULL" +
                    ")");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 4→7 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 4→7: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 3 to 7 (for users who skip versions 4, 5, and 6)
     */
    static final Migration MIGRATION_3_7 = new Migration(3, 7) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 3 to 7 (skipping versions 4, 5, and 6)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Create doctor_wallets table (from migration 3→4)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_wallets (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "balance REAL NOT NULL DEFAULT 0, " +
                    "totalEarnings REAL NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 2: Create doctors table with isApproved (from migration 4→5→6)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctors (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "specialty TEXT, " +
                    "consultationFee REAL NOT NULL DEFAULT 0, " +
                    "availableTimes TEXT, " +
                    "isApproved INTEGER NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 3: Create doctor_ratings table (from migration 6→7)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_ratings (" +
                    "ratingId TEXT NOT NULL PRIMARY KEY, " +
                    "doctorId TEXT NOT NULL, " +
                    "patientId TEXT NOT NULL, " +
                    "patientName TEXT, " +
                    "rating REAL NOT NULL, " +
                    "comment TEXT, " +
                    "appointmentId TEXT, " +
                    "createdAt INTEGER NOT NULL" +
                    ")");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 3→7 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 3→7: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 2 to 7 (for users who skip versions 3, 4, 5, and 6)
     */
    static final Migration MIGRATION_2_7 = new Migration(2, 7) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 2 to 7 (skipping versions 3, 4, 5, and 6)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Create news_posts table (from migration 2→3)
                database.execSQL("CREATE TABLE IF NOT EXISTS news_posts (" +
                    "postId TEXT NOT NULL PRIMARY KEY, " +
                    "type TEXT, " +
                    "title TEXT, " +
                    "description TEXT, " +
                    "profileName TEXT, " +
                    "tags TEXT, " +
                    "music TEXT, " +
                    "videoPath TEXT, " +
                    "imagePath TEXT, " +
                    "thumbnailPath TEXT, " +
                    "status TEXT, " +
                    "likes INTEGER NOT NULL DEFAULT 0, " +
                    "comments INTEGER NOT NULL DEFAULT 0, " +
                    "shares INTEGER NOT NULL DEFAULT 0, " +
                    "createdAt INTEGER NOT NULL, " +
                    "updatedAt INTEGER NOT NULL" +
                    ")");
                
                // Step 2: Create doctor_wallets table (from migration 3→4)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_wallets (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "balance REAL NOT NULL DEFAULT 0, " +
                    "totalEarnings REAL NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 3: Create doctors table with isApproved (from migration 4→5→6)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctors (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "specialty TEXT, " +
                    "consultationFee REAL NOT NULL DEFAULT 0, " +
                    "availableTimes TEXT, " +
                    "isApproved INTEGER NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 4: Create doctor_ratings table (from migration 6→7)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_ratings (" +
                    "ratingId TEXT NOT NULL PRIMARY KEY, " +
                    "doctorId TEXT NOT NULL, " +
                    "patientId TEXT NOT NULL, " +
                    "patientName TEXT, " +
                    "rating REAL NOT NULL, " +
                    "comment TEXT, " +
                    "appointmentId TEXT, " +
                    "createdAt INTEGER NOT NULL" +
                    ")");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 2→7 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 2→7: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 1 to 7 (for users who skip versions 2, 3, 4, 5, and 6)
     */
    static final Migration MIGRATION_1_7 = new Migration(1, 7) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 1 to 7 (skipping versions 2, 3, 4, 5, and 6)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Create audit_logs table (from migration 1→2)
                database.execSQL("CREATE TABLE IF NOT EXISTS audit_logs (" +
                    "logId TEXT NOT NULL PRIMARY KEY, " +
                    "userId TEXT, " +
                    "userName TEXT, " +
                    "userRole TEXT, " +
                    "action TEXT, " +
                    "description TEXT, " +
                    "entityType TEXT, " +
                    "entityId TEXT, " +
                    "timestamp INTEGER NOT NULL, " +
                    "ipAddress TEXT, " +
                    "deviceInfo TEXT" +
                    ")");
                
                // Step 2: Add profileImage column to users table
                try {
                    database.execSQL("ALTER TABLE users ADD COLUMN profileImage TEXT");
                    Log.d(TAG, "Added profileImage column to users table");
                } catch (Exception e) {
                    Log.d(TAG, "profileImage column may already exist: " + e.getMessage());
                }
                
                // Step 3: Create news_posts table (from migration 2→3)
                database.execSQL("CREATE TABLE IF NOT EXISTS news_posts (" +
                    "postId TEXT NOT NULL PRIMARY KEY, " +
                    "type TEXT, " +
                    "title TEXT, " +
                    "description TEXT, " +
                    "profileName TEXT, " +
                    "tags TEXT, " +
                    "music TEXT, " +
                    "videoPath TEXT, " +
                    "imagePath TEXT, " +
                    "thumbnailPath TEXT, " +
                    "status TEXT, " +
                    "likes INTEGER NOT NULL DEFAULT 0, " +
                    "comments INTEGER NOT NULL DEFAULT 0, " +
                    "shares INTEGER NOT NULL DEFAULT 0, " +
                    "createdAt INTEGER NOT NULL, " +
                    "updatedAt INTEGER NOT NULL" +
                    ")");
                
                // Step 4: Create doctor_wallets table (from migration 3→4)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_wallets (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "balance REAL NOT NULL DEFAULT 0, " +
                    "totalEarnings REAL NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 5: Create doctors table with isApproved (from migration 4→5→6)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctors (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "specialty TEXT, " +
                    "consultationFee REAL NOT NULL DEFAULT 0, " +
                    "availableTimes TEXT, " +
                    "isApproved INTEGER NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 6: Create doctor_ratings table (from migration 6→7)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_ratings (" +
                    "ratingId TEXT NOT NULL PRIMARY KEY, " +
                    "doctorId TEXT NOT NULL, " +
                    "patientId TEXT NOT NULL, " +
                    "patientName TEXT, " +
                    "rating REAL NOT NULL, " +
                    "comment TEXT, " +
                    "appointmentId TEXT, " +
                    "createdAt INTEGER NOT NULL" +
                    ")");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 1→7 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 1→7: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 7 to 8
     * Adds location field to doctors table
     */
    static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 7 to 8");
            
            try {
                database.beginTransaction();
                
                // Add location column to doctors table
                database.execSQL("ALTER TABLE doctors ADD COLUMN location TEXT");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 7→8 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 7→8: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 6 to 8 (for users who skip version 7)
     */
    static final Migration MIGRATION_6_8 = new Migration(6, 8) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 6 to 8 (skipping version 7)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Create doctor_ratings table (from migration 6→7)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_ratings (" +
                    "ratingId TEXT NOT NULL PRIMARY KEY, " +
                    "doctorId TEXT NOT NULL, " +
                    "patientId TEXT NOT NULL, " +
                    "patientName TEXT, " +
                    "rating REAL NOT NULL, " +
                    "comment TEXT, " +
                    "appointmentId TEXT, " +
                    "createdAt INTEGER NOT NULL" +
                    ")");
                
                // Step 2: Add location column to doctors table (from migration 7→8)
                database.execSQL("ALTER TABLE doctors ADD COLUMN location TEXT");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 6→8 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 6→8: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 5 to 8 (for users who skip versions 6 and 7)
     */
    static final Migration MIGRATION_5_8 = new Migration(5, 8) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 5 to 8 (skipping versions 6 and 7)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Add isApproved column to doctors table (from migration 5→6)
                database.execSQL("ALTER TABLE doctors ADD COLUMN isApproved INTEGER NOT NULL DEFAULT 0");
                
                // Step 2: Create doctor_ratings table (from migration 6→7)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_ratings (" +
                    "ratingId TEXT NOT NULL PRIMARY KEY, " +
                    "doctorId TEXT NOT NULL, " +
                    "patientId TEXT NOT NULL, " +
                    "patientName TEXT, " +
                    "rating REAL NOT NULL, " +
                    "comment TEXT, " +
                    "appointmentId TEXT, " +
                    "createdAt INTEGER NOT NULL" +
                    ")");
                
                // Step 3: Add location column to doctors table (from migration 7→8)
                database.execSQL("ALTER TABLE doctors ADD COLUMN location TEXT");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 5→8 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 5→8: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 4 to 8 (for users who skip versions 5, 6 and 7)
     */
    static final Migration MIGRATION_4_8 = new Migration(4, 8) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 4 to 8 (skipping versions 5, 6 and 7)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Create doctors table with isApproved (from migration 4→5→6)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctors (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "specialty TEXT, " +
                    "consultationFee REAL NOT NULL DEFAULT 0, " +
                    "availableTimes TEXT, " +
                    "isApproved INTEGER NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 2: Create doctor_ratings table (from migration 6→7)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_ratings (" +
                    "ratingId TEXT NOT NULL PRIMARY KEY, " +
                    "doctorId TEXT NOT NULL, " +
                    "patientId TEXT NOT NULL, " +
                    "patientName TEXT, " +
                    "rating REAL NOT NULL, " +
                    "comment TEXT, " +
                    "appointmentId TEXT, " +
                    "createdAt INTEGER NOT NULL" +
                    ")");
                
                // Step 3: Add location column to doctors table (from migration 7→8)
                database.execSQL("ALTER TABLE doctors ADD COLUMN location TEXT");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 4→8 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 4→8: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 3 to 8 (for users who skip versions 4, 5, 6 and 7)
     */
    static final Migration MIGRATION_3_8 = new Migration(3, 8) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 3 to 8 (skipping versions 4, 5, 6 and 7)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Create doctor_wallets table (from migration 3→4)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_wallets (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "balance REAL NOT NULL DEFAULT 0, " +
                    "totalEarnings REAL NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 2: Create doctors table with isApproved (from migration 4→5→6)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctors (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "specialty TEXT, " +
                    "consultationFee REAL NOT NULL DEFAULT 0, " +
                    "availableTimes TEXT, " +
                    "isApproved INTEGER NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 3: Create doctor_ratings table (from migration 6→7)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_ratings (" +
                    "ratingId TEXT NOT NULL PRIMARY KEY, " +
                    "doctorId TEXT NOT NULL, " +
                    "patientId TEXT NOT NULL, " +
                    "patientName TEXT, " +
                    "rating REAL NOT NULL, " +
                    "comment TEXT, " +
                    "appointmentId TEXT, " +
                    "createdAt INTEGER NOT NULL" +
                    ")");
                
                // Step 4: Add location column to doctors table (from migration 7→8)
                database.execSQL("ALTER TABLE doctors ADD COLUMN location TEXT");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 3→8 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 3→8: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 2 to 8 (for users who skip versions 3, 4, 5, 6 and 7)
     */
    static final Migration MIGRATION_2_8 = new Migration(2, 8) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 2 to 8 (skipping versions 3, 4, 5, 6 and 7)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Create news_posts table (from migration 2→3)
                database.execSQL("CREATE TABLE IF NOT EXISTS news_posts (" +
                    "postId TEXT NOT NULL PRIMARY KEY, " +
                    "type TEXT, " +
                    "title TEXT, " +
                    "description TEXT, " +
                    "profileName TEXT, " +
                    "tags TEXT, " +
                    "music TEXT, " +
                    "videoPath TEXT, " +
                    "imagePath TEXT, " +
                    "thumbnailPath TEXT, " +
                    "status TEXT, " +
                    "likes INTEGER NOT NULL DEFAULT 0, " +
                    "comments INTEGER NOT NULL DEFAULT 0, " +
                    "shares INTEGER NOT NULL DEFAULT 0, " +
                    "createdAt INTEGER NOT NULL, " +
                    "updatedAt INTEGER NOT NULL" +
                    ")");
                
                // Step 2: Create doctor_wallets table (from migration 3→4)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_wallets (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "balance REAL NOT NULL DEFAULT 0, " +
                    "totalEarnings REAL NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 3: Create doctors table with isApproved (from migration 4→5→6)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctors (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "specialty TEXT, " +
                    "consultationFee REAL NOT NULL DEFAULT 0, " +
                    "availableTimes TEXT, " +
                    "isApproved INTEGER NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 4: Create doctor_ratings table (from migration 6→7)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_ratings (" +
                    "ratingId TEXT NOT NULL PRIMARY KEY, " +
                    "doctorId TEXT NOT NULL, " +
                    "patientId TEXT NOT NULL, " +
                    "patientName TEXT, " +
                    "rating REAL NOT NULL, " +
                    "comment TEXT, " +
                    "appointmentId TEXT, " +
                    "createdAt INTEGER NOT NULL" +
                    ")");
                
                // Step 5: Add location column to doctors table (from migration 7→8)
                database.execSQL("ALTER TABLE doctors ADD COLUMN location TEXT");
                
                // Step 6: Add profileImage column to doctors table (from migration 8→9)
                database.execSQL("ALTER TABLE doctors ADD COLUMN profileImage TEXT");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 2→8 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 2→8: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 1 to 8 (for users who skip versions 2, 3, 4, 5, 6 and 7)
     */
    static final Migration MIGRATION_1_8 = new Migration(1, 8) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 1 to 8 (skipping versions 2, 3, 4, 5, 6 and 7)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Create audit_logs table (from migration 1→2)
                database.execSQL("CREATE TABLE IF NOT EXISTS audit_logs (" +
                    "logId TEXT NOT NULL PRIMARY KEY, " +
                    "userId TEXT, " +
                    "userName TEXT, " +
                    "userRole TEXT, " +
                    "action TEXT, " +
                    "description TEXT, " +
                    "entityType TEXT, " +
                    "entityId TEXT, " +
                    "timestamp INTEGER NOT NULL, " +
                    "ipAddress TEXT, " +
                    "deviceInfo TEXT" +
                    ")");
                
                // Step 2: Add profileImage column to users table
                try {
                    database.execSQL("ALTER TABLE users ADD COLUMN profileImage TEXT");
                    Log.d(TAG, "Added profileImage column to users table");
                } catch (Exception e) {
                    Log.d(TAG, "profileImage column may already exist: " + e.getMessage());
                }
                
                // Step 3: Create news_posts table (from migration 2→3)
                database.execSQL("CREATE TABLE IF NOT EXISTS news_posts (" +
                    "postId TEXT NOT NULL PRIMARY KEY, " +
                    "type TEXT, " +
                    "title TEXT, " +
                    "description TEXT, " +
                    "profileName TEXT, " +
                    "tags TEXT, " +
                    "music TEXT, " +
                    "videoPath TEXT, " +
                    "imagePath TEXT, " +
                    "thumbnailPath TEXT, " +
                    "status TEXT, " +
                    "likes INTEGER NOT NULL DEFAULT 0, " +
                    "comments INTEGER NOT NULL DEFAULT 0, " +
                    "shares INTEGER NOT NULL DEFAULT 0, " +
                    "createdAt INTEGER NOT NULL, " +
                    "updatedAt INTEGER NOT NULL" +
                    ")");
                
                // Step 4: Create doctor_wallets table (from migration 3→4)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_wallets (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "balance REAL NOT NULL DEFAULT 0, " +
                    "totalEarnings REAL NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 5: Create doctors table with isApproved (from migration 4→5→6)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctors (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "specialty TEXT, " +
                    "consultationFee REAL NOT NULL DEFAULT 0, " +
                    "availableTimes TEXT, " +
                    "isApproved INTEGER NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 6: Create doctor_ratings table (from migration 6→7)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_ratings (" +
                    "ratingId TEXT NOT NULL PRIMARY KEY, " +
                    "doctorId TEXT NOT NULL, " +
                    "patientId TEXT NOT NULL, " +
                    "patientName TEXT, " +
                    "rating REAL NOT NULL, " +
                    "comment TEXT, " +
                    "appointmentId TEXT, " +
                    "createdAt INTEGER NOT NULL" +
                    ")");
                
                // Step 7: Add location column to doctors table (from migration 7→8)
                database.execSQL("ALTER TABLE doctors ADD COLUMN location TEXT");
                
                // Step 8: Add profileImage column to doctors table (from migration 8→9)
                database.execSQL("ALTER TABLE doctors ADD COLUMN profileImage TEXT");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 1→8 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 1→8: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 8 to 9
     * Adds profileImage field to doctors table
     */
    static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 8 to 9");
            
            try {
                database.beginTransaction();
                
                // Add profileImage column to doctors table
                database.execSQL("ALTER TABLE doctors ADD COLUMN profileImage TEXT");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 8→9 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 8→9: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 7 to 9 (for users who skip version 8)
     */
    static final Migration MIGRATION_7_9 = new Migration(7, 9) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 7 to 9 (skipping version 8)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Add location column to doctors table (from migration 7→8)
                database.execSQL("ALTER TABLE doctors ADD COLUMN location TEXT");
                
                // Step 2: Add profileImage column to doctors table (from migration 8→9)
                database.execSQL("ALTER TABLE doctors ADD COLUMN profileImage TEXT");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 7→9 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 7→9: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 6 to 9 (for users who skip versions 7 and 8)
     */
    static final Migration MIGRATION_6_9 = new Migration(6, 9) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 6 to 9 (skipping versions 7 and 8)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Create doctor_ratings table (from migration 6→7)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_ratings (" +
                    "ratingId TEXT NOT NULL PRIMARY KEY, " +
                    "doctorId TEXT NOT NULL, " +
                    "patientId TEXT NOT NULL, " +
                    "patientName TEXT, " +
                    "rating REAL NOT NULL, " +
                    "comment TEXT, " +
                    "appointmentId TEXT, " +
                    "createdAt INTEGER NOT NULL" +
                    ")");
                
                // Step 2: Add location column to doctors table (from migration 7→8)
                database.execSQL("ALTER TABLE doctors ADD COLUMN location TEXT");
                
                // Step 3: Add profileImage column to doctors table (from migration 8→9)
                database.execSQL("ALTER TABLE doctors ADD COLUMN profileImage TEXT");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 6→9 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 6→9: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 5 to 9 (for users who skip versions 6, 7 and 8)
     */
    static final Migration MIGRATION_5_9 = new Migration(5, 9) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 5 to 9 (skipping versions 6, 7 and 8)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Add isApproved column to doctors table (from migration 5→6)
                database.execSQL("ALTER TABLE doctors ADD COLUMN isApproved INTEGER NOT NULL DEFAULT 0");
                
                // Step 2: Create doctor_ratings table (from migration 6→7)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_ratings (" +
                    "ratingId TEXT NOT NULL PRIMARY KEY, " +
                    "doctorId TEXT NOT NULL, " +
                    "patientId TEXT NOT NULL, " +
                    "patientName TEXT, " +
                    "rating REAL NOT NULL, " +
                    "comment TEXT, " +
                    "appointmentId TEXT, " +
                    "createdAt INTEGER NOT NULL" +
                    ")");
                
                // Step 3: Add location column to doctors table (from migration 7→8)
                database.execSQL("ALTER TABLE doctors ADD COLUMN location TEXT");
                
                // Step 4: Add profileImage column to doctors table (from migration 8→9)
                database.execSQL("ALTER TABLE doctors ADD COLUMN profileImage TEXT");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 5→9 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 5→9: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 4 to 9 (for users who skip versions 5, 6, 7 and 8)
     */
    static final Migration MIGRATION_4_9 = new Migration(4, 9) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 4 to 9 (skipping versions 5, 6, 7 and 8)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Create doctors table with isApproved (from migration 4→5→6)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctors (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "specialty TEXT, " +
                    "consultationFee REAL NOT NULL DEFAULT 0, " +
                    "availableTimes TEXT, " +
                    "isApproved INTEGER NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 2: Create doctor_ratings table (from migration 6→7)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_ratings (" +
                    "ratingId TEXT NOT NULL PRIMARY KEY, " +
                    "doctorId TEXT NOT NULL, " +
                    "patientId TEXT NOT NULL, " +
                    "patientName TEXT, " +
                    "rating REAL NOT NULL, " +
                    "comment TEXT, " +
                    "appointmentId TEXT, " +
                    "createdAt INTEGER NOT NULL" +
                    ")");
                
                // Step 3: Add location column to doctors table (from migration 7→8)
                database.execSQL("ALTER TABLE doctors ADD COLUMN location TEXT");
                
                // Step 4: Add profileImage column to doctors table (from migration 8→9)
                database.execSQL("ALTER TABLE doctors ADD COLUMN profileImage TEXT");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 4→9 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 4→9: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 3 to 9 (for users who skip versions 4, 5, 6, 7 and 8)
     */
    static final Migration MIGRATION_3_9 = new Migration(3, 9) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 3 to 9 (skipping versions 4, 5, 6, 7 and 8)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Create doctor_wallets table (from migration 3→4)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_wallets (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "balance REAL NOT NULL DEFAULT 0, " +
                    "totalEarnings REAL NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 2: Create doctors table with isApproved (from migration 4→5→6)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctors (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "specialty TEXT, " +
                    "consultationFee REAL NOT NULL DEFAULT 0, " +
                    "availableTimes TEXT, " +
                    "isApproved INTEGER NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 3: Create doctor_ratings table (from migration 6→7)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_ratings (" +
                    "ratingId TEXT NOT NULL PRIMARY KEY, " +
                    "doctorId TEXT NOT NULL, " +
                    "patientId TEXT NOT NULL, " +
                    "patientName TEXT, " +
                    "rating REAL NOT NULL, " +
                    "comment TEXT, " +
                    "appointmentId TEXT, " +
                    "createdAt INTEGER NOT NULL" +
                    ")");
                
                // Step 4: Add location column to doctors table (from migration 7→8)
                database.execSQL("ALTER TABLE doctors ADD COLUMN location TEXT");
                
                // Step 5: Add profileImage column to doctors table (from migration 8→9)
                database.execSQL("ALTER TABLE doctors ADD COLUMN profileImage TEXT");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 3→9 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 3→9: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 2 to 9 (for users who skip versions 3, 4, 5, 6, 7 and 8)
     */
    static final Migration MIGRATION_2_9 = new Migration(2, 9) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 2 to 9 (skipping versions 3, 4, 5, 6, 7 and 8)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Create news_posts table (from migration 2→3)
                database.execSQL("CREATE TABLE IF NOT EXISTS news_posts (" +
                    "postId TEXT NOT NULL PRIMARY KEY, " +
                    "type TEXT, " +
                    "title TEXT, " +
                    "description TEXT, " +
                    "profileName TEXT, " +
                    "tags TEXT, " +
                    "music TEXT, " +
                    "videoPath TEXT, " +
                    "imagePath TEXT, " +
                    "thumbnailPath TEXT, " +
                    "status TEXT, " +
                    "likes INTEGER NOT NULL DEFAULT 0, " +
                    "comments INTEGER NOT NULL DEFAULT 0, " +
                    "shares INTEGER NOT NULL DEFAULT 0, " +
                    "createdAt INTEGER NOT NULL, " +
                    "updatedAt INTEGER NOT NULL" +
                    ")");
                
                // Step 2: Create doctor_wallets table (from migration 3→4)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_wallets (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "balance REAL NOT NULL DEFAULT 0, " +
                    "totalEarnings REAL NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 3: Create doctors table with isApproved (from migration 4→5→6)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctors (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "specialty TEXT, " +
                    "consultationFee REAL NOT NULL DEFAULT 0, " +
                    "availableTimes TEXT, " +
                    "isApproved INTEGER NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 4: Create doctor_ratings table (from migration 6→7)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_ratings (" +
                    "ratingId TEXT NOT NULL PRIMARY KEY, " +
                    "doctorId TEXT NOT NULL, " +
                    "patientId TEXT NOT NULL, " +
                    "patientName TEXT, " +
                    "rating REAL NOT NULL, " +
                    "comment TEXT, " +
                    "appointmentId TEXT, " +
                    "createdAt INTEGER NOT NULL" +
                    ")");
                
                // Step 5: Add location column to doctors table (from migration 7→8)
                database.execSQL("ALTER TABLE doctors ADD COLUMN location TEXT");
                
                // Step 6: Add profileImage column to doctors table (from migration 8→9)
                database.execSQL("ALTER TABLE doctors ADD COLUMN profileImage TEXT");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 2→9 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 2→9: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 1 to 9 (for users who skip versions 2, 3, 4, 5, 6, 7 and 8)
     */
    static final Migration MIGRATION_1_9 = new Migration(1, 9) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 1 to 9 (skipping versions 2, 3, 4, 5, 6, 7 and 8)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Create audit_logs table (from migration 1→2)
                database.execSQL("CREATE TABLE IF NOT EXISTS audit_logs (" +
                    "logId TEXT NOT NULL PRIMARY KEY, " +
                    "userId TEXT, " +
                    "userName TEXT, " +
                    "userRole TEXT, " +
                    "action TEXT, " +
                    "description TEXT, " +
                    "entityType TEXT, " +
                    "entityId TEXT, " +
                    "timestamp INTEGER NOT NULL, " +
                    "ipAddress TEXT, " +
                    "deviceInfo TEXT" +
                    ")");
                
                // Step 2: Add profileImage column to users table
                try {
                    database.execSQL("ALTER TABLE users ADD COLUMN profileImage TEXT");
                    Log.d(TAG, "Added profileImage column to users table");
                } catch (Exception e) {
                    Log.d(TAG, "profileImage column may already exist: " + e.getMessage());
                }
                
                // Step 3: Create news_posts table (from migration 2→3)
                database.execSQL("CREATE TABLE IF NOT EXISTS news_posts (" +
                    "postId TEXT NOT NULL PRIMARY KEY, " +
                    "type TEXT, " +
                    "title TEXT, " +
                    "description TEXT, " +
                    "profileName TEXT, " +
                    "tags TEXT, " +
                    "music TEXT, " +
                    "videoPath TEXT, " +
                    "imagePath TEXT, " +
                    "thumbnailPath TEXT, " +
                    "status TEXT, " +
                    "likes INTEGER NOT NULL DEFAULT 0, " +
                    "comments INTEGER NOT NULL DEFAULT 0, " +
                    "shares INTEGER NOT NULL DEFAULT 0, " +
                    "createdAt INTEGER NOT NULL, " +
                    "updatedAt INTEGER NOT NULL" +
                    ")");
                
                // Step 4: Create doctor_wallets table (from migration 3→4)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_wallets (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "balance REAL NOT NULL DEFAULT 0, " +
                    "totalEarnings REAL NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 5: Create doctors table with isApproved (from migration 4→5→6)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctors (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "specialty TEXT, " +
                    "consultationFee REAL NOT NULL DEFAULT 0, " +
                    "availableTimes TEXT, " +
                    "isApproved INTEGER NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 6: Create doctor_ratings table (from migration 6→7)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_ratings (" +
                    "ratingId TEXT NOT NULL PRIMARY KEY, " +
                    "doctorId TEXT NOT NULL, " +
                    "patientId TEXT NOT NULL, " +
                    "patientName TEXT, " +
                    "rating REAL NOT NULL, " +
                    "comment TEXT, " +
                    "appointmentId TEXT, " +
                    "createdAt INTEGER NOT NULL" +
                    ")");
                
                // Step 7: Add location column to doctors table (from migration 7→8)
                database.execSQL("ALTER TABLE doctors ADD COLUMN location TEXT");
                
                // Step 8: Add profileImage column to doctors table (from migration 8→9)
                database.execSQL("ALTER TABLE doctors ADD COLUMN profileImage TEXT");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 1→9 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 1→9: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 9 to 10
     * Adds appointmentType field to appointments table
     */
    static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 9 to 10");
            
            try {
                database.beginTransaction();
                
                // Add appointmentType column to appointments table
                database.execSQL("ALTER TABLE appointments ADD COLUMN appointmentType TEXT NOT NULL DEFAULT 'Visit'");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 9→10 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 9→10: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 8 to 10 (for users who skip version 9)
     */
    static final Migration MIGRATION_8_10 = new Migration(8, 10) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 8 to 10 (skipping version 9)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Add profileImage column to doctors table (from migration 8→9)
                database.execSQL("ALTER TABLE doctors ADD COLUMN profileImage TEXT");
                
                // Step 2: Add appointmentType column to appointments table (from migration 9→10)
                database.execSQL("ALTER TABLE appointments ADD COLUMN appointmentType TEXT NOT NULL DEFAULT 'Visit'");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 8→10 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 8→10: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 7 to 10 (for users who skip versions 8 and 9)
     */
    static final Migration MIGRATION_7_10 = new Migration(7, 10) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 7 to 10 (skipping versions 8 and 9)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Add location column to doctors table (from migration 7→8)
                database.execSQL("ALTER TABLE doctors ADD COLUMN location TEXT");
                
                // Step 2: Add profileImage column to doctors table (from migration 8→9)
                database.execSQL("ALTER TABLE doctors ADD COLUMN profileImage TEXT");
                
                // Step 3: Add appointmentType column to appointments table (from migration 9→10)
                database.execSQL("ALTER TABLE appointments ADD COLUMN appointmentType TEXT NOT NULL DEFAULT 'Visit'");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 7→10 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 7→10: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 6 to 10 (for users who skip versions 7, 8 and 9)
     */
    static final Migration MIGRATION_6_10 = new Migration(6, 10) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 6 to 10 (skipping versions 7, 8 and 9)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Create doctor_ratings table (from migration 6→7)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_ratings (" +
                    "ratingId TEXT NOT NULL PRIMARY KEY, " +
                    "doctorId TEXT NOT NULL, " +
                    "patientId TEXT NOT NULL, " +
                    "patientName TEXT, " +
                    "rating REAL NOT NULL, " +
                    "comment TEXT, " +
                    "appointmentId TEXT, " +
                    "createdAt INTEGER NOT NULL" +
                    ")");
                
                // Step 2: Add location column to doctors table (from migration 7→8)
                database.execSQL("ALTER TABLE doctors ADD COLUMN location TEXT");
                
                // Step 3: Add profileImage column to doctors table (from migration 8→9)
                database.execSQL("ALTER TABLE doctors ADD COLUMN profileImage TEXT");
                
                // Step 4: Add appointmentType column to appointments table (from migration 9→10)
                database.execSQL("ALTER TABLE appointments ADD COLUMN appointmentType TEXT NOT NULL DEFAULT 'Visit'");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 6→10 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 6→10: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 5 to 10 (for users who skip versions 6, 7, 8 and 9)
     */
    static final Migration MIGRATION_5_10 = new Migration(5, 10) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 5 to 10 (skipping versions 6, 7, 8 and 9)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Add isApproved column to doctors table (from migration 5→6)
                database.execSQL("ALTER TABLE doctors ADD COLUMN isApproved INTEGER NOT NULL DEFAULT 0");
                
                // Step 2: Create doctor_ratings table (from migration 6→7)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_ratings (" +
                    "ratingId TEXT NOT NULL PRIMARY KEY, " +
                    "doctorId TEXT NOT NULL, " +
                    "patientId TEXT NOT NULL, " +
                    "patientName TEXT, " +
                    "rating REAL NOT NULL, " +
                    "comment TEXT, " +
                    "appointmentId TEXT, " +
                    "createdAt INTEGER NOT NULL" +
                    ")");
                
                // Step 3: Add location column to doctors table (from migration 7→8)
                database.execSQL("ALTER TABLE doctors ADD COLUMN location TEXT");
                
                // Step 4: Add profileImage column to doctors table (from migration 8→9)
                database.execSQL("ALTER TABLE doctors ADD COLUMN profileImage TEXT");
                
                // Step 5: Add appointmentType column to appointments table (from migration 9→10)
                database.execSQL("ALTER TABLE appointments ADD COLUMN appointmentType TEXT NOT NULL DEFAULT 'Visit'");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 5→10 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 5→10: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 4 to 10 (for users who skip versions 5, 6, 7, 8 and 9)
     */
    static final Migration MIGRATION_4_10 = new Migration(4, 10) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 4 to 10 (skipping versions 5, 6, 7, 8 and 9)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Create doctors table with isApproved (from migration 4→5→6)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctors (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "specialty TEXT, " +
                    "consultationFee REAL NOT NULL DEFAULT 0, " +
                    "availableTimes TEXT, " +
                    "isApproved INTEGER NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 2: Create doctor_ratings table (from migration 6→7)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_ratings (" +
                    "ratingId TEXT NOT NULL PRIMARY KEY, " +
                    "doctorId TEXT NOT NULL, " +
                    "patientId TEXT NOT NULL, " +
                    "patientName TEXT, " +
                    "rating REAL NOT NULL, " +
                    "comment TEXT, " +
                    "appointmentId TEXT, " +
                    "createdAt INTEGER NOT NULL" +
                    ")");
                
                // Step 3: Add location column to doctors table (from migration 7→8)
                database.execSQL("ALTER TABLE doctors ADD COLUMN location TEXT");
                
                // Step 4: Add profileImage column to doctors table (from migration 8→9)
                database.execSQL("ALTER TABLE doctors ADD COLUMN profileImage TEXT");
                
                // Step 5: Add appointmentType column to appointments table (from migration 9→10)
                database.execSQL("ALTER TABLE appointments ADD COLUMN appointmentType TEXT NOT NULL DEFAULT 'Visit'");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 4→10 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 4→10: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 3 to 10 (for users who skip versions 4, 5, 6, 7, 8 and 9)
     */
    static final Migration MIGRATION_3_10 = new Migration(3, 10) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 3 to 10 (skipping versions 4, 5, 6, 7, 8 and 9)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Create doctor_wallets table (from migration 3→4)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_wallets (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "balance REAL NOT NULL DEFAULT 0, " +
                    "totalEarnings REAL NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 2: Create doctors table with isApproved (from migration 4→5→6)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctors (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "specialty TEXT, " +
                    "consultationFee REAL NOT NULL DEFAULT 0, " +
                    "availableTimes TEXT, " +
                    "isApproved INTEGER NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 3: Create doctor_ratings table (from migration 6→7)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_ratings (" +
                    "ratingId TEXT NOT NULL PRIMARY KEY, " +
                    "doctorId TEXT NOT NULL, " +
                    "patientId TEXT NOT NULL, " +
                    "patientName TEXT, " +
                    "rating REAL NOT NULL, " +
                    "comment TEXT, " +
                    "appointmentId TEXT, " +
                    "createdAt INTEGER NOT NULL" +
                    ")");
                
                // Step 4: Add location column to doctors table (from migration 7→8)
                database.execSQL("ALTER TABLE doctors ADD COLUMN location TEXT");
                
                // Step 5: Add profileImage column to doctors table (from migration 8→9)
                database.execSQL("ALTER TABLE doctors ADD COLUMN profileImage TEXT");
                
                // Step 6: Add appointmentType column to appointments table (from migration 9→10)
                database.execSQL("ALTER TABLE appointments ADD COLUMN appointmentType TEXT NOT NULL DEFAULT 'Visit'");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 3→10 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 3→10: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 2 to 10 (for users who skip versions 3, 4, 5, 6, 7, 8 and 9)
     */
    static final Migration MIGRATION_2_10 = new Migration(2, 10) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 2 to 10 (skipping versions 3, 4, 5, 6, 7, 8 and 9)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Create news_posts table (from migration 2→3)
                database.execSQL("CREATE TABLE IF NOT EXISTS news_posts (" +
                    "postId TEXT NOT NULL PRIMARY KEY, " +
                    "type TEXT, " +
                    "title TEXT, " +
                    "description TEXT, " +
                    "profileName TEXT, " +
                    "tags TEXT, " +
                    "music TEXT, " +
                    "videoPath TEXT, " +
                    "imagePath TEXT, " +
                    "thumbnailPath TEXT, " +
                    "status TEXT, " +
                    "likes INTEGER NOT NULL DEFAULT 0, " +
                    "comments INTEGER NOT NULL DEFAULT 0, " +
                    "shares INTEGER NOT NULL DEFAULT 0, " +
                    "createdAt INTEGER NOT NULL, " +
                    "updatedAt INTEGER NOT NULL" +
                    ")");
                
                // Step 2: Create doctor_wallets table (from migration 3→4)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_wallets (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "balance REAL NOT NULL DEFAULT 0, " +
                    "totalEarnings REAL NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 3: Create doctors table with isApproved (from migration 4→5→6)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctors (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "specialty TEXT, " +
                    "consultationFee REAL NOT NULL DEFAULT 0, " +
                    "availableTimes TEXT, " +
                    "isApproved INTEGER NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 4: Create doctor_ratings table (from migration 6→7)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_ratings (" +
                    "ratingId TEXT NOT NULL PRIMARY KEY, " +
                    "doctorId TEXT NOT NULL, " +
                    "patientId TEXT NOT NULL, " +
                    "patientName TEXT, " +
                    "rating REAL NOT NULL, " +
                    "comment TEXT, " +
                    "appointmentId TEXT, " +
                    "createdAt INTEGER NOT NULL" +
                    ")");
                
                // Step 5: Add location column to doctors table (from migration 7→8)
                database.execSQL("ALTER TABLE doctors ADD COLUMN location TEXT");
                
                // Step 6: Add profileImage column to doctors table (from migration 8→9)
                database.execSQL("ALTER TABLE doctors ADD COLUMN profileImage TEXT");
                
                // Step 7: Add appointmentType column to appointments table (from migration 9→10)
                database.execSQL("ALTER TABLE appointments ADD COLUMN appointmentType TEXT NOT NULL DEFAULT 'Visit'");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 2→10 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 2→10: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 1 to 10 (for users who skip versions 2, 3, 4, 5, 6, 7, 8 and 9)
     */
    static final Migration MIGRATION_1_10 = new Migration(1, 10) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 1 to 10 (skipping versions 2, 3, 4, 5, 6, 7, 8 and 9)");
            
            try {
                database.beginTransaction();
                
                // Step 1: Create audit_logs table (from migration 1→2)
                database.execSQL("CREATE TABLE IF NOT EXISTS audit_logs (" +
                    "logId TEXT NOT NULL PRIMARY KEY, " +
                    "userId TEXT, " +
                    "userName TEXT, " +
                    "userRole TEXT, " +
                    "action TEXT, " +
                    "description TEXT, " +
                    "entityType TEXT, " +
                    "entityId TEXT, " +
                    "timestamp INTEGER NOT NULL, " +
                    "ipAddress TEXT, " +
                    "deviceInfo TEXT" +
                    ")");
                
                // Step 2: Add profileImage column to users table
                try {
                    database.execSQL("ALTER TABLE users ADD COLUMN profileImage TEXT");
                    Log.d(TAG, "Added profileImage column to users table");
                } catch (Exception e) {
                    Log.d(TAG, "profileImage column may already exist: " + e.getMessage());
                }
                
                // Step 3: Create news_posts table (from migration 2→3)
                database.execSQL("CREATE TABLE IF NOT EXISTS news_posts (" +
                    "postId TEXT NOT NULL PRIMARY KEY, " +
                    "type TEXT, " +
                    "title TEXT, " +
                    "description TEXT, " +
                    "profileName TEXT, " +
                    "tags TEXT, " +
                    "music TEXT, " +
                    "videoPath TEXT, " +
                    "imagePath TEXT, " +
                    "thumbnailPath TEXT, " +
                    "status TEXT, " +
                    "likes INTEGER NOT NULL DEFAULT 0, " +
                    "comments INTEGER NOT NULL DEFAULT 0, " +
                    "shares INTEGER NOT NULL DEFAULT 0, " +
                    "createdAt INTEGER NOT NULL, " +
                    "updatedAt INTEGER NOT NULL" +
                    ")");
                
                // Step 4: Create doctor_wallets table (from migration 3→4)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_wallets (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "balance REAL NOT NULL DEFAULT 0, " +
                    "totalEarnings REAL NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 5: Create doctors table with isApproved (from migration 4→5→6)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctors (" +
                    "doctorId TEXT NOT NULL PRIMARY KEY, " +
                    "specialty TEXT, " +
                    "consultationFee REAL NOT NULL DEFAULT 0, " +
                    "availableTimes TEXT, " +
                    "isApproved INTEGER NOT NULL DEFAULT 0, " +
                    "lastUpdated INTEGER NOT NULL" +
                    ")");
                
                // Step 6: Create doctor_ratings table (from migration 6→7)
                database.execSQL("CREATE TABLE IF NOT EXISTS doctor_ratings (" +
                    "ratingId TEXT NOT NULL PRIMARY KEY, " +
                    "doctorId TEXT NOT NULL, " +
                    "patientId TEXT NOT NULL, " +
                    "patientName TEXT, " +
                    "rating REAL NOT NULL, " +
                    "comment TEXT, " +
                    "appointmentId TEXT, " +
                    "createdAt INTEGER NOT NULL" +
                    ")");
                
                // Step 7: Add location column to doctors table (from migration 7→8)
                database.execSQL("ALTER TABLE doctors ADD COLUMN location TEXT");
                
                // Step 8: Add profileImage column to doctors table (from migration 8→9)
                database.execSQL("ALTER TABLE doctors ADD COLUMN profileImage TEXT");
                
                // Step 9: Add appointmentType column to appointments table (from migration 9→10)
                database.execSQL("ALTER TABLE appointments ADD COLUMN appointmentType TEXT NOT NULL DEFAULT 'Visit'");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 1→10 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 1→10: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };

    /**
     * Migration from version 10 to 11
     * Adds authorId column to news_posts table
     */
    static final Migration MIGRATION_10_11 = new Migration(10, 11) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 10 to 11");
            
            try {
                database.beginTransaction();
                
                // Add authorId column to news_posts table
                database.execSQL("ALTER TABLE news_posts ADD COLUMN authorId TEXT");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 10→11 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 10→11: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };

    /**
     * Migration from version 11 to 12
     * Adds views column to article_posts table
     */
    static final Migration MIGRATION_11_12 = new Migration(11, 12) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 11 to 12");
            
            try {
                database.beginTransaction();
                
                // Add views column to article_posts table
                database.execSQL("ALTER TABLE article_posts ADD COLUMN views INTEGER NOT NULL DEFAULT 0");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 11→12 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 11→12: " + e.getMessage(), e);
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    
    /**
     * Migration from version 12 to 13
     * Adds prescriptions table
     */
    static final Migration MIGRATION_12_13 = new Migration(12, 13) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 12 to 13");
            
            try {
                database.beginTransaction();
                
                // Create prescriptions table
                database.execSQL("CREATE TABLE IF NOT EXISTS prescriptions (" +
                    "prescriptionId TEXT NOT NULL PRIMARY KEY, " +
                    "appointmentId TEXT, " +
                    "patientId TEXT, " +
                    "patientName TEXT, " +
                    "doctorId TEXT, " +
                    "doctorName TEXT, " +
                    "medicinesJson TEXT, " +
                    "instructions TEXT, " +
                    "imageUrl TEXT, " +
                    "createdAt INTEGER NOT NULL, " +
                    "updatedAt INTEGER NOT NULL" +
                    ")");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 12→13 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Migration 12→13 failed: " + e.getMessage());
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };

    /**
     * Migration from version 13 to 14
     * Adds age and gender to users table
     */
    static final Migration MIGRATION_13_14 = new Migration(13, 14) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 13 to 14");
            
            try {
                database.beginTransaction();
                
                // Add age column to users table
                database.execSQL("ALTER TABLE users ADD COLUMN age INTEGER DEFAULT 0");
                
                // Add gender column to users table
                database.execSQL("ALTER TABLE users ADD COLUMN gender TEXT DEFAULT ''");
                
                // Add isOnline and onlineStatus to doctors table
                database.execSQL("ALTER TABLE doctors ADD COLUMN isOnline INTEGER DEFAULT 0");
                database.execSQL("ALTER TABLE doctors ADD COLUMN onlineStatus TEXT DEFAULT 'offline'");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 13→14 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Migration 13→14 failed: " + e.getMessage());
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };

    /**
     * Migration from version 14 to 15
     * Adds chat duration tracking to appointments table
     */
    static final Migration MIGRATION_14_15 = new Migration(14, 15) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 14 to 15");
            
            try {
                database.beginTransaction();
                
                // Add chat duration columns to appointments table
                database.execSQL("ALTER TABLE appointments ADD COLUMN chatStartTime INTEGER DEFAULT 0");
                database.execSQL("ALTER TABLE appointments ADD COLUMN chatEndTime INTEGER DEFAULT 0");
                database.execSQL("ALTER TABLE appointments ADD COLUMN chatDuration INTEGER DEFAULT 0");
                database.execSQL("ALTER TABLE appointments ADD COLUMN isChatActive INTEGER DEFAULT 0");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 14→15 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Migration 14→15 failed: " + e.getMessage());
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };

    /**
     * Migration from version 15 to 16
     * Schema update
     */
    static final Migration MIGRATION_15_16 = new Migration(15, 16) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 15 to 16");
            // Schema update - no specific changes needed
        }
    };

    /**
     * Migration from version 16 to 17
     * Adds withdrawal_requests table for wallet withdrawal system
     */
    static final Migration MIGRATION_16_17 = new Migration(16, 17) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 16 to 17");
            
            try {
                database.beginTransaction();
                
                // Create withdrawal_requests table
                database.execSQL("CREATE TABLE IF NOT EXISTS withdrawal_requests (" +
                    "requestId TEXT NOT NULL PRIMARY KEY, " +
                    "doctorId TEXT, " +
                    "doctorName TEXT, " +
                    "amount REAL DEFAULT 0, " +
                    "method TEXT, " +
                    "accountNumber TEXT, " +
                    "accountName TEXT, " +
                    "bankName TEXT, " +
                    "status TEXT, " +
                    "requestedAt INTEGER DEFAULT 0, " +
                    "processedAt INTEGER DEFAULT 0, " +
                    "processedBy TEXT, " +
                    "rejectionReason TEXT, " +
                    "notes TEXT" +
                    ")");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 16→17 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Migration 16→17 failed: " + e.getMessage());
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };
    /**
     * Migration from version 17 to 18
     * Adds imageUrl, views, and authorId columns to article_posts table
     */
    static final Migration MIGRATION_17_18 = new Migration(17, 18) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 17 to 18");
            
            try {
                database.beginTransaction();
                
                // Add imageUrl column
                database.execSQL("ALTER TABLE article_posts ADD COLUMN imageUrl TEXT");
                
                // Add views column
                database.execSQL("ALTER TABLE article_posts ADD COLUMN views INTEGER NOT NULL DEFAULT 0");
                
                // Add authorId column
                database.execSQL("ALTER TABLE article_posts ADD COLUMN authorId TEXT");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 17→18 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Migration 17→18 failed: " + e.getMessage());
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };

    /**
     * Migration from version 18 to 19:
     * - Added isDemo field to doctors table
     */
    static final Migration MIGRATION_18_19 = new Migration(18, 19) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 18 to 19");
            
            try {
                database.beginTransaction();
                
                // Add isDemo column to doctors table
                database.execSQL("ALTER TABLE doctors ADD COLUMN isDemo INTEGER NOT NULL DEFAULT 0");
                
                database.setTransactionSuccessful();
                Log.d(TAG, "Migration 18→19 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Migration 18→19 failed: " + e.getMessage());
                throw e;
            } finally {
                database.endTransaction();
            }
        }
    };

    static final Migration MIGRATION_19_20 = new Migration(19, 20) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE users_secure (" +
                    "userId TEXT NOT NULL PRIMARY KEY, " +
                    "email TEXT, " +
                    "fullName TEXT, " +
                    "phone TEXT, " +
                    "role TEXT, " +
                    "profileImage TEXT, " +
                    "age INTEGER NOT NULL, " +
                    "gender TEXT, " +
                    "createdAt INTEGER NOT NULL, " +
                    "regNo TEXT)");
            database.execSQL("INSERT INTO users_secure (userId, email, fullName, phone, role, profileImage, age, gender, createdAt, regNo) " +
                    "SELECT userId, email, fullName, phone, role, profileImage, age, gender, createdAt, regNo FROM users");
            database.execSQL("DROP TABLE users");
            database.execSQL("ALTER TABLE users_secure RENAME TO users");
        }
    };
    
    
    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    
    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "hasetapp_database")
                            // Remove destructive migration - use proper migrations instead
                            // .fallbackToDestructiveMigration() // ❌ REMOVED - causes data loss
                            
                            // Add all migration paths
                            .addMigrations(
                                MIGRATION_1_2,  // Handles 1→2
                                MIGRATION_2_3,  // Handles 2→3
                                MIGRATION_1_3,  // Handles 1→3 (direct jump)
                                MIGRATION_3_4,  // Handles 3→4
                                MIGRATION_2_4,  // Handles 2→4 (direct jump)
                                MIGRATION_1_4,  // Handles 1→4 (direct jump)
                                MIGRATION_4_5,  // Handles 4→5
                                MIGRATION_3_5,  // Handles 3→5 (direct jump)
                                MIGRATION_2_5,  // Handles 2→5 (direct jump)
                                MIGRATION_1_5,  // Handles 1→5 (direct jump)
                                MIGRATION_5_6,  // Handles 5→6
                                MIGRATION_4_6,  // Handles 4→6 (direct jump)
                                MIGRATION_3_6,  // Handles 3→6 (direct jump)
                                MIGRATION_2_6,  // Handles 2→6 (direct jump)
                                MIGRATION_1_6,  // Handles 1→6 (direct jump)
                                MIGRATION_6_7,  // Handles 6→7
                                MIGRATION_5_7,  // Handles 5→7 (direct jump)
                                MIGRATION_4_7,  // Handles 4→7 (direct jump)
                                MIGRATION_3_7,  // Handles 3→7 (direct jump)
                                MIGRATION_2_7,  // Handles 2→7 (direct jump)
                                MIGRATION_1_7,   // Handles 1→7 (direct jump)
                                MIGRATION_7_8, // Handles 7→8
                                MIGRATION_6_8, // Handles 6→8
                                MIGRATION_5_8, // Handles 5→8
                                MIGRATION_4_8, // Handles 4→8
                                MIGRATION_3_8, // Handles 3→8
                                MIGRATION_2_8, // Handles 2→8
                                MIGRATION_1_8,  // Handles 1→8
                                MIGRATION_8_9, // Handles 8→9
                                MIGRATION_7_9, // Handles 7→9
                                MIGRATION_6_9, // Handles 6→9
                                MIGRATION_5_9, // Handles 5→9
                                MIGRATION_4_9, // Handles 4→9
                                MIGRATION_3_9, // Handles 3→9
                                MIGRATION_2_9, // Handles 2→9
                                MIGRATION_1_9,  // Handles 1→9
                                MIGRATION_9_10, // Handles 9→10
                                MIGRATION_8_10, // Handles 8→10
                                MIGRATION_7_10, // Handles 7→10
                                MIGRATION_6_10, // Handles 6→10
                                MIGRATION_5_10, // Handles 5→10
                                MIGRATION_4_10, // Handles 4→10
                                MIGRATION_3_10, // Handles 3→10
                                MIGRATION_2_10, // Handles 2→10
                                MIGRATION_1_10, // Handles 1→10
                                MIGRATION_10_11, // Handles 10→11
                                MIGRATION_11_12, // Handles 11→12
                                MIGRATION_12_13, // Handles 12→13
                                MIGRATION_13_14, // Handles 13→14 - adds age and gender
                                MIGRATION_14_15,  // Handles 14→15 - adds chat duration tracking
                                MIGRATION_15_16,  // Handles 15→16 - schema update
                                MIGRATION_16_17,   // Handles 16→17 - withdrawal requests table
                                MIGRATION_17_18,   // Handles 17→18 - adds imageUrl, views, authorId to article_posts
                                MIGRATION_18_19,   // Handles 18→19 - adds isDemo field to doctors table
                                MIGRATION_19_20    // Handles 19→20 - removes obsolete local password storage
                            )
                            .build();
                    
                    Log.d(TAG, "AppDatabase instance created with migrations configured");
                }
            }
        }
        return INSTANCE;
    }
}
