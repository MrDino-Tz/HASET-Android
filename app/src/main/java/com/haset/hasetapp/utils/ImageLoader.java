package com.haset.hasetapp.utils;

import android.content.Context;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import com.haset.hasetapp.R;

/**
 * Standardized image loading utility for consistent Glide usage across the app.
 * Provides optimized image loading with proper caching and memory management.
 */
public class ImageLoader {
    private static final String TAG = "ImageLoader";
    private static final int DEFAULT_PLACEHOLDER = R.drawable.placeholder_image;
    private static final int DEFAULT_ERROR = R.drawable.ic_error_outline;
    
    // Standard RequestOptions for different use cases
    private static final RequestOptions DEFAULT_OPTIONS = new RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .skipMemoryCache(false)
            .format(DecodeFormat.PREFER_RGB_565) // Less memory than ARGB_8888
            .placeholder(DEFAULT_PLACEHOLDER)
            .error(DEFAULT_ERROR);
    
    private static final RequestOptions CIRCLE_OPTIONS = DEFAULT_OPTIONS
            .transform(new CircleCrop());
    
    private static final RequestOptions ROUNDED_OPTIONS = DEFAULT_OPTIONS
            .transform(new CenterCrop(), new RoundedCorners(12));
    
    private static final RequestOptions BANNER_OPTIONS = new RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .skipMemoryCache(false)
            .format(DecodeFormat.PREFER_RGB_565)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.placeholder_image)
            .centerCrop();
    
    private static final RequestOptions PROFILE_OPTIONS = new RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .skipMemoryCache(false)
            .format(DecodeFormat.PREFER_RGB_565)
            .placeholder(R.drawable.profile_photo)
            .error(R.drawable.profile_photo)
            .transform(new CircleCrop());
    
    private static final RequestOptions ARTICLE_OPTIONS = new RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .skipMemoryCache(false)
            .format(DecodeFormat.PREFER_RGB_565)
            .placeholder(R.drawable.banner_purple_small_bg)
            .error(R.drawable.banner_purple_small_bg)
            .centerCrop();
    
    /**
     * Loads an image with default settings
     */
    public static void loadImage(@NonNull Context context, @Nullable String url, 
                                @NonNull ImageView target) {
        if (isValidUrl(url)) {
            Glide.with(context)
                    .load(url)
                    .apply(DEFAULT_OPTIONS)
                    .into(target);
        } else {
            target.setImageResource(DEFAULT_PLACEHOLDER);
        }
    }
    
    /**
     * Loads a circular profile image
     */
    public static void loadProfileImage(@NonNull Context context, @Nullable String url, 
                                      @NonNull ImageView target) {
        if (isValidUrl(url)) {
            Glide.with(context)
                    .load(url)
                    .apply(PROFILE_OPTIONS)
                    .into(target);
        } else {
            target.setImageResource(R.drawable.profile_photo);
        }
    }
    
    /**
     * Loads a banner image with banner-specific settings
     */
    public static void loadBannerImage(@NonNull Context context, @Nullable String url, 
                                     @NonNull ImageView target) {
        if (isValidUrl(url)) {
            Glide.with(context)
                    .load(url)
                    .apply(BANNER_OPTIONS)
                    .into(target);
        } else {
            target.setImageResource(R.drawable.placeholder_image);
        }
    }
    
    /**
     * Loads an article image with article-specific settings
     */
    public static void loadArticleImage(@NonNull Context context, @Nullable String url, 
                                      @NonNull ImageView target) {
        if (isValidUrl(url)) {
            Glide.with(context)
                    .load(url)
                    .apply(ARTICLE_OPTIONS)
                    .into(target);
        } else {
            target.setImageResource(R.drawable.banner_purple_small_bg);
        }
    }
    
    /**
     * Loads a circular image
     */
    public static void loadCircularImage(@NonNull Context context, @Nullable String url, 
                                       @NonNull ImageView target) {
        if (isValidUrl(url)) {
            Glide.with(context)
                    .load(url)
                    .apply(CIRCLE_OPTIONS)
                    .into(target);
        } else {
            target.setImageResource(DEFAULT_PLACEHOLDER);
        }
    }
    
    /**
     * Loads a rounded corner image
     */
    public static void loadRoundedImage(@NonNull Context context, @Nullable String url, 
                                       @NonNull ImageView target) {
        if (isValidUrl(url)) {
            Glide.with(context)
                    .load(url)
                    .apply(ROUNDED_OPTIONS)
                    .into(target);
        } else {
            target.setImageResource(DEFAULT_PLACEHOLDER);
        }
    }
    
    /**
     * Loads an image with custom listener for monitoring
     */
    public static void loadImageWithListener(@NonNull Context context, @Nullable String url, 
                                           @NonNull ImageView target,
                                           @Nullable RequestListener listener) {
        if (isValidUrl(url)) {
            RequestBuilder<?> builder = Glide.with(context).load(url).apply(DEFAULT_OPTIONS);
            if (listener != null) {
                builder = builder.listener(listener);
            }
            builder.into(target);
        } else {
            target.setImageResource(DEFAULT_PLACEHOLDER);
        }
    }
    
    /**
     * Clears memory cache for the specific image
     */
    public static void clearImage(@NonNull Context context, @Nullable String url) {
        if (isValidUrl(url)) {
            // Create a temporary target to clear the specific image
            Glide.with(context).clear(Glide.with(context).load(url).submit());
        }
    }
    
    /**
     * Clears all memory cache
     */
    public static void clearMemoryCache(@NonNull Context context) {
        // Clear Glide's memory cache
        Glide.get(context).clearMemory();
        Log.d(TAG, "Glide memory cache cleared");
    }
    
    /**
     * Validates if URL is safe to load
     */
    private static boolean isValidUrl(@Nullable String url) {
        return url != null && !url.trim().isEmpty() && 
               !url.equalsIgnoreCase("null") && !url.equalsIgnoreCase("default");
    }
    
    /**
     * Gets current memory usage from Glide
     */
    public static void logGlideMemoryUsage(@NonNull Context context) {
        try {
            // Note: This requires Glide 4.12+ for detailed memory info
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            
            Log.d(TAG, String.format("App Memory: %d MB, Glide active", usedMemory / 1024 / 1024));
        } catch (Exception e) {
            Log.w(TAG, "Could not get Glide memory info", e);
        }
    }
    
    /**
     * Preloads critical images for better performance
     */
    public static void preloadCriticalImages(@NonNull Context context, @NonNull String... urls) {
        for (String url : urls) {
            if (isValidUrl(url)) {
                Glide.with(context)
                        .load(url)
                        .preload(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
            }
        }
        Log.d(TAG, "Preloaded " + urls.length + " critical images");
    }
}
