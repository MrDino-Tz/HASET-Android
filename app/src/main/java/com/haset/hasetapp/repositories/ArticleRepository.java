package com.haset.hasetapp.repositories;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.haset.hasetapp.database.AppDatabase;
import com.haset.hasetapp.database.dao.ArticlePostDao;
import com.haset.hasetapp.database.entities.ArticlePostEntity;
import com.haset.hasetapp.firebase.ArticlePostHelper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ArticleRepository {
    private final ArticlePostDao articlePostDao;
    private final ArticlePostHelper articlePostHelper;
    private final ExecutorService executorService;

    public ArticleRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        articlePostDao = db.articlePostDao();
        articlePostHelper = ArticlePostHelper.getInstance();
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<ArticlePostEntity>> getPublishedArticles() {
        MutableLiveData<List<ArticlePostEntity>> data = new MutableLiveData<>();
        
        // Try local database first
        executorService.execute(() -> {
            List<ArticlePostEntity> localPosts = articlePostDao.getPublishedPosts();
            if (!localPosts.isEmpty()) {
                data.postValue(localPosts);
            }
        });

        // Always fetch from Firebase to keep it up to date
        articlePostHelper.getPublishedArticles(new ArticlePostHelper.OnCompleteListener<List<ArticlePostEntity>>() {
            @Override
            public void onSuccess(List<ArticlePostEntity> result) {
                data.postValue(result);
                // Cache to local database
                executorService.execute(() -> {
                    for (ArticlePostEntity post : result) {
                        ArticlePostEntity existing = articlePostDao.getPostById(post.getPostId());
                        if (existing == null) {
                            articlePostDao.insert(post);
                        } else {
                            articlePostDao.update(post);
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                // Handle error if needed
            }
        });

        return data;
    }

    public LiveData<List<ArticlePostEntity>> getArticlesByAuthor(String authorId) {
        MutableLiveData<List<ArticlePostEntity>> data = new MutableLiveData<>();
        
        // Use real-time listener for author's articles
        articlePostHelper.listenForArticlesUpdates(new ArticlePostHelper.OnCompleteListener<List<ArticlePostEntity>>() {
            @Override
            public void onSuccess(List<ArticlePostEntity> result) {
                List<ArticlePostEntity> myPosts = new java.util.ArrayList<>();
                for (ArticlePostEntity post : result) {
                    if (authorId != null && authorId.equals(post.getAuthorId())) {
                        myPosts.add(post);
                    }
                }
                // Sort by creation date (newest first)
                myPosts.sort((p1, p2) -> Long.compare(p2.getCreatedAt(), p1.getCreatedAt()));
                data.postValue(myPosts);
            }

            @Override
            public void onError(String error) {
                // Handle error
            }
        });
        
        return data;
    }

    public void createArticle(ArticlePostEntity articlePost, ArticlePostHelper.OnCompleteListener<String> callback) {
        articlePostHelper.createArticle(articlePost, new ArticlePostHelper.OnCompleteListener<String>() {
            @Override
            public void onSuccess(String result) {
                executorService.execute(() -> articlePostDao.insert(articlePost));
                callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void updateArticle(ArticlePostEntity articlePost, ArticlePostHelper.OnCompleteListener<Void> callback) {
        articlePostHelper.updateArticle(articlePost, new ArticlePostHelper.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                executorService.execute(() -> articlePostDao.update(articlePost));
                callback.onSuccess(null);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void getArticleById(String postId, ArticlePostHelper.OnCompleteListener<ArticlePostEntity> callback) {
        articlePostHelper.getArticleById(postId, callback);
    }

    public void deleteArticle(String postId, ArticlePostHelper.OnCompleteListener<Void> callback) {
        articlePostHelper.deleteArticle(postId, new ArticlePostHelper.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                executorService.execute(() -> articlePostDao.deletePostById(postId));
                callback.onSuccess(null);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void toggleLike(String postId, String userId, ArticlePostHelper.OnCompleteListener<Boolean> callback) {
        articlePostHelper.toggleLike(postId, userId, callback);
    }

    public void toggleSave(String postId, String userId, ArticlePostHelper.OnCompleteListener<Boolean> callback) {
        articlePostHelper.toggleSave(postId, userId, callback);
    }

    public LiveData<List<ArticlePostEntity>> getSavedArticles(String userId) {
        MutableLiveData<List<ArticlePostEntity>> data = new MutableLiveData<>();
        articlePostHelper.getSavedArticles(userId, new ArticlePostHelper.OnCompleteListener<List<ArticlePostEntity>>() {
            @Override
            public void onSuccess(List<ArticlePostEntity> result) {
                data.postValue(result);
            }

            @Override
            public void onError(String error) {
                // Return empty list on error
                data.postValue(new java.util.ArrayList<>());
            }
        });
        return data;
    }

    public void isPostSavedByUser(String postId, String userId, ArticlePostHelper.OnCompleteListener<Boolean> callback) {
        articlePostHelper.isPostSavedByUser(postId, userId, callback);
    }

    public void incrementViews(String postId) {
        articlePostHelper.incrementViews(postId, null);
    }
}
