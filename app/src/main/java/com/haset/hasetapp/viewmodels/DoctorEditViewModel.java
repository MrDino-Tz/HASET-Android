package com.haset.hasetapp.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.haset.hasetapp.database.entities.DoctorEntity;
import com.haset.hasetapp.repositories.DoctorRepository;
import com.haset.hasetapp.utils.FirebaseHelper;

public class DoctorEditViewModel extends AndroidViewModel {
    private final DoctorRepository repository;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>();
    private LiveData<DoctorEntity> doctorEntity;

    public DoctorEditViewModel(@NonNull Application application) {
        super(application);
        repository = new DoctorRepository();
    }

    public LiveData<DoctorEntity> getDoctorEntity(String doctorId) {
        if (doctorEntity == null) {
            doctorEntity = repository.getDoctorEntityById(doctorId);
        }
        return doctorEntity;
    }

    public void saveDoctorProfile(DoctorEntity doctor) {
        loading.setValue(true);
        repository.saveDoctorProfile(doctor, new FirebaseHelper.OnCompleteListener<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                loading.postValue(false);
                saveSuccess.postValue(true);
            }

            @Override
            public void onError(String err) {
                loading.postValue(false);
                error.postValue(err);
            }
        });
    }

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getSaveSuccess() { return saveSuccess; }
}
