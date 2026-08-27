package com.haset.hasetapp.adapters;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.haset.hasetapp.R;
import com.haset.hasetapp.database.entities.ArticlePostEntity;
import com.haset.hasetapp.firebase.ArticlePostHelper;
import com.haset.hasetapp.utils.ProfilePhotoHelper;
import java.io.File;
import java.text.SimpleDateFormat;
import com.haset.hasetapp.utils.AuditLogger;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PostFeedAdapter extends RecyclerView.Adapter<PostFeedAdapter.PostViewHolder> {
    private List<ArticlePostEntity> posts;
    private Context context;
    private boolean isLoading = false;
    private ArticlePostHelper articlePostHelper;
    private com.haset.hasetapp.firebase.FirebaseHelper firebaseHelper;
    private com.haset.hasetapp.utils.PreferenceManager preferenceManager;
    private java.util.Set<String> viewedPosts = new java.util.HashSet<>();
    private OnArticleClickListener articleClickListener;
    // Cache: postId -> isLiked. Prevents async Firebase reads from resetting
    // the optimistic UI state on every onBindViewHolder call.
    private final java.util.Map<String, Boolean> likedPostsCache = new java.util.HashMap<>();
    // Tracks which posts have had their like status fetched from Firebase already.
    private final java.util.Set<String> likeStatusFetched = new java.util.HashSet<>();
    // Posts the user has explicitly interacted with. Stale async read callbacks
    // must NOT override the UI for these posts.
    private final java.util.Set<String> userInteractedPosts = new java.util.HashSet<>();

    public interface OnArticleClickListener {
        void onArticleClick(ArticlePostEntity article);
    }

    public void setOnArticleClickListener(OnArticleClickListener listener) {
        this.articleClickListener = listener;
    }

    public PostFeedAdapter(List<ArticlePostEntity> posts, Context context) {
        this.posts = posts != null ? posts : new java.util.ArrayList<>();
        this.context = context;
        this.articlePostHelper = ArticlePostHelper.getInstance();
        this.firebaseHelper = com.haset.hasetapp.firebase.FirebaseHelper.getInstance();
        this.preferenceManager = new com.haset.hasetapp.utils.PreferenceManager(context);
    }

    public void setPosts(List<ArticlePostEntity> newPosts) {
        if (newPosts == null) newPosts = new java.util.ArrayList<>();
        
        if (this.posts.isEmpty()) {
            this.posts = newPosts;
            notifyDataSetChanged();
            return;
        }

        // Merge: update existing post data but do NOT override the like state
        // that the user just set (cached in likedPostsCache)
        java.util.Map<String, ArticlePostEntity> existingMap = new java.util.HashMap<>();
        for (ArticlePostEntity p : this.posts) {
            if (p != null && p.getPostId() != null) existingMap.put(p.getPostId(), p);
        }

        for (ArticlePostEntity incoming : newPosts) {
            if (incoming == null || incoming.getPostId() == null) continue;
            // If we have a cached like state for this post, restore it into the
            // incoming post object so the count stays consistent with the icon.
            if (likedPostsCache.containsKey(incoming.getPostId())) {
                boolean cachedLiked = Boolean.TRUE.equals(likedPostsCache.get(incoming.getPostId()));
                ArticlePostEntity existing = existingMap.get(incoming.getPostId());
                if (existing != null) {
                    // Use the count from existing (optimistic) if it differs from
                    // what Firebase returned by exactly ±1 (i.e. our optimistic delta)
                    int incomingLikes = incoming.getLikes();
                    int existingLikes = existing.getLikes();
                    // Trust Firebase count but never let it wipe our icon
                    incoming.setLikes(incomingLikes);
                }
            }
        }

        this.posts = newPosts;
        notifyDataSetChanged();
    }
    
    public void setLoading(boolean loading) {
        this.isLoading = loading;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 1) {
            // Shimmer item
            com.facebook.shimmer.ShimmerFrameLayout shimmerLayout = 
                com.haset.hasetapp.utils.ShimmerHelper.createShimmerLayout(
                    parent.getContext(), 
                    R.layout.shimmer_layout_post_item
                );
            shimmerLayout.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            return new PostViewHolder(shimmerLayout);
        } else {
            // Regular post item
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_article_feed, parent, false);
            return new PostViewHolder(view);
        }
    }
    
    @Override
    public int getItemViewType(int position) {
        // Return 1 for shimmer items (null posts), 0 for regular posts
        if (isLoading && (position >= posts.size() || posts.get(position) == null)) {
            return 1; // Shimmer type
        }
        return 0; // Regular post type
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        if (position >= posts.size()) return;
        
        // If loading and this is a shimmer item, just show shimmer (already set in onCreateViewHolder)
        if (isLoading && posts.get(position) == null) {
            // Shimmer is already set up in onCreateViewHolder, just start it
            if (holder.itemView instanceof com.facebook.shimmer.ShimmerFrameLayout) {
                ((com.facebook.shimmer.ShimmerFrameLayout) holder.itemView).startShimmer();
            }
            return;
        }
        
        ArticlePostEntity post = posts.get(position);
        if (post == null) return;
        
        // Profile name
        holder.tvProfileName.setText(post.getProfileName() != null ? post.getProfileName() : context.getString(R.string.app_name));
        
        // Load author profile photo with shimmer
        if (post.getAuthorId() != null && !post.getAuthorId().isEmpty()) {
            ProfilePhotoHelper.loadProfilePhoto(context, post.getAuthorId(), holder.ivProfile, holder.shimmerProfile);
        } else {
            // Default/Fallback
            holder.ivProfile.setImageResource(R.drawable.profile_photo);
            if (holder.shimmerProfile != null) {
                holder.shimmerProfile.stopShimmer();
                holder.shimmerProfile.setVisibility(View.GONE);
            }
        }
        
        // Time ago
        holder.tvTimeAgo.setText(getTimeAgo(post.getCreatedAt()));
        
        // Title
        String localizedTitle = post.getLocalizedTitle(com.haset.hasetapp.utils.LocaleHelper.getLanguage(context));
        String localizedDescription = post.getLocalizedDescription(com.haset.hasetapp.utils.LocaleHelper.getLanguage(context));
        if (localizedTitle != null && !localizedTitle.isEmpty()) {
            holder.tvPostTitle.setText(localizedTitle);
            holder.tvPostTitle.setVisibility(View.VISIBLE);
        } else {
            holder.tvPostTitle.setVisibility(View.GONE);
        }
        
        // Description
        if (localizedDescription != null && !localizedDescription.isEmpty()) {
            holder.tvPostDescription.setText(localizedDescription);
            holder.tvPostDescription.setVisibility(View.VISIBLE);
        } else {
            holder.tvPostDescription.setVisibility(View.GONE);
        }
        
        // Media content - Reset media views
        // Media content - Reset media views
        if (holder.containerMedia != null) holder.containerMedia.setVisibility(View.GONE);
        if (holder.ivPostImage != null) holder.ivPostImage.setVisibility(View.GONE);
        if (holder.containerVideo != null) {
            holder.containerVideo.setVisibility(View.GONE);
        }
        
        String postType = post.getType();
        if ("image".equals(postType)) {
            String imageUrl = post.getImageUrl();
            String imagePath = post.getImagePath();
            
            if (imageUrl != null && !imageUrl.isEmpty()) {
                if (holder.containerMedia != null) holder.containerMedia.setVisibility(View.VISIBLE);
                if (holder.ivPostImage != null) holder.ivPostImage.setVisibility(View.VISIBLE);
                loadImageFromUrl(holder.ivPostImage, imageUrl);
            } else if (imagePath != null && !imagePath.isEmpty()) {
                if (holder.containerMedia != null) holder.containerMedia.setVisibility(View.VISIBLE);
                if (holder.ivPostImage != null) holder.ivPostImage.setVisibility(View.VISIBLE);
                loadImage(holder.ivPostImage, imagePath);
            }
        } else if ("text".equals(postType)) {
            if (holder.containerMedia != null) holder.containerMedia.setVisibility(View.GONE);
        }
        
        // Tags and Music (for video posts)
        if (post.getTags() != null && !post.getTags().isEmpty()) {
            if (holder.tvTags != null) {
                holder.tvTags.setText(post.getTags());
                holder.tvTags.setVisibility(View.VISIBLE);
            }
            if (holder.containerTagsMusic != null) {
                holder.containerTagsMusic.setVisibility(View.VISIBLE);
            }
        } else {
            if (holder.tvTags != null) holder.tvTags.setVisibility(View.GONE);
        }
        
        
        if (holder.containerTagsMusic != null && holder.tvTags != null) {
            if (holder.containerTagsMusic.getVisibility() == View.VISIBLE && 
                holder.tvTags.getVisibility() == View.GONE) {
                holder.containerTagsMusic.setVisibility(View.GONE);
            }
        }
        
        // Like, Comment, Share counts
        if (holder.tvLikeCount != null) updateActionCount(holder.tvLikeCount, post.getLikes(), "likes");
        if (holder.tvCommentCount != null) updateActionCount(holder.tvCommentCount, post.getComments(), "comments");
        if (holder.tvViewCount != null) updateActionCount(holder.tvViewCount, post.getViews(), "views");
        
        // Get current user ID and make it final for lambda usage
        String userIdFromFirebase = firebaseHelper.getCurrentUserId();
        String userIdFromPrefs = preferenceManager.getUserId();
        final String currentUserId = (userIdFromFirebase != null && !userIdFromFirebase.isEmpty()) 
                ? userIdFromFirebase 
                : (userIdFromPrefs != null && !userIdFromPrefs.isEmpty() ? userIdFromPrefs : null);
        
        // Use holder's tag to store liked state
        final java.util.concurrent.atomic.AtomicBoolean isLiked = new java.util.concurrent.atomic.AtomicBoolean(false);

        // --- Like state: cache-first with generation guard ---
        final String postId = post.getPostId();
        final int bindGen = holder.bindGeneration.incrementAndGet();

        if (likedPostsCache.containsKey(postId)) {
            // We already know the state (from a previous fetch or a user action)
            boolean cachedState = Boolean.TRUE.equals(likedPostsCache.get(postId));
            isLiked.set(cachedState);
            updateLikeUI(holder, cachedState, post.getLikes());
        } else if (currentUserId != null && !currentUserId.isEmpty()) {
            // First time seeing this post — fetch from Firebase once
            articlePostHelper.isPostLikedByUser(postId, currentUserId,
                new ArticlePostHelper.OnCompleteListener<Boolean>() {
                    @Override
                    public void onSuccess(Boolean liked) {
                        // Discard if holder was rebound OR user already interacted
                        if (holder.bindGeneration.get() != bindGen) return;
                        if (userInteractedPosts.contains(postId)) return;
                        likedPostsCache.put(postId, liked);
                        isLiked.set(liked);
                        updateLikeUI(holder, liked, post.getLikes());
                    }

                    @Override
                    public void onError(String error) {
                        if (error != null) com.haset.hasetapp.utils.ErrorLogger.log(error, error);
                        if (holder.bindGeneration.get() != bindGen) return;
                        if (userInteractedPosts.contains(postId)) return;
                        likedPostsCache.put(postId, false);
                        isLiked.set(false);
                        updateLikeUI(holder, false, post.getLikes());
                    }
                });
        } else {
            isLiked.set(false);
            updateLikeUI(holder, false, post.getLikes());
        }
        
        // Set click listeners
        if (holder.btnLike != null) {
            holder.btnLike.setOnClickListener(v -> {
                if (currentUserId == null || currentUserId.isEmpty()) {
                    return;
                }
                
                holder.btnLike.setEnabled(false);
                
                final boolean currentLikedState = isLiked.get();
                final int currentLikes = post.getLikes();
                
                boolean newLikedState = !currentLikedState;
                int newLikes = newLikedState ? currentLikes + 1 : Math.max(0, currentLikes - 1);
                post.setLikes(newLikes);
                isLiked.set(newLikedState);
                // Mark user interaction and update cache immediately
                userInteractedPosts.add(postId);
                likedPostsCache.put(postId, newLikedState);
                updateLikeUI(holder, newLikedState, newLikes);
                
                articlePostHelper.toggleLike(post.getPostId(), currentUserId, 
                    new ArticlePostHelper.OnCompleteListener<Boolean>() {
                        @Override
                        public void onSuccess(Boolean isLikedResult) {
                            // Keep cache in sync with server truth
                            likedPostsCache.put(post.getPostId(), isLikedResult);
                            if (isLikedResult != newLikedState) {
                                post.setLikes(isLikedResult ? currentLikes + 1 : Math.max(0, currentLikes - 1));
                                isLiked.set(isLikedResult);
                                updateLikeUI(holder, isLikedResult, post.getLikes());
                            }
                            AuditLogger.getInstance(context).logPostLiked(post.getPostId(), isLikedResult, post.getType());
                            if (holder.btnLike != null) holder.btnLike.setEnabled(true);
                            
                            String message = isLikedResult ? context.getString(R.string.article_liked) : context.getString(R.string.article_unliked);
                            Snackbar.make(holder.itemView, message, Snackbar.LENGTH_SHORT).show();
                        }
                        
                        @Override
                        public void onError(String error) {
                            if (error != null) com.haset.hasetapp.utils.ErrorLogger.log(error, error);
                            // Revert cache on error
                            likedPostsCache.put(post.getPostId(), currentLikedState);
                            post.setLikes(currentLikes);
                            isLiked.set(currentLikedState);
                            updateLikeUI(holder, currentLikedState, currentLikes);
                            if (holder.btnLike != null) holder.btnLike.setEnabled(true);
                            com.google.android.material.snackbar.Snackbar.make(holder.itemView,
                                    context.getString(R.string.article_like_error, error != null ? error : ""),
                                    com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                        }
                    });
            });
        }
        
        if (holder.btnComment != null) {
            holder.btnComment.setOnClickListener(v -> {
                if (currentUserId == null || currentUserId.isEmpty()) {
                    return;
                }
                showCommentDialog(post, currentUserId);
            });
        }
        
        if (holder.btnShare != null) {
            holder.btnShare.setOnClickListener(v -> {
                sharePost(post, holder.itemView);
            });
        }
        
        // Views count
        if (holder.tvViewCount != null) {
            holder.tvViewCount.setText(formatCount(post.getViews()));
            
            if (!viewedPosts.contains(post.getPostId())) {
                viewedPosts.add(post.getPostId());
                articlePostHelper.incrementViews(post.getPostId(), null);
            }
        }
        
        if (holder.ivMoreOptions != null) {
            holder.ivMoreOptions.setOnClickListener(v -> {
                showBottomSheetOptions(post);
            });
        }

        // Save button logic
        if (currentUserId != null && !currentUserId.isEmpty()) {
            final boolean[] isSavedSelf = {false};
            articlePostHelper.isPostSavedByUser(post.getPostId(), currentUserId, new ArticlePostHelper.OnCompleteListener<Boolean>() {
                @Override
                public void onSuccess(Boolean saved) {
                    isSavedSelf[0] = saved;
                    updateSaveUI(holder, saved);
                }

                @Override
                public void onError(String error) {
                    if (error != null) com.haset.hasetapp.utils.ErrorLogger.log(error, error);
                    updateSaveUI(holder, false);
                }
            });

            if (holder.ivSave != null) {
                holder.ivSave.setOnClickListener(v -> {
                    holder.ivSave.setEnabled(false);
                    boolean newSavedState = !isSavedSelf[0];
                    articlePostHelper.toggleSave(post.getPostId(), currentUserId, new ArticlePostHelper.OnCompleteListener<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            isSavedSelf[0] = result;
                            updateSaveUI(holder, result);
                            if (holder.ivSave != null) holder.ivSave.setEnabled(true);
                            String msg = result ? "Article saved" : "Article removed from saved";
                            Snackbar.make(holder.itemView, msg, Snackbar.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String error) {
                            if (error != null) com.haset.hasetapp.utils.ErrorLogger.log(error, error);
                            if (holder.ivSave != null) holder.ivSave.setEnabled(true);
                        }
                    });
                });
            }
        }

        // Article click listener - open detail screen
        holder.itemView.setOnClickListener(v -> {
            if (articleClickListener != null) {
                articleClickListener.onArticleClick(post);
            }
        });
    }

    /**
     * Load image from Cloudinary URL (or any cloud storage URL) using Glide
     * Images will maintain aspect ratio and fit within the post width
     */
    private void loadImageFromUrl(ImageView imageView, String imageUrl) {
        try {
            // Get screen width to optimize image loading
            android.util.DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            int screenWidth = displayMetrics.widthPixels;
            
            RequestOptions requestOptions = new RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_news)
                    .error(R.drawable.ic_news)
                    .centerCrop() // Fill the width and height
                    .override(screenWidth); // Load image at screen width for optimal quality
            
            Glide.with(context)
                    .load(imageUrl)
                    .apply(requestOptions)
                    .into(imageView);
        } catch (Exception e) {
            android.util.Log.e("PostFeedAdapter", "Error loading image from URL", e);
            imageView.setImageResource(R.drawable.ic_news);
        }
    }
    
    /**
     * Load image from local file path (backward compatibility)
     */
    private void loadImage(ImageView imageView, String imagePath) {
        try {
            String cleanPath = imagePath;
            if (cleanPath.startsWith("file://")) {
                cleanPath = cleanPath.substring(7);
            }
            
            File imageFile = new File(cleanPath);
            if (imageFile.exists()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2; // Reduce memory usage
                android.graphics.Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                } else {
                    imageView.setImageResource(R.drawable.ic_news);
                }
            } else {
                imageView.setImageResource(R.drawable.ic_news);
            }
        } catch (Exception e) {
            android.util.Log.e("PostFeedAdapter", "Error loading image", e);
            imageView.setImageResource(R.drawable.ic_news);
        }
    }

    private void updateActionCount(TextView textView, int count, String type) {
        if (count > 0) {
            textView.setText(count + " " + type);
        } else {
            textView.setText("0 " + type);
        }
    }


    private String getTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        if (diff < TimeUnit.MINUTES.toMillis(1)) {
            return context.getString(R.string.just_now);
        } else if (diff < TimeUnit.HOURS.toMillis(1)) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            if (minutes == 1) return context.getString(R.string.minute_ago);
            return context.getString(R.string.minutes_ago, (int) minutes);
        } else if (diff < TimeUnit.DAYS.toMillis(1)) {
            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            if (hours == 1) return context.getString(R.string.hour_ago);
            return context.getString(R.string.hours_ago, (int) hours);
        } else if (diff < TimeUnit.DAYS.toMillis(7)) {
            long days = TimeUnit.MILLISECONDS.toDays(diff);
            if (days == 1) return context.getString(R.string.day_ago);
            return context.getString(R.string.days_ago, (int) days);
        } else {
            // Show date
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }
    
    /**
     * Update like button UI based on liked state
     */
    private void updateLikeUI(PostViewHolder holder, boolean isLiked, int likeCount) {
        if (holder.ivLike != null) {
            if (isLiked) {
                holder.ivLike.setImageResource(R.drawable.ic_like_red);
                androidx.core.widget.ImageViewCompat.setImageTintList(holder.ivLike, 
                        android.content.res.ColorStateList.valueOf(context.getResources().getColor(R.color.red_primary, null)));
            } else {
                holder.ivLike.setImageResource(R.drawable.ic_like);
                androidx.core.widget.ImageViewCompat.setImageTintList(holder.ivLike, 
                        android.content.res.ColorStateList.valueOf(context.getResources().getColor(R.color.text_primary, null)));
            }
        }
        
        if (holder.tvLikeCount != null) {
            updateActionCount(holder.tvLikeCount, likeCount, "likes");
        }
    }

    /**
     * Update save button UI based on saved state
     */
    private void updateSaveUI(PostViewHolder holder, boolean isSaved) {
        if (holder.ivSave != null) {
            if (isSaved) {
                holder.ivSave.setImageResource(R.drawable.ic_bookmark_filled);
                holder.ivSave.setColorFilter(context.getResources().getColor(R.color.green_primary, null));
            } else {
                holder.ivSave.setImageResource(R.drawable.ic_bookmark_outline);
                holder.ivSave.setColorFilter(context.getResources().getColor(R.color.text_secondary, null));
            }
        }
    }

    /**
     * Format counts (e.g. 1200 -> 1.2k)
     */
    private String formatCount(int count) {
        if (count < 1000) return String.valueOf(count);
        if (count < 1000000) return String.format(Locale.getDefault(), "%.1fk", count / 1000.0);
        return String.format(Locale.getDefault(), "%.1fM", count / 1000000.0);
    }

    /**
     * Show comment bottom sheet
     */
    private void showCommentDialog(ArticlePostEntity post, String userId) {
        // Create bottom sheet dialog
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = 
                new com.google.android.material.bottomsheet.BottomSheetDialog(context);
        
        android.view.View bottomSheetView = android.view.LayoutInflater.from(context)
                .inflate(R.layout.bottom_sheet_article_comments, null);
        
        bottomSheetDialog.setContentView(bottomSheetView);
        
        // Configure bottom sheet to adjust for keyboard
        android.view.View bottomSheetInternal = bottomSheetDialog.findViewById(
                com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheetInternal != null) {
            com.google.android.material.bottomsheet.BottomSheetBehavior<android.view.View> behavior = 
                    com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheetInternal);
            behavior.setPeekHeight(android.view.ViewGroup.LayoutParams.MATCH_PARENT);
            behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        }
        
        // Set window flags to adjust for keyboard
        if (bottomSheetDialog.getWindow() != null) {
            bottomSheetDialog.getWindow().setSoftInputMode(
                    android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        
        // Find views
        androidx.recyclerview.widget.RecyclerView rvComments = bottomSheetView.findViewById(R.id.rvComments);
        android.widget.EditText etCommentInput = bottomSheetView.findViewById(R.id.etCommentInput);
        android.widget.ImageView ivSendComment = bottomSheetView.findViewById(R.id.ivSendComment);
        
        // Initialize comments list (empty for now, can be loaded from Firebase later)
        java.util.List<Comment> commentList = new java.util.ArrayList<>();
        CommentsAdapter commentsAdapter = new CommentsAdapter(commentList);
        rvComments.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(context));
        rvComments.setAdapter(commentsAdapter);
        
        // Get the root view for scrolling
        android.view.View rootView = bottomSheetView.getRootView();
        androidx.core.widget.NestedScrollView scrollView = null;
        
        // Find NestedScrollView in the view hierarchy
        if (rootView instanceof androidx.core.widget.NestedScrollView) {
            scrollView = (androidx.core.widget.NestedScrollView) rootView;
        } else {
            // Try to find it in the view hierarchy
            android.view.View found = bottomSheetView.findViewById(android.R.id.content);
            if (found != null && found.getParent() instanceof androidx.core.widget.NestedScrollView) {
                scrollView = (androidx.core.widget.NestedScrollView) found.getParent();
            }
        }
        
        final androidx.core.widget.NestedScrollView finalScrollView = scrollView;
        
        // Listen for keyboard visibility changes
        bottomSheetView.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (finalScrollView == null) return;
                
                android.graphics.Rect r = new android.graphics.Rect();
                bottomSheetView.getWindowVisibleDisplayFrame(r);
                int screenHeight = bottomSheetView.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;
                
                if (keypadHeight > screenHeight * 0.15) { // Keyboard is visible
                    // Scroll to input field
                    etCommentInput.post(() -> {
                        int[] location = new int[2];
                        etCommentInput.getLocationInWindow(location);
                        finalScrollView.smoothScrollTo(0, location[1] - 100); // Scroll with some padding
                    });
                }
            }
        });
        
        // Focus on input when bottom sheet is shown
        etCommentInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && finalScrollView != null) {
                finalScrollView.postDelayed(() -> {
                    int[] location = new int[2];
                    etCommentInput.getLocationInWindow(location);
                    finalScrollView.smoothScrollTo(0, location[1] - 100);
                }, 200);
            }
        });
        
        // Send comment button click
        ivSendComment.setOnClickListener(v -> {
            String commentText = etCommentInput.getText().toString().trim();
            if (!commentText.isEmpty()) {
                // Disable send button during operation
                ivSendComment.setEnabled(false);
                
                // Save comment to Firebase and increment count
                saveCommentToFirebase(post.getPostId(), userId, commentText, new ArticlePostHelper.OnCompleteListener<String>() {
                    @Override
                    public void onSuccess(String commentId) {
                        // Increment comment count in Firebase
                        ArticlePostHelper.getInstance().incrementComments(post.getPostId(), new ArticlePostHelper.OnCompleteListener<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                // Update local post data
                                post.setComments(post.getComments() + 1);
                                
                                // Get user name for the new comment
                                String userName = preferenceManager.getUserName();
                                if (userName == null || userName.isEmpty()) {
                                    userName = "User";
                                }
                                
                                // Add comment to list
                                Comment newComment = new Comment(commentId, userId, userName, commentText, System.currentTimeMillis());
                                commentList.add(newComment);
                                commentsAdapter.notifyItemInserted(commentList.size() - 1);
                                
                                // Clear input and scroll to bottom
                                etCommentInput.setText("");
                                rvComments.scrollToPosition(commentList.size() - 1);
                                
                                // Update post count in adapter
                                notifyItemChanged(posts.indexOf(post));
                                
                                // Log audit event
                                AuditLogger.getInstance(context).logPostCommented(post.getPostId(), post.getType());
                                
                                ivSendComment.setEnabled(true);
                                
                                // Show Snackbar for successful comment
                                android.view.View rootView = bottomSheetView.findViewById(android.R.id.content);
                                if (rootView == null) {
                                    rootView = bottomSheetView;
                                }
                                Snackbar.make(rootView, context.getString(R.string.comment_posted), Snackbar.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String error) {
                                if (error != null) com.haset.hasetapp.utils.ErrorLogger.log(error, error);
                                ivSendComment.setEnabled(true);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        if (error != null) com.haset.hasetapp.utils.ErrorLogger.log(error, error);
                        ivSendComment.setEnabled(true);
                    }
                });
            }
        });
        
        // Load existing comments from Firebase
        loadCommentsFromFirebase(post.getPostId(), commentList, commentsAdapter);
        
        bottomSheetDialog.show();
    }
    
    /**
     * Save comment to Firebase
     */
    private void saveCommentToFirebase(String postId, String userId, String commentText, 
                                     ArticlePostHelper.OnCompleteListener<String> listener) {
        // Get current user's name from preferences or Firebase
        String userName = preferenceManager.getUserName();
        if (userName == null || userName.isEmpty()) {
            // Try to get from Firebase
            firebaseHelper.getUsersRef().child(userId).child("fullName")
                    .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                            String fullName = snapshot.getValue(String.class);
                            saveCommentWithUserName(postId, userId, fullName != null ? fullName : "User", commentText, listener);
                        }
                        
                        @Override
                        public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                            saveCommentWithUserName(postId, userId, "User", commentText, listener);
                        }
                    });
        } else {
            saveCommentWithUserName(postId, userId, userName, commentText, listener);
        }
    }
    
    /**
     * Save comment with user name to Firebase
     */
    private void saveCommentWithUserName(String postId, String userId, String userName, String commentText,
                                        ArticlePostHelper.OnCompleteListener<String> listener) {
        com.google.firebase.database.DatabaseReference commentsRef = firebaseHelper.getDatabaseReference()
                .child("post_comments").child(postId);
        
        String commentId = commentsRef.push().getKey();
        if (commentId == null) {
            listener.onError("Failed to generate comment ID");
            return;
        }
        
        java.util.Map<String, Object> commentData = new java.util.HashMap<>();
        commentData.put("commentId", commentId);
        commentData.put("userId", userId);
        commentData.put("userName", userName);
        commentData.put("commentText", commentText);
        commentData.put("timestamp", System.currentTimeMillis());
        
        commentsRef.child(commentId).setValue(commentData)
                .addOnSuccessListener(aVoid -> listener.onSuccess(commentId))
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
    
    /**
     * Load comments from Firebase
     */
    private void loadCommentsFromFirebase(String postId, java.util.List<Comment> commentList, CommentsAdapter adapter) {
        com.google.firebase.database.DatabaseReference commentsRef = firebaseHelper.getDatabaseReference()
                .child("post_comments").child(postId);
        
        commentsRef.orderByChild("timestamp").addListenerForSingleValueEvent(
                new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                        commentList.clear();
                        int totalComments = (int) snapshot.getChildrenCount();
                        final int[] loadedCount = {0};
                        
                        if (totalComments == 0) {
                            adapter.notifyDataSetChanged();
                            return;
                        }
                        
                        for (com.google.firebase.database.DataSnapshot commentSnapshot : snapshot.getChildren()) {
                            String commentId = commentSnapshot.getKey();
                            String userId = commentSnapshot.child("userId").getValue(String.class);
                            String userName = commentSnapshot.child("userName").getValue(String.class);
                            String commentText = commentSnapshot.child("commentText").getValue(String.class);
                            Long timestamp = commentSnapshot.child("timestamp").getValue(Long.class);
                            
                            if (commentId != null && commentText != null) {
                                // If userName is not stored, fetch it from users table
                                if (userName == null || userName.isEmpty()) {
                                    fetchUserNameForComment(userId, commentId, commentText, 
                                            timestamp != null ? timestamp : System.currentTimeMillis(),
                                            commentList, adapter, totalComments, loadedCount);
                                } else {
                                    Comment comment = new Comment(commentId, userId != null ? userId : "Unknown", 
                                            userName, commentText, timestamp != null ? timestamp : System.currentTimeMillis());
                                    commentList.add(comment);
                                    loadedCount[0]++;
                                    
                                    if (loadedCount[0] == totalComments) {
                                        adapter.notifyDataSetChanged();
                                    }
                                }
                            } else {
                                loadedCount[0]++;
                                if (loadedCount[0] == totalComments) {
                                    adapter.notifyDataSetChanged();
                                }
                            }
                        }
                    }
                    
                    @Override
                    public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                        android.util.Log.e("PostFeedAdapter", "Error loading comments: " + error.getMessage());
                    }
                });
    }
    
    /**
     * Fetch user name from Firebase users table for a comment
     */
    private void fetchUserNameForComment(String userId, String commentId, String commentText, long timestamp,
                                        java.util.List<Comment> commentList, CommentsAdapter adapter,
                                        int totalComments, int[] loadedCount) {
        if (userId == null || userId.isEmpty()) {
            Comment comment = new Comment(commentId, "Unknown", "Unknown User", commentText, timestamp);
            commentList.add(comment);
            loadedCount[0]++;
            if (loadedCount[0] == totalComments) {
                adapter.notifyDataSetChanged();
            }
            return;
        }
        
        firebaseHelper.getUsersRef().child(userId).child("fullName")
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                        String fullName = snapshot.getValue(String.class);
                        String userName = (fullName != null && !fullName.isEmpty()) ? fullName : "User";
                        
                        Comment comment = new Comment(commentId, userId, userName, commentText, timestamp);
                        commentList.add(comment);
                        loadedCount[0]++;
                        
                        if (loadedCount[0] == totalComments) {
                            adapter.notifyDataSetChanged();
                        }
                    }
                    
                    @Override
                    public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                        Comment comment = new Comment(commentId, userId, "User", commentText, timestamp);
                        commentList.add(comment);
                        loadedCount[0]++;
                        
                        if (loadedCount[0] == totalComments) {
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
    }
    
    /**
     * Simple Comment model class
     */
    private static class Comment {
        private String commentId;
        private String userId;
        private String userName;
        private String content;
        private long timestamp;
        
        public Comment(String commentId, String userId, String userName, String content, long timestamp) {
            this.commentId = commentId;
            this.userId = userId;
            this.userName = userName;
            this.content = content;
            this.timestamp = timestamp;
        }
        
        public String getCommentId() { return commentId; }
        public String getUserId() { return userId; }
        public String getUserName() { return userName; }
        public String getContent() { return content; }
        public long getTimestamp() { return timestamp; }
        
        public void setUserName(String userName) {
            this.userName = userName;
        }
    }
    
    /**
     * Comments adapter for RecyclerView
     */
    private static class CommentsAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {
        private java.util.List<Comment> comments;
        
        public CommentsAdapter(java.util.List<Comment> comments) {
            this.comments = comments;
        }
        
        @NonNull
        @Override
        public CommentViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_comment, parent, false);
            return new CommentViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
            Comment comment = comments.get(position);
            holder.tvCommentContent.setText(comment.getContent());
            
            // Display user name, fallback to userId if name not available
            String displayName = comment.getUserName();
            if (displayName == null || displayName.isEmpty()) {
                displayName = comment.getUserId() != null ? comment.getUserId() : "Unknown User";
            }
            holder.tvCommentAuthor.setText(displayName);
            
            holder.tvCommentTimestamp.setText(formatTimestamp(holder.itemView.getContext(), comment.getTimestamp()));
            
            // Load profile photo using ProfilePhotoHelper
            String userId = comment.getUserId();
            if (userId != null && !userId.isEmpty() && !userId.equals("Unknown")) {
                com.haset.hasetapp.utils.ProfilePhotoHelper.loadProfilePhoto(
                        holder.itemView.getContext(), userId, holder.ivCommentProfile);
            } else {
                holder.ivCommentProfile.setImageResource(R.drawable.profile_photo);
            }
        }
        
        @Override
        public int getItemCount() {
            return comments.size();
        }
        
        private String formatTimestamp(Context context, long timestamp) {
            long now = System.currentTimeMillis();
            long diff = now - timestamp;
            
            if (diff < java.util.concurrent.TimeUnit.MINUTES.toMillis(1)) {
                return context.getString(R.string.just_now);
            } else if (diff < java.util.concurrent.TimeUnit.HOURS.toMillis(1)) {
                long minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(diff);
                if (minutes == 1) return context.getString(R.string.minute_ago);
                return context.getString(R.string.minutes_ago, (int) minutes);
            } else if (diff < java.util.concurrent.TimeUnit.DAYS.toMillis(1)) {
                long hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(diff);
                if (hours == 1) return context.getString(R.string.hour_ago);
                return context.getString(R.string.hours_ago, (int) hours);
            } else {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
                return sdf.format(new java.util.Date(timestamp));
            }
        }
        
        static class CommentViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            de.hdodenhof.circleimageview.CircleImageView ivCommentProfile;
            android.widget.TextView tvCommentAuthor, tvCommentContent, tvCommentTimestamp;
            
            public CommentViewHolder(@NonNull android.view.View itemView) {
                super(itemView);
                ivCommentProfile = itemView.findViewById(R.id.ivCommentProfile);
                tvCommentAuthor = itemView.findViewById(R.id.tvCommentAuthor);
                tvCommentContent = itemView.findViewById(R.id.tvCommentContent);
                tvCommentTimestamp = itemView.findViewById(R.id.tvCommentTimestamp);
            }
        }
    }
    
    /**
     * Share post using Android share intent
     */
    private void sharePost(ArticlePostEntity post, View view) {
        try {
            android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            
            // Build share text
            StringBuilder shareText = new StringBuilder();
            if (post.getTitle() != null && !post.getTitle().isEmpty()) {
                shareText.append(post.getTitle()).append("\n\n");
            }
            if (post.getDescription() != null && !post.getDescription().isEmpty()) {
                shareText.append(post.getDescription()).append("\n\n");
            }
            
            // Add media URL if available
            if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
                shareText.append("Image: ").append(post.getImageUrl()).append("\n");
            }
            
            shareText.append("\n").append(context.getString(R.string.shared_from));
            
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareText.toString());
            shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, post.getTitle() != null ? post.getTitle() : context.getString(R.string.haset_post));
            
            // Increment share count
            String currentUserId = firebaseHelper.getCurrentUserId();
            if (currentUserId == null || currentUserId.isEmpty()) {
                currentUserId = preferenceManager.getUserId();
            }
            
            if (currentUserId != null && !currentUserId.isEmpty()) {
                articlePostHelper.incrementShares(post.getPostId(), 
                    new ArticlePostHelper.OnCompleteListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            // Update local post data
                            post.setShares(post.getShares() + 1);
                            // Notify adapter to update UI
                            notifyItemChanged(posts.indexOf(post));
                            
                            // Log audit event
                            AuditLogger.getInstance(context).logPostShared(post.getPostId(), post.getType());
                        }
                        
                        @Override
                        public void onError(String error) {
                            if (error != null) com.haset.hasetapp.utils.ErrorLogger.log(error, error);
                        }
                    });
            }
            
            context.startActivity(android.content.Intent.createChooser(shareIntent, context.getString(R.string.share_post_via)));
            
            // Show Snackbar after share intent is launched
            if (view != null) {
                Snackbar.make(view, context.getString(R.string.sharing_post), Snackbar.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            android.util.Log.e("PostFeedAdapter", "Error sharing post: " + e.getMessage(), e);
        }
    }

    private void showBottomSheetOptions(ArticlePostEntity post) {
        android.view.View bottomSheetView = android.view.LayoutInflater.from(context)
                .inflate(R.layout.bottom_sheet_article_options, null);
        
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = 
                new com.google.android.material.bottomsheet.BottomSheetDialog(context);
        bottomSheetDialog.setContentView(bottomSheetView);
        
        // Find views
        android.view.View optionSave = bottomSheetView.findViewById(R.id.optionSave);
        android.view.View optionAboutAccount = bottomSheetView.findViewById(R.id.optionAboutAccount);
        android.view.View optionAddToFavorite = bottomSheetView.findViewById(R.id.optionAddToFavorite);
        
        // Save option
        optionSave.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Toast.makeText(context, context.getString(R.string.feature_coming_soon, context.getString(R.string.save_feature)), Toast.LENGTH_SHORT).show();
        });
        
        // About the account option
        optionAboutAccount.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Toast.makeText(context, context.getString(R.string.feature_coming_soon, context.getString(R.string.account_info_feature)), Toast.LENGTH_SHORT).show();
        });
        
        // Add to favorite option
        optionAddToFavorite.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Toast.makeText(context, context.getString(R.string.feature_coming_soon, context.getString(R.string.favorite_feature)), Toast.LENGTH_SHORT).show();
        });
        
        bottomSheetDialog.show();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvProfileName, tvTimeAgo, tvPostTitle, tvPostDescription;
        TextView tvTags, tvMusic, tvLikeCount, tvCommentCount, tvShareCount;
        ImageView ivPostImage, ivLike, ivComment, ivShare, ivSave, ivMoreOptions;
        de.hdodenhof.circleimageview.CircleImageView ivProfile;
        com.facebook.shimmer.ShimmerFrameLayout shimmerProfile;
        FrameLayout containerMedia, containerVideo;
        View containerTagsMusic;
        View btnLike, btnComment, btnShare;
        TextView tvViewCount;
        // Incremented each time this ViewHolder is bound. Stale async callbacks
        // compare against this to detect if the holder was recycled/rebound.
        final java.util.concurrent.atomic.AtomicInteger bindGeneration = new java.util.concurrent.atomic.AtomicInteger(0);

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            try {
                tvProfileName = itemView.findViewById(R.id.tvProfileName);
                tvTimeAgo = itemView.findViewById(R.id.tvTimeAgo);
                tvPostTitle = itemView.findViewById(R.id.tvPostTitle);
                tvPostDescription = itemView.findViewById(R.id.tvPostDescription);
                tvTags = itemView.findViewById(R.id.tvTags);
                tvMusic = itemView.findViewById(R.id.tvMusic);
                tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
                tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
                tvShareCount = itemView.findViewById(R.id.tvShareCount);
                
                ivProfile = itemView.findViewById(R.id.ivProfile);
                shimmerProfile = itemView.findViewById(R.id.shimmerProfile);
                ivPostImage = itemView.findViewById(R.id.ivPostImage);
                
                btnLike = itemView.findViewById(R.id.btnLike);
                ivLike = itemView.findViewById(R.id.ivLike);
                if (ivLike == null && btnLike instanceof ImageView) {
                    ivLike = (ImageView) btnLike;
                }
                
                btnComment = itemView.findViewById(R.id.btnComment);
                ivComment = itemView.findViewById(R.id.ivComment);
                if (ivComment == null && btnComment instanceof ImageView) {
                    ivComment = (ImageView) btnComment;
                }
                
                btnShare = itemView.findViewById(R.id.btnShare);
                ivShare = itemView.findViewById(R.id.ivShare);
                if (ivShare == null && btnShare instanceof ImageView) {
                    ivShare = (ImageView) btnShare;
                }
                
                ivSave = itemView.findViewById(R.id.ivSave);
                ivMoreOptions = itemView.findViewById(R.id.ivMoreOptions);
                
                containerMedia = itemView.findViewById(R.id.containerMedia);
                containerVideo = itemView.findViewById(R.id.containerVideo);
                containerTagsMusic = itemView.findViewById(R.id.containerTagsMusic);
                
                tvViewCount = itemView.findViewById(R.id.tvViewCount);
            } catch (Exception e) {
                // Shimmer item
            }
        }
    }
}
