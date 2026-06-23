package com.haset.hasetapp.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.haset.hasetapp.R;
import com.haset.hasetapp.database.entities.ArticlePostEntity;
import com.haset.hasetapp.firebase.ArticlePostHelper;
import com.haset.hasetapp.firebase.FirebaseHelper;
import com.haset.hasetapp.utils.AuditLogger;
import com.haset.hasetapp.utils.PreferenceManager;
import com.haset.hasetapp.utils.ProfilePhotoHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ArticleDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ARTICLE = "extra_article";

    private ArticlePostEntity article;
    private ArticlePostHelper articlePostHelper;
    private FirebaseHelper firebaseHelper;
    private PreferenceManager preferenceManager;
    private boolean isLiked = false;
    private Gson gson = new Gson();

    private CircleImageView ivAuthorProfile;
    private TextView tvAuthorName, tvPublishDate, tvArticleTitle, tvTags, tvArticleContent;
    private TextView tvViewsCount, tvLikesCount, tvCommentsCount;
    private ImageView ivArticleImage, ivLikeIcon;
    private View containerImage, layoutLikes, layoutComments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);

        articlePostHelper = ArticlePostHelper.getInstance();
        firebaseHelper = FirebaseHelper.getInstance();
        preferenceManager = new PreferenceManager(this);

        initViews();
        setupStatusBar();
        loadArticle();
        setupClickListeners();
    }
    
    private void setupStatusBar() {
        Window window = getWindow();
        View decorView = window.getDecorView();
        
        if ((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
            window.setStatusBarColor(getColor(R.color.background_primary));
            decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        } else {
            window.setStatusBarColor(getColor(R.color.green_primary));
            decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshLikeStatus();
    }

    private void refreshLikeStatus() {
        if (article == null) return;
        String userId = getCurrentUserId();
        if (userId == null) {
            isLiked = false;
            updateLikeIcon();
            return;
        }

        articlePostHelper.isPostLikedByUser(article.getPostId(), userId,
                new ArticlePostHelper.OnCompleteListener<Boolean>() {
                    @Override
                    public void onSuccess(Boolean liked) {
                        isLiked = liked;
                        runOnUiThread(() -> updateLikeIcon());
                    }

                    @Override
                    public void onError(String error) {
                        isLiked = false;
                        runOnUiThread(() -> updateLikeIcon());
                    }
                });
    }

    private void initViews() {
        ivAuthorProfile = findViewById(R.id.ivAuthorProfile);
        tvAuthorName = findViewById(R.id.tvAuthorName);
        tvPublishDate = findViewById(R.id.tvPublishDate);
        tvArticleTitle = findViewById(R.id.tvArticleTitle);
        tvTags = findViewById(R.id.tvTags);
        tvArticleContent = findViewById(R.id.tvArticleContent);
        tvViewsCount = findViewById(R.id.tvViewsCount);
        tvLikesCount = findViewById(R.id.tvLikesCount);
        tvCommentsCount = findViewById(R.id.tvCommentsCount);
        ivArticleImage = findViewById(R.id.ivArticleImage);
        ivLikeIcon = findViewById(R.id.ivLikeIcon);
        containerImage = findViewById(R.id.containerImage);
        layoutLikes = findViewById(R.id.layoutLikes);
        layoutComments = findViewById(R.id.layoutComments);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnShare).setOnClickListener(v -> shareArticle());
    }

    private void loadArticle() {
        String articleJson = getIntent().getStringExtra(EXTRA_ARTICLE);
        if (articleJson != null) {
            article = gson.fromJson(articleJson, ArticlePostEntity.class);
        }

        if (article == null) {
            Toast.makeText(this, R.string.error_loading_article, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        displayArticle();
        checkLikeStatus();
        incrementViews();
    }

    private void displayArticle() {
        tvArticleTitle.setText(article.getTitle() != null ? article.getTitle() : "");
        tvArticleContent.setText(article.getDescription() != null ? article.getDescription() : "");

        tvAuthorName.setText(article.getProfileName() != null ? article.getProfileName() : getString(R.string.app_name));

        if (article.getCreatedAt() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            tvPublishDate.setText(sdf.format(new Date(article.getCreatedAt())));
        }

        if (article.getTags() != null && !article.getTags().isEmpty()) {
            tvTags.setText(article.getTags());
            tvTags.setVisibility(View.VISIBLE);
        } else {
            tvTags.setVisibility(View.GONE);
        }

        if ("image".equals(article.getType())) {
            if (article.getImageUrl() != null && !article.getImageUrl().isEmpty()) {
                containerImage.setVisibility(View.VISIBLE);
                Glide.with(this)
                        .load(article.getImageUrl())
                        .placeholder(R.drawable.ic_news)
                        .error(R.drawable.ic_news)
                        .centerCrop()
                        .into(ivArticleImage);
            } else {
                containerImage.setVisibility(View.GONE);
            }
        } else {
            containerImage.setVisibility(View.GONE);
        }

        updateStats();
    }

    private void updateStats() {
        tvViewsCount.setText(formatCount(article.getViews()) + " " + getString(R.string.views));
        tvLikesCount.setText(formatCount(article.getLikes()) + " " + getString(R.string.likes));
        tvCommentsCount.setText(formatCount(article.getComments()) + " " + getString(R.string.comments));
    }

    private String formatCount(int count) {
        if (count < 1000) return String.valueOf(count);
        if (count < 1000000) return String.format(Locale.getDefault(), "%.1fk", count / 1000.0);
        return String.format(Locale.getDefault(), "%.1fM", count / 1000000.0);
    }

    private void checkLikeStatus() {
        String userId = getCurrentUserId();
        if (userId == null) return;

        articlePostHelper.isPostLikedByUser(article.getPostId(), userId,
                new ArticlePostHelper.OnCompleteListener<Boolean>() {
                    @Override
                    public void onSuccess(Boolean liked) {
                        isLiked = liked;
                        runOnUiThread(() -> updateLikeIcon());
                    }

                    @Override
                    public void onError(String error) {
                        isLiked = false;
                        runOnUiThread(() -> updateLikeIcon());
                    }
                });
    }

    private void updateLikeIcon() {
        if (isLiked) {
            ivLikeIcon.setImageResource(R.drawable.ic_like_red);
            ivLikeIcon.setColorFilter(getResources().getColor(R.color.red_primary, null));
            tvLikesCount.setText(formatCount(article.getLikes()) + " " + getString(R.string.liked));
        } else {
            ivLikeIcon.setImageResource(R.drawable.ic_like);
            ivLikeIcon.setColorFilter(getResources().getColor(R.color.text_secondary, null));
            tvLikesCount.setText(formatCount(article.getLikes()) + " " + getString(R.string.likes));
        }
    }

    private void incrementViews() {
        String userId = getCurrentUserId();
        articlePostHelper.incrementViews(article.getPostId(), userId != null ? new ArticlePostHelper.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                article.setViews(article.getViews() + 1);
                runOnUiThread(() -> updateStats());
            }

            @Override
            public void onError(String error) {
            }
        } : null);
    }

    private void setupClickListeners() {
        layoutLikes.setOnClickListener(v -> toggleLike());
        layoutComments.setOnClickListener(v -> showCommentsBottomSheet());
    }

    private void toggleLike() {
        String userId = getCurrentUserId();
        if (userId == null) {
            Snackbar.make(layoutLikes, R.string.login_required, Snackbar.LENGTH_SHORT).show();
            return;
        }

        final boolean newLikedState = !isLiked;
        final int likesDelta = newLikedState ? 1 : -1;

        isLiked = newLikedState;
        article.setLikes(article.getLikes() + likesDelta);
        updateLikeIcon();
        updateStats();

        articlePostHelper.toggleLike(article.getPostId(), userId,
                new ArticlePostHelper.OnCompleteListener<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        AuditLogger.getInstance(ArticleDetailActivity.this)
                                .logPostLiked(article.getPostId(), result, article.getType());
                    }

                    @Override
                    public void onError(String error) {
                        isLiked = !newLikedState;
                        article.setLikes(article.getLikes() - likesDelta);
                        runOnUiThread(() -> {
                            updateLikeIcon();
                            updateStats();
                        });
                    }
                });
    }

    private void showCommentsBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.CustomBottomSheetDialogTheme);
        View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_article_comments, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        View bottomSheetInternal = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheetInternal != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheetInternal);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        }

        RecyclerView rvComments = bottomSheetView.findViewById(R.id.rvComments);
        EditText etCommentInput = bottomSheetView.findViewById(R.id.etCommentInput);
        ImageView ivSendComment = bottomSheetView.findViewById(R.id.ivSendComment);

        List<Map<String, Object>> commentsList = new ArrayList<>();
        CommentsAdapter commentsAdapter = new CommentsAdapter(commentsList);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(commentsAdapter);

        loadComments(commentsList, commentsAdapter);

        ivSendComment.setOnClickListener(v -> {
            String commentText = etCommentInput.getText().toString().trim();
            if (!commentText.isEmpty()) {
                String userId = getCurrentUserId();
                if (userId != null) {
                    saveComment(userId, commentText, commentsList, commentsAdapter, etCommentInput);
                } else {
                    Snackbar.make(bottomSheetView, R.string.login_required, Snackbar.LENGTH_SHORT).show();
                }
            }
        });

        bottomSheetDialog.show();
    }

    private void loadComments(List<Map<String, Object>> commentsList, CommentsAdapter adapter) {
        firebaseHelper.getDatabaseReference()
                .child("post_comments")
                .child(article.getPostId())
                .orderByChild("timestamp")
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                        commentsList.clear();
                        for (com.google.firebase.database.DataSnapshot commentSnapshot : snapshot.getChildren()) {
                            Map<String, Object> comment = new HashMap<>();
                            comment.put("commentId", commentSnapshot.getKey());
                            comment.put("userId", commentSnapshot.child("userId").getValue(String.class));
                            comment.put("userName", commentSnapshot.child("userName").getValue(String.class));
                            comment.put("commentText", commentSnapshot.child("commentText").getValue(String.class));
                            comment.put("timestamp", commentSnapshot.child("timestamp").getValue(Long.class));
                            commentsList.add(comment);
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                    }
                });
    }

    private void saveComment(String userId, String commentText, List<Map<String, Object>> commentsList, 
                             CommentsAdapter adapter, EditText etCommentInput) {
        String commentId = firebaseHelper.getDatabaseReference()
                .child("post_comments")
                .child(article.getPostId())
                .push().getKey();

        if (commentId == null) return;

        String userName = preferenceManager.getUserName();
        if (userName == null || userName.isEmpty()) {
            userName = "User";
        }

        Map<String, Object> commentData = new HashMap<>();
        commentData.put("commentId", commentId);
        commentData.put("userId", userId);
        commentData.put("userName", userName);
        commentData.put("commentText", commentText);
        commentData.put("timestamp", System.currentTimeMillis());

        firebaseHelper.getDatabaseReference()
                .child("post_comments")
                .child(article.getPostId())
                .child(commentId)
                .setValue(commentData)
                .addOnSuccessListener(aVoid -> {
                    article.setComments(article.getComments() + 1);
                    articlePostHelper.incrementComments(article.getPostId(), null);
                    commentsList.add(commentData);
                    adapter.notifyItemInserted(commentsList.size() - 1);
                    updateStats();
                    etCommentInput.setText("");
                    AuditLogger.getInstance(this).logPostCommented(article.getPostId(), article.getType());
                });
    }

    private void shareArticle() {
        StringBuilder shareText = new StringBuilder();
        if (article.getTitle() != null && !article.getTitle().isEmpty()) {
            shareText.append(article.getTitle()).append("\n\n");
        }
        if (article.getDescription() != null && !article.getDescription().isEmpty()) {
            shareText.append(article.getDescription()).append("\n\n");
        }
        shareText.append("\n\n").append(getString(R.string.shared_from));

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, article.getTitle());
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());

        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_article)));

        articlePostHelper.incrementShares(article.getPostId(), new ArticlePostHelper.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                article.setShares(article.getShares() + 1);
                AuditLogger.getInstance(ArticleDetailActivity.this)
                        .logPostShared(article.getPostId(), article.getType());
            }

            @Override
            public void onError(String error) {
            }
        });
    }

    private String getCurrentUserId() {
        String userId = firebaseHelper.getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            userId = preferenceManager.getUserId();
        }
        return (userId != null && !userId.isEmpty()) ? userId : null;
    }

    private class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {
        private List<Map<String, Object>> comments;

        CommentsAdapter(List<Map<String, Object>> comments) {
            this.comments = comments;
        }

        @NonNull
        @Override
        public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_comment, parent, false);
            return new CommentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
            Map<String, Object> comment = comments.get(position);
            holder.tvCommentContent.setText((String) comment.get("commentText"));
            String userName = (String) comment.get("userName");
            holder.tvCommentAuthor.setText(userName != null ? userName : "User");
            
            Long timestamp = (Long) comment.get("timestamp");
            if (timestamp != null) {
                holder.tvCommentTimestamp.setText(getTimeAgo(timestamp));
            }

            String userId = (String) comment.get("userId");
            if (userId != null && !userId.isEmpty()) {
                ProfilePhotoHelper.loadProfilePhoto(ArticleDetailActivity.this, userId, holder.ivCommentProfile);
            } else {
                holder.ivCommentProfile.setImageResource(R.drawable.profile_photo);
            }
        }

        @Override
        public int getItemCount() {
            return comments.size();
        }

        private String getTimeAgo(long timestamp) {
            long now = System.currentTimeMillis();
            long diff = now - timestamp;
            
            if (diff < 60000) {
                return getString(R.string.just_now);
            } else if (diff < 3600000) {
                long minutes = diff / 60000;
                return getString(R.string.minutes_ago, (int) minutes);
            } else if (diff < 86400000) {
                long hours = diff / 3600000;
                return getString(R.string.hours_ago, (int) hours);
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                return sdf.format(new Date(timestamp));
            }
        }

        class CommentViewHolder extends RecyclerView.ViewHolder {
            de.hdodenhof.circleimageview.CircleImageView ivCommentProfile;
            TextView tvCommentAuthor, tvCommentContent, tvCommentTimestamp;

            CommentViewHolder(@NonNull View itemView) {
                super(itemView);
                ivCommentProfile = itemView.findViewById(R.id.ivCommentProfile);
                tvCommentAuthor = itemView.findViewById(R.id.tvCommentAuthor);
                tvCommentContent = itemView.findViewById(R.id.tvCommentContent);
                tvCommentTimestamp = itemView.findViewById(R.id.tvCommentTimestamp);
            }
        }
    }
}
