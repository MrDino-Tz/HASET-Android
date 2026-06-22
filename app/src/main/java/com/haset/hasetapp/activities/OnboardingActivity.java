package com.haset.hasetapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.haset.hasetapp.R;
import com.haset.hasetapp.utils.PreferenceManager;

import java.util.Arrays;
import java.util.List;

public class OnboardingActivity extends BaseActivity {
    private ViewPager2 viewPager;
    private MaterialButton btnNext, btnSkip;
    private LinearLayout dotsIndicator;
    private final int NUM_PAGES = 3;
    private boolean userInteracted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.onboarding_viewpager);
        btnNext = findViewById(R.id.btn_next);
        btnSkip = findViewById(R.id.btn_skip);
        dotsIndicator = findViewById(R.id.dots_indicator);

        List<Fragment> pages = Arrays.asList(
            OnboardingPageFragment.newInstance(R.drawable.onboard_img1, getString(R.string.onboarding_title1), getString(R.string.onboarding_desc1)),
            OnboardingPageFragment.newInstance(R.drawable.onboard_img2, getString(R.string.onboarding_title2), getString(R.string.onboarding_desc2)),
            OnboardingPageFragment.newInstance(R.drawable.onboard_img3, getString(R.string.onboarding_title3), getString(R.string.onboarding_desc3), true)
        );

        FragmentStateAdapter adapter = new FragmentStateAdapter(this) {
            @Override public int getItemCount() { return pages.size(); }
            @NonNull @Override public Fragment createFragment(int position) { return pages.get(position); }
        };
        viewPager.setAdapter(adapter);

        setupButtonAnimations(btnNext);
        setupButtonAnimations(btnSkip);

        btnNext.setOnClickListener(v -> {
            userInteracted = true;
            if (viewPager.getCurrentItem() < NUM_PAGES - 1) {
                animatePageTransition(viewPager.getCurrentItem() + 1);
            } else {
                animateAndComplete();
            }
        });
        btnSkip.setOnClickListener(v -> {
            userInteracted = true;
            animateAndComplete();
        });

        // Optionally, add a dots indicator update
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                userInteracted = true;
                updateDots(position);
                if (position == NUM_PAGES-1) {
                    btnNext.setText(R.string.get_started);
                    btnSkip.setVisibility(View.GONE);
                } else {
                    btnNext.setText(R.string.next);
                    btnSkip.setVisibility(View.VISIBLE);
                }
            }
        });
        updateDots(0);
    }

    private void animatePageTransition(int nextPosition) {
        Animation fadeOut = new AlphaAnimation(1.0f, 0.0f);
        fadeOut.setDuration(150);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationRepeat(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                viewPager.setCurrentItem(nextPosition, true);
                Animation fadeIn = new AlphaAnimation(0.0f, 1.0f);
                fadeIn.setDuration(150);
                viewPager.startAnimation(fadeIn);
            }
        });
        viewPager.startAnimation(fadeOut);
    }

    private void animateAndComplete() {
        Animation slideUp = new TranslateAnimation(0, 0, 0, -500);
        slideUp.setDuration(300);
        slideUp.setFillAfter(true);
        
        Animation fadeOut = new AlphaAnimation(1.0f, 0.0f);
        fadeOut.setDuration(300);
        fadeOut.setFillAfter(true);
        
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationRepeat(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                completeOnboarding();
            }
        });
        
        viewPager.startAnimation(fadeOut);
    }

    private void setupButtonAnimations(MaterialButton button) {
        button.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                v.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .start();
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP || 
                       event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                v.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(100)
                    .start();
            }
            return false;
        });
    }

    @Override
    public void onBackPressed() {
        if (userInteracted) {
            super.onBackPressed();
        } else {
            Toast.makeText(this, R.string.please_complete_onboarding, Toast.LENGTH_SHORT).show();
        }
    }

    private void completeOnboarding() {
        android.util.Log.d("Onboarding", "completeOnboarding called, userInteracted=" + userInteracted);
        new PreferenceManager(this).setOnboardingSeen(true);
        startActivity(new Intent(this, SplashActivity.class));
        finish();
    }

    private void updateDots(int currentPage) {
        dotsIndicator.removeAllViews();
        for (int i = 0; i < NUM_PAGES; i++) {
            TextView dot = new TextView(this);
            dot.setText("●");
            dot.setTextSize(24);
            dot.setTextColor(i == currentPage ? getColor(R.color.green_primary) : getColor(R.color.grey_dark));
            
            if (i == currentPage) {
                dot.setAlpha(0f);
                dot.animate().alpha(1f).setDuration(200).start();
            }
            
            dotsIndicator.addView(dot);
        }
    }
}
