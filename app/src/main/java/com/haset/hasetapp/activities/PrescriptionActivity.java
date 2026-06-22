package com.haset.hasetapp.activities;

import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import com.haset.hasetapp.R;
import com.haset.hasetapp.fragments.PrescriptionsFragment;

public class PrescriptionActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prescription);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fade_through_enter, R.anim.fade_through_exit)
                    .replace(R.id.fragmentContainer, new PrescriptionsFragment())
                    .commit();
        }
    }

    public void setToolbarTitle(String title) {
        android.widget.TextView tvTitle = findViewById(R.id.tvToolbarTitle);
        if (tvTitle != null) {
            tvTitle.setText(title);
        }
    }

    public void setDownloadButtonVisible(boolean visible, android.view.View.OnClickListener listener) {
        android.view.View btnDownload = findViewById(R.id.btnDownload);
        if (btnDownload != null) {
            btnDownload.setVisibility(visible ? android.view.View.VISIBLE : android.view.View.GONE);
            btnDownload.setOnClickListener(listener);
        }
    }

    @Override
    public void onNetworkAvailable() {}

    @Override
    public void onNetworkUnavailable() {}
}
