package com.haset.hasetapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.haset.hasetapp.R;
import com.haset.hasetapp.activities.CreatePostWizardActivity;
import com.haset.hasetapp.database.entities.ArticlePostEntity;
import com.haset.hasetapp.firebase.ArticlePostHelper;
import com.haset.hasetapp.utils.AuditLogger;
import com.haset.hasetapp.utils.PreferenceManager;
import com.haset.hasetapp.adapters.ArticlePostAdapter;
import com.haset.hasetapp.viewmodels.ArticleViewModel;

public class ArticleCenterMyPostsFragment extends Fragment {
    private RecyclerView rvPosts;
    private TextView tvEmpty;
    private ArticleViewModel viewModel;
    private String userId;
    private ArticlePostAdapter adapter;
    private FloatingActionButton fabAddPost;
    private TabLayout tabFilter;
    private String currentFilter = "all"; // "all", "published", "draft"
    
    private List<ArticlePostEntity> allPosts = new ArrayList<>();

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        rvPosts = view.findViewById(R.id.rvPosts);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        fabAddPost = view.findViewById(R.id.fabAddPost);
        tabFilter = view.findViewById(R.id.tabFilter);
        
        PreferenceManager preferenceManager = new PreferenceManager(requireContext());
        userId = preferenceManager.getUserId();
        
        viewModel = new androidx.lifecycle.ViewModelProvider(requireActivity()).get(ArticleViewModel.class);
        
        setupTabs();
        
        adapter = new ArticlePostAdapter(new ArrayList<>(), new ArticlePostAdapter.OnPostActionListener() {
            @Override
            public void onEditClick(ArticlePostEntity post) {
                Intent intent = new Intent(requireContext(), CreatePostWizardActivity.class);
                intent.putExtra("post_id", post.getPostId());
                intent.putExtra("post_type", post.getType());
                startActivity(intent);
            }

            @Override
            public void onDeleteClick(ArticlePostEntity post) {
                showDeleteConfirmation(post);
            }
        });
        
        rvPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPosts.setAdapter(adapter);
        
        fabAddPost.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CreatePostWizardActivity.class);
            intent.putExtra("post_type", "image");
            startActivity(intent);
        });
        
        setupObservers();
    }

    private void setupObservers() {
        viewModel.getArticlesByAuthor(userId).observe(getViewLifecycleOwner(), posts -> {
            if (posts != null) {
                allPosts = posts;
                filterAndDisplayPosts();
            } else {
                allPosts = new ArrayList<>();
                filterAndDisplayPosts();
            }
        });
    }

    private void filterAndDisplayPosts() {
        if (allPosts == null || allPosts.isEmpty()) {
            showEmptyState();
            return;
        }

        List<ArticlePostEntity> filteredPosts = new ArrayList<>();
        if ("all".equals(currentFilter)) {
            filteredPosts.addAll(allPosts);
        } else {
            for (ArticlePostEntity post : allPosts) {
                String status = post.getStatus();
                if (status != null && status.equalsIgnoreCase(currentFilter)) {
                    filteredPosts.add(post);
                }
            }
        }
        displayPosts(filteredPosts);
    }

    private void setupTabs() {
        tabFilter.addTab(tabFilter.newTab().setText(getString(R.string.tab_all)));
        tabFilter.addTab(tabFilter.newTab().setText(getString(R.string.tab_posted)));
        tabFilter.addTab(tabFilter.newTab().setText(getString(R.string.tab_drafted)));
        
        tabFilter.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        currentFilter = "all";
                        break;
                    case 1:
                        currentFilter = "published";
                        break;
                    case 2:
                        currentFilter = "draft";
                        break;
                }
                filterAndDisplayPosts();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void showDeleteConfirmation(ArticlePostEntity post) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_article)
            .setMessage(getString(R.string.delete_article_confirm))
            .setPositiveButton("Delete", (dialog, which) -> {
                viewModel.deleteArticle(post.getPostId(), new ArticlePostHelper.OnCompleteListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        AuditLogger.getInstance(requireContext()).logPostDeleted(post.getPostId(), post.getTitle(), post.getType());
                        Toast.makeText(requireContext(), R.string.article_deleted_success, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(requireContext(), "Failed to delete article: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // LiveData will auto-update
    }

    private void displayPosts(List<ArticlePostEntity> posts) {
        if (!isAdded()) return;
        if (posts == null || posts.isEmpty()) {
            showEmptyState();
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvPosts.setVisibility(View.VISIBLE);
            adapter.updatePosts(posts);
        }
    }

    private void showEmptyState() {
        if (!isAdded()) return;
        tvEmpty.setVisibility(View.VISIBLE);
        rvPosts.setVisibility(View.GONE);
        String message;
        if ("draft".equals(currentFilter)) {
            message = getString(R.string.no_drafts_yet);
        } else if ("published".equals(currentFilter)) {
            message = getString(R.string.no_published_articles);
        } else {
            message = getString(R.string.no_posts_create_hint);
        }
        tvEmpty.setText(message);
    }
}

