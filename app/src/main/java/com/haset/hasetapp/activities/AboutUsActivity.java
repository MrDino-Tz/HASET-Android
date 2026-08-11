package com.haset.hasetapp.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.haset.hasetapp.R;
import com.haset.hasetapp.utils.Constants;

public class AboutUsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_about_us);
        
        // Setup window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // Setup back button
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
        


        // Setup legal links
        TextView btnPrivacyPolicy = findViewById(R.id.btnPrivacyPolicy);
        TextView btnTermsOfService = findViewById(R.id.btnTermsOfService);

        if (btnPrivacyPolicy != null) {
            btnPrivacyPolicy.setOnClickListener(v -> openWebPage(Constants.PRIVACY_POLICY_URL));
        }

        if (btnTermsOfService != null) {
            btnTermsOfService.setOnClickListener(v -> openWebPage(Constants.TERMS_CONDITIONS_URL));
        }
    }

    private void openWebPage(String url) {
        try {
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            builder.setToolbarColor(ContextCompat.getColor(this, R.color.green_primary));
            builder.setShowTitle(true);
            CustomTabsIntent customTabsIntent = builder.build();
            customTabsIntent.launchUrl(this, Uri.parse(url));
        } catch (Exception e) {
            // Fallback to standard browser if Custom Tabs fails
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        }
    }
}