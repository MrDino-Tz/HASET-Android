package com.haset.hasetapp.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.haset.hasetapp.adapters.PatientBannerAdapter;
import com.haset.hasetapp.repositories.AdminRepository;
import com.haset.hasetapp.utils.FirebaseHelper;
import com.haset.hasetapp.utils.CloudinaryUploadHelper;
import android.net.Uri;
import android.content.Context;
import java.util.UUID;

import java.util.List;

public class AdminBannersViewModel extends AndroidViewModel {
    private final AdminRepository repository;
    private LiveData<List<PatientBannerAdapter.BannerItem>> banners;
    private final MutableLiveData<Boolean> processing = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> operationSuccess = new MutableLiveData<>();

    public AdminBannersViewModel(@NonNull Application application) {
        super(application);
        repository = new AdminRepository();
    }

    public LiveData<List<PatientBannerAdapter.BannerItem>> getBanners() {
        if (banners == null) {
            banners = repository.getBanners();
        }
        return banners;
    }

    public LiveData<Boolean> getProcessing() { return processing; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getOperationSuccess() { return operationSuccess; }

    public void addBanner(PatientBannerAdapter.BannerItem banner) {
        processing.setValue(true);
        repository.addBanner(banner, new FirebaseHelper.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                processing.postValue(false);
                operationSuccess.postValue(true);
            }

            @Override
            public void onError(String err) {
                processing.postValue(false);
                error.postValue(err);
            }
        });
    }

    public void updateBanner(String key, PatientBannerAdapter.BannerItem banner) {
        processing.setValue(true);
        repository.updateBanner(key, banner, new FirebaseHelper.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                processing.postValue(false);
                operationSuccess.postValue(true);
            }

            @Override
            public void onError(String err) {
                processing.postValue(false);
                error.postValue(err);
            }
        });
    }

    public void deleteBanner(String key) {
        repository.deleteBanner(key, new FirebaseHelper.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                // Success handled by LiveData listener in repository
            }

            @Override
            public void onError(String err) {
                error.postValue(err);
            }
        });
    }

    public void uploadBannerImage(Context context, Uri uri, String t1, String t2, String b, String btnTxt, PatientBannerAdapter.BannerItem.BannerType type, String existingKey) {
        uploadBannerImage(context, uri, t1, t2, b, btnTxt, type, null, existingKey);
    }

    public void uploadBannerImage(Context context, Uri uri, String t1, String t2, String b, String btnTxt, PatientBannerAdapter.BannerItem.BannerType type, PatientBannerAdapter.BannerItem.BannerType actionType, String existingKey) {
        processing.setValue(true);
        CloudinaryUploadHelper.uploadFile(context, uri, "image", UUID.randomUUID().toString(), "banners", 
            new CloudinaryUploadHelper.OnFileUploadListener() {
                @Override
                public void onUploadStart() {}
                @Override
                public void onUploadProgress(double prog) {}
                @Override
                public void onUploadSuccess(String downloadUrl, String uploadedFileName) {
                    PatientBannerAdapter.BannerItem item;
                    if (type == PatientBannerAdapter.BannerItem.BannerType.IMAGE_BANNER) {
                        item = PatientBannerAdapter.BannerItem.createImageBanner(downloadUrl, actionType);
                    } else {
                        item = new PatientBannerAdapter.BannerItem(t1, t2, b, btnTxt, downloadUrl, type);
                    }
                    if (existingKey != null) {
                        updateBanner(existingKey, item);
                    } else {
                        addBanner(item);
                    }
                }
                @Override
                public void onUploadError(String err) {
                    processing.postValue(false);
                    error.postValue("Upload failed: " + err);
                }
        });
    }
}
