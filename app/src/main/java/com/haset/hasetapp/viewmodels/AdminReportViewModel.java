package com.haset.hasetapp.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.haset.hasetapp.database.entities.AppointmentEntity;
import com.haset.hasetapp.database.entities.UserEntity;
import com.haset.hasetapp.database.entities.AuditLogEntity;
import com.haset.hasetapp.repositories.AdminRepository;

import java.util.List;

public class AdminReportViewModel extends AndroidViewModel {
    private final AdminRepository repository;

    public AdminReportViewModel(@NonNull Application application) {
        super(application);
        repository = new AdminRepository();
    }

    public LiveData<List<UserEntity>> getAllUsers() {
        return repository.getAllUsers();
    }

    public LiveData<List<AppointmentEntity>> getAllAppointments() {
        return repository.getAllAppointments();
    }

    public LiveData<List<AuditLogEntity>> getAuditLogs() {
        return repository.getAuditLogs();
    }
}
