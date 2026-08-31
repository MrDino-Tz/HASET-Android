package com.haset.hasetapp.repositories;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.haset.hasetapp.utils.FirebaseHelper;
import com.haset.hasetapp.utils.CrashMonitor;
import com.haset.hasetapp.models.Appointment;
import com.haset.hasetapp.database.entities.AppointmentEntity;

import java.util.ArrayList;
import java.util.List;
import android.os.Handler;
import android.os.Looper;

public class AppointmentRepository {
    private final FirebaseHelper firebaseHelper = FirebaseHelper.getInstance();

    public LiveData<List<Appointment>> getAppointments(String userId, String role) {
        MutableLiveData<List<Appointment>> appointmentsLiveData = new MutableLiveData<>();
        final boolean[] completed = {false};
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!completed[0]) appointmentsLiveData.postValue(new ArrayList<>());
        }, 15000);
        
        FirebaseHelper.getAppointmentsByUser(userId, role, new FirebaseHelper.OnCompleteListener<List<AppointmentEntity>>() {
            @Override
            public void onSuccess(List<AppointmentEntity> appointmentEntities) {
                completed[0] = true;
                List<Appointment> list = new ArrayList<>();
                for (AppointmentEntity entity : appointmentEntities) {
                    list.add(new Appointment(entity));
                }
                appointmentsLiveData.postValue(list);
            }

            @Override
            public void onError(String error) {
                completed[0] = true;
                // Always complete the loading stream. Leaving it unset makes
                // every appointment tab display an endless spinner on a rules
                // or connectivity error.
                appointmentsLiveData.postValue(new ArrayList<>());
            }
        });
        
        return appointmentsLiveData;
    }

    public void updateAppointmentStatus(Appointment appointment, String status, FirebaseHelper.OnCompleteListener<Void> callback) {
        AppointmentEntity entity = new AppointmentEntity(appointment, status);
        FirebaseHelper.updateAppointment(entity, callback);
    }

    public void createAppointment(AppointmentEntity appointment, FirebaseHelper.OnCompleteListener<AppointmentEntity> callback) {
        CrashMonitor.step("appointment", "AppointmentRepository.create",
                "create appointment patient=" + appointment.getPatientId() + " doctor=" + appointment.getDoctorId());
        FirebaseHelper.createAppointment(appointment, new FirebaseHelper.OnCompleteListener<AppointmentEntity>() {
            @Override
            public void onSuccess(AppointmentEntity result) {
                CrashMonitor.breadcrumb("appointment created id=" + result.getAppointmentId());
                callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                CrashMonitor.report("appointment", "AppointmentRepository.create",
                        "create appointment failed: " + error, null);
                callback.onError(error);
            }
        });
    }
}
