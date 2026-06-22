package com.haset.hasetapp.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.haset.hasetapp.R;
import com.haset.hasetapp.utils.SimpleShimmerHelper;

/**
 * ShimmerTestActivity - Test shimmer effects
 * Use this to verify shimmer is working before implementing in main app
 */
public class ShimmerTestActivity extends AppCompatActivity {

    private LinearLayout container;
    private TextView testText;
    private Button btnShowShimmer;
    private Button btnHideShimmer;
    private Button btnListShimmer;
    private ShimmerFrameLayout currentShimmer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shimmer_test);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        container = findViewById(R.id.container);
        testText = findViewById(R.id.testText);
        btnShowShimmer = findViewById(R.id.btnShowShimmer);
        btnHideShimmer = findViewById(R.id.btnHideShimmer);
        btnListShimmer = findViewById(R.id.btnListShimmer);
    }

    private void setupClickListeners() {
        btnShowShimmer.setOnClickListener(v -> {
            // Show shimmer for the text view
            SimpleShimmerHelper.showShimmerForView(this, testText);
        });

        btnHideShimmer.setOnClickListener(v -> {
            // Hide shimmer and show text
            SimpleShimmerHelper.hideShimmerForView(currentShimmer, testText);
        });

        btnListShimmer.setOnClickListener(v -> {
            // Show list shimmer
            SimpleShimmerHelper.showListShimmer(this, container, 5);
            
            // Auto-hide after 3 seconds
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                SimpleShimmerHelper.hideListShimmer(container);
                
                // Add some test content
                addTestContent();
            }, 3000);
        });
    }

    private void addTestContent() {
        container.removeAllViews();
        
        for (int i = 0; i < 5; i++) {
            TextView item = new TextView(this);
            item.setText("Test Item " + (i + 1));
            item.setTextColor(getResources().getColor(R.color.text_primary));
            item.setPadding(16, 16, 16, 16);
            item.setBackgroundColor(getResources().getColor(R.color.background_card));
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 16);
            
            container.addView(item, params);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Clean up shimmer
        if (currentShimmer != null) {
            currentShimmer.stopShimmer();
        }
    }
}
