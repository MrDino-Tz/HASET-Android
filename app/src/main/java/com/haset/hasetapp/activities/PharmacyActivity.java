package com.haset.hasetapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.haset.hasetapp.R;
import com.haset.hasetapp.fragments.PharmacyCartFragment;
import com.haset.hasetapp.fragments.PharmacyHomeFragment;
import com.haset.hasetapp.utils.AuditLogger;
import com.haset.hasetapp.utils.PreferenceManager;
import com.haset.hasetapp.viewmodels.PharmacyViewModel;
import androidx.lifecycle.ViewModelProvider;

public class PharmacyActivity extends BaseActivity {

    private TextInputEditText etPharmacySearch;
    private ImageView btnBack;
    private ImageView btnCameraSearch;
    private ImageView btnCart;
    private TextView tvCartBadge;
    private BottomNavigationView bottomNavPharmacy;
    private FrameLayout fragmentContainer;

    private PharmacyHomeFragment homeFragment;
    private PharmacyCartFragment cartFragment;
    private PharmacyViewModel viewModel;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pharmacy);

        preferenceManager = new PreferenceManager(this);
        viewModel = new ViewModelProvider(this).get(PharmacyViewModel.class);
        viewModel.setUserId(preferenceManager.getUserId());

        initViews();
        setupFragments();
        setupBottomNavigation();
        setupObservers();
        setupClickListeners();
        
        // Log activity
        AuditLogger.getInstance(this).logAction("OPEN_PHARMACY", "User opened Pharmacy section", "PHARMACY", null);
    }

    private void setupObservers() {
        viewModel.getCartCount().observe(this, this::updateCartBadge);
    }

    private void initViews() {
        etPharmacySearch = findViewById(R.id.etPharmacySearch);
        btnBack = findViewById(R.id.btnBack);
        btnCameraSearch = findViewById(R.id.btnCameraSearch);
        btnCart = findViewById(R.id.btnCart);
        tvCartBadge = findViewById(R.id.tvCartBadge);
        bottomNavPharmacy = findViewById(R.id.bottomNavPharmacy);
        fragmentContainer = findViewById(R.id.fragmentContainer);
    }

    private void setupFragments() {
        homeFragment = new PharmacyHomeFragment();
        cartFragment = new PharmacyCartFragment();

        // Load home fragment by default
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.fade_through_enter, R.anim.fade_through_exit)
                .replace(R.id.fragmentContainer, homeFragment)
                .commit();
    }

    private void setupBottomNavigation() {
        bottomNavPharmacy.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_pharmacy_home) {
                switchToHomeFragment();
                return true;
            } else if (itemId == R.id.nav_pharmacy_cart) {
                switchToCartFragment();
                return true;
            }
            
            return false;
        });
        
        // Set home as selected by default
        bottomNavPharmacy.setSelectedItemId(R.id.nav_pharmacy_home);
    }

    public void switchToHomeFragment() {
        if (homeFragment == null) {
            homeFragment = new PharmacyHomeFragment();
        }
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.fade_through_enter, R.anim.fade_through_exit)
                .replace(R.id.fragmentContainer, homeFragment)
                .commit();
    }

    public void switchToCartFragment() {
        if (cartFragment == null) {
            cartFragment = new PharmacyCartFragment();
        }
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.fade_through_enter, R.anim.fade_through_exit)
                .replace(R.id.fragmentContainer, cartFragment)
                .commit();
    }

    private void setupClickListeners() {
        // Search functionality - pass to home fragment
        etPharmacySearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setSearchQuery(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Back Button - Navigate to Dashboard
        btnBack.setOnClickListener(v -> {
            Intent dashboardIntent = new Intent(this, DashboardActivity.class);
            dashboardIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(dashboardIntent);
            finish();
        });

        // Camera Search
        btnCameraSearch.setOnClickListener(v -> {
            // Open camera for image search
            // Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // startActivity(cameraIntent);
        });

        // Cart - switch to cart fragment
        btnCart.setOnClickListener(v -> {
            switchToCartFragment();
            bottomNavPharmacy.setSelectedItemId(R.id.nav_pharmacy_cart);
        });
    }

    public void updateCartBadge(int count) {
        if (count > 0) {
            tvCartBadge.setText(String.valueOf(count));
            tvCartBadge.setVisibility(View.VISIBLE);
        } else {
            tvCartBadge.setVisibility(View.GONE);
        }
    }
}
