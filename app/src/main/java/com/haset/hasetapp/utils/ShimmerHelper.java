package com.haset.hasetapp.utils;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.haset.hasetapp.R;

/**
 * ShimmerHelper - Reusable shimmer loading effects
 * Provides skeleton loading animations for better UX
 */
public class ShimmerHelper {

    /**
     * Create shimmer layout for appointment cards
     */
    public static ShimmerFrameLayout createAppointmentShimmer(Context context) {
        return createShimmerLayout(context, R.layout.shimmer_layout_appointment_card);
    }

    /**
     * Create shimmer layout for profile cards
     */
    public static ShimmerFrameLayout createProfileShimmer(Context context) {
        return createShimmerLayout(context, R.layout.shimmer_layout_profile_card);
    }

    /**
     * Create shimmer layout for doctor lists
     */
    public static ShimmerFrameLayout createDoctorListShimmer(Context context) {
        return createShimmerLayout(context, R.layout.shimmer_layout_doctor_list);
    }

    /**
     * Create shimmer layout with custom layout resource
     */
    public static ShimmerFrameLayout createShimmerLayout(Context context, int layoutResId) {
        ShimmerFrameLayout shimmerFrameLayout = new ShimmerFrameLayout(context);
        
        // Inflate the shimmer content
        View shimmerContent = android.view.LayoutInflater.from(context)
                .inflate(layoutResId, null);
        
        // Add shimmer content to shimmer layout
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        shimmerFrameLayout.addView(shimmerContent, params);
        
        // Configure shimmer animation using correct API
        com.facebook.shimmer.Shimmer shimmer = new com.facebook.shimmer.Shimmer.ColorHighlightBuilder()
                .setBaseColor(context.getResources().getColor(R.color.shimmer_placeholder))
                .setHighlightColor(context.getResources().getColor(R.color.shimmer_color))
                .setDirection(com.facebook.shimmer.Shimmer.Direction.LEFT_TO_RIGHT)
                .setAutoStart(true)
                .build();
        shimmerFrameLayout.setShimmer(shimmer);
        
        return shimmerFrameLayout;
    }

    /**
     * Show shimmer loading
     */
    public static void showShimmer(ShimmerFrameLayout shimmerLayout, View targetView) {
        if (shimmerLayout != null && targetView != null) {
            // Hide actual content
            targetView.setVisibility(View.GONE);
            
            // Show shimmer in its place
            if (shimmerLayout.getParent() != null) {
                ((android.view.ViewGroup) shimmerLayout.getParent()).removeView(shimmerLayout);
            }
            
            // Add shimmer to the same parent as target view
            if (targetView.getParent() instanceof android.view.ViewGroup) {
                android.view.ViewGroup parent = (android.view.ViewGroup) targetView.getParent();
                int index = parent.indexOfChild(targetView);
                
                android.view.ViewGroup.LayoutParams params = targetView.getLayoutParams();
                parent.addView(shimmerLayout, index, params);
            }
            
            shimmerLayout.setVisibility(View.VISIBLE);
            shimmerLayout.startShimmer();
        }
    }

    /**
     * Hide shimmer loading and show actual content
     */
    public static void hideShimmer(ShimmerFrameLayout shimmerLayout, View targetView) {
        if (shimmerLayout != null && targetView != null) {
            // Stop shimmer animation
            shimmerLayout.stopShimmer();
            
            // Remove shimmer from parent
            if (shimmerLayout.getParent() instanceof android.view.ViewGroup) {
                ((android.view.ViewGroup) shimmerLayout.getParent()).removeView(shimmerLayout);
            }
            
            // Show actual content
            targetView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Simple shimmer container for list items
     */
    public static void showListShimmer(Context context, android.view.ViewGroup container, 
                                    int itemCount, int itemLayoutResId) {
        // Clear existing views
        container.removeAllViews();
        
        // Add shimmer items
        for (int i = 0; i < itemCount; i++) {
            ShimmerFrameLayout shimmerItem = createShimmerLayout(context, itemLayoutResId);
            container.addView(shimmerItem);
            shimmerItem.startShimmer();
        }
    }

    /**
     * Hide list shimmer and populate with actual data
     */
    public static void hideListShimmer(android.view.ViewGroup container) {
        // Stop all shimmer animations
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof ShimmerFrameLayout) {
                ((ShimmerFrameLayout) child).stopShimmer();
            }
        }
        
        // Clear shimmer items
        container.removeAllViews();
    }

    /**
     * Create shimmer for any custom view
     */
    public static ShimmerFrameLayout wrapWithShimmer(Context context, View view) {
        ShimmerFrameLayout shimmerLayout = new ShimmerFrameLayout(context);
        
        // Configure shimmer using correct API
        com.facebook.shimmer.Shimmer shimmer = new com.facebook.shimmer.Shimmer.ColorHighlightBuilder()
                .setBaseColor(context.getResources().getColor(R.color.shimmer_placeholder))
                .setHighlightColor(context.getResources().getColor(R.color.shimmer_color))
                .setDirection(com.facebook.shimmer.Shimmer.Direction.LEFT_TO_RIGHT)
                .setAutoStart(true)
                .build();
        shimmerLayout.setShimmer(shimmer);
        
        // Add the view to shimmer layout
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        shimmerLayout.addView(view, params);
        
        return shimmerLayout;
    }
    
    /**
     * Create circular shimmer for profile images
     */
    public static ShimmerFrameLayout createProfileImageShimmer(Context context) {
        return createShimmerLayout(context, R.layout.shimmer_layout_profile_image);
    }
}
