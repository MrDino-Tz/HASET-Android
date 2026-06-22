package com.haset.hasetapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import android.widget.TextView;

import com.haset.hasetapp.R;
import com.haset.hasetapp.utils.Constants;
import com.haset.hasetapp.viewmodels.AuthViewModel;

public class RoleSelectionActivity extends BaseActivity {
    private CardView cardPatient, cardDoctor; // cardAdmin removed
    private ImageView btnBack;
    // private TextView tvAdminRegister; // Removed admin registration option

    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        authViewModel = new androidx.lifecycle.ViewModelProvider(this).get(AuthViewModel.class);

        cardPatient = findViewById(R.id.cardPatient);
        cardDoctor = findViewById(R.id.cardDoctor);
        // Language Switcher Toggle logic
        com.haset.hasetapp.utils.LanguageToggleHelper.setup(this, findViewById(android.R.id.content), languageCode -> {
            com.haset.hasetapp.utils.CustomDialog.showLoading(this, getString(R.string.switching_language));
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                com.haset.hasetapp.utils.LocaleHelper.setLocale(this, languageCode);
                com.haset.hasetapp.utils.PreferenceManager pm = new com.haset.hasetapp.utils.PreferenceManager(this);
                pm.setLanguage(languageCode);
                com.haset.hasetapp.utils.CustomDialog.hideLoading();
                overridePendingTransition(R.anim.fade_through_enter, R.anim.fade_through_exit);
                recreate();
                overridePendingTransition(R.anim.fade_through_enter, R.anim.fade_through_exit);
            }, 500);
        });
        // cardAdmin = findViewById(R.id.cardAdmin); // Commented out
        btnBack = findViewById(R.id.btnBack);
        ImageView btnMore = findViewById(R.id.btnMore);

        if (btnMore != null) {
            btnMore.setOnClickListener(v -> {
                // Placeholder for more options (e.g. About, Help)
            });
        }

        // Setup back button
        btnBack.setOnClickListener(v -> {
            finish();
        });

        cardPatient.setOnClickListener(v -> {
            navigateToRegister(Constants.ROLE_PATIENT, v);
        });
        
        cardDoctor.setOnClickListener(v -> {
            navigateToRegister(Constants.ROLE_DOCTOR, v);
        });
        
//        if (cardAdmin != null) {
//            cardAdmin.setOnClickListener(v -> {
//                navigateToRegister("admin", v);
//            });
//        }
    }

    private void navigateToRegister(String role, android.view.View sourceView) {
        Intent intent = new Intent(RoleSelectionActivity.this, RegisterActivity.class);
        intent.putExtra("role", role);
        
        startActivity(intent);
        overridePendingTransition(R.anim.fade_through_enter, R.anim.fade_through_exit);
    }
}
