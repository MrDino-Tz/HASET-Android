package com.haset.hasetapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.haset.hasetapp.R;

public class ArticleTabFragment extends Fragment {
    private static final String ARG_TITLE = "tab_title";
    private static final String ARG_HIGHLIGHT_ARTICLE_ID = "highlight_article_id";
    private String tabName;
    private String highlightArticleId;

    public static ArticleTabFragment newInstance(String title) {
        return newInstance(title, null);
    }

    public static ArticleTabFragment newInstance(String title, String articleId) {
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_HIGHLIGHT_ARTICLE_ID, articleId);
        ArticleTabFragment frag = new ArticleTabFragment();
        frag.setArguments(args);
        return frag;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        tabName = getArguments() != null ? getArguments().getString(ARG_TITLE, "Articles") : "Articles";
        highlightArticleId = getArguments() != null ? getArguments().getString(ARG_HIGHLIGHT_ARTICLE_ID) : null;
        
        FrameLayout wrapper = new FrameLayout(requireContext());
        wrapper.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        int containerId = View.generateViewId();
        wrapper.setId(containerId);
        
        getChildFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fade_through_enter, R.anim.fade_through_exit)
                .replace(containerId, com.haset.hasetapp.fragments.ArticleFragment.newInstance(tabName, highlightArticleId))
                .commitNowAllowingStateLoss();
        
        return wrapper;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tabName = getArguments() != null ? getArguments().getString(ARG_TITLE, "") : "";
        
        // Only set text if this is not a special tab (Articles) that uses a wrapper
        if (!"Articles".equalsIgnoreCase(tabName) && !"posts".equalsIgnoreCase(tabName)) {
            TextView tvTabTitle = view.findViewById(R.id.tvTabTitle);
            TextView tvTabContent = view.findViewById(R.id.tvTabContent);
            
            // Check if views exist before using them
            if (tvTabTitle != null && tvTabContent != null) {
                tvTabTitle.setText(tabName);
                tvTabContent.setText(getString(R.string.tab_description, tabName));
            }
        }
    }
}
