package com.haset.hasetapp.firebase;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.haset.hasetapp.database.entities.ArticlePostEntity;
import com.haset.hasetapp.utils.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * Firebase helper class for Article operations.
 * Provides methods to create, read, update, and delete articles in Firebase.
 */
public class ArticlePostHelper {
    private static ArticlePostHelper instance;
    private DatabaseReference articlePostsRef;
    
    private ArticlePostHelper() {
        articlePostsRef = FirebaseHelper.getInstance().getDatabaseReference().child("article_posts");
    }
    
    public static synchronized ArticlePostHelper getInstance() {
        if (instance == null) {
            instance = new ArticlePostHelper();
        }
        return instance;
    }
    
    /**
     * Interface for handling Firebase operation results
     */
    public interface OnCompleteListener<T> {
        void onSuccess(T result);
        void onError(String error);
    }
    
    /**
     * Create a new article
     */
    public void createArticle(ArticlePostEntity articlePost, OnCompleteListener<String> listener) {
        if (articlePost.getPostId() == null || articlePost.getPostId().isEmpty()) {
            articlePost.setPostId(articlePostsRef.push().getKey());
        }
        
        articlePostsRef.child(articlePost.getPostId())
                .setValue(articlePost)
                .addOnSuccessListener(aVoid -> listener.onSuccess(articlePost.getPostId()))
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
    
    /**
     * Get all articles
     */
    public void getAllArticles(OnCompleteListener<List<ArticlePostEntity>> listener) {
        articlePostsRef.get()
                .addOnSuccessListener(dataSnapshot -> {
                    List<ArticlePostEntity> posts = new ArrayList<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        ArticlePostEntity post = snapshot.getValue(ArticlePostEntity.class);
                        if (post != null) {
                            posts.add(post);
                        }
                    }
                    loadInteractionCounts(posts, listener);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
    
    /**
     * Get articles by type (image, text)
     */
    public void getArticlesByType(String type, OnCompleteListener<List<ArticlePostEntity>> listener) {
        articlePostsRef.orderByChild("type").equalTo(type)
                .get()
                .addOnSuccessListener(dataSnapshot -> {
                    List<ArticlePostEntity> posts = new ArrayList<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        ArticlePostEntity post = snapshot.getValue(ArticlePostEntity.class);
                        if (post != null) {
                            posts.add(post);
                        }
                    }
                    listener.onSuccess(posts);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
    
    /**
     * Get published articles only
     */
    public void getPublishedArticles(OnCompleteListener<List<ArticlePostEntity>> listener) {
        articlePostsRef.orderByChild("status").equalTo("published")
                .get()
                .addOnSuccessListener(dataSnapshot -> {
                    List<ArticlePostEntity> posts = new ArrayList<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        ArticlePostEntity post = snapshot.getValue(ArticlePostEntity.class);
                        if (post != null && "published".equals(post.getStatus())) {
                            posts.add(post);
                        }
                    }
                    // Sort by creation date (newest first)
                    posts.sort((p1, p2) -> Long.compare(p2.getCreatedAt(), p1.getCreatedAt()));
                    loadInteractionCounts(posts, listener);
                })
                .addOnFailureListener(e -> {
                    // Fallback: Get all posts and filter locally
                    android.util.Log.w("ArticlePostHelper", "Query by status failed, falling back to getAllArticles: " + e.getMessage());
                    articlePostsRef.get()
                            .addOnSuccessListener(dataSnapshot -> {
                                List<ArticlePostEntity> posts = new ArrayList<>();
                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                    ArticlePostEntity post = snapshot.getValue(ArticlePostEntity.class);
                                    if (post != null && "published".equals(post.getStatus())) {
                                        posts.add(post);
                                    }
                                }
                                // Sort by creation date (newest first)
                                posts.sort((p1, p2) -> Long.compare(p2.getCreatedAt(), p1.getCreatedAt()));
                                loadInteractionCounts(posts, listener);
                            })
                            .addOnFailureListener(e2 -> listener.onError(e2.getMessage()));
                });
    }
    
    /**
     * Get published articles by type
     */
    public void getPublishedArticlesByType(String type, OnCompleteListener<List<ArticlePostEntity>> listener) {
        articlePostsRef.orderByChild("type").equalTo(type)
                .get()
                .addOnSuccessListener(dataSnapshot -> {
                    List<ArticlePostEntity> posts = new ArrayList<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        ArticlePostEntity post = snapshot.getValue(ArticlePostEntity.class);
                        if (post != null && "published".equals(post.getStatus())) {
                            posts.add(post);
                        }
                    }
                    // Sort by creation date (newest first)
                    posts.sort((p1, p2) -> Long.compare(p2.getCreatedAt(), p1.getCreatedAt()));
                    listener.onSuccess(posts);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
    
    /**
     * Get a specific article by ID
     */
    public void getArticleById(String postId, OnCompleteListener<ArticlePostEntity> listener) {
        articlePostsRef.child(postId)
                .get()
                .addOnSuccessListener(dataSnapshot -> {
                    ArticlePostEntity post = dataSnapshot.getValue(ArticlePostEntity.class);
                    if (post != null) {
                        listener.onSuccess(post);
                    } else {
                        listener.onError("Article not found");
                    }
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
    
    /**
     * Update an existing article
     */
    public void updateArticle(ArticlePostEntity articlePost, OnCompleteListener<Void> listener) {
        if (articlePost.getPostId() == null || articlePost.getPostId().isEmpty()) {
            listener.onError("Article ID cannot be null or empty");
            return;
        }
        
        // Update the timestamp
        articlePost.setUpdatedAt(System.currentTimeMillis());
        
        articlePostsRef.child(articlePost.getPostId())
                .setValue(articlePost)
                .addOnSuccessListener(aVoid -> listener.onSuccess(null))
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
    
    /**
     * Update article status (published/draft)
     */
    public void updateArticleStatus(String postId, String status, OnCompleteListener<Void> listener) {
        articlePostsRef.child(postId).child("status")
                .setValue(status)
                .addOnSuccessListener(aVoid -> {
                    // Also update the updatedAt timestamp
                    articlePostsRef.child(postId).child("updatedAt")
                            .setValue(System.currentTimeMillis())
                            .addOnSuccessListener(aVoid2 -> listener.onSuccess(null))
                            .addOnFailureListener(e -> listener.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
    
    /**
     * Increment likes count for an article
     */
    public void incrementLikes(String postId, OnCompleteListener<Void> listener) {
        articlePostsRef.child(postId).child("likes")
                .get()
                .addOnSuccessListener(dataSnapshot -> {
                    Integer currentLikes = dataSnapshot.getValue(Integer.class);
                    int newLikes = (currentLikes != null ? currentLikes : 0) + 1;
                    
                    articlePostsRef.child(postId).child("likes")
                            .setValue(newLikes)
                            .addOnSuccessListener(aVoid -> listener.onSuccess(null))
                            .addOnFailureListener(e -> listener.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
    
    /**
     * Toggle like for an article
     */
    public void toggleLike(String postId, String userId, OnCompleteListener<Boolean> listener) {
        if (postId == null || postId.isEmpty() || userId == null || userId.isEmpty()) {
            listener.onError("Post ID and User ID are required");
            return;
        }
        
        DatabaseReference postLikesRef = FirebaseHelper.getInstance().getDatabaseReference()
                .child("post_likes").child(postId).child(userId);
        
        // Check if user already liked this post
        postLikesRef.get().addOnSuccessListener(dataSnapshot -> {
            boolean isLiked = dataSnapshot.exists() && Boolean.TRUE.equals(dataSnapshot.getValue(Boolean.class));
            
            if (isLiked) {
                // The per-user child is the source of truth. Normal users cannot
                // write the protected article_posts counter.
                postLikesRef.removeValue()
                        .addOnSuccessListener(aVoid -> listener.onSuccess(false))
                        .addOnFailureListener(e -> listener.onError(e.getMessage()));
            } else {
                postLikesRef.setValue(true)
                        .addOnSuccessListener(aVoid -> listener.onSuccess(true))
                        .addOnFailureListener(e -> listener.onError(e.getMessage()));
            }
        }).addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
    
    /**
     * Check if a user has liked a post
     */
    public void isPostLikedByUser(String postId, String userId, OnCompleteListener<Boolean> listener) {
        if (postId == null || postId.isEmpty() || userId == null || userId.isEmpty()) {
            listener.onSuccess(false);
            return;
        }
        
        DatabaseReference postLikesRef = FirebaseHelper.getInstance().getDatabaseReference()
                .child("post_likes").child(postId).child(userId);
        
        postLikesRef.get().addOnSuccessListener(dataSnapshot -> {
            boolean isLiked = dataSnapshot.exists() && Boolean.TRUE.equals(dataSnapshot.getValue(Boolean.class));
            listener.onSuccess(isLiked);
        }).addOnFailureListener(e -> {
            // If error, assume not liked
            listener.onSuccess(false);
        });
    }
    
    /**
     * Increment comments count for an article
     */
    public void incrementComments(String postId, OnCompleteListener<Void> listener) {
        FirebaseHelper.getInstance().getDatabaseReference()
                .child("post_comments").child(postId).get()
                .addOnSuccessListener(dataSnapshot -> {
                    if (listener != null) listener.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }
    
    /**
     * Increment shares count for an article
     */
    public void incrementShares(String postId, OnCompleteListener<Void> listener) {
        articlePostsRef.child(postId).child("shares")
                .get()
                .addOnSuccessListener(dataSnapshot -> {
                    Integer currentShares = dataSnapshot.getValue(Integer.class);
                    int newShares = (currentShares != null ? currentShares : 0) + 1;
                    
                    articlePostsRef.child(postId).child("shares")
                            .setValue(newShares)
                            .addOnSuccessListener(aVoid -> {
                                if (listener != null) listener.onSuccess(null);
                            })
                            .addOnFailureListener(e -> {
                                if (listener != null) listener.onError(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }
    
    /**
     * Increment views count for an article
     */
    public void incrementViews(String postId, OnCompleteListener<Void> listener) {
        articlePostsRef.child(postId).child("views")
                .get()
                .addOnSuccessListener(dataSnapshot -> {
                    Integer currentViews = dataSnapshot.getValue(Integer.class);
                    int newViews = (currentViews != null ? currentViews : 0) + 1;
                    
                    articlePostsRef.child(postId).child("views")
                            .setValue(newViews)
                            .addOnSuccessListener(aVoid -> {
                                if (listener != null) listener.onSuccess(null);
                            })
                            .addOnFailureListener(e -> {
                                if (listener != null) listener.onError(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }
    
    /**
     * Delete an article
     */
    public void deleteArticle(String postId, OnCompleteListener<Void> listener) {
        articlePostsRef.child(postId)
                .removeValue()
                .addOnSuccessListener(aVoid -> listener.onSuccess(null))
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
    
    /**
     * Get trending (most viewed) published articles
     * @param limit Maximum number of articles to return
     */
    public void getTrendingArticles(int limit, OnCompleteListener<List<ArticlePostEntity>> listener) {
        articlePostsRef.get()
                .addOnSuccessListener(dataSnapshot -> {
                    List<ArticlePostEntity> posts = new ArrayList<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        ArticlePostEntity post = snapshot.getValue(ArticlePostEntity.class);
                        if (post != null && "published".equals(post.getStatus()) && post.getViews() >= 50) {
                            posts.add(post);
                        }
                    }
                    posts.sort((p1, p2) -> Integer.compare(p2.getViews(), p1.getViews()));
                    if (posts.size() > limit) {
                        posts = posts.subList(0, limit);
                    }
                    listener.onSuccess(posts);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
    
    /**
     * Listen for real-time updates to all articles
     */
    public void listenForArticlesUpdates(OnCompleteListener<List<ArticlePostEntity>> listener) {
        articlePostsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<ArticlePostEntity> posts = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ArticlePostEntity post = snapshot.getValue(ArticlePostEntity.class);
                    if (post != null) {
                        posts.add(post);
                    }
                }
                loadInteractionCounts(posts, listener);
            }
            
            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onError(databaseError.getMessage());
            }
        });
    }

    private void loadInteractionCounts(List<ArticlePostEntity> posts,
                                       OnCompleteListener<List<ArticlePostEntity>> listener) {
        DatabaseReference databaseRoot = FirebaseHelper.getInstance().getDatabaseReference();
        com.google.android.gms.tasks.Task<DataSnapshot> likesTask = databaseRoot.child("post_likes").get();
        com.google.android.gms.tasks.Task<DataSnapshot> commentsTask = databaseRoot.child("post_comments").get();

        com.google.android.gms.tasks.Tasks.whenAllComplete(likesTask, commentsTask)
                .addOnCompleteListener(task -> {
                    DataSnapshot likes = likesTask.isSuccessful() ? likesTask.getResult() : null;
                    DataSnapshot comments = commentsTask.isSuccessful() ? commentsTask.getResult() : null;
                    for (ArticlePostEntity post : posts) {
                        String postId = post.getPostId();
                        if (likes != null) {
                            post.setLikes((int) likes.child(postId).getChildrenCount());
                        }
                        if (comments != null) {
                            post.setComments((int) comments.child(postId).getChildrenCount());
                        }
                    }
                    listener.onSuccess(posts);
                });
    }
    /**
     * Toggle save status for an article
     */
    public void toggleSave(String postId, String userId, OnCompleteListener<Boolean> listener) {
        if (postId == null || postId.isEmpty() || userId == null || userId.isEmpty()) {
            listener.onError("Post ID and User ID are required");
            return;
        }
        
        DatabaseReference savedArticlesRef = FirebaseHelper.getInstance().getDatabaseReference()
                .child("saved_articles").child(userId).child(postId);
        
        savedArticlesRef.get().addOnSuccessListener(dataSnapshot -> {
            boolean isSaved = dataSnapshot.exists();
            if (isSaved) {
                savedArticlesRef.removeValue().addOnSuccessListener(aVoid -> listener.onSuccess(false))
                        .addOnFailureListener(e -> listener.onError(e.getMessage()));
            } else {
                savedArticlesRef.setValue(System.currentTimeMillis()).addOnSuccessListener(aVoid -> listener.onSuccess(true))
                        .addOnFailureListener(e -> listener.onError(e.getMessage()));
            }
        }).addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
    
    /**
     * Check if a user has saved a post
     */
    public void isPostSavedByUser(String postId, String userId, OnCompleteListener<Boolean> listener) {
        if (postId == null || postId.isEmpty() || userId == null || userId.isEmpty()) {
            listener.onSuccess(false);
            return;
        }
        
        DatabaseReference savedArticlesRef = FirebaseHelper.getInstance().getDatabaseReference()
                .child("saved_articles").child(userId).child(postId);
        
        savedArticlesRef.get().addOnSuccessListener(dataSnapshot -> {
            listener.onSuccess(dataSnapshot.exists());
        }).addOnFailureListener(e -> listener.onSuccess(false));
    }
    
    /**
     * Get all articles saved by a user
     */
    public void getSavedArticles(String userId, OnCompleteListener<List<ArticlePostEntity>> listener) {
        DatabaseReference savedRef = FirebaseHelper.getInstance().getDatabaseReference().child("saved_articles").child(userId);
        savedRef.get().addOnSuccessListener(snapshot -> {
            List<String> savedIds = new ArrayList<>();
            for (DataSnapshot child : snapshot.getChildren()) {
                savedIds.add(child.getKey());
            }
            
            if (savedIds.isEmpty()) {
                listener.onSuccess(new ArrayList<>());
                return;
            }
            
            // Get all published articles and filter by saved IDs
            getPublishedArticles(new OnCompleteListener<List<ArticlePostEntity>>() {
                @Override
                public void onSuccess(List<ArticlePostEntity> allPosts) {
                    List<ArticlePostEntity> savedPosts = new ArrayList<>();
                    for (ArticlePostEntity post : allPosts) {
                        if (savedIds.contains(post.getPostId())) {
                            savedPosts.add(post);
                        }
                    }
                    listener.onSuccess(savedPosts);
                }
                
                @Override
                public void onError(String error) {
                    listener.onError(error);
                }
            });
        }).addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
}
