package com.haset.hasetapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.haset.hasetapp.R;
import com.haset.hasetapp.fragments.ArticleCenterImageTextFragment;
import com.haset.hasetapp.fragments.ArticleCenterMyPostsFragment;
import com.haset.hasetapp.viewmodels.AuthViewModel;

public class ArticleCenterActivity extends AppCompatActivity {
    // TAB_TITLES removed in favor of direct resource usage in setupTabMediator
    
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ImageView btnBack;
    private ImageView btnAddPost;

    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_center);
        
        authViewModel = new androidx.lifecycle.ViewModelProvider(this).get(AuthViewModel.class);
        
        initViews();
        setupViewPager();
        setupTabMediator();
        setupObservers();
    }
    
    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnAddPost = findViewById(R.id.btnAddPost);
        tabLayout = findViewById(R.id.tabs);
        viewPager = findViewById(R.id.viewPager);
        
        btnAddPost.setVisibility(View.GONE); // Default to hidden
        
        btnBack.setOnClickListener(v -> finish());
        btnAddPost.setOnClickListener(v -> {
            Intent intent = new Intent(ArticleCenterActivity.this, CreatePostWizardActivity.class);
            intent.putExtra("post_type", "image");
            startActivity(intent);
        });
    }

    private void setupObservers() {
        // Observe current user to update FAB visibility
        authViewModel.getCurrentUser().observe(this, user -> {
            if (user != null && "admin".equalsIgnoreCase(user.getRole())) {
                btnAddPost.setVisibility(View.VISIBLE);
            } else {
                btnAddPost.setVisibility(View.GONE);
            }
        });

        // Trigger user data fetch if not already available
        String uid = new com.haset.hasetapp.utils.PreferenceManager(this).getUserId();
        if (uid != null) {
            authViewModel.fetchUserData(uid);
        }
    }
    
    private void setupViewPager() {
        viewPager.setAdapter(new ArticleCenterTabAdapter(this));
    }
    
    private void setupTabMediator() {
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText(getString(R.string.all_articles));
            } else {
                tab.setText(getString(R.string.my_posts));
            }
        }).attach();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh fragments when returning from create post
        if (viewPager != null && viewPager.getAdapter() != null) {
            viewPager.getAdapter().notifyDataSetChanged();
        }
    }
    
    public static class ArticleCenterTabAdapter extends FragmentStateAdapter {
        public ArticleCenterTabAdapter(FragmentActivity fa) {
            super(fa);
        }
        
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new ArticleCenterImageTextFragment();
            } else {
                return new ArticleCenterMyPostsFragment();
            }
        }
        
        @Override
        public int getItemCount() {
            return 2; // Fixed number of tabs
        }
    }
}

