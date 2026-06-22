package com.haset.hasetapp.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.FileProvider;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.haset.hasetapp.R;
import com.haset.hasetapp.database.entities.ArticlePostEntity;
import com.haset.hasetapp.firebase.ArticlePostHelper;
import com.haset.hasetapp.utils.CloudinaryUploadHelper;
import com.haset.hasetapp.fragments.CreatePostStep1Fragment;
import com.haset.hasetapp.fragments.CreatePostStep2Fragment;
import com.haset.hasetapp.fragments.CreatePostStep3Fragment;
import com.haset.hasetapp.utils.AuditLogger;
import com.haset.hasetapp.utils.PreferenceManager;
import com.haset.hasetapp.viewmodels.ArticleViewModel;

import java.io.File;
import java.util.UUID;

public class CreatePostWizardActivity extends AppCompatActivity {
    private static final int TOTAL_STEPS = 3;
    
    private ViewPager2 viewPagerSteps;
    private MaterialButton btnPrevious, btnNext, btnSaveDraft;
    private TextView tvStepTitle, tvStepName, tvStepIndicator;
    private ProgressBar progressBar;
    private ImageView btnBack;
    
    // Post data that will be passed between steps
    private ArticlePostEntity currentPost;
    private String postType; // "image", "text"
    private Uri selectedMediaUri;
    private String selectedMediaPath;
    private ArticlePostHelper articlePostHelper;
    private PreferenceManager preferenceManager;

    private ArticleViewModel articleViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post_wizard);
        
        // Get post type from intent
        postType = getIntent().getStringExtra("post_type");
        if (postType == null) postType = "image";
        
        articleViewModel = new androidx.lifecycle.ViewModelProvider(this).get(ArticleViewModel.class);
        preferenceManager = new PreferenceManager(this);

        // Security check: Only admins can create/edit articles
        String userRole = preferenceManager.getUserRole();
        if (!"admin".equalsIgnoreCase(userRole)) {
            Toast.makeText(this, R.string.access_denied_admin_only, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Check if editing existing post
        String postId = getIntent().getStringExtra("post_id");
        if (postId != null && !postId.isEmpty()) {
            loadPostForEditing(postId);
        } else {
            currentPost = new ArticlePostEntity();
            currentPost.setPostId(UUID.randomUUID().toString());
            currentPost.setType(postType);
            currentPost.setStatus("draft");
            currentPost.setCreatedAt(System.currentTimeMillis());
            currentPost.setUpdatedAt(System.currentTimeMillis());
            currentPost.setAuthorId(preferenceManager.getUserId());
        }
        
        initViews();
        setupViewPager();
        setupObservers();
        updateTitle();
        updateStepIndicator(0);
    }

    private void setupObservers() {
        articleViewModel.getLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            btnNext.setEnabled(!isLoading);
        });

        articleViewModel.getError().observe(this, err -> {
            if (err != null) {
                Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
                btnNext.setText(R.string.publish);
            }
        });
    }
    
    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvStepTitle = findViewById(R.id.tvStepTitle);
        tvStepName = findViewById(R.id.tvStepName);
        tvStepIndicator = findViewById(R.id.tvStepIndicator);
        progressBar = findViewById(R.id.progressBar);
        viewPagerSteps = findViewById(R.id.viewPagerSteps);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
        btnSaveDraft = findViewById(R.id.btnSaveDraft);
        
        btnBack.setOnClickListener(v -> finish());
        btnPrevious.setOnClickListener(v -> goToPreviousStep());
        btnNext.setOnClickListener(v -> goToNextStep());
        btnSaveDraft.setOnClickListener(v -> saveDraft());
        
        // Disable swipe
        viewPagerSteps.setUserInputEnabled(false);
    }
    
    private void setupViewPager() {
        viewPagerSteps.setAdapter(new WizardStepAdapter(this));
        
        viewPagerSteps.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateStepIndicator(position);
                updateNavigationButtons(position);
                
                // If moving to preview step, refresh it
                if (position == 2) {
                    for (Fragment f : getSupportFragmentManager().getFragments()) {
                        if (f instanceof CreatePostStep3Fragment) {
                            ((CreatePostStep3Fragment) f).refreshPreview();
                            break;
                        }
                    }
                }
            }
        });
    }
    
    private void updateStepIndicator(int step) {
        tvStepIndicator.setText((step + 1) + "/" + TOTAL_STEPS);
        progressBar.setProgress((int) (((step + 1) / (float) TOTAL_STEPS) * 100));
        
        String[] stepTitles = {"Select Content", "Add Details", "Preview"};
        if (tvStepName != null) {
            tvStepName.setText(stepTitles[step]);
        }
    }
    
    private void updateNavigationButtons(int step) {
        btnPrevious.setVisibility(step > 0 ? View.VISIBLE : View.GONE);
        
        if (step == TOTAL_STEPS - 1) {
            btnNext.setText(R.string.publish);
        } else {
            btnNext.setText("Next");
        }
    }
    
    private void goToPreviousStep() {
        int currentStep = viewPagerSteps.getCurrentItem();
        if (currentStep > 0) {
            viewPagerSteps.setCurrentItem(currentStep - 1, true);
        }
    }
    
    private void goToNextStep() {
        int currentStep = viewPagerSteps.getCurrentItem();
        
        // Validate current step before proceeding
        if (!validateCurrentStep(currentStep)) {
            return;
        }
        
        if (currentStep < TOTAL_STEPS - 1) {
            viewPagerSteps.setCurrentItem(currentStep + 1, true);
        } else {
            // Last step - publish post
            publishPost();
        }
    }
    
    private boolean validateCurrentStep(int step) {
        // Get fragment from ViewPager2 adapter
        Fragment fragment = null;
        try {
            long itemId = viewPagerSteps.getAdapter().getItemId(step);
            fragment = getSupportFragmentManager().findFragmentByTag("f" + itemId);
        } catch (Exception e) {
            // Fallback: iterate through fragments
            for (Fragment f : getSupportFragmentManager().getFragments()) {
                if (step == 0 && f instanceof CreatePostStep1Fragment) {
                    fragment = f;
                    break;
                } else if (step == 1 && f instanceof CreatePostStep2Fragment) {
                    fragment = f;
                    break;
                }
            }
        }
        
        switch (step) {
            case 0:
                if (fragment instanceof CreatePostStep1Fragment) {
                    return ((CreatePostStep1Fragment) fragment).validateStep();
                }
                break;
            case 1:
                if (fragment instanceof CreatePostStep2Fragment) {
                    return ((CreatePostStep2Fragment) fragment).validateStep();
                }
                break;
        }
        return true;
    }
    
    private void saveDraft() {
        // Collect data from all steps
        collectDataFromSteps();
        
        // For drafts, title is optional but recommended
        if (currentPost.getTitle() == null || currentPost.getTitle().isEmpty()) {
            currentPost.setTitle("Untitled Draft");
        }
        
        currentPost.setStatus("draft");
        
        // For drafts, we can save with local paths (media will be uploaded when published)
        // But if media already has Cloudinary URL, keep it
        saveDraftToFirebase();
    }
    
    private void saveDraftToFirebase() {
        if (currentPost.getTitle() == null || currentPost.getTitle().isEmpty()) {
            currentPost.setTitle("Untitled Draft");
        }
        currentPost.setStatus("draft");
        
        articleViewModel.createArticle(currentPost, new ArticlePostHelper.OnCompleteListener<String>() {
            @Override
            public void onSuccess(String result) {
                AuditLogger.getInstance(CreatePostWizardActivity.this).logPostAction("SAVE_DRAFT", result, postType);
                Toast.makeText(CreatePostWizardActivity.this, R.string.draft_saved, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onError(String error) {
                // Handled by observer
            }
        });
    }
    
    private void publishPost() {
        // Collect data from all steps
        collectDataFromSteps();
        
        // Validate required fields
        if (currentPost.getTitle() == null || currentPost.getTitle().isEmpty()) {
            Toast.makeText(this, R.string.title_required, Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (currentPost.getDescription() == null || currentPost.getDescription().isEmpty()) {
            Toast.makeText(this, R.string.description_required, Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (currentPost.getProfileName() == null || currentPost.getProfileName().isEmpty()) {
            Toast.makeText(this, R.string.profile_name_required, Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (currentPost.getTags() == null || currentPost.getTags().isEmpty()) {
            Toast.makeText(this, R.string.tags_required, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check if media is selected
        if (!"text".equals(postType) && (selectedMediaPath == null || selectedMediaPath.isEmpty())) {
            Toast.makeText(this, R.string.please_select_image_or_use_text, Toast.LENGTH_SHORT).show();
            return;
        }
        
        currentPost.setStatus("published");
        currentPost.setUpdatedAt(System.currentTimeMillis());
        
        // Upload media to Cloudinary before saving, or skip if text-only
        if ("text".equals(postType)) {
            savePostToFirebase();
        } else {
            uploadMediaAndPublish();
        }
    }
    
    /**
     * Upload media files to Cloudinary, then publish the post
     */
    private void uploadMediaAndPublish() {
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        btnNext.setEnabled(false);
        btnNext.setText(R.string.uploading);
        
        // Check if media already has Cloudinary URL (for edits)
        String existingPostId = getIntent().getStringExtra("post_id");
        boolean hasCloudinaryUrl = false;
        
        if (existingPostId != null && !existingPostId.isEmpty()) {
            // Check if post already has Cloudinary URLs
            if ("image".equals(postType) && currentPost.getImageUrl() != null && !currentPost.getImageUrl().isEmpty()) {
                hasCloudinaryUrl = true;
            }
        }
        
        // If media already uploaded and not changed, skip upload
        if (hasCloudinaryUrl && selectedMediaPath != null) {
            File mediaFile = new File(selectedMediaPath);
            if (mediaFile.exists()) {
                savePostToFirebase();
                return;
            }
        }
        
        // Upload media file - get proper URI
        Uri mediaUri = selectedMediaUri;
        
        if (mediaUri != null && (mediaUri.getScheme() != null && mediaUri.getScheme().equals("content"))) {
            android.util.Log.d("CreatePostWizard", "Using content URI: " + mediaUri.toString());
        } else if (selectedMediaPath != null) {
            File mediaFile = new File(selectedMediaPath);
            if (mediaFile.exists()) {
                try {
                    mediaUri = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".fileprovider",
                        mediaFile
                    );
                } catch (Exception e) {
                    Toast.makeText(this, "Error accessing file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    btnNext.setEnabled(true);
                    btnNext.setText(R.string.publish);
                    return;
                }
            } else {
                Toast.makeText(this, R.string.media_file_not_found, Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                btnNext.setEnabled(true);
                btnNext.setText(R.string.publish);
                return;
            }
        }
        
        if (mediaUri == null) {
            Toast.makeText(this, R.string.no_media_selected, Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            btnNext.setEnabled(true);
            btnNext.setText(R.string.publish);
            return;
        }
        
        // Determinate file type and name
        String fileType = "image";
        String fileName = new File(selectedMediaPath).getName();
        if (fileName == null || fileName.isEmpty()) {
            fileName = "image_" + System.currentTimeMillis() + ".jpg";
        }
        
        // Upload to Cloudinary
        CloudinaryUploadHelper.uploadFile(this, mediaUri, fileType, fileName, "article_posts", 
            new CloudinaryUploadHelper.OnFileUploadListener() {
                @Override
                public void onUploadStart() {}
                
                @Override
                public void onUploadProgress(double progress) {
                    progressBar.setProgress((int) progress);
                }
                
                @Override
                public void onUploadSuccess(String downloadUrl, String uploadedFileName) {
                    currentPost.setImageUrl(downloadUrl);
                    savePostToFirebase();
                }
                
                @Override
                public void onUploadError(String error) {
                    Toast.makeText(CreatePostWizardActivity.this, "Upload failed: " + error, Toast.LENGTH_LONG).show();
                    progressBar.setVisibility(View.GONE);
                    btnNext.setEnabled(true);
                    btnNext.setText(R.string.publish);
                }
            });
    }
    
    /**
     * Save post to Firebase Database
     */
    private void savePostToFirebase() {
        articleViewModel.createArticle(currentPost, new ArticlePostHelper.OnCompleteListener<String>() {
            @Override
            public void onSuccess(String result) {
                AuditLogger.getInstance(CreatePostWizardActivity.this).logPostCreated(result, currentPost.getTitle(), postType);
                Toast.makeText(CreatePostWizardActivity.this, R.string.article_published, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onError(String error) {
                // Handled by observer
            }
        });
    }
    
    private void collectDataFromSteps() {
        if (currentPost.getPostId() == null || currentPost.getPostId().isEmpty()) {
            currentPost.setPostId(UUID.randomUUID().toString());
        }
        
        if (currentPost.getType() == null || currentPost.getType().isEmpty()) {
            currentPost.setType(postType);
        }
        
        if (currentPost.getCreatedAt() == 0) {
            currentPost.setCreatedAt(System.currentTimeMillis());
        }
        currentPost.setUpdatedAt(System.currentTimeMillis());
        
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof CreatePostStep1Fragment) {
                ((CreatePostStep1Fragment) fragment).collectData(currentPost);
            } else if (fragment instanceof CreatePostStep2Fragment) {
                ((CreatePostStep2Fragment) fragment).collectData(currentPost);
            }
        }
        
        if (selectedMediaPath != null && !selectedMediaPath.isEmpty()) {
            currentPost.setImagePath(selectedMediaPath);
        }
        
        if (currentPost.getProfileName() == null || currentPost.getProfileName().isEmpty()) {
            currentPost.setProfileName("HASET Admin");
        }
        if (currentPost.getTags() == null) {
            currentPost.setTags("");
        }
    }
    
    private void loadPostForEditing(String postId) {
        articleViewModel.getArticleById(postId, new ArticlePostHelper.OnCompleteListener<ArticlePostEntity>() {
            @Override
            public void onSuccess(ArticlePostEntity post) {
                if (post != null) {
                    currentPost = post;
                    postType = post.getType() != null ? post.getType() : "image";
                    
                    // Ensure authorId is set (for posts created before authorId was added)
                    if (currentPost.getAuthorId() == null || currentPost.getAuthorId().isEmpty()) {
                        currentPost.setAuthorId(preferenceManager.getUserId());
                    }
                    
                    if (post.getImagePath() != null && !post.getImagePath().isEmpty()) {
                        selectedMediaPath = post.getImagePath();
                        File imageFile = new File(selectedMediaPath);
                        if (imageFile.exists()) {
                            selectedMediaUri = Uri.fromFile(imageFile);
                        }
                    }
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(CreatePostWizardActivity.this, "Failed to load article: " + error, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
    
    public ArticlePostEntity getCurrentPost() {
        return currentPost;
    }
    
    public String getPostType() {
        return postType;
    }
    
    public void setPostType(String postType) {
        this.postType = postType;
        if (currentPost != null) {
            currentPost.setType(postType);
        }
        updateTitle();
    }
    
    private void updateTitle() {
        if (tvStepTitle != null) {
            tvStepTitle.setText(R.string.create_article);
        }
    }
    
    public Uri getSelectedMediaUri() {
        return selectedMediaUri;
    }
    
    public void setSelectedMediaUri(Uri uri) {
        this.selectedMediaUri = uri;
    }
    
    public String getSelectedMediaPath() {
        return selectedMediaPath;
    }
    
    public void setSelectedMediaPath(String path) {
        this.selectedMediaPath = path;
    }
    
    public static class WizardStepAdapter extends FragmentStateAdapter {
        public WizardStepAdapter(FragmentActivity fa) {
            super(fa);
        }
        
        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: return new CreatePostStep1Fragment();
                case 1: return new CreatePostStep2Fragment();
                case 2: return new CreatePostStep3Fragment();
                default: return new CreatePostStep1Fragment();
            }
        }
        
        @Override
        public int getItemCount() {
            return TOTAL_STEPS;
        }
    }
}

