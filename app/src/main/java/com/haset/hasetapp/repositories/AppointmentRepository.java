package com.haset.hasetapp.repositories;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.haset.hasetapp.utils.FirebaseHelper;
import com.haset.hasetapp.models.Appointment;
import com.haset.hasetapp.database.entities.AppointmentEntity;

import java.util.ArrayList;
import java.util.List;

public class AppointmentRepository {
    private final FirebaseHelper firebaseHelper = FirebaseHelper.getInstance();

    public LiveData<List<Appointment>> getAppointments(String userId, String role) {
        MutableLiveData<List<Appointment>> appointmentsLiveData = new MutableLiveData<>();
        
        FirebaseHelper.getAppointmentsByUser(userId, role, new FirebaseHelper.OnCompleteListener<List<AppointmentEntity>>() {
            @Override
            public void onSuccess(List<AppointmentEntity> appointmentEntities) {
                List<Appointment> list = new ArrayList<>();
                for (AppointmentEntity entity : appointmentEntities) {
                    list.add(new Appointment(entity));
                }
                appointmentsLiveData.postValue(list);
            }

            @Override
            public void onError(String error) {
                // Could implement error state
            }
        });
        
        return appointmentsLiveData;
    }

    public void updateAppointmentStatus(Appointment appointment, String status, FirebaseHelper.OnCompleteListener<Void> callback) {
        AppointmentEntity entity = new AppointmentEntity(appointment, status);
        FirebaseHelper.updateAppointment(entity, callback);
    }

    public void createAppointment(AppointmentEntity appointment, FirebaseHelper.OnCompleteListener<AppointmentEntity> callback) {
        FirebaseHelper.createAppointment(appointment, callback);
    }
}
