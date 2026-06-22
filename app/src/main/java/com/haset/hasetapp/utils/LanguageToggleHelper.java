package com.haset.hasetapp.utils;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.card.MaterialCardView;
import com.haset.hasetapp.R;

public class LanguageToggleHelper {

    public interface OnLanguageChangeListener {
        void onLanguageChanged(String languageCode);
    }

    public static void setup(Activity activity, View root, OnLanguageChangeListener listener) {
        MaterialCardView cvEn = root.findViewById(R.id.cvEn);
        MaterialCardView cvSw = root.findViewById(R.id.cvSw);
        ImageView ivFlagEn = root.findViewById(R.id.ivFlagEn);
        ImageView ivFlagSw = root.findViewById(R.id.ivFlagSw);
        TextView tvEn = root.findViewById(R.id.tvEn);
        TextView tvSw = root.findViewById(R.id.tvSw);
        ShimmerFrameLayout shimmerEn = root.findViewById(R.id.shimmerEn);
        ShimmerFrameLayout shimmerSw = root.findViewById(R.id.shimmerSw);

        String currentLang = LocaleHelper.getLanguage(activity);

        // Update UI for current selection
        updateSelectedState(activity, currentLang, cvEn, cvSw, tvEn, tvSw);

        // Load flags
        loadFlag(activity, "https://res.cloudinary.com/dpbgmirz9/image/upload/v1768311357/usa_4628635_lkbaa3.png", ivFlagEn, shimmerEn);
        loadFlag(activity, "https://res.cloudinary.com/dpbgmirz9/image/upload/v1768311296/tanzania_9994139_kpxsyh.png", ivFlagSw, shimmerSw);

        cvEn.setOnClickListener(v -> {
            if (!currentLang.equals("en")) {
                listener.onLanguageChanged("en");
            }
        });

        cvSw.setOnClickListener(v -> {
            if (!currentLang.equals("sw")) {
                listener.onLanguageChanged("sw");
            }
        });
    }

    private static void updateSelectedState(Context context, String lang, MaterialCardView cvEn, MaterialCardView cvSw, TextView tvEn, TextView tvSw) {
        int selectedColor = context.getColor(R.color.green_primary);
        int unselectedColor = context.getColor(android.R.color.transparent);
        int selectedTextColor = context.getColor(R.color.white_primary);
        int unselectedTextColor = context.getColor(R.color.text_secondary);

        if ("sw".equals(lang)) {
            cvSw.setCardBackgroundColor(selectedColor);
            tvSw.setTextColor(selectedTextColor);
            cvEn.setCardBackgroundColor(unselectedColor);
            tvEn.setTextColor(unselectedTextColor);
        } else {
            cvEn.setCardBackgroundColor(selectedColor);
            tvEn.setTextColor(selectedTextColor);
            cvSw.setCardBackgroundColor(unselectedColor);
            tvSw.setTextColor(unselectedTextColor);
        }
    }

    private static void loadFlag(Context context, String url, ImageView imageView, ShimmerFrameLayout shimmer) {
        Glide.with(context)
            .load(url)
            .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                @Override
                public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                    shimmer.stopShimmer();
                    shimmer.setVisibility(View.GONE);
                    return false;
                }

                @Override
                public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                    shimmer.stopShimmer();
                    shimmer.setVisibility(View.GONE);
                    imageView.setImageTintList(null);
                    return false;
                }
            })
            .into(imageView);
    }
}
