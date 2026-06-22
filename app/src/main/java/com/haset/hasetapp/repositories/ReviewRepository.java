package com.haset.hasetapp.repositories;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.haset.hasetapp.database.AppDatabase;
// DISABLED FOR V1 - RATING SYSTEM COMING IN VERSION 2.0
// import com.haset.hasetapp.database.entities.DoctorRatingEntity;
// import com.haset.hasetapp.utils.FirebaseHelper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/* DISABLED FOR V1 - RATING SYSTEM COMING IN VERSION 2.0
public class ReviewRepository {
    private final AppDatabase database;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ReviewRepository(Context context) {
        this.database = AppDatabase.getInstance(context);
    }

    public LiveData<List<DoctorRatingEntity>> getReviews(String doctorId) {
        MutableLiveData<List<DoctorRatingEntity>> reviewsLiveData = new MutableLiveData<>();

        executor.execute(() -> {
            List<DoctorRatingEntity> localReviews = database.doctorRatingDao().getRatingsByDoctorId(doctorId);
            if (!localReviews.isEmpty()) {
                reviewsLiveData.postValue(localReviews);
            }

            FirebaseHelper.getRatingsByDoctor(doctorId, new FirebaseHelper.OnCompleteListener<List<DoctorRatingEntity>>() {
                @Override
                public void onSuccess(List<DoctorRatingEntity> remoteReviews) {
                    if (remoteReviews != null) {
                        reviewsLiveData.postValue(remoteReviews);
                        
                        executor.execute(() -> {
                            for (DoctorRatingEntity review : remoteReviews) {
                                database.doctorRatingDao().insert(review);
                            }
                        });
                    }
                }

                @Override
                public void onError(String error) {
                }
            });
        });

        return reviewsLiveData;
    }
    
    public LiveData<Double> getAverageRating(String doctorId) {
        MutableLiveData<Double> ratingLiveData = new MutableLiveData<>();
        executor.execute(() -> {
            Double average = database.doctorRatingDao().getAverageRating(doctorId);
            ratingLiveData.postValue(average != null ? average : 0.0);
        });
        return ratingLiveData;
    }

    public LiveData<Integer> getRatingCount(String doctorId) {
        MutableLiveData<Integer> countLiveData = new MutableLiveData<>();
        executor.execute(() -> {
            int count = database.doctorRatingDao().getRatingCount(doctorId);
            countLiveData.postValue(count);
        });
        return countLiveData;
    }
}
*/

// Placeholder class - Rating system disabled for V1
public class ReviewRepository {
    public ReviewRepository(Context context) {}
}
