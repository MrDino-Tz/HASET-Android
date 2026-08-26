package com.haset.hasetapp.activities;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

public abstract class LocalizedAppCompatActivity extends AppCompatActivity {
    private String attachedLanguage;

    @Override
    protected void attachBaseContext(Context newBase) {
        attachedLanguage = com.haset.hasetapp.utils.LocaleHelper.getLanguage(newBase);
        super.attachBaseContext(com.haset.hasetapp.utils.LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentLanguage = com.haset.hasetapp.utils.LocaleHelper.getLanguage(this);
        if (attachedLanguage != null && !attachedLanguage.equals(currentLanguage)) {
            recreate();
        }
    }
}
