package com.haset.hasetapp.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.haset.hasetapp.R;
import com.haset.hasetapp.utils.AppointmentReminderHelper;

public class AppointmentReminderReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String appointmentId = intent.getStringExtra("appointment_id");
        String doctorName = intent.getStringExtra("doctor_name");
        String appointmentTime = intent.getStringExtra("appointment_time");
        String reminderType = intent.getStringExtra("reminder_type");
        
        if (appointmentId != null && doctorName != null && appointmentTime != null) {
            String title = getReminderTitle(reminderType);
            String message = getReminderMessage(doctorName, appointmentTime, reminderType);
            
            AppointmentReminderHelper reminderHelper = new AppointmentReminderHelper(context);
            reminderHelper.showNotification(title, message, appointmentId);
        }
    }
    
    private String getReminderTitle(String reminderType) {
        switch (reminderType) {
            case "24h":
                return "Appointment Tomorrow";
            case "2h":
                return "Appointment Soon";
            case "0h":
                return "Appointment in 30 Minutes";
            default:
                return "Appointment Reminder";
        }
    }
    
    private String getReminderMessage(String doctorName, String appointmentTime, String reminderType) {
        switch (reminderType) {
            case "24h":
                return "Your appointment with Dr. " + doctorName + " is tomorrow at " + appointmentTime;
            case "2h":
                return "Your appointment with Dr. " + doctorName + " is in 2 hours at " + appointmentTime;
            case "0h":
                return "Your appointment with Dr. " + doctorName + " starts in 30 minutes at " + appointmentTime;
            default:
                return "Your appointment with Dr. " + doctorName + " is scheduled for " + appointmentTime;
        }
    }
}
