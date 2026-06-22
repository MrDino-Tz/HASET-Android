package com.haset.hasetapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.haset.hasetapp.R;
import com.haset.hasetapp.utils.ImageLoader;

import java.util.ArrayList;
import java.util.List;

import java.io.Serializable;

public class PatientBannerAdapter extends RecyclerView.Adapter<PatientBannerAdapter.ViewHolder> {

    private List<BannerItem> banners;
    private OnBannerClickListener listener;

    public static class BannerItem implements Serializable {
        public String titleLine1;
        public String titleLine2;
        public String discount;
        public String buttonText;
        public int imageRes;
        public String imageUrl;
        public BannerType bannerType;
        public String targetAction;
        public String key;

        public enum BannerType {
            PHARMACY,
            MESSAGING,
            APPOINTMENT,
            DOCTORS,
            ARTICLE,
            IMAGE_BANNER
        }

        public BannerItem() {}

        public BannerItem(String titleLine1, String titleLine2, String discount, String buttonText, int imageRes, BannerType bannerType) {
            this.titleLine1 = titleLine1;
            this.titleLine2 = titleLine2;
            this.discount = discount;
            this.buttonText = buttonText;
            this.imageRes = imageRes;
            this.bannerType = bannerType;
        }

        public BannerItem(String titleLine1, String titleLine2, String discount, String buttonText, String imageUrl, BannerType bannerType) {
            this.titleLine1 = titleLine1;
            this.titleLine2 = titleLine2;
            this.discount = discount;
            this.buttonText = buttonText;
            this.imageUrl = imageUrl;
            this.bannerType = bannerType;
        }

        public static BannerItem createImageBanner(String imageUrl, BannerType actionType) {
            BannerItem item = new BannerItem();
            item.imageUrl = imageUrl;
            item.bannerType = BannerType.IMAGE_BANNER;
            item.targetAction = actionType != null ? actionType.name() : null;
            return item;
        }
    }

    public interface OnBannerClickListener {
        void onBannerClick(BannerItem banner);
    }

    public PatientBannerAdapter(List<BannerItem> banners, OnBannerClickListener listener) {
        this.banners = banners != null ? banners : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_patient_banner, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BannerItem banner = banners.get(position);
        
        boolean isImageBanner = banner.bannerType == BannerItem.BannerType.IMAGE_BANNER;
        
        if (isImageBanner) {
            // IMAGE_BANNER: Show full image with gradient overlay
            holder.layoutDetailedContent.setVisibility(View.GONE);
            holder.viewGradientOverlay.setVisibility(View.VISIBLE);
            
            if (banner.imageUrl != null && !banner.imageUrl.isEmpty()) {
                ImageLoader.loadBannerImage(holder.itemView.getContext(), banner.imageUrl, holder.ivBannerImage);
            } else {
                holder.ivBannerImage.setImageResource(banner.imageRes);
            }
        } else {
            // DETAILED BANNER: Show text overlay
            holder.layoutDetailedContent.setVisibility(View.VISIBLE);
            holder.viewGradientOverlay.setVisibility(View.GONE);
            
            holder.tvTitleLine1.setText(banner.titleLine1);
            holder.tvTitleLine2.setText(banner.titleLine2);
            holder.tvDiscountBadge.setText(banner.discount);
            holder.btnBuyNow.setText(banner.buttonText);
            
            if (banner.imageUrl != null && !banner.imageUrl.isEmpty()) {
                ImageLoader.loadBannerImage(holder.itemView.getContext(), banner.imageUrl, holder.ivBannerImage);
            } else {
                holder.ivBannerImage.setImageResource(banner.imageRes);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBannerClick(banner);
            }
        });
    }

    @Override
    public int getItemCount() {
        return banners.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBannerImage;
        LinearLayout layoutDetailedContent;
        TextView tvTitleLine1;
        TextView tvTitleLine2;
        TextView tvDiscountBadge;
        MaterialButton btnBuyNow;
        ImageView ivBannerImageSmall;
        View viewGradientOverlay;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBannerImage = itemView.findViewById(R.id.ivBannerImage);
            layoutDetailedContent = itemView.findViewById(R.id.layoutDetailedContent);
            tvTitleLine1 = itemView.findViewById(R.id.tvTitleLine1);
            tvTitleLine2 = itemView.findViewById(R.id.tvTitleLine2);
            tvDiscountBadge = itemView.findViewById(R.id.tvDiscountBadge);
            btnBuyNow = itemView.findViewById(R.id.btnBuyNow);
            ivBannerImageSmall = itemView.findViewById(R.id.ivBannerImageSmall);
            viewGradientOverlay = itemView.findViewById(R.id.viewGradientOverlay);
        }
    }
}
