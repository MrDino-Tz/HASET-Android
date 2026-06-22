package com.haset.hasetapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.haset.hasetapp.R;

import java.util.ArrayList;
import java.util.List;

public class PharmacyBannerAdapter extends RecyclerView.Adapter<PharmacyBannerAdapter.ViewHolder> {

    private List<Banner> banners;
    private OnBannerClickListener listener;
    private int currentPosition = 0;

    public static class Banner {
        public String title;
        public String discount;
        public String buttonText;
        public String imageUrl;
        public int imageRes;

        public Banner(String title, String discount, String buttonText, int imageRes) {
            this.title = title;
            this.discount = discount;
            this.buttonText = buttonText;
            this.imageRes = imageRes;
        }

        public Banner(String title, String discount, String buttonText, String imageUrl) {
            this.title = title;
            this.discount = discount;
            this.buttonText = buttonText;
            this.imageUrl = imageUrl;
        }
    }

    public interface OnBannerClickListener {
        void onBannerClick(Banner banner);
        void onBuyNowClick(Banner banner);
    }

    public PharmacyBannerAdapter(List<Banner> banners, OnBannerClickListener listener) {
        this.banners = banners != null ? banners : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pharmacy_banner, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Banner banner = banners.get(position);
        
        holder.tvBannerTitle.setText(banner.title);
        holder.tvDiscountBadge.setText(banner.discount);
        holder.btnBuyNow.setText(banner.buttonText);
        
        // Load image
        if (banner.imageUrl != null && !banner.imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(banner.imageUrl)
                    .placeholder(banner.imageRes != 0 ? banner.imageRes : R.drawable.placeholder_image)
                    .error(banner.imageRes != 0 ? banner.imageRes : R.drawable.placeholder_image)
                    .into(holder.ivBannerImage);
        } else if (banner.imageRes != 0) {
            holder.ivBannerImage.setImageResource(banner.imageRes);
        } else {
            holder.ivBannerImage.setImageResource(R.drawable.placeholder_image);
        }
        
        // Setup pagination indicators
        setupPaginationIndicators(holder.layoutPagination, currentPosition, banners.size());
        
        // Click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBannerClick(banner);
            }
        });
        
        holder.btnBuyNow.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBuyNowClick(banner);
            }
        });
    }

    private void setupPaginationIndicators(LinearLayout layout, int currentPos, int total) {
        layout.removeAllViews();
        
        for (int i = 0; i < total; i++) {
            View indicator = new View(layout.getContext());
            int size = 8; // dp
            int margin = 4; // dp
            float density = layout.getContext().getResources().getDisplayMetrics().density;
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int) (size * density),
                    (int) (size * density)
            );
            params.setMargins((int) (margin * density), 0, (int) (margin * density), 0);
            indicator.setLayoutParams(params);
            
            if (i == currentPos) {
                indicator.setBackgroundResource(R.drawable.bg_pagination_indicator_active);
            } else {
                indicator.setBackgroundResource(R.drawable.bg_pagination_indicator_inactive);
            }
            
            layout.addView(indicator);
        }
    }

    @Override
    public int getItemCount() {
        return banners.size();
    }

    public void setCurrentPosition(int position) {
        if (this.currentPosition != position) {
            int oldPosition = this.currentPosition;
            this.currentPosition = position;
            // Only notify the changed items to update indicators
            notifyItemChanged(oldPosition);
            notifyItemChanged(position);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBannerTitle;
        TextView tvDiscountBadge;
        MaterialButton btnBuyNow;
        ImageView ivBannerImage;
        LinearLayout layoutPagination;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBannerTitle = itemView.findViewById(R.id.tvBannerTitle);
            tvDiscountBadge = itemView.findViewById(R.id.tvDiscountBadge);
            btnBuyNow = itemView.findViewById(R.id.btnBuyNow);
            ivBannerImage = itemView.findViewById(R.id.ivBannerImage);
            layoutPagination = itemView.findViewById(R.id.layoutPagination);
        }
    }
}

