package com.haset.hasetapp.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

import com.haset.hasetapp.R;
import com.haset.hasetapp.utils.Constants;

public class ServiceAgreementActivity extends LocalizedAppCompatActivity {
    
    private LinearLayout btnPrivacyPolicy, btnTermsConditions;
    private ImageView btnBack;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_agreement);
        
        initViews();
        setupClickListeners();
    }
    
    private void initViews() {
        btnPrivacyPolicy = findViewById(R.id.btnPrivacyPolicy);
        btnTermsConditions = findViewById(R.id.btnTermsConditions);
        btnBack = findViewById(R.id.btnBack);
        
        // Add null checks to prevent crashes
        if (btnBack == null) {
            // Handle missing back button
            finish();
            return;
        }
    }
    
    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnPrivacyPolicy.setOnClickListener(v -> {
            openUrlInChromeCustomTab(Constants.PRIVACY_POLICY_URL);
        });
        
        btnTermsConditions.setOnClickListener(v -> {
            openUrlInChromeCustomTab(Constants.TERMS_CONDITIONS_URL);
        });
    }
    
    private void openUrlInChromeCustomTab(String url) {
        try {
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            builder.setShowTitle(true);
            builder.setUrlBarHidingEnabled(false);
            builder.setShareState(CustomTabsIntent.SHARE_STATE_ON);
            
            // Set app colors
            builder.setToolbarColor(getResources().getColor(R.color.text_primary));
            
            CustomTabsIntent customTabsIntent = builder.build();
            customTabsIntent.launchUrl(this, Uri.parse(url));
        } catch (Exception e) {
            // Fallback to external browser if Chrome Custom Tabs fails
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        }
    }
}
