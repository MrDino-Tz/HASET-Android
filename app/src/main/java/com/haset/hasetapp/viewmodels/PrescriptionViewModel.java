package com.haset.hasetapp.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.haset.hasetapp.models.Prescription;
import com.haset.hasetapp.repositories.PrescriptionRepository;
import com.haset.hasetapp.utils.FirebaseHelper;

import java.util.List;

public class PrescriptionViewModel extends AndroidViewModel {
    private final PrescriptionRepository repository;
    private final MutableLiveData<String> currentUserId = new MutableLiveData<>();
    private final MutableLiveData<String> currentUserRole = new MutableLiveData<>();

    public PrescriptionViewModel(@NonNull Application application) {
        super(application);
        repository = new PrescriptionRepository(application);
    }

    public void setUserInfo(String userId, String role) {
        currentUserId.setValue(userId);
        currentUserRole.setValue(role);
    }

    public LiveData<List<Prescription>> getPrescriptions() {
        return Transformations.switchMap(currentUserId, userId -> {
            String role = currentUserRole.getValue();
            if (userId == null || role == null) {
                return new MutableLiveData<>();
            }
            if ("doctor".equalsIgnoreCase(role)) {
                return repository.getPrescriptionsByDoctor(userId);
            } else {
                return repository.getPrescriptionsByPatient(userId);
            }
        });
    }

    public LiveData<Prescription> getPrescriptionById(String id) {
        return repository.getPrescriptionById(id);
    }

    public void createPrescription(Prescription prescription, FirebaseHelper.OnCompleteListener<Void> callback) {
        repository.createPrescription(prescription, callback);
    }

    public void deletePrescription(String id, FirebaseHelper.OnCompleteListener<Void> callback) {
        repository.deletePrescription(id, callback);
    }
}
