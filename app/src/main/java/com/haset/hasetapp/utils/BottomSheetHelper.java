package com.haset.hasetapp.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.haset.hasetapp.R;

/**
 * BottomSheetHelper - Reusable bottom sheet dialogs
 * Provides consistent bottom sheet UI for various actions
 */
public class    BottomSheetHelper {

    /**
     * Show Contact Us bottom sheet
     */
    public static void showContactUsBottomSheet(Context context) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_contact_us, null);
        
        // Initialize views
        LinearLayout llEmail = view.findViewById(R.id.llEmail);
        LinearLayout llPhone = view.findViewById(R.id.llPhone);
        LinearLayout llWhatsApp = view.findViewById(R.id.llWhatsApp);
        LinearLayout llWebsite = view.findViewById(R.id.llWebsite);
//        com.google.android.material.button.MaterialButton btnClose = view.findViewById(R.id.btnClose);
        
        // Set click listeners
        llEmail.setOnClickListener(v -> {
            openEmail(context);
            bottomSheetDialog.dismiss();
        });
        
        llPhone.setOnClickListener(v -> {
            openPhone(context);
            bottomSheetDialog.dismiss();
        });
        
        llWhatsApp.setOnClickListener(v -> {
            openWhatsApp(context);
            bottomSheetDialog.dismiss();
        });
        
        llWebsite.setOnClickListener(v -> {
            openWebsite(context);
            bottomSheetDialog.dismiss();
        });
        
//        btnClose.setOnClickListener(v -> bottomSheetDialog.dismiss());
        
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }
    
    /**
     * Open email client
     */
    private static void openEmail(Context context) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:info@hasethospital.or.tz"));
        intent.putExtra(Intent.EXTRA_SUBJECT, "HASET Support");
        intent.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.email_support_body));
        
        try {
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.send_email_via)));
        } catch (android.content.ActivityNotFoundException e) {
            android.widget.Toast.makeText(context, "No email app found", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Open phone dialer
     */
    private static void openPhone(Context context) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:+255754501671"));
        
        try {
            context.startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            android.widget.Toast.makeText(context, "No phone dialer found", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Open WhatsApp
     */
    private static void openWhatsApp(Context context) {
        String msg = context.getString(R.string.whatsapp_support_msg);
        String whatsappUrl = "https://wa.me/255754501671?text=" + Uri.encode(msg);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(whatsappUrl));
        
        try {
            context.startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            // Fallback to web browser if WhatsApp is not installed
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl));
            context.startActivity(webIntent);
        }
    }
    
    /**
     * Open website
     */
    private static void openWebsite(Context context) {
        try {
            androidx.browser.customtabs.CustomTabsIntent.Builder builder = new androidx.browser.customtabs.CustomTabsIntent.Builder();
            builder.setShowTitle(true);
            androidx.browser.customtabs.CustomTabsIntent customTabsIntent = builder.build();
            customTabsIntent.launchUrl(context, Uri.parse("https://hasethospital.or.tz/"));
        } catch (Exception e) {
            // fallback if Chrome Custom Tabs is completely unavailable
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://hasethospital.or.tz/"));
            try {
                context.startActivity(intent);
            } catch (android.content.ActivityNotFoundException ex) {
                android.widget.Toast.makeText(context, "No web browser found", android.widget.Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * Generic bottom sheet with custom layout
     */
    public static BottomSheetDialog createCustomBottomSheet(Context context, int layoutResId) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(layoutResId, null);
        bottomSheetDialog.setContentView(view);
        return bottomSheetDialog;
    }
    
    /**
     * Show a simple list bottom sheet
     */
    public static void showListBottomSheet(Context context, String[] items, OnItemSelectedListener listener) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        
        // Create a simple list layout programmatically
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(48, 48, 48, 48);
        container.setBackgroundResource(R.drawable.dialog_background);
        
        // Add items
        for (int i = 0; i < items.length; i++) {
            com.google.android.material.button.MaterialButton button = new com.google.android.material.button.MaterialButton(context);
            button.setText(items[i]);
            button.setTextColor(context.getColor(R.color.text_primary));
            button.setBackgroundTintList(context.getResources().getColorStateList(android.R.color.transparent));
            button.setStrokeColor(context.getResources().getColorStateList(R.color.divider));
            button.setStrokeWidth(1);
            button.setCornerRadius(8);
            button.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
            button.setPadding(32, 24, 32, 24);
            
            final int index = i;
            button.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemSelected(index, items[index]);
                }
                bottomSheetDialog.dismiss();
            });
            
            // Add margin between items
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, i < items.length - 1 ? 24 : 0);
            container.addView(button, params);
        }
        
        bottomSheetDialog.setContentView(container);
        bottomSheetDialog.show();
    }
    
    /**
     * Show Language Selection bottom sheet
     */
    public static void showLanguageBottomSheet(Context context, String currentLanguage, OnLanguageSelectedListener listener) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_language, null);
        
        // Initialize views
        LinearLayout llEnglish = view.findViewById(R.id.llEnglish);
        LinearLayout llSwahili = view.findViewById(R.id.llSwahili);
        android.widget.ImageView ivEnglishCheck = view.findViewById(R.id.ivEnglishCheck);
        android.widget.ImageView ivSwahiliCheck = view.findViewById(R.id.ivSwahiliCheck);
        android.widget.ImageView ivEnglishFlag = view.findViewById(R.id.ivEnglishFlag);
        android.widget.ImageView ivSwahiliFlag = view.findViewById(R.id.ivSwahiliFlag);
        com.facebook.shimmer.ShimmerFrameLayout shimmerEnglish = view.findViewById(R.id.shimmerEnglishFlag);
        com.facebook.shimmer.ShimmerFrameLayout shimmerSwahili = view.findViewById(R.id.shimmerSwahiliFlag);
        
        // Start shimmer
        shimmerEnglish.startShimmer();
        shimmerSwahili.startShimmer();
        
        // Load flag icons from Cloudinary
        // English Flag (USA)
        com.bumptech.glide.Glide.with(context)
            .load("https://res.cloudinary.com/dpbgmirz9/image/upload/v1768311357/usa_4628635_lkbaa3.png")
            .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                @Override
                public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                    shimmerEnglish.stopShimmer();
                    shimmerEnglish.setVisibility(View.GONE);
                    return false;
                }

                @Override
                public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                    shimmerEnglish.stopShimmer();
                    shimmerEnglish.setVisibility(View.GONE);
                    ivEnglishFlag.setImageTintList(null); // Clear tint on success
                    return false;
                }
            })
            .into(ivEnglishFlag);
            
        // Swahili Flag (Tanzania)
        com.bumptech.glide.Glide.with(context)
            .load("https://res.cloudinary.com/dpbgmirz9/image/upload/v1768311296/tanzania_9994139_kpxsyh.png")
            .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                @Override
                public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                    shimmerSwahili.stopShimmer();
                    shimmerSwahili.setVisibility(View.GONE);
                    return false;
                }

                @Override
                public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                    shimmerSwahili.stopShimmer();
                    shimmerSwahili.setVisibility(View.GONE);
                    ivSwahiliFlag.setImageTintList(null); // Clear tint on success
                    return false;
                }
            })
            .into(ivSwahiliFlag);
        
        // Show check mark for current language ("en" or "sw")
        if ("en".equals(currentLanguage)) {
            ivEnglishCheck.setVisibility(android.view.View.VISIBLE);
            ivSwahiliCheck.setVisibility(android.view.View.GONE);
        } else if ("sw".equals(currentLanguage)) {
            ivEnglishCheck.setVisibility(android.view.View.GONE);
            ivSwahiliCheck.setVisibility(android.view.View.VISIBLE);
        } else {
            // Default to English if unknown
            ivEnglishCheck.setVisibility(android.view.View.VISIBLE);
            ivSwahiliCheck.setVisibility(android.view.View.GONE);
        }
        
        // Set click listeners
        llEnglish.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLanguageSelected("en");
            }
            bottomSheetDialog.dismiss();
        });
        
        llSwahili.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLanguageSelected("sw");
            }
            bottomSheetDialog.dismiss();
        });
        
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }
    
    /**
     * Interface for language selection callback
     */
    public interface OnLanguageSelectedListener {
        void onLanguageSelected(String language);
    }
    
    /**
     * Interface for item selection callback
     */
    public interface OnItemSelectedListener {
        void onItemSelected(int index, String item);
    }
}
