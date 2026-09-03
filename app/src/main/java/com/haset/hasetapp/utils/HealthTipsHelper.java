package com.haset.hasetapp.utils;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.haset.hasetapp.R;
import com.haset.hasetapp.activities.DashboardActivity;
import com.haset.hasetapp.receivers.HealthTipsReceiver;
import java.util.Calendar;
import java.util.Random;

public class HealthTipsHelper {

    public static final String NAVIGATE_TO_HEALTH_TIP = "health_tip";
    public static final String EXTRA_HEALTH_TIP_TITLE = "health_tip_title";
    public static final String EXTRA_HEALTH_TIP_TEXT = "health_tip_text";
    
    private static final String CHANNEL_ID = "health_tips_channel";
    private static final String CHANNEL_NAME = "Vidokezo vya Afya";
    private static final String CHANNEL_DESCRIPTION = "Vidokezo vya afya vya kila siku na vikumbusho kwa wagonjwa";
    private static final String PREF_HEALTH_TIPS = "health_tips_preferences";
    private static final String PREF_TIPS_ENABLED = "health_tips_enabled";
    private static final String PREF_LAST_TIP_DATE = "last_tip_date";
    
    // Notification grouping
    private static final String GROUP_KEY_HEALTH_TIPS = "com.haset.hasetapp.HEALTH_TIPS";
    private static final String GROUP_SUMMARY_HEALTH_TIPS_ID = "health_tips_summary";
    
    // Notification IDs for different times
    private static final int MORNING_TIP_ID = 2001;
    private static final int MIDDAY_TIP_ID = 2002;
    private static final int AFTERNOON_TIP_ID = 2003;
    private static final int EVENING_TIP_ID = 2004;
    private static final int BEDTIME_TIP_ID = 2005;
    
    private final Context context;
    private final AlarmManager alarmManager;
    private final NotificationManager notificationManager;
    private final SharedPreferences preferences;
    private final PreferenceManager preferenceManager;
    
    public HealthTipsHelper(Context context) {
        this.context = context;
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.preferences = context.getSharedPreferences(PREF_HEALTH_TIPS, Context.MODE_PRIVATE);
        this.preferenceManager = new PreferenceManager(context);
        createNotificationChannel();
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableLights(true);
            channel.setShowBadge(true);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    public void scheduleDailyHealthTips() {
        if (!isHealthTipsEnabled()) {
            Log.d("HealthTips", "Health tips disabled in preferences");
            return;
        }
        
        // Check if user is a patient
        String userRole = preferenceManager.getUserRole();
        if (!Constants.ROLE_PATIENT.equals(userRole)) {
            Log.d("HealthTips", "Health tips only for patients, user role: " + userRole);
            return;
        }
        
        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) 
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.d("HealthTips", "Notification permission not granted, skipping health tips");
                return;
            }
        }
        
        // Cancel existing alarms
        cancelAllHealthTips();
        
        // Schedule daily tips at optimal times
        scheduleMorningTip();
        scheduleMiddayTip();
        scheduleAfternoonTip();
        scheduleEveningTip();
        scheduleBedtimeTip();
        
        Log.d("HealthTips", "Daily health tips scheduled successfully");
    }
    
    private void scheduleMorningTip() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 8); // 8:00 AM
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        
        // If time has passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        Intent intent = new Intent(context, HealthTipsReceiver.class);
        intent.putExtra("tip_type", "morning");
        intent.putExtra("tip_id", MORNING_TIP_ID);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                MORNING_TIP_ID, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Schedule repeating daily
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );
        
        Log.d("HealthTips", "Morning tip scheduled for: " + calendar.getTime());
    }
    
    private void scheduleMiddayTip() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 12); // 12:00 PM
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        Intent intent = new Intent(context, HealthTipsReceiver.class);
        intent.putExtra("tip_type", "midday");
        intent.putExtra("tip_id", MIDDAY_TIP_ID);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                MIDDAY_TIP_ID, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );
        
        Log.d("HealthTips", "Midday tip scheduled for: " + calendar.getTime());
    }
    
    private void scheduleAfternoonTip() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 15); // 3:00 PM
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        Intent intent = new Intent(context, HealthTipsReceiver.class);
        intent.putExtra("tip_type", "afternoon");
        intent.putExtra("tip_id", AFTERNOON_TIP_ID);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                AFTERNOON_TIP_ID, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );
        
        Log.d("HealthTips", "Afternoon tip scheduled for: " + calendar.getTime());
    }
    
    private void scheduleEveningTip() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 19); // 7:00 PM
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        Intent intent = new Intent(context, HealthTipsReceiver.class);
        intent.putExtra("tip_type", "evening");
        intent.putExtra("tip_id", EVENING_TIP_ID);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                EVENING_TIP_ID, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );
        
        Log.d("HealthTips", "Evening tip scheduled for: " + calendar.getTime());
    }
    
    private void scheduleBedtimeTip() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 21); // 9:00 PM
        calendar.set(Calendar.MINUTE, 30);
        calendar.set(Calendar.SECOND, 0);
        
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        Intent intent = new Intent(context, HealthTipsReceiver.class);
        intent.putExtra("tip_type", "bedtime");
        intent.putExtra("tip_id", BEDTIME_TIP_ID);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                BEDTIME_TIP_ID, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );
        
        Log.d("HealthTips", "Bedtime tip scheduled for: " + calendar.getTime());
    }
    
    public void cancelAllHealthTips() {
        cancelTip(MORNING_TIP_ID);
        cancelTip(MIDDAY_TIP_ID);
        cancelTip(AFTERNOON_TIP_ID);
        cancelTip(EVENING_TIP_ID);
        cancelTip(BEDTIME_TIP_ID);
        Log.d("HealthTips", "All health tips cancelled");
    }
    
    private void cancelTip(int notificationId) {
        Intent intent = new Intent(context, HealthTipsReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                notificationId, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
    }
    
    public void showHealthTip(String tipType, int notificationId) {
        if (!isHealthTipsEnabled()) {
            return;
        }
        
        String[] tips = getHealthTipsByType(tipType);
        if (tips == null || tips.length == 0) {
            return;
        }
        
        // Random tip from the category
        Random random = new Random();
        String tip = tips[random.nextInt(tips.length)];
        String title = getTipTitleByType(tipType);
        
        // Carry the exact notification content into the app so tapping a tip
        // displays the tip instead of opening the old placeholder MainActivity.
        Intent appIntent = createHealthTipIntent(title, tip);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                notificationId,
                appIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(title)
                .setContentText(tip)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(tip))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setGroup(GROUP_KEY_HEALTH_TIPS) // Add to health tips group
                .setContentIntent(pendingIntent);
        
        // Show notification
        notificationManager.notify(notificationId, builder.build());
        
        // Create group summary for health tips
        createHealthTipsGroupSummary(title, tip);
        
        Log.d("HealthTips", "Health tip shown: " + tipType + " - " + tip);
    }
    
    private String[] getHealthTipsByType(String tipType) {
        switch (tipType) {
            case "morning":
                return new String[]{
                    "💊 Tumia dawa zako za asubuhi kama ulivyoelekezwa",
                    "🥗 Anza siku yako na kifungua kinywa chenye afya na nyuzinyuzi",
                    "💧 Kunywa glasi ya maji kabla ya kunywa chai au kahawa ya asubuhi",
                    "🏃 Fanya mazoezi ya kunyoosha viungo kwa dakika 10 asubuhi",
                    "📊 Pima presha yako kama una shinikizo la damu",
                    "🌅 Pata mwanga wa jua la asubuhi kurekebisha mzunguko wako wa kulala",
                    "🥤 Epuka vinywaji vyenye sukari nyingi asubuhi"
                };
                
            case "midday":
                return new String[]{
                    "🥗 Chagua chakula cha mchana kilichosawazishwa chenye mboga na protini",
                    "🚶 Tembea kwa dakika 5 baada ya kula kusaidia mmeng'enyo",
                    "💊 Usisahau kutumia dawa zako za mchana kama umepangiwa",
                    "📱 Hifadhi kipimo chako cha presha baada ya chakula cha mchana",
                    "💧 Kaa na maji mwilini - kunywa maji pamoja na mlo wako",
                    "🍎 Chagua matunda badala ya vitafunwa vyenye sukari nyingi",
                    "🧘 Vuta pumzi ndefu chache kupunguza msongo wa mawazo"
                };
                
            case "afternoon":
                return new String[]{
                    "🫖 Muda wa kupata maji - kunywa glasi ya maji",
                    "👁️ Pumzisha macho yako kutoka kwenye skrini kwa dakika 5",
                    "🧘 Fanya mazoezi ya kuvuta pumzi ndefu ili kupunguza msongo wa mawazo",
                    "🍎 Chagua kitafunwa chenye afya kama karanga au matunda",
                    "🚶 Simama na ujinyooshe ikiwa umekaa kwa muda mrefu",
                    "📝 Pitia malengo yako ya afya kwa siku ya leo",
                    "☕ Punguza unywaji wa vinywaji vyenye kafeini mchana"
                };
                
            case "evening":
                return new String[]{
                    "💊 Tumia dawa zako za jioni kama ulivyoelekezwa",
                    "🍽️ Furahia chakula cha jioni chepesi na chenye virutubisho",
                    "📝 Pitia rekodi yako ya afya na dalili za leo",
                    "😴 Jiandae kwa usingizi mwanana usiku huu",
                    "🧘 Fanya tafakari nyepesi ya jioni",
                    "📵 Punguza matumizi ya skrini saa 1 kabla ya kulala",
                    "🌡️ Pima joto lako la mwili ikiwa hujisikii vizuri"
                };
                
            case "bedtime":
                return new String[]{
                    "😴 Muda wa kulala - lenga kupata masaa 7-8 ya usingizi",
                    "📵 Weka pembeni simu na tableti dakika 30 kabla ya kulala",
                    "🧘 Jaribu zoezi hili la kupumua: mbinu ya 4-7-8",
                    "💊 Hakikisha umetumia dawa zako zote za jioni",
                    "🌙 Unda utaratibu mzuri wa kupumzika kabla ya kulala",
                    "🌡️ Hakikisha chumba chako cha kulala kina ubaridi kwa usingizi mzuri",
                    "📚 Soma kitabu badala ya kuangalia TV kabla ya kulala"
                };
                
            default:
                return new String[]{"💡 Dokezo ya Afya: Kaa katika shughuli na kula vizuri!"};
        }
    }
    
    private String getTipTitleByType(String tipType) {
        switch (tipType) {
            case "morning":
                return "☀️ Dokezo la Afya la Asubuhi";
            case "midday":
                return "🌞 Dokezo la Afya la Mchana";
            case "afternoon":
                return "🌆 Dokezo la Afya la Alasiri";
            case "evening":
                return "🌃 Dokezo la Afya la Jioni";
            case "bedtime":
                return "🌙 Dokezo la Afya Wakati wa Kulala";
            default:
                return "💡 Dokezo la Afya";
        }
    }
    
    public boolean isHealthTipsEnabled() {
        return preferences.getBoolean(PREF_TIPS_ENABLED, true); // Default to enabled for patients
    }
    
    public void setHealthTipsEnabled(boolean enabled) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(PREF_TIPS_ENABLED, enabled);
        editor.apply();
        
        if (enabled) {
            scheduleDailyHealthTips();
        } else {
            cancelAllHealthTips();
        }
    }
    
    private void createHealthTipsGroupSummary(String latestTipTitle, String latestTip) {
        // Create intent for group summary
        Intent intent = createHealthTipIntent(latestTipTitle, latestTip);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                GROUP_SUMMARY_HEALTH_TIPS_ID.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Build group summary notification
        NotificationCompat.Builder summaryBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.haset_logo)
                .setContentTitle("Vidokezo vya Afya vya Kila Siku")
                .setContentText("Vidokezo vyako vya afya kwa leo")
                .setStyle(new NotificationCompat.BigTextStyle().bigText("Vidokezo na vikumbusho vyako vya afya vya kila siku"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setGroup(GROUP_KEY_HEALTH_TIPS)
                .setGroupSummary(true) // Mark as group summary
                .setContentIntent(pendingIntent);
        
        // Show group summary
        notificationManager.notify(GROUP_SUMMARY_HEALTH_TIPS_ID.hashCode(), summaryBuilder.build());
    }

    public static Intent createHealthTipIntent(Context context, String title, String tip) {
        Intent intent = new Intent(context, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("navigate_to", NAVIGATE_TO_HEALTH_TIP);
        intent.putExtra(EXTRA_HEALTH_TIP_TITLE, title);
        intent.putExtra(EXTRA_HEALTH_TIP_TEXT, tip);
        return intent;
    }

    private Intent createHealthTipIntent(String title, String tip) {
        return createHealthTipIntent(context, title, tip);
    }
    
    public void saveLastTipDate() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREF_LAST_TIP_DATE, getCurrentDateString());
        editor.apply();
    }
    
    private String getCurrentDateString() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int year = calendar.get(java.util.Calendar.YEAR);
        int month = calendar.get(java.util.Calendar.MONTH);
        int day = calendar.get(java.util.Calendar.DAY_OF_MONTH);
        
        return year + "-" + (month + 1) + "-" + day;
    }
}
