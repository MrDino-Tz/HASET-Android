package com.haset.hasetapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.haset.hasetapp.R;
import com.haset.hasetapp.database.entities.ArticlePostEntity;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ArticleAdapter extends RecyclerView.Adapter<ArticleAdapter.ArticleViewHolder> {

    private List<ArticlePostEntity> articles;
    private OnPostActionListener listener;
    private boolean isLoading = false;

    public interface OnPostActionListener {
        void onCommentClick(ArticlePostEntity article);
        void onShareClick(ArticlePostEntity article);
        void onLikeClick(ArticlePostEntity article);
        void onProfileClick(ArticlePostEntity article);
    }

    public ArticleAdapter(List<ArticlePostEntity> articles, OnPostActionListener listener) {
        this.articles = articles != null ? articles : new ArrayList<>();
        this.listener = listener;
    }

    public void setPosts(List<ArticlePostEntity> articles) {
        this.articles = articles != null ? articles : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public void setLoading(boolean loading) {
        this.isLoading = loading;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ArticleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 1) {
            com.facebook.shimmer.ShimmerFrameLayout shimmerLayout = 
                com.haset.hasetapp.utils.ShimmerHelper.createShimmerLayout(
                    parent.getContext(), 
                    R.layout.shimmer_layout_post_item
                );
            shimmerLayout.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            return new ArticleViewHolder(shimmerLayout);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_article, parent, false);
            return new ArticleViewHolder(view);
        }
    }
    
    @Override
    public int getItemViewType(int position) {
        return isLoading ? 1 : 0;
    }

    @Override
    public void onBindViewHolder(@NonNull ArticleViewHolder holder, int position) {
        if (isLoading) {
            if (holder.itemView instanceof com.facebook.shimmer.ShimmerFrameLayout) {
                ((com.facebook.shimmer.ShimmerFrameLayout) holder.itemView).startShimmer();
            }
            return;
        }
        
        if (position >= articles.size()) return;
        
        ArticlePostEntity article = articles.get(position);
        if (article == null) return;
        
        holder.bind(article, listener);
    }

    @Override
    public int getItemCount() {
        if (isLoading) return 3;
        return articles != null ? articles.size() : 0;
    }

    static class ArticleViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivProfileImage;
        TextView tvProfileName, tvTimestamp, tvPostTitle, tvPostText, tvLikeCount, tvCommentCount, tvShareCount;
        ImageView ivOptions, ivPostImage, ivLike, ivComment, ivShare;
        FrameLayout flVideoContainer;

        public ArticleViewHolder(@NonNull View itemView) {
            super(itemView);
            try {
                ivProfileImage = itemView.findViewById(R.id.ivProfileImage);
                tvProfileName = itemView.findViewById(R.id.tvProfileName);
                tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
                ivOptions = itemView.findViewById(R.id.ivOptions);
                ivPostImage = itemView.findViewById(R.id.ivPostImage);
                flVideoContainer = itemView.findViewById(R.id.flVideoContainer);
                tvPostTitle = itemView.findViewById(R.id.tvPostTitle);
                tvPostText = itemView.findViewById(R.id.tvPostText);
                ivLike = itemView.findViewById(R.id.ivLike);
                tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
                ivComment = itemView.findViewById(R.id.ivComment);
                tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
                ivShare = itemView.findViewById(R.id.ivShare);
                tvShareCount = itemView.findViewById(R.id.tvShareCount);
            } catch (Exception e) {
                // Shimmer item
            }
        }

        public void bind(ArticlePostEntity article, OnPostActionListener listener) {
            ivProfileImage.setImageResource(R.drawable.profile_photo);
            tvProfileName.setText(article.getProfileName() != null ? article.getProfileName() : "HASET User");
            tvTimestamp.setText("Just now"); 
            
            if (article.getTitle() != null && !article.getTitle().isEmpty()) {
                tvPostTitle.setText(article.getTitle());
                tvPostTitle.setVisibility(View.VISIBLE);
            } else {
                tvPostTitle.setVisibility(View.GONE);
            }
            
            tvPostText.setText(article.getDescription() != null ? article.getDescription() : "No content");
            tvLikeCount.setText(String.valueOf(article.getLikes()) + " Likes");
            tvCommentCount.setText(String.valueOf(article.getComments()) + " Comments");
            tvShareCount.setText(String.valueOf(article.getShares()) + " Shares");

            String imageUrl = article.getImageUrl();
            String imagePath = article.getImagePath();
            
            if ((imageUrl != null && !imageUrl.isEmpty()) || (imagePath != null && !imagePath.isEmpty())) {
                ivPostImage.setVisibility(View.VISIBLE);
                if (flVideoContainer != null) flVideoContainer.setVisibility(View.GONE);
                
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    RequestOptions requestOptions = new RequestOptions()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.banner_purple_small_bg)
                            .error(R.drawable.banner_purple_small_bg);
                    Glide.with(itemView.getContext())
                            .load(imageUrl)
                            .apply(requestOptions)
                            .centerCrop()
                            .into(ivPostImage);
                } else {
                    ivPostImage.setImageResource(R.drawable.banner_purple_small_bg);
                }
            } else {
                ivPostImage.setVisibility(View.GONE);
                if (flVideoContainer != null) flVideoContainer.setVisibility(View.GONE);
            }

            itemView.findViewById(R.id.llLike).setOnClickListener(v -> listener.onLikeClick(article));
            itemView.findViewById(R.id.llComment).setOnClickListener(v -> listener.onCommentClick(article));
            itemView.findViewById(R.id.llShare).setOnClickListener(v -> listener.onShareClick(article));
            ivProfileImage.setOnClickListener(v -> listener.onProfileClick(article));
            tvProfileName.setOnClickListener(v -> listener.onProfileClick(article));
        }
    }
}
