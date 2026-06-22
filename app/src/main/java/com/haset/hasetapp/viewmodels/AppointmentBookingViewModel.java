package com.haset.hasetapp.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.haset.hasetapp.database.entities.AppointmentEntity;
import com.haset.hasetapp.utils.FirebaseHelper;
import com.haset.hasetapp.models.Doctor;
import com.haset.hasetapp.repositories.AppointmentRepository;
import com.haset.hasetapp.repositories.DoctorRepository;

public class AppointmentBookingViewModel extends AndroidViewModel {
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private MutableLiveData<Doctor> doctorLiveData = new MutableLiveData<>();
    private MutableLiveData<Boolean> bookingProcessing = new MutableLiveData<>(false);
    private MutableLiveData<String> bookingError = new MutableLiveData<>();
    private MutableLiveData<AppointmentEntity> bookingSuccess = new MutableLiveData<>();

    public AppointmentBookingViewModel(@NonNull Application application) {
        super(application);
        doctorRepository = new DoctorRepository();
        appointmentRepository = new AppointmentRepository();
    }

    public LiveData<Doctor> getDoctorDetails(String doctorId) {
        return doctorRepository.getDoctorById(doctorId);
    }

    public LiveData<Boolean> getBookingProcessing() {
        return bookingProcessing;
    }

    public LiveData<String> getBookingError() {
        return bookingError;
    }

    public LiveData<AppointmentEntity> getBookingSuccess() {
        return bookingSuccess;
    }

    public void createAppointment(AppointmentEntity appointment) {
        bookingProcessing.setValue(true);
        appointmentRepository.createAppointment(appointment, new FirebaseHelper.OnCompleteListener<AppointmentEntity>() {
            @Override
            public void onSuccess(AppointmentEntity result) {
                bookingProcessing.postValue(false);
                bookingSuccess.postValue(result);
            }

            @Override
            public void onError(String error) {
                bookingProcessing.postValue(false);
                bookingError.postValue(error);
            }
        });
    }
}
