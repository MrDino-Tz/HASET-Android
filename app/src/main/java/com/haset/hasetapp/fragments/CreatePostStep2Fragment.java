package com.haset.hasetapp.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.haset.hasetapp.R;
import com.haset.hasetapp.activities.CreatePostWizardActivity;
import com.haset.hasetapp.database.entities.ArticlePostEntity;

public class CreatePostStep2Fragment extends Fragment {
    private EditText etTitle, etDescription, etProfileName, etTags;
    private TextView tvTitleCount, tvDescriptionCount;
    private String postType;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_post_step2, container, false);
        
        postType = ((CreatePostWizardActivity) requireActivity()).getPostType();
        
        initViews(view);
        setupTextWatchers();
        loadExistingData();
        
        // Hide music field
        View containerMusic = view.findViewById(R.id.containerMusic);
        if (containerMusic != null) {
            containerMusic.setVisibility(View.GONE);
        }
        
        return view;
    }
    
    private void initViews(View view) {
        etTitle = view.findViewById(R.id.etTitle);
        etDescription = view.findViewById(R.id.etDescription);
        etProfileName = view.findViewById(R.id.etProfileName);
        etTags = view.findViewById(R.id.etTags);
        tvTitleCount = view.findViewById(R.id.tvTitleCount);
        tvDescriptionCount = view.findViewById(R.id.tvDescriptionCount);
    }
    
    private void setupTextWatchers() {
        etTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvTitleCount.setText(s.length() + "/100");
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        etDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvDescriptionCount.setText(s.length() + "/500");
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    private void loadExistingData() {
        ArticlePostEntity post = ((CreatePostWizardActivity) requireActivity()).getCurrentPost();
        if (post != null) {
            if (post.getTitle() != null) etTitle.setText(post.getTitle());
            if (post.getDescription() != null) etDescription.setText(post.getDescription());
            if (post.getProfileName() != null) etProfileName.setText(post.getProfileName());
            if (post.getTags() != null) etTags.setText(post.getTags());
        }
    }
    
    public boolean validateStep() {
        if (etTitle.getText().toString().trim().isEmpty()) {
            etTitle.setError(getString(R.string.title_required));
            etTitle.requestFocus();
            return false;
        }
        
        if (etDescription.getText().toString().trim().isEmpty()) {
            etDescription.setError(getString(R.string.description_required));
            etDescription.requestFocus();
            return false;
        }
        
        if (etProfileName.getText().toString().trim().isEmpty()) {
            etProfileName.setError(getString(R.string.profile_name_required));
            etProfileName.requestFocus();
            return false;
        }
        
        if (etTags.getText().toString().trim().isEmpty()) {
            etTags.setError(getString(R.string.tags_required));
            etTags.requestFocus();
            return false;
        }
        
        return true;
    }
    
    public void collectData(ArticlePostEntity post) {
        post.setTitle(etTitle.getText().toString().trim());
        post.setDescription(etDescription.getText().toString().trim());
        post.setProfileName(etProfileName.getText().toString().trim());
        post.setTags(etTags.getText().toString().trim());
    }
}

