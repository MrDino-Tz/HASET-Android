package com.haset.hasetapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.Locale;

import com.google.android.material.imageview.ShapeableImageView;

public class PopularArticleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_WITH_IMAGE = 0;
    private static final int TYPE_TEXT_ONLY = 1;

    private List<ArticlePostEntity> articles;
    private Context context;
    private OnArticleClickListener listener;

    public interface OnArticleClickListener {
        void onArticleClick(ArticlePostEntity article);
    }

    public PopularArticleAdapter(List<ArticlePostEntity> articles, Context context, OnArticleClickListener listener) {
        this.articles = articles != null ? articles : new ArrayList<>();
        this.context = context;
        this.listener = listener;
    }

    public void setArticles(List<ArticlePostEntity> articles) {
        this.articles = articles != null ? articles : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<ArticlePostEntity> getArticles() {
        return articles;
    }

    @Override
    public int getItemViewType(int position) {
        ArticlePostEntity article = articles.get(position);
        String imageUrl = article.getImageUrl();
        String imagePath = article.getImagePath();
        
        if ((imageUrl != null && !imageUrl.isEmpty()) || (imagePath != null && !imagePath.isEmpty())) {
            return TYPE_WITH_IMAGE;
        }
        return TYPE_TEXT_ONLY;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_WITH_IMAGE) {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.item_popular_article_list, parent, false);
            return new ArticleWithImageViewHolder(view);
        } else {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.item_popular_article_text_only, parent, false);
            return new ArticleTextOnlyViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ArticlePostEntity article = articles.get(position);
        
        if (holder instanceof ArticleWithImageViewHolder) {
            ((ArticleWithImageViewHolder) holder).bind(article);
        } else if (holder instanceof ArticleTextOnlyViewHolder) {
            ((ArticleTextOnlyViewHolder) holder).bind(article);
        }
    }

    @Override
    public int getItemCount() {
        return articles.size();
    }

    class ArticleWithImageViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivArticleImage;
        TextView tvTitle, tvViewCount;

        public ArticleWithImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivArticleImage = itemView.findViewById(R.id.ivArticleImage);
            tvTitle = itemView.findViewById(R.id.tvArticleTitle);
            tvViewCount = itemView.findViewById(R.id.tvViewCount);
        }

        public void bind(ArticlePostEntity article) {
            tvTitle.setText(article.getLocalizedTitle(com.haset.hasetapp.utils.LocaleHelper.getLanguage(itemView.getContext())) != null
                    ? article.getLocalizedTitle(com.haset.hasetapp.utils.LocaleHelper.getLanguage(itemView.getContext())) : "");
            tvViewCount.setText(formatCount(article.getViews()) + " views");

            String imageUrl = article.getImageUrl();
            String imagePath = article.getImagePath();
            
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(context)
                        .load(imageUrl)
                        .apply(new RequestOptions()
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .placeholder(R.drawable.ic_news)
                                .error(R.drawable.ic_news))
                        .into(ivArticleImage);
            } else if (imagePath != null && !imagePath.isEmpty()) {
                Glide.with(context)
                        .load(imagePath)
                        .apply(new RequestOptions()
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .placeholder(R.drawable.ic_news)
                                .error(R.drawable.ic_news))
                        .into(ivArticleImage);
            } else {
                ivArticleImage.setImageResource(R.drawable.ic_news);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onArticleClick(article);
                }
            });
        }
    }

    class ArticleTextOnlyViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvViewCount;

        public ArticleTextOnlyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvArticleTitle);
            tvDescription = itemView.findViewById(R.id.tvArticleDescription);
            tvViewCount = itemView.findViewById(R.id.tvViewCount);
        }

        public void bind(ArticlePostEntity article) {
            tvTitle.setText(article.getLocalizedTitle(com.haset.hasetapp.utils.LocaleHelper.getLanguage(itemView.getContext())) != null
                    ? article.getLocalizedTitle(com.haset.hasetapp.utils.LocaleHelper.getLanguage(itemView.getContext())) : "");
            tvViewCount.setText(formatCount(article.getViews()) + " views");

            String description = article.getDescription();
            if (description != null && !description.isEmpty()) {
                tvDescription.setText(description);
                tvDescription.setVisibility(View.VISIBLE);
            } else {
                tvDescription.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onArticleClick(article);
                }
            });
        }
    }

    private String formatCount(int count) {
        if (count < 1000) return String.valueOf(count);
        if (count < 1000000) return String.format(Locale.getDefault(), "%.1fk", count / 1000.0);
        return String.format(Locale.getDefault(), "%.1fM", count / 1000000.0);
    }
}
