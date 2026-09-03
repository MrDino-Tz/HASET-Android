package com.haset.hasetapp.activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.haset.hasetapp.R;
import com.haset.hasetapp.fragments.ArticleTabFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import com.haset.hasetapp.utils.AuditLogger;
import com.haset.hasetapp.viewmodels.AuthViewModel;
import com.haset.hasetapp.viewmodels.ArticleViewModel;
import com.haset.hasetapp.database.entities.ArticlePostEntity;
import com.haset.hasetapp.adapters.PostFeedAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.ArrayList;
import java.util.List;
import android.text.Editable;
import android.text.TextWatcher;
import com.google.gson.Gson;

public class ArticleActivity extends LocalizedAppCompatActivity {
    public static final String EXTRA_ARTICLE_ID = "extra_article_id";
    private static final String[] TAB_TITLES = {"Articles", "Health Tips", "Saved"};
    private AuthViewModel authViewModel;
    private ArticleViewModel articleViewModel;
    private String highlightArticleId;
    private List<ArticlePostEntity> allArticles = new ArrayList<>();
    private PostFeedAdapter searchAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article);
        overridePendingTransition(R.anim.anim_slide_up, 0);
        
        if (getIntent() != null) {
            highlightArticleId = getIntent().getStringExtra(EXTRA_ARTICLE_ID);
        }
        
        authViewModel = new androidx.lifecycle.ViewModelProvider(this).get(AuthViewModel.class);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        ImageView btnMoreOptions = findViewById(R.id.btnMoreOptions);
        if (btnMoreOptions != null) {
            btnMoreOptions.setOnClickListener(v -> showArticleOptionsMenu());
        }
        
        com.google.android.material.floatingactionbutton.FloatingActionButton fabAddArticle = findViewById(R.id.fabAddArticle);
        fabAddArticle.setVisibility(android.view.View.GONE); // Default hidden
        
        tabLayout = findViewById(R.id.tabsArticles);
        viewPager = findViewById(R.id.vpArticleTabs);
        
        
        viewPager.setAdapter(new ArticleTabAdapter(this, highlightArticleId));
        new TabLayoutMediator(tabLayout, viewPager, (tab, pos) -> {
            tab.setText(TAB_TITLES[pos]);
        }).attach();
        
        com.google.android.material.search.SearchBar searchBar = findViewById(R.id.searchBar);
        com.google.android.material.search.SearchView searchView = findViewById(R.id.searchView);
        
        articleViewModel = new androidx.lifecycle.ViewModelProvider(this).get(ArticleViewModel.class);
        setupSearchAlgorithm(searchView);

        setupObservers(fabAddArticle);
    }
    
    private void setupSearchAlgorithm(com.google.android.material.search.SearchView searchView) {
        RecyclerView rvSearchResults = findViewById(R.id.rvSearchResults);
        android.view.View layoutSearchEmptyState = findViewById(R.id.layoutSearchEmptyState);
        android.widget.TextView tvEmptyTitle = findViewById(R.id.tvEmptyStateTitle);
        android.widget.TextView tvEmptySubtitle = findViewById(R.id.tvEmptyStateSubtitle);
        android.widget.ImageView ivEmptyIcon = findViewById(R.id.ivEmptyStateIcon);
        
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        searchAdapter = new PostFeedAdapter(new ArrayList<>(), this);
        searchAdapter.setOnArticleClickListener(article -> {
            android.content.Intent intent = new android.content.Intent(this, com.haset.hasetapp.activities.ArticleDetailActivity.class);
            intent.putExtra(com.haset.hasetapp.activities.ArticleDetailActivity.EXTRA_ARTICLE, new Gson().toJson(article));
            startActivity(intent);
        });
        rvSearchResults.setAdapter(searchAdapter);

        // Fetch all articles into memory for live search
        articleViewModel.getPublishedArticles().observe(this, posts -> {
            if (posts != null) {
                allArticles = posts;
            }
        });

        searchView.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase().trim();
                if (query.isEmpty()) {
                    searchAdapter.setPosts(new ArrayList<>());
                    layoutSearchEmptyState.setVisibility(android.view.View.VISIBLE);
                    if(tvEmptyTitle != null) {
                        tvEmptyTitle.setText("Search Articles");
                        tvEmptySubtitle.setText("Type a keyword, tag, or author name to find exactly what you need.");
                        ivEmptyIcon.setImageResource(R.drawable.ic_search);
                    }
                    return;
                }

                List<ArticlePostEntity> filtered = new ArrayList<>();
                for (ArticlePostEntity post : allArticles) {
                    boolean matchesTitle = post.getTitle() != null && post.getTitle().toLowerCase().contains(query);
                    boolean matchesDesc = post.getDescription() != null && post.getDescription().toLowerCase().contains(query);
                    boolean matchesTags = post.getTags() != null && post.getTags().toLowerCase().contains(query);
                    boolean matchesAuthor = post.getProfileName() != null && post.getProfileName().toLowerCase().contains(query);
                    
                    if (matchesTitle || matchesDesc || matchesTags || matchesAuthor) {
                        filtered.add(post);
                    }
                }
                
                searchAdapter.setPosts(filtered);
                
                if (filtered.isEmpty()) {
                    layoutSearchEmptyState.setVisibility(android.view.View.VISIBLE);
                    if(tvEmptyTitle != null) {
                        tvEmptyTitle.setText("No Matches Found");
                        tvEmptySubtitle.setText("We couldn't find any articles matching '" + query + "'. Try different keywords.");
                        ivEmptyIcon.setImageResource(R.drawable.ic_no_data);
                    }
                } else {
                    layoutSearchEmptyState.setVisibility(android.view.View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Initial empty state
        layoutSearchEmptyState.setVisibility(android.view.View.VISIBLE);
        if(tvEmptyTitle != null) {
            tvEmptyTitle.setText("Search Articles");
            tvEmptySubtitle.setText("Type a keyword, tag, or author name to find exactly what you need.");
            ivEmptyIcon.setImageResource(R.drawable.ic_search); // Ensure you have an ic_search drawable or fallback
        }
    }

    private void setupObservers(com.google.android.material.floatingactionbutton.FloatingActionButton fab) {
        fab.setVisibility(android.view.View.GONE);

        String uid = new com.haset.hasetapp.utils.PreferenceManager(this).getUserId();
        if (uid != null) {
            authViewModel.fetchUserData(uid);
        }
    }
    
    private void showArticleOptionsMenu() {
        android.view.View btnMore = findViewById(R.id.btnMoreOptions);
        if (btnMore == null) return;

        android.view.View popupView = android.view.LayoutInflater.from(this).inflate(R.layout.popup_article_menu, null);
        
        final android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(
                popupView, 
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 
                true
        );

        popupWindow.setElevation(10);
        
        popupView.findViewById(R.id.tvAbout).setOnClickListener(v -> {
            popupWindow.dismiss();
            showAboutDialog();
        });
        
        popupView.findViewById(R.id.tvSettings).setOnClickListener(v -> {
            popupWindow.dismiss();
            showSettingsBottomSheet();
        });
        
        popupWindow.showAsDropDown(btnMore, 0, 8);
    }

    private void showAboutDialog() {
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_about_h_article, null);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
        }
        
        dialogView.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showSettingsBottomSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = 
            new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        android.view.View view = getLayoutInflater().inflate(R.layout.layout_notification_settings, null);
        
        com.haset.hasetapp.utils.PreferenceManager prefManager = new com.haset.hasetapp.utils.PreferenceManager(this);
        com.google.android.material.materialswitch.MaterialSwitch switchArticles = view.findViewById(R.id.switchArticles);
        com.google.android.material.materialswitch.MaterialSwitch switchTips = view.findViewById(R.id.switchTips);

        // Load current states
        switchArticles.setChecked(prefManager.isArticleNotificationsEnabled());
        switchTips.setChecked(prefManager.isHealthTipNotificationsEnabled());
        
        view.findViewById(R.id.btnSaveSettings).setOnClickListener(v -> {
            // Save new states
            prefManager.setArticleNotificationsEnabled(switchArticles.isChecked());
            prefManager.setHealthTipNotificationsEnabled(switchTips.isChecked());
            
            bottomSheetDialog.dismiss();
            Toast.makeText(this, "Notification preferences saved", Toast.LENGTH_SHORT).show();
            
            // Log for audit
            AuditLogger.getInstance(this).logSettingsUpdated("User updated article notification preferences");
        });
        
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }
    
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    
    public static class ArticleTabAdapter extends FragmentStateAdapter {
        private String highlightArticleId;
        
        public ArticleTabAdapter(FragmentActivity fa, String articleId) { 
            super(fa);
            this.highlightArticleId = articleId;
        }
        @Override
        public Fragment createFragment(int pos) {
            return ArticleTabFragment.newInstance(TAB_TITLES[pos], highlightArticleId);
        }
        @Override
        public int getItemCount() { return TAB_TITLES.length; }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.anim_slide_down);
    }
}
