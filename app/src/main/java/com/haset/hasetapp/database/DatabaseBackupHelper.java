package com.haset.hasetapp.database;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Helper class for backing up and restoring the database
 * Use this before major migrations or updates
 */
public class DatabaseBackupHelper {
    
    private static final String TAG = "DatabaseBackupHelper";
    private static final String BACKUP_DIR = "database_backups";
    
    /**
     * Creates a backup of the database before migration
     * 
     * @param context Application context
     * @return Path to backup file, or null if backup failed
     */
    public static String backupDatabase(Context context) {
        try {
            File dbFile = context.getDatabasePath("hasetapp_database");
            
            if (!dbFile.exists()) {
                Log.w(TAG, "Database file does not exist, nothing to backup");
                return null;
            }
            
            // Create backup directory
            File backupDir = new File(context.getFilesDir(), BACKUP_DIR);
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
            
            // Create backup file with timestamp
            String backupFileName = "hasetapp_database_backup_" + System.currentTimeMillis() + ".db";
            File backupFile = new File(backupDir, backupFileName);
            
            // Copy database file
            copyFile(dbFile, backupFile);
            
            Log.i(TAG, "Database backed up to: " + backupFile.getAbsolutePath());
            return backupFile.getAbsolutePath();
            
        } catch (Exception e) {
            Log.e(TAG, "Error backing up database: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Restores database from backup file
     * 
     * @param context Application context
     * @param backupFilePath Path to backup file
     * @return true if restore successful, false otherwise
     */
    public static boolean restoreDatabase(Context context, String backupFilePath) {
        try {
            File backupFile = new File(backupFilePath);
            
            if (!backupFile.exists()) {
                Log.e(TAG, "Backup file does not exist: " + backupFilePath);
                return false;
            }
            
            // Close database instance if exists
            AppDatabase.getInstance(context).close();
            
            // Get database file
            File dbFile = context.getDatabasePath("hasetapp_database");
            
            // Delete existing database files
            if (dbFile.exists()) {
                dbFile.delete();
            }
            
            // Delete journal files
            File journalFile = new File(dbFile.getPath() + "-journal");
            if (journalFile.exists()) {
                journalFile.delete();
            }
            
            // Copy backup to database location
            copyFile(backupFile, dbFile);
            
            Log.i(TAG, "Database restored from: " + backupFilePath);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error restoring database: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Copies a file from source to destination
     */
    private static void copyFile(File source, File dest) throws IOException {
        FileChannel sourceChannel = null;
        FileChannel destChannel = null;
        
        try {
            sourceChannel = new FileInputStream(source).getChannel();
            destChannel = new FileOutputStream(dest).getChannel();
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        } finally {
            if (sourceChannel != null) {
                sourceChannel.close();
            }
            if (destChannel != null) {
                destChannel.close();
            }
        }
    }
    
    /**
     * Gets the most recent backup file
     * 
     * @param context Application context
     * @return Path to most recent backup, or null if no backups exist
     */
    public static String getMostRecentBackup(Context context) {
        File backupDir = new File(context.getFilesDir(), BACKUP_DIR);
        
        if (!backupDir.exists() || !backupDir.isDirectory()) {
            return null;
        }
        
        File[] backupFiles = backupDir.listFiles((dir, name) -> name.startsWith("hasetapp_database_backup_") && name.endsWith(".db"));
        
        if (backupFiles == null || backupFiles.length == 0) {
            return null;
        }
        
        // Find most recent backup
        File mostRecent = backupFiles[0];
        for (File file : backupFiles) {
            if (file.lastModified() > mostRecent.lastModified()) {
                mostRecent = file;
            }
        }
        
        return mostRecent.getAbsolutePath();
    }
    
    /**
     * Deletes old backup files (keeps only the most recent N backups)
     * 
     * @param context Application context
     * @param keepCount Number of recent backups to keep
     */
    public static void cleanupOldBackups(Context context, int keepCount) {
        File backupDir = new File(context.getFilesDir(), BACKUP_DIR);
        
        if (!backupDir.exists() || !backupDir.isDirectory()) {
            return;
        }
        
        File[] backupFiles = backupDir.listFiles((dir, name) -> name.startsWith("hasetapp_database_backup_") && name.endsWith(".db"));
        
        if (backupFiles == null || backupFiles.length <= keepCount) {
            return;
        }
        
        // Sort by last modified (newest first)
        java.util.Arrays.sort(backupFiles, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
        
        // Delete old backups
        for (int i = keepCount; i < backupFiles.length; i++) {
            boolean deleted = backupFiles[i].delete();
            if (deleted) {
                Log.d(TAG, "Deleted old backup: " + backupFiles[i].getName());
            }
        }
    }
}

