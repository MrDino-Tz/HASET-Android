package com.haset.hasetapp.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.haset.hasetapp.models.Appointment;
import com.haset.hasetapp.repositories.AppointmentRepository;
import com.haset.hasetapp.utils.Constants;
import com.haset.hasetapp.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.List;

public class AppointmentsViewModel extends AndroidViewModel {
    private final AppointmentRepository repository;
    private final MutableLiveData<UserRoleInfo> userRoleInfo = new MutableLiveData<>();
    private final LiveData<List<Appointment>> allAppointments;
    private final LiveData<List<Appointment>> upcomingAppointments;
    private final LiveData<List<Appointment>> pendingAppointments;
    private final LiveData<List<Appointment>> pastAppointments;
    private final LiveData<List<Appointment>> cancelledAppointments;

    public AppointmentsViewModel(@NonNull Application application) {
        super(application);
        repository = new AppointmentRepository();

        allAppointments = Transformations.switchMap(userRoleInfo, info -> 
                repository.getAppointments(info.userId, info.role));

        upcomingAppointments = Transformations.map(allAppointments, appointments -> {
            List<Appointment> filtered = new ArrayList<>();
            if (appointments != null) {
                for (Appointment a : appointments) {
                    if (a.isUpcoming()) {
                        filtered.add(a);
                    }
                }
            }
            return filtered;
        });

        pendingAppointments = Transformations.map(allAppointments, appointments -> {
            List<Appointment> filtered = new ArrayList<>();
            if (appointments != null) {
                for (Appointment a : appointments) {
                    if (Constants.STATUS_PENDING.equalsIgnoreCase(a.getStatus())) {
                        filtered.add(a);
                    }
                }
            }
            return filtered;
        });

        pastAppointments = Transformations.map(allAppointments, appointments -> {
            List<Appointment> filtered = new ArrayList<>();
            if (appointments != null) {
                for (Appointment a : appointments) {
                    if (a.isPast()) {
                        filtered.add(a);
                    }
                }
            }
            return filtered;
        });

        cancelledAppointments = Transformations.map(allAppointments, appointments -> {
            List<Appointment> filtered = new ArrayList<>();
            if (appointments != null) {
                for (Appointment a : appointments) {
                    if (a.isCancelled()) {
                        filtered.add(a);
                    }
                }
            }
            return filtered;
        });
    }

    public void setUserInfo(String userId, String role) {
        // Creating a new UserRoleInfo object ensures the switchMap trigger always fires
        userRoleInfo.setValue(new UserRoleInfo(userId, role));
    }

    public void refresh() {
        UserRoleInfo current = userRoleInfo.getValue();
        if (current != null) {
            setUserInfo(current.userId, current.role);
        }
    }

    public LiveData<List<Appointment>> getAllAppointments() {
        return allAppointments;
    }

    public LiveData<List<Appointment>> getUpcomingAppointments() {
        return upcomingAppointments;
    }

    public LiveData<List<Appointment>> getPendingAppointments() {
        return pendingAppointments;
    }

    public LiveData<List<Appointment>> getPastAppointments() {
        return pastAppointments;
    }

    public LiveData<List<Appointment>> getCancelledAppointments() {
        return cancelledAppointments;
    }

    public void updateStatus(Appointment appointment, String status, FirebaseHelper.OnCompleteListener<Void> callback) {
        repository.updateAppointmentStatus(appointment, status, new FirebaseHelper.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                refresh(); // Explicitly refresh after status update
                callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    private static class UserRoleInfo {
        String userId;
        String role;

        UserRoleInfo(String userId, String role) {
            this.userId = userId;
            this.role = role;
        }
    }
}
