package com.haset.hasetapp.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.haset.hasetapp.database.entities.AppointmentEntity;
import com.haset.hasetapp.database.entities.UserEntity;
import com.haset.hasetapp.repositories.AdminRepository;

import java.util.List;

public class AdminHomeViewModel extends AndroidViewModel {
    private final AdminRepository repository;
    private LiveData<List<UserEntity>> allUsers;
    private LiveData<List<AppointmentEntity>> allAppointments;
    private LiveData<List<com.haset.hasetapp.database.entities.AuditLogEntity>> auditLogs;
    private final MutableLiveData<String> adminTip = new MutableLiveData<>();
    private final String[] tips = {
        "🔐 Review security settings and ensure proper access controls are in place.",
        "📊 Check system performance metrics and address any bottlenecks.",
        "💾 Verify that automated backups are completing successfully.",
        "👥 Monitor user registration patterns for any unusual activity.",
        "🏥 Review doctor verification requests and process pending applications.",
        "📱 Test notification systems to ensure they're working properly.",
        "📈 Analyze user engagement metrics and identify improvement opportunities.",
        "📝 Review admin logs for any errors or warnings."
    };

    public AdminHomeViewModel(@NonNull Application application) {
        super(application);
        repository = new AdminRepository();
        updateRandomTip();
    }

    public LiveData<List<UserEntity>> getAllUsers() {
        if (allUsers == null) {
            allUsers = repository.getAllUsers();
        }
        return allUsers;
    }

    public LiveData<List<AppointmentEntity>> getAllAppointments() {
        if (allAppointments == null) {
            allAppointments = repository.getAllAppointments();
        }
        return allAppointments;
    }

    public LiveData<List<com.haset.hasetapp.database.entities.AuditLogEntity>> getAuditLogs() {
        if (auditLogs == null) {
            auditLogs = repository.getAuditLogs();
        }
        return auditLogs;
    }

    public LiveData<String> getAdminTip() {
        return adminTip;
    }

    public void updateRandomTip() {
        String tip = tips[new java.util.Random().nextInt(tips.length)];
        adminTip.postValue(tip);
    }

    public void refresh() {
        allUsers = repository.getAllUsers();
        allAppointments = repository.getAllAppointments();
        auditLogs = repository.getAuditLogs();
        updateRandomTip();
    }

    public LiveData<DashboardStats> getDashboardStats() {
        return Transformations.map(getAllUsers(), users -> {
            int total = users != null ? users.size() : 0;
            int doctors = 0;
            int patients = 0;
            if (users != null) {
                for (UserEntity user : users) {
                    if ("doctor".equalsIgnoreCase(user.getRole())) doctors++;
                    else if ("patient".equalsIgnoreCase(user.getRole())) patients++;
                }
            }
            return new DashboardStats(total, doctors, patients);
        });
    }

    public static class DashboardStats {
        public final int totalUsers;
        public final int totalDoctors;
        public final int totalPatients;

        public DashboardStats(int totalUsers, int totalDoctors, int totalPatients) {
            this.totalUsers = totalUsers;
            this.totalDoctors = totalDoctors;
            this.totalPatients = totalPatients;
        }
    }
}
