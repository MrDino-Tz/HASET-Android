package com.haset.hasetapp.utils;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.haset.hasetapp.R;

/**
 * SimpleShimmerHelper - Simplified shimmer loading
 * Easy to use shimmer effects for the HASET app
 */
public class SimpleShimmerHelper {

    /**
     * Create a simple shimmer container
     */
    public static ShimmerFrameLayout createSimpleShimmer(Context context) {
        ShimmerFrameLayout shimmerLayout = new ShimmerFrameLayout(context);
        
        // Configure shimmer with basic settings
        com.facebook.shimmer.Shimmer shimmer = new com.facebook.shimmer.Shimmer.ColorHighlightBuilder()
                .setBaseAlpha(0.7f)
                .setHighlightAlpha(0.9f)
                .setDirection(com.facebook.shimmer.Shimmer.Direction.LEFT_TO_RIGHT)
                .setAutoStart(true)
                .build();
        
        shimmerLayout.setShimmer(shimmer);
        
        // Add a placeholder view
        View placeholder = new View(context);
        placeholder.setBackgroundColor(context.getResources().getColor(R.color.shimmer_placeholder));
        
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                100 // Height in pixels
        );
        shimmerLayout.addView(placeholder, params);
        
        return shimmerLayout;
    }

    /**
     * Show shimmer instead of a view
     */
    public static void showShimmerForView(Context context, View targetView) {
        if (targetView == null || targetView.getParent() == null) return;
        
        // Create shimmer
        ShimmerFrameLayout shimmerLayout = createSimpleShimmer(context);
        
        // Get parent and position
        android.view.ViewGroup parent = (android.view.ViewGroup) targetView.getParent();
        int index = parent.indexOfChild(targetView);
        
        // Copy layout params
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                targetView.getWidth() > 0 ? targetView.getWidth() : FrameLayout.LayoutParams.MATCH_PARENT,
                targetView.getHeight() > 0 ? targetView.getHeight() : FrameLayout.LayoutParams.WRAP_CONTENT
        );
        
        // Hide target and show shimmer
        targetView.setVisibility(View.GONE);
        parent.addView(shimmerLayout, index, params);
        shimmerLayout.startShimmer();
    }

    /**
     * Hide shimmer and show original view
     */
    public static void hideShimmerForView(ShimmerFrameLayout shimmerLayout, View targetView) {
        if (shimmerLayout != null && targetView != null) {
            shimmerLayout.stopShimmer();
            
            // Remove shimmer from parent
            if (shimmerLayout.getParent() instanceof android.view.ViewGroup) {
                ((android.view.ViewGroup) shimmerLayout.getParent()).removeView(shimmerLayout);
            }
            
            // Show original view
            targetView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Create shimmer for list items
     */
    public static void showListShimmer(Context context, android.view.ViewGroup container, int itemCount) {
        if (container == null) return;
        
        container.removeAllViews();
        
        for (int i = 0; i < itemCount; i++) {
            ShimmerFrameLayout shimmerItem = createSimpleShimmer(context);
            
            // Set different heights for variety
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    80 + (i * 10) // Varying heights
            );
            params.setMargins(0, 0, 0, 16);
            
            container.addView(shimmerItem, params);
            shimmerItem.startShimmer();
        }
    }

    /**
     * Hide list shimmer
     */
    public static void hideListShimmer(android.view.ViewGroup container) {
        if (container == null) return;
        
        // Stop all shimmer animations
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof ShimmerFrameLayout) {
                ((ShimmerFrameLayout) child).stopShimmer();
            }
        }
        
        container.removeAllViews();
    }

    /**
     * Quick shimmer for any view
     */
    public static void addShimmerToView(Context context, View view) {
        if (view == null || view.getParent() == null) return;
        
        ShimmerFrameLayout shimmerLayout = new ShimmerFrameLayout(context);
        
        // Configure shimmer
        com.facebook.shimmer.Shimmer shimmer = new com.facebook.shimmer.Shimmer.ColorHighlightBuilder()
                .setBaseAlpha(0.7f)
                .setHighlightAlpha(0.9f)
                .setDirection(com.facebook.shimmer.Shimmer.Direction.LEFT_TO_RIGHT)
                .setAutoStart(true)
                .build();
        
        shimmerLayout.setShimmer(shimmer);
        
        // Wrap the view
        if (view.getParent() instanceof android.view.ViewGroup) {
            android.view.ViewGroup parent = (android.view.ViewGroup) view.getParent();
            int index = parent.indexOfChild(view);
            android.view.ViewGroup.LayoutParams params = view.getLayoutParams();
            
            parent.removeView(view);
            shimmerLayout.addView(view);
            parent.addView(shimmerLayout, index, params);
        }
        
        shimmerLayout.startShimmer();
    }
}
