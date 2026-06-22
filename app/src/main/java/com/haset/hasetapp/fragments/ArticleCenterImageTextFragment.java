package com.haset.hasetapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.haset.hasetapp.R;
import com.haset.hasetapp.activities.CreatePostWizardActivity;
import com.haset.hasetapp.adapters.ArticlePostAdapter;
import com.haset.hasetapp.database.entities.ArticlePostEntity;
import com.haset.hasetapp.firebase.ArticlePostHelper;
import com.haset.hasetapp.utils.ShimmerHelper;
import com.haset.hasetapp.viewmodels.ArticleViewModel;

import java.util.ArrayList;
import java.util.List;

public class ArticleCenterImageTextFragment extends Fragment {
    private RecyclerView rvPosts;
    private TextView tvEmpty;
    private android.widget.LinearLayout shimmerContainer;
    private ArticlePostAdapter adapter;
    private TabLayout tabFilter;
    private String currentFilter = "all"; // "all", "published", "draft"
    private List<ArticlePostEntity> allPosts = new ArrayList<>();
    private ArticleViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_article_center_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        rvPosts = view.findViewById(R.id.rvPosts);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        shimmerContainer = view.findViewById(R.id.shimmerContainer);
        tabFilter = view.findViewById(R.id.tabFilter);
        
        viewModel = new androidx.lifecycle.ViewModelProvider(requireActivity()).get(ArticleViewModel.class);
        
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
        
        setupTabs();
        setupObservers();
    }

    private void setupObservers() {
        viewModel.getPublishedArticles().observe(getViewLifecycleOwner(), posts -> {
            hideShimmer();
            if (posts != null) {
                allPosts = posts;
                filterAndDisplayPosts();
            } else {
                allPosts = new ArrayList<>();
                filterAndDisplayPosts();
            }
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) showShimmer();
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) {
                hideShimmer();
                showEmptyState();
            }
        });
    }

    private void filterAndDisplayPosts() {
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

    private void showShimmer() {
        if (shimmerContainer != null) {
            rvPosts.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.GONE);
            shimmerContainer.setVisibility(View.VISIBLE);
            ShimmerHelper.showListShimmer(requireContext(), shimmerContainer, 5, R.layout.shimmer_layout_post_item);
        }
    }

    private void hideShimmer() {
        if (shimmerContainer != null) {
            ShimmerHelper.hideListShimmer(shimmerContainer);
            shimmerContainer.setVisibility(View.GONE);
            rvPosts.setVisibility(View.VISIBLE);
        }
    }

    private void displayPosts(List<ArticlePostEntity> posts) {
        if (posts == null || posts.isEmpty()) {
            showEmptyState();
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvPosts.setVisibility(View.VISIBLE);
            adapter.updatePosts(posts);
        }
    }

    private void showEmptyState() {
        tvEmpty.setVisibility(View.VISIBLE);
        rvPosts.setVisibility(View.GONE);
        tvEmpty.setText(R.string.no_articles_yet);
    }

    private void showDeleteConfirmation(ArticlePostEntity post) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_article))
            .setMessage("Are you sure you want to delete '" + post.getTitle() + "'? This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> {
                viewModel.deleteArticle(post.getPostId(), new ArticlePostHelper.OnCompleteListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        com.haset.hasetapp.utils.AuditLogger.getInstance(requireContext())
                            .logPostDeleted(post.getPostId(), post.getTitle(), post.getType());
                        android.widget.Toast.makeText(requireContext(), 
                            "Article deleted successfully", android.widget.Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        android.widget.Toast.makeText(requireContext(), 
                            "Failed to delete: " + error, android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
