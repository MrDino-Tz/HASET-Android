package com.haset.hasetapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.haset.hasetapp.R;
import com.haset.hasetapp.fragments.AdminAllUsersFragment;
import com.haset.hasetapp.fragments.AdminDoctorsFragment;
import com.haset.hasetapp.fragments.AdminPatientsFragment;
import com.haset.hasetapp.fragments.AdminAppointmentsFragment;

public class AdminManagementActivity extends AppCompatActivity {
    public static final String EXTRA_SELECTED_TAB = "selected_tab";
    
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ImageView btnBack;
    private FloatingActionButton fabCreateUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_management);
        
        initViews();
        setupViewPager();
        setupTabMediator();
        
        int selectedTab = getIntent().getIntExtra(EXTRA_SELECTED_TAB, 0);
        if (selectedTab >= 0 && selectedTab < 4) {
            viewPager.setCurrentItem(selectedTab, false);
        }
    }
    
    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tabLayout = findViewById(R.id.tabs);
        viewPager = findViewById(R.id.vpAdminTabs);
        fabCreateUser = findViewById(R.id.fabCreateUser);
        
        btnBack.setOnClickListener(v -> finish());
        fabCreateUser.setOnClickListener(v -> showCreateOptionsDialog());
    }
    
    private void showCreateOptionsDialog() {
        String[] options = {
            getString(R.string.create_user),
            getString(R.string.create_demo_doctor)
        };
        
        new AlertDialog.Builder(this)
            .setTitle(R.string.create_new)
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    Intent intent = new Intent(this, AdminUserEditActivity.class);
                    intent.putExtra("isEdit", false);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(this, AdminDemoDoctorActivity.class);
                    startActivity(intent);
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    private void setupViewPager() {
        viewPager.setAdapter(new AdminManagementTabAdapter(this));
    }
    
    private void setupTabMediator() {
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText(getString(R.string.all_users)); break;
                case 1: tab.setText(getString(R.string.doctors)); break;
                case 2: tab.setText(getString(R.string.patients)); break;
                case 3: tab.setText(getString(R.string.appointments)); break;
            }
        }).attach();
    }
    
    public static class AdminManagementTabAdapter extends FragmentStateAdapter {
        public AdminManagementTabAdapter(FragmentActivity fa) {
            super(fa);
        }
        
        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new AdminAllUsersFragment();
                case 1:
                    return new AdminDoctorsFragment();
                case 2:
                    return new AdminPatientsFragment();
                case 3:
                    return new AdminAppointmentsFragment();
                default:
                    return new AdminAllUsersFragment();
            }
        }
        
        @Override
        public int getItemCount() {
            return 4; // Fixed number of tabs
        }
    }
}

