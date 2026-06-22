package com.haset.hasetapp.utils;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.haset.hasetapp.R;
import com.haset.hasetapp.models.Appointment;
import com.haset.hasetapp.receivers.AppointmentReminderReceiver;
import com.haset.hasetapp.utils.PreferenceManager;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AppointmentReminderHelper {
    
    private static final String CHANNEL_ID = "appointment_reminders";
    private static final String CHANNEL_NAME = "Appointment Reminders";
    private static final String CHANNEL_DESCRIPTION = "Reminders for upcoming appointments";
    private static final String PREF_REMINDERS = "appointment_reminders";
    
    // Notification grouping
    private static final String GROUP_KEY_APPOINTMENTS = "com.haset.hasetapp.APPOINTMENTS";
    private static final String GROUP_SUMMARY_APPOINTMENTS_ID = "appointments_summary";
    
    private final Context context;
    private final AlarmManager alarmManager;
    private final NotificationManager notificationManager;
    private final SharedPreferences preferences;
    
    public AppointmentReminderHelper(Context context) {
        this.context = context;
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.preferences = context.getSharedPreferences(PREF_REMINDERS, Context.MODE_PRIVATE);
        createNotificationChannel();
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    public void scheduleReminders(Appointment appointment) {
        if (!new PreferenceManager(context).isNotificationEnabled()) {
            return;
        }
        
        // Check for notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d("AppointmentReminder", "Notification permission not granted, skipping reminders");
                return;
            }
        }
        
        try {
            // Parse appointment date and time
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault());
            String dateTimeString = appointment.getDate() + " " + appointment.getTime();
            Date appointmentDate = sdf.parse(dateTimeString);
            
            if (appointmentDate != null) {
                Calendar appointmentCalendar = Calendar.getInstance();
                appointmentCalendar.setTime(appointmentDate);
                
                // Schedule 24-hour reminder
                scheduleReminder(appointment, appointmentCalendar, 24, 24 * 60 * 60 * 1000);
                
                // Schedule 2-hour reminder
                scheduleReminder(appointment, appointmentCalendar, 2, 2 * 60 * 60 * 1000);
                
                // Schedule 30-minute reminder
                scheduleReminder(appointment, appointmentCalendar, 0, 30 * 60 * 1000);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
    
    private void scheduleReminder(Appointment appointment, Calendar appointmentCalendar, int hoursBefore, long millisBefore) {
        Calendar reminderTime = (Calendar) appointmentCalendar.clone();
        reminderTime.add(Calendar.MILLISECOND, -(int) millisBefore);
        
        // Only schedule if reminder time is in the future
        if (reminderTime.after(Calendar.getInstance())) {
            Intent intent = new Intent(context, AppointmentReminderReceiver.class);
            intent.putExtra("appointment_id", appointment.getAppointmentId());
            intent.putExtra("doctor_name", appointment.getDoctorName());
            intent.putExtra("appointment_time", appointment.getDate() + " at " + appointment.getTime());
            intent.putExtra("reminder_type", hoursBefore + "h");
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    appointment.getAppointmentId().hashCode() + hoursBefore,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime.getTimeInMillis(), pendingIntent);
            
            // Save reminder info
            preferences.edit()
                    .putLong(appointment.getAppointmentId() + "_" + hoursBefore, reminderTime.getTimeInMillis())
                    .apply();
        }
    }
    
    public void cancelReminders(Appointment appointment) {
        if (appointment != null && appointment.getAppointmentId() != null) {
            cancelRemindersByAppointmentId(appointment.getAppointmentId());
        }
    }

    public void cancelRemindersByAppointmentId(String appointmentId) {
        if (appointmentId == null) return;
        
        int[] reminderHours = {24, 2, 0}; // 24h, 2h, 30min reminders
        
        for (int hours : reminderHours) {
            Intent intent = new Intent(context, AppointmentReminderReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    appointmentId.hashCode() + hours,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            alarmManager.cancel(pendingIntent);
            
            // Remove from preferences
            preferences.edit()
                    .remove(appointmentId + "_" + hours)
                    .apply();
        }
    }
    
    public void showImmediateReminder(Appointment appointment) {
        if (!new PreferenceManager(context).isNotificationEnabled()) {
            return;
        }
        
        Intent intent = new Intent(context, com.haset.hasetapp.MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bell_dot)
                .setContentTitle("Kikumbusho cha Miadi")
                .setContentText("Miadi yako na Dkt. " + appointment.getDoctorName() + 
                        " imepangwa tarehe " + appointment.getDate() + " saa " + appointment.getTime())
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Miadi yako na Dkt. " + appointment.getDoctorName() + 
                                " imepangwa tarehe " + appointment.getDate() + " saa " + appointment.getTime() + 
                                "\n\nSababu: " + (appointment.getReason() != null ? appointment.getReason() : "Hakuna sababu iliyobainishwa")))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setGroup(GROUP_KEY_APPOINTMENTS) // Add to appointments group
                .setContentIntent(pendingIntent);
        
        notificationManager.notify(appointment.getAppointmentId().hashCode(), builder.build());
    }
    
    public void showNotification(String title, String message, String appointmentId) {
        if (!new PreferenceManager(context).isNotificationEnabled()) {
            return;
        }
        
        Intent intent = new Intent(context, com.haset.hasetapp.MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.haset_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setGroup(GROUP_KEY_APPOINTMENTS) // Add to appointments group
                .setContentIntent(pendingIntent);
        
        notificationManager.notify(appointmentId.hashCode(), builder.build());
        
        // Create group summary for appointments
        createAppointmentsGroupSummary();
    }
    
    private void createAppointmentsGroupSummary() {
        // Create intent for group summary
        Intent intent = new Intent(context, com.haset.hasetapp.MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Build group summary notification
        NotificationCompat.Builder summaryBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.haset_logo)
                .setContentTitle("Vikumbusho vya Miadi")
                .setContentText("Una miadi inayokuja")
                .setStyle(new NotificationCompat.BigTextStyle().bigText("Una miadi inayokuja"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setGroup(GROUP_KEY_APPOINTMENTS)
                .setGroupSummary(true) // Mark as group summary
                .setContentIntent(pendingIntent);
        
        // Show group summary
        notificationManager.notify(GROUP_SUMMARY_APPOINTMENTS_ID.hashCode(), summaryBuilder.build());
    }
}
