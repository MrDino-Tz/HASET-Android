package com.haset.hasetapp.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.haset.hasetapp.database.entities.UserEntity;
import com.haset.hasetapp.repositories.AdminRepository;

import java.util.ArrayList;
import java.util.List;

public class AdminUsersViewModel extends AndroidViewModel {
    private final AdminRepository repository;
    private LiveData<List<UserEntity>> allUsers;

    public AdminUsersViewModel(@NonNull Application application) {
        super(application);
        repository = new AdminRepository();
    }

    public LiveData<List<UserEntity>> getAllUsers() {
        if (allUsers == null) {
            allUsers = repository.getAllUsers();
        }
        return allUsers;
    }

    public LiveData<List<UserEntity>> getDoctors() {
        return Transformations.map(getAllUsers(), users -> {
            List<UserEntity> doctors = new ArrayList<>();
            if (users != null) {
                for (UserEntity user : users) {
                    if ("doctor".equalsIgnoreCase(user.getRole())) {
                        doctors.add(user);
                    }
                }
            }
            return doctors;
        });
    }

    public LiveData<List<UserEntity>> getPatients() {
        return Transformations.map(getAllUsers(), users -> {
            List<UserEntity> patients = new ArrayList<>();
            if (users != null) {
                for (UserEntity user : users) {
                    if ("patient".equalsIgnoreCase(user.getRole())) {
                        patients.add(user);
                    }
                }
            }
            return patients;
        });
    }
}
