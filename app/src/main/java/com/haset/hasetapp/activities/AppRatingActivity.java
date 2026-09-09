package com.haset.hasetapp.activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.haset.hasetapp.R;
import com.haset.hasetapp.utils.AppRatingHelper;

public class AppRatingActivity extends LocalizedAppCompatActivity {

    private ImageView[] starViews;
    private TextView tvRatingText;
    private MaterialButton btnRateNow;
    private int selectedRating = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_app_rating);
        overridePendingTransition(R.anim.anim_slide_up, 0);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvRatingText = findViewById(R.id.tvRatingText);
        btnRateNow = findViewById(R.id.btnRateNow);
        MaterialButton btnNotNow = findViewById(R.id.btnNotNow);

        starViews = new ImageView[5];
        starViews[0] = findViewById(R.id.ivStar1);
        starViews[1] = findViewById(R.id.ivStar2);
        starViews[2] = findViewById(R.id.ivStar3);
        starViews[3] = findViewById(R.id.ivStar4);
        starViews[4] = findViewById(R.id.ivStar5);

        for (int i = 0; i < starViews.length; i++) {
            final int rating = i + 1;
            starViews[i].setContentDescription(getString(R.string.star_content_desc, rating));
            starViews[i].setOnClickListener(v -> {
                selectedRating = rating;
                updateStarDisplay();
                updateRatingText();
                btnRateNow.setEnabled(true);
            });
        }

        btnRateNow.setOnClickListener(v -> {
            AppRatingHelper ratingHelper = new AppRatingHelper(this);
            ratingHelper.completeRatingAndLaunch();
            finish();
        });

        btnNotNow.setOnClickListener(v -> finish());

        updateStarDisplay();
    }

    private void updateStarDisplay() {
        for (int i = 0; i < starViews.length; i++) {
            if (i < selectedRating) {
                starViews[i].setImageResource(R.drawable.ic_star_filled);
                starViews[i].setColorFilter(ContextCompat.getColor(this, R.color.star_yellow));
            } else {
                starViews[i].setImageResource(R.drawable.ic_star_outline);
                starViews[i].setColorFilter(ContextCompat.getColor(this, R.color.star_gray));
            }
        }
    }

    private void updateRatingText() {
        String[] ratingTexts = {
            getString(R.string.tap_to_rate),
            getString(R.string.rating_poor),
            getString(R.string.rating_fair),
            getString(R.string.rating_good),
            getString(R.string.rating_very_good),
            getString(R.string.rating_excellent)
        };
        tvRatingText.setText(ratingTexts[selectedRating]);
    }
}