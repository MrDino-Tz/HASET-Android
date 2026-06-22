package com.haset.hasetapp.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

// DISABLED FOR V1 - RATING SYSTEM COMING IN VERSION 2.0
// import com.haset.hasetapp.database.entities.DoctorRatingEntity;
// import com.haset.hasetapp.repositories.ReviewRepository;

import java.util.List;

/* DISABLED FOR V1 - RATING SYSTEM COMING IN VERSION 2.0
public class ReviewsViewModel extends AndroidViewModel {
    private final ReviewRepository repository;
    private LiveData<List<DoctorRatingEntity>> reviews;

    public ReviewsViewModel(@NonNull Application application) {
        super(application);
        this.repository = new ReviewRepository(application);
    }

    public LiveData<List<DoctorRatingEntity>> getReviews(String doctorId) {
        if (reviews == null) {
            reviews = repository.getReviews(doctorId);
        }
        return reviews;
    }

    public LiveData<Double> getAverageRating(String doctorId) {
        return repository.getAverageRating(doctorId);
    }

    public LiveData<Integer> getRatingCount(String doctorId) {
        return repository.getRatingCount(doctorId);
    }
}
*/

// Placeholder class - Rating system disabled for V1
public class ReviewsViewModel extends AndroidViewModel {
    public ReviewsViewModel(@NonNull Application application) {
        super(application);
    }
}
