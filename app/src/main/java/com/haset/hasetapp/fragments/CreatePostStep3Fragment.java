package com.haset.hasetapp.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.haset.hasetapp.R;
import com.haset.hasetapp.activities.CreatePostWizardActivity;
import com.haset.hasetapp.database.entities.ArticlePostEntity;

public class CreatePostStep3Fragment extends Fragment {
    private ImageView ivPreviewImage;
    private TextView tvTitle, tvDescription, tvProfileName, tvTags, tvType, tvStatus;
    private ViewGroup containerImage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_post_step3, container, false);
        
        initViews(view);
        loadPreviewData();
        
        return view;
    }
    
    private void initViews(View view) {
        ivPreviewImage = view.findViewById(R.id.ivPreviewImage);
        tvTitle = view.findViewById(R.id.tvTitle);
        tvDescription = view.findViewById(R.id.tvDescription);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvTags = view.findViewById(R.id.tvTags);
        tvType = view.findViewById(R.id.tvType);
        tvStatus = view.findViewById(R.id.tvStatus);
        containerImage = view.findViewById(R.id.containerImage);
        
        // Hide video view and music if they exist
        View vvPreviewVideo = view.findViewById(R.id.vvPreviewVideo);
        View containerVideo = view.findViewById(R.id.containerVideo);
        View tvMusic = view.findViewById(R.id.tvMusic);
        if (vvPreviewVideo != null) vvPreviewVideo.setVisibility(View.GONE);
        if (containerVideo != null) containerVideo.setVisibility(View.GONE);
        if (tvMusic != null) tvMusic.setVisibility(View.GONE);
    }
    
    public void refreshPreview() {
        if (getView() != null) {
            loadPreviewData();
        }
    }
    
    private void loadPreviewData() {
        CreatePostWizardActivity activity = (CreatePostWizardActivity) requireActivity();
        ArticlePostEntity post = activity.getCurrentPost();
        String postType = activity.getPostType();
        Uri mediaUri = activity.getSelectedMediaUri();
        
        // Collect latest data from Step 2
        // We look through fragments because fragment tags in ViewPager2 are not guaranteed to be simple "f1"
        for (Fragment f : getParentFragmentManager().getFragments()) {
            if (f instanceof CreatePostStep2Fragment) {
                ((CreatePostStep2Fragment) f).collectData(post);
                break;
            }
        }
        
        // Display media preview or hide it for text articles
        if ("text".equals(postType)) {
            containerImage.setVisibility(View.GONE);
            tvType.setText(R.string.article_type_text);
            tvType.setTextColor(getResources().getColor(R.color.green_primary));
        } else {
            if (mediaUri != null) {
                containerImage.setVisibility(View.VISIBLE);
                ivPreviewImage.setImageURI(mediaUri);
            } else {
                containerImage.setVisibility(View.GONE);
            }
            tvType.setText(R.string.article_type_image);
            tvType.setTextColor(getResources().getColor(R.color.blue_primary));
        }
        
        // Display text data
        tvTitle.setText(post.getTitle() != null && !post.getTitle().isEmpty() ? post.getTitle() : "No title");
        tvDescription.setText(post.getDescription() != null && !post.getDescription().isEmpty() ? post.getDescription() : "No description");
        tvProfileName.setText(post.getProfileName() != null && !post.getProfileName().isEmpty() ? post.getProfileName() : "No profile name");
        tvTags.setText(post.getTags() != null && !post.getTags().isEmpty() ? post.getTags() : "No tags");
        
        tvStatus.setText(R.string.ready_to_publish);
    }
}

