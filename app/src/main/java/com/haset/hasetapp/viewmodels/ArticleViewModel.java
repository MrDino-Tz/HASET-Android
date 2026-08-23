package com.haset.hasetapp.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.haset.hasetapp.database.entities.ArticlePostEntity;
import com.haset.hasetapp.repositories.ArticleRepository;
import com.haset.hasetapp.firebase.ArticlePostHelper;
import com.haset.hasetapp.utils.SingleLiveEvent;

import java.util.List;

public class ArticleViewModel extends AndroidViewModel {
    private final ArticleRepository repository;
    private final SingleLiveEvent<String> error = new SingleLiveEvent<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    private final MutableLiveData<List<ArticlePostEntity>> publishedArticles = new MutableLiveData<>();
    private final MutableLiveData<List<ArticlePostEntity>> savedArticles = new MutableLiveData<>();

    public ArticleViewModel(@NonNull Application application) {
        super(application);
        repository = new ArticleRepository(application);
    }

    public LiveData<List<ArticlePostEntity>> getPublishedArticles() {
        if (publishedArticles.getValue() == null) {
            refreshPublishedArticles();
        }
        return publishedArticles;
    }

    public void refreshPublishedArticles() {
        loading.setValue(true);
        repository.getPublishedArticles().observeForever(new androidx.lifecycle.Observer<List<ArticlePostEntity>>() {
            @Override
            public void onChanged(List<ArticlePostEntity> posts) {
                publishedArticles.postValue(posts);
                loading.postValue(false);
            }
        });
    }

    public LiveData<List<ArticlePostEntity>> getSavedArticles(String userId) {
        if (savedArticles.getValue() == null) {
            refreshSavedArticles(userId);
        }
        return savedArticles;
    }

    public void refreshSavedArticles(String userId) {
        loading.setValue(true);
        repository.getSavedArticles(userId).observeForever(new androidx.lifecycle.Observer<List<ArticlePostEntity>>() {
            @Override
            public void onChanged(List<ArticlePostEntity> posts) {
                savedArticles.postValue(posts);
                loading.postValue(false);
            }
        });
    }

    public LiveData<List<ArticlePostEntity>> getArticlesByAuthor(String authorId) {
        return repository.getArticlesByAuthor(authorId);
    }

    public void createArticle(ArticlePostEntity articlePost, ArticlePostHelper.OnCompleteListener<String> callback) {
        loading.setValue(true);
        repository.createArticle(articlePost, new ArticlePostHelper.OnCompleteListener<String>() {
            @Override
            public void onSuccess(String result) {
                loading.setValue(false);
                callback.onSuccess(result);
            }

            @Override
            public void onError(String err) {
                loading.setValue(false);
                error.setValue(err);
                callback.onError(err);
            }
        });
    }

    public void updateArticle(ArticlePostEntity articlePost, ArticlePostHelper.OnCompleteListener<Void> callback) {
        loading.setValue(true);
        repository.updateArticle(articlePost, new ArticlePostHelper.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                loading.setValue(false);
                callback.onSuccess(null);
            }

            @Override
            public void onError(String err) {
                loading.setValue(false);
                error.setValue(err);
                callback.onError(err);
            }
        });
    }

    public void getArticleById(String postId, ArticlePostHelper.OnCompleteListener<ArticlePostEntity> callback) {
        repository.getArticleById(postId, callback);
    }

    public void deleteArticle(String postId, ArticlePostHelper.OnCompleteListener<Void> callback) {
        repository.deleteArticle(postId, callback);
    }

    public void toggleLike(String postId, String userId, ArticlePostHelper.OnCompleteListener<Boolean> callback) {
        repository.toggleLike(postId, userId, callback);
    }

    public void toggleSave(String postId, String userId, ArticlePostHelper.OnCompleteListener<Boolean> callback) {
        repository.toggleSave(postId, userId, callback);
    }


    public void incrementViews(String postId) {
        repository.incrementViews(postId);
    }

    public LiveData<String> getError() {
        return error;
    }

    public void clearError() {
        error.setValue(null);
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }
}
