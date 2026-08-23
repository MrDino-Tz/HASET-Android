package com.haset.hasetapp.workers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.haset.hasetapp.R;
import com.haset.hasetapp.activities.ArticleActivity;
import com.haset.hasetapp.activities.DashboardActivity;
import com.haset.hasetapp.database.entities.ArticlePostEntity;
import com.haset.hasetapp.firebase.ArticlePostHelper;
import com.haset.hasetapp.utils.Constants;
import com.haset.hasetapp.utils.PreferenceManager;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class TrendingArticlesWorker extends Worker {
    
    private static final String TAG = "TrendingArticlesWorker";
    private static final String WORK_NAME = "trending_articles_check";
    private static final String CHANNEL_ID = "trending_articles_worker_channel";
    private static final String PREF_NAME = "trending_articles_worker_prefs";
    private static final int TRENDING_LIMIT = 5;
    
    private final Context context;
    private final PreferenceManager preferenceManager;
    
    public TrendingArticlesWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
        this.preferenceManager = new PreferenceManager(context);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting trending articles check...");
        
        if (!shouldSendNotifications()) {
            Log.d(TAG, "Skipping - user is not logged in or notifications disabled");
            return Result.success();
        }
        
        createNotificationChannel();
        
        ArticlePostHelper.getInstance().getTrendingArticles(TRENDING_LIMIT, 
            new ArticlePostHelper.OnCompleteListener<List<ArticlePostEntity>>() {
                @Override
                public void onSuccess(List<ArticlePostEntity> result) {
                    if (result != null && !result.isEmpty()) {
                        for (ArticlePostEntity article : result) {
                            checkAndNotifyTrendingArticle(article);
                        }
                        Log.d(TAG, "Sent notifications for " + result.size() + " trending articles");
                    } else {
                        Log.d(TAG, "No trending articles found");
                    }
                }

                @Override
                public void onError(String error) {
                    if (error != null) com.haset.hasetapp.utils.ErrorLogger.log(error, error);
                }
            });
        
        return Result.success();
    }
    
    private boolean shouldSendNotifications() {
        if (!preferenceManager.isLoggedIn()) {
            return false;
        }
        
        if (!preferenceManager.isNotificationEnabled()) {
            return false;
        }
        
        String role = preferenceManager.getUserRole();
        return Constants.ROLE_PATIENT.equals(role) || Constants.ROLE_DOCTOR.equals(role);
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Trending Articles",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifications about trending/popular articles");
            channel.enableLights(true);
            channel.setShowBadge(true);
            
            NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    private void checkAndNotifyTrendingArticle(ArticlePostEntity article) {
        if (article == null || article.getPostId() == null) return;
        
        SharedPreferences notifiedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long lastNotifiedViews = notifiedPrefs.getLong(article.getPostId() + "_views", 0);
        
        if (article.getViews() > lastNotifiedViews && article.getViews() >= Constants.TRENDING_VIEWS_THRESHOLD) {
            showTrendingNotification(article);
            notifiedPrefs.edit().putLong(article.getPostId() + "_views", article.getViews()).apply();
        }
    }
    
    private void showTrendingNotification(ArticlePostEntity article) {
        String title = "Trending Article: " + article.getTitle();
        String description = article.getDescription() != null ? article.getDescription() : "";
        String summary = description.length() > 100 ? description.substring(0, 97) + "..." : description;
        
        Intent intent = new Intent(context, ArticleActivity.class);
        intent.putExtra("article_id", article.getPostId());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                new Random().nextInt(10000),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.haset_logo)
                .setContentTitle(title)
                .setContentText(summary)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(description + "\n\nPopular: " + article.getViews() + " views"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(("trending_worker_" + article.getPostId()).hashCode(), builder.build());
        }
        
        Log.d(TAG, "Trending notification shown: " + article.getTitle());
    }
    
    public static void schedule(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                TrendingArticlesWorker.class,
                6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build();
        
        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                        WORK_NAME,
                        ExistingPeriodicWorkPolicy.KEEP,
                        workRequest);
        
        Log.d(TAG, "Trending articles worker scheduled (every 6 hours when connected)");
    }
    
    public static void cancel(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
        Log.d(TAG, "Trending articles worker cancelled");
    }
}
