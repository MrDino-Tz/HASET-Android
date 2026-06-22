package com.haset.hasetapp.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.haset.hasetapp.database.entities.AuditLogEntity;
import com.haset.hasetapp.repositories.AdminRepository;

import java.util.List;

public class AuditLogsViewModel extends AndroidViewModel {
    private final AdminRepository repository;
    private LiveData<List<AuditLogEntity>> auditLogs;

    public AuditLogsViewModel(@NonNull Application application) {
        super(application);
        repository = new AdminRepository();
    }

    public LiveData<List<AuditLogEntity>> getAuditLogs() {
        if (auditLogs == null) {
            auditLogs = repository.getAuditLogs();
        }
        return auditLogs;
    }
    
    public void refreshLogs() {
        auditLogs = repository.getAuditLogs();
    }
}
