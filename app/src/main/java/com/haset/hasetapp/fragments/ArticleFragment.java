package com.haset.hasetapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.gson.Gson;
import com.haset.hasetapp.R;
import com.haset.hasetapp.activities.ArticleDetailActivity;
import com.haset.hasetapp.adapters.PostFeedAdapter;
import com.haset.hasetapp.firebase.ArticlePostHelper;
import com.haset.hasetapp.database.entities.ArticlePostEntity;
import com.haset.hasetapp.viewmodels.ArticleViewModel;

import java.util.ArrayList;
import java.util.List;

public class ArticleFragment extends Fragment {

    private static final String ARG_HIGHLIGHT_ARTICLE_ID = "highlight_article_id";
    
    private RecyclerView rvPosts;
    private SwipeRefreshLayout swipeRefresh;
    private View layoutEmptyState;
    private android.widget.TextView tvEmptyTitle, tvEmptySubtitle;
    private android.widget.ImageView ivEmptyIcon;
    private PostFeedAdapter articleAdapter;
    private Gson gson = new Gson();
    private String highlightArticleId;
    private boolean shouldScrollToHighlighted = false;

    public static ArticleFragment newInstance(String tabTitle) {
        return newInstance(tabTitle, null);
    }

    public static ArticleFragment newInstance(String tabTitle, String articleId) {
        ArticleFragment fragment = new ArticleFragment();
        Bundle args = new Bundle();
        args.putString("tab_title", tabTitle);
        args.putString(ARG_HIGHLIGHT_ARTICLE_ID, articleId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_article, container, false);
    }

    private ArticleViewModel viewModel;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        highlightArticleId = getArguments() != null ? getArguments().getString(ARG_HIGHLIGHT_ARTICLE_ID) : null;

        rvPosts = view.findViewById(R.id.rvPosts);
        rvPosts.setLayoutManager(new LinearLayoutManager(getContext()));

        articleAdapter = new PostFeedAdapter(new ArrayList<>(), requireContext());

        articleAdapter.setOnArticleClickListener(article -> {
            Intent intent = new Intent(requireContext(), ArticleDetailActivity.class);
            intent.putExtra(ArticleDetailActivity.EXTRA_ARTICLE, gson.toJson(article));
            startActivity(intent);
        });

        rvPosts.setAdapter(articleAdapter);

        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        tvEmptyTitle = view.findViewById(R.id.tvEmptyStateTitle);
        tvEmptySubtitle = view.findViewById(R.id.tvEmptyStateSubtitle);
        ivEmptyIcon = view.findViewById(R.id.ivEmptyStateIcon);

        viewModel = new androidx.lifecycle.ViewModelProvider(requireActivity()).get(ArticleViewModel.class);
        setupSwipeRefresh(view);
        setupObservers();
    }

    private void setupSwipeRefresh(View view) {
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        swipeRefresh.setColorSchemeResources(R.color.green_primary);
        swipeRefresh.setOnRefreshListener(() -> {
            String tabTitle = getArguments() != null ? getArguments().getString("tab_title") : "Articles";
            if ("Saved".equalsIgnoreCase(tabTitle)) {
                String userId = new com.haset.hasetapp.utils.PreferenceManager(requireContext()).getUserId();
                if (userId != null) {
                    viewModel.refreshSavedArticles(userId);
                } else {
                    swipeRefresh.setRefreshing(false);
                }
            } else {
                viewModel.refreshPublishedArticles();
            }
        });
    }

    private void setupObservers() {
        String tabTitle = getArguments() != null ? getArguments().getString("tab_title") : "Articles";
        
        if ("Saved".equalsIgnoreCase(tabTitle)) {
            String userId = new com.haset.hasetapp.utils.PreferenceManager(requireContext()).getUserId();
            if (userId != null) {
                viewModel.getSavedArticles(userId).observe(getViewLifecycleOwner(), posts -> {
                    hideShimmerLoading();
                    if (posts != null && !posts.isEmpty()) {
                        articleAdapter.setPosts(posts);
                        layoutEmptyState.setVisibility(View.GONE);
                    } else {
                        showEmptyState("Saved");
                    }
                });
            }
        } else {
            viewModel.getPublishedArticles().observe(getViewLifecycleOwner(), posts -> {
                hideShimmerLoading();
                if (posts != null && !posts.isEmpty()) {
                    List<ArticlePostEntity> filteredPosts;
                    if ("Health Tips".equalsIgnoreCase(tabTitle)) {
                        filteredPosts = new ArrayList<>();
                        for (ArticlePostEntity post : posts) {
                            String tags = post.getTags() != null ? post.getTags().toLowerCase() : "";
                            if (tags.contains("#health") || tags.contains("#tip") || tags.contains("#medical")) {
                                filteredPosts.add(post);
                            }
                        }
                    } else {
                        filteredPosts = posts;
                    }
                    
                    if (filteredPosts.isEmpty()) {
                        showEmptyState(tabTitle);
                    } else {
                        articleAdapter.setPosts(filteredPosts);
                        layoutEmptyState.setVisibility(View.GONE);
                        scrollToHighlightedArticle(filteredPosts);
                    }
                } else {
                    showEmptyState(tabTitle);
                }
            });
        }

        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                showShimmerLoading();
                layoutEmptyState.setVisibility(View.GONE);
            } else {
                hideShimmerLoading();
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) {
                hideShimmerLoading();
                showErrorState(err);
            }
        });
    }

    private void showEmptyState(String tabTitle) {
        articleAdapter.setPosts(new ArrayList<>());
        layoutEmptyState.setVisibility(View.VISIBLE);
        
        if ("Health Tips".equalsIgnoreCase(tabTitle)) {
            tvEmptyTitle.setText("No Health Tips Yet");
            tvEmptySubtitle.setText("We are currently curating the best health tips for you. Check back later!");
            ivEmptyIcon.setImageResource(R.drawable.ic_health_assessment); // Using an existing health related icon
        } else if ("Saved".equalsIgnoreCase(tabTitle)) {
            tvEmptyTitle.setText("No Saved Articles");
            tvEmptySubtitle.setText("Start bookmarking your favorite health articles and they will appear right here.");
            ivEmptyIcon.setImageResource(R.drawable.ic_bookmark_outline);
        } else {
            tvEmptyTitle.setText("No Articles Found");
            tvEmptySubtitle.setText("We haven't published any articles in this section yet. Stay tuned for updates!");
            ivEmptyIcon.setImageResource(R.drawable.ic_no_data);
        }
    }

    private void scrollToHighlightedArticle(List<ArticlePostEntity> posts) {
        if (highlightArticleId == null || posts == null || posts.isEmpty()) return;
        
        for (int i = 0; i < posts.size(); i++) {
            ArticlePostEntity post = posts.get(i);
            if (post != null && highlightArticleId.equals(post.getPostId())) {
                final int position = i;
                rvPosts.post(() -> {
                    if (rvPosts != null) {
                        rvPosts.scrollToPosition(position);
                        shouldScrollToHighlighted = true;
                    }
                });
                break;
            }
        }
    }

    private void showErrorState(String error) {
        articleAdapter.setPosts(new ArrayList<>());
        layoutEmptyState.setVisibility(View.VISIBLE);
        tvEmptyTitle.setText("Error Loading Articles");
        tvEmptySubtitle.setText("Could not load content: " + error + ". Please check your connection and try again.");
        ivEmptyIcon.setImageResource(R.drawable.ic_no_data); // Or an error icon if available
    }

    private void loadPosts() {
        if (getArguments() == null) return;
        String tabTitle = getArguments().getString("tab_title", "Articles");
        
        if ("Saved".equalsIgnoreCase(tabTitle)) {
            String userId = new com.haset.hasetapp.utils.PreferenceManager(requireContext()).getUserId();
            if (userId != null) {
                viewModel.refreshSavedArticles(userId);
            }
        } else {
            viewModel.refreshPublishedArticles();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPosts();
    }
    
    private void showShimmerLoading() {
        if (articleAdapter != null) {
            articleAdapter.setLoading(true);
        }
    }
    
    private void hideShimmerLoading() {
        if (articleAdapter != null) {
            articleAdapter.setLoading(false);
        }
    }
}
