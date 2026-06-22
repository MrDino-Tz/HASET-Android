package com.haset.hasetapp.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.haset.hasetapp.models.Doctor;
import com.haset.hasetapp.repositories.HomeRepository;
import com.haset.hasetapp.adapters.PatientBannerAdapter;
import com.haset.hasetapp.utils.NotificationBadgeHelper;

import com.haset.hasetapp.models.PharmacyProduct;
import com.haset.hasetapp.database.entities.ArticlePostEntity;
import com.haset.hasetapp.database.entities.AppointmentEntity;
import com.haset.hasetapp.models.Doctor;

import java.util.List;

public class HomeViewModel extends AndroidViewModel {
    private final HomeRepository repository;
    private NotificationBadgeHelper badgeHelper;
    private LiveData<List<Doctor>> doctors;
    private LiveData<List<PatientBannerAdapter.BannerItem>> banners;
    private LiveData<List<ArticlePostEntity>> popularArticles;
    private LiveData<List<PharmacyProduct>> featuredMedicines;
    private LiveData<AppointmentEntity> upcomingAppointment;
    private MutableLiveData<Integer> notificationCount;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        this.repository = new HomeRepository();
        this.badgeHelper = new NotificationBadgeHelper(application);
        this.notificationCount = new MutableLiveData<>(0);
        updateNotificationCount();
    }

    public LiveData<List<Doctor>> getDoctors() {
        if (doctors == null) {
            doctors = repository.getDoctors();
        }
        return doctors;
    }

    public LiveData<Integer> getNotificationCount(String userId, String role) {
        return notificationCount;
    }
    
    /**
     * Update notification count from badge helper (shows only NEW notifications since last open)
     */
    public void updateNotificationCount() {
        if (badgeHelper != null) {
            int newCount = badgeHelper.getNewNotificationsSinceLastOpen();
            notificationCount.postValue(newCount);
        }
    }

    /**
     * Increment notification count (called when new notification arrives)
     */
    public void incrementNotificationCount() {
        if (badgeHelper != null) {
            badgeHelper.incrementGeneralNotifications();
            badgeHelper.incrementNewNotifications();
            int newCount = badgeHelper.getNewNotificationsSinceLastOpen();
            notificationCount.postValue(newCount);
        }
    }

    /**
     * Clear notification count (called when user views notifications)
     */
    public void clearNotificationCount() {
        if (badgeHelper != null) {
            badgeHelper.markGeneralNotificationsAsRead();
            notificationCount.postValue(0);
        }
    }

    public LiveData<List<PatientBannerAdapter.BannerItem>> getBanners() {
        if (banners == null) {
            banners = repository.getBanners();
        }
        return banners;
    }

    public LiveData<List<ArticlePostEntity>> getPopularArticles() {
        if (popularArticles == null) {
            popularArticles = repository.getPopularArticles();
        }
        return popularArticles;
    }

    public LiveData<List<PharmacyProduct>> getFeaturedMedicines() {
        if (featuredMedicines == null) {
            featuredMedicines = repository.getFeaturedMedicines();
        }
        return featuredMedicines;
    }
    
    public LiveData<AppointmentEntity> getUpcomingAppointment(String userId, String role) {
        if (upcomingAppointment == null) {
            upcomingAppointment = repository.getUpcomingAppointment(userId, role);
        }
        return upcomingAppointment;
    }
}
