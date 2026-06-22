package com.haset.hasetapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.haset.hasetapp.R;
import com.haset.hasetapp.database.entities.ArticlePostEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ArticlePostAdapter extends RecyclerView.Adapter<ArticlePostAdapter.PostViewHolder> {
    private List<ArticlePostEntity> posts;
    private OnPostActionListener listener;

    public interface OnPostActionListener {
        void onEditClick(ArticlePostEntity post);
        void onDeleteClick(ArticlePostEntity post);
    }

    public ArticlePostAdapter(List<ArticlePostEntity> posts, OnPostActionListener listener) {
        this.posts = posts != null ? posts : new ArrayList<>();
        this.listener = listener;
    }

    public void updatePosts(List<ArticlePostEntity> newPosts) {
        this.posts = newPosts != null ? newPosts : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_article_post_admin_list, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        ArticlePostEntity post = posts.get(position);
        holder.bind(post, listener);
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTitle, tvType, tvStatus, tvDate, tvProfileName;
        private ImageView ivEdit, ivDelete, ivTypeIcon, ivPostThumbnail;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvType = itemView.findViewById(R.id.tvType);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvProfileName = itemView.findViewById(R.id.tvProfileName);
            ivEdit = itemView.findViewById(R.id.ivEdit);
            ivDelete = itemView.findViewById(R.id.ivDelete);
            ivTypeIcon = itemView.findViewById(R.id.ivTypeIcon);
            ivPostThumbnail = itemView.findViewById(R.id.ivPostThumbnail);
        }

        public void bind(ArticlePostEntity post, OnPostActionListener listener) {
            tvTitle.setText(post.getTitle());
            tvProfileName.setText(post.getProfileName());
            
            tvType.setText(itemView.getContext().getString(R.string.status_article));
            
            String status = post.getStatus() != null ? post.getStatus() : "draft";
            if ("published".equalsIgnoreCase(status)) {
                tvStatus.setText(itemView.getContext().getString(R.string.tab_posted));
                tvStatus.setBackgroundResource(R.drawable.bg_status_published);
            } else {
                tvStatus.setText(itemView.getContext().getString(R.string.tab_drafted));
                tvStatus.setBackgroundResource(R.drawable.bg_status_draft);
            }
            
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            tvDate.setText(sdf.format(new Date(post.getCreatedAt())));
            
            ivTypeIcon.setImageResource(R.drawable.ic_news);
            
            // Show image thumbnail if available
            String imageUrl = post.getImageUrl();
            String imagePath = post.getImagePath();
            
            if ((imageUrl != null && !imageUrl.isEmpty()) || (imagePath != null && !imagePath.isEmpty())) {
                ivPostThumbnail.setVisibility(View.VISIBLE);
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    com.bumptech.glide.Glide.with(itemView.getContext())
                            .load(imageUrl)
                            .placeholder(R.drawable.banner_purple_small_bg)
                            .error(R.drawable.banner_purple_small_bg)
                            .centerCrop()
                            .into(ivPostThumbnail);
                } else {
                    ivPostThumbnail.setImageResource(R.drawable.banner_purple_small_bg);
                }
            } else {
                ivPostThumbnail.setVisibility(View.GONE);
            }
            
            ivEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditClick(post);
                }
            });
            
            ivDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(post);
                }
            });
        }
    }
}

