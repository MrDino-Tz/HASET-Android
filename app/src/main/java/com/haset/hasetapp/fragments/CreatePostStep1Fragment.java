                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    package com.haset.hasetapp.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.haset.hasetapp.R;
import com.haset.hasetapp.activities.CreatePostWizardActivity;
import com.haset.hasetapp.database.entities.ArticlePostEntity;
import com.haset.hasetapp.utils.CloudinaryUploadHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CreatePostStep1Fragment extends Fragment {
    private static final int REQUEST_CAMERA_IMAGE = 1001;
    private static final int REQUEST_GALLERY_IMAGE = 1002;
    private static final int REQUEST_PERMISSIONS = 1005;
    
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    
    private MaterialButton btnOpenMediaOptions;
    private ImageView ivPreviewImage;
    private TextView tvFileInfo, tvEmptyState;
    private ViewGroup containerImage;
    
    private Uri currentMediaUri;
    private String currentMediaPath;
    private String postType;
    private String uploadedImageUrl; // Cloudinary URL for uploaded image
    private boolean isUploading = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_post_step1, container, false);
        
        postType = ((CreatePostWizardActivity) requireActivity()).getPostType();
        
        initViews(view);
        setupClickListeners();
        loadExistingMedia();
        
        // Auto-show options if no media is selected yet
        if (currentMediaPath == null || currentMediaPath.isEmpty()) {
            view.post(() -> {
                if (isAdded()) {
                    showMediaOptions();
                }
            });
        }
        
        return view;
    }
    
    private void loadExistingMedia() {
        CreatePostWizardActivity activity = (CreatePostWizardActivity) requireActivity();
        ArticlePostEntity post = activity.getCurrentPost();
        Uri mediaUri = activity.getSelectedMediaUri();
        String mediaPath = activity.getSelectedMediaPath();
        
        if (post != null && mediaPath != null && !mediaPath.isEmpty()) {
            File mediaFile = new File(mediaPath);
            if (mediaFile.exists()) {
                currentMediaPath = mediaPath;
                if (mediaUri == null) {
                    mediaUri = Uri.fromFile(mediaFile);
                    activity.setSelectedMediaUri(mediaUri);
                }
                currentMediaUri = mediaUri;
                
                long fileSize = mediaFile.length();
                displayPreview(mediaUri, true, fileSize);
            }
        }
    }
    
    private void initViews(View view) {
        btnOpenMediaOptions = view.findViewById(R.id.btnOpenMediaOptions);
        ivPreviewImage = view.findViewById(R.id.ivPreviewImage);
        tvFileInfo = view.findViewById(R.id.tvFileInfo);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        containerImage = view.findViewById(R.id.containerImage);
        
        btnOpenMediaOptions.setText(R.string.select_content);
        
        // Hide video container if it exists in layout
        View containerVideo = view.findViewById(R.id.containerVideo);
        if (containerVideo != null) {
            containerVideo.setVisibility(View.GONE);
        }
    }
    
    private void setupClickListeners() {
        btnOpenMediaOptions.setOnClickListener(v -> showMediaOptions());
    }

    private void showMediaOptions() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = 
                new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_select_media, null);
        dialog.setContentView(view);

        View actionCamera = view.findViewById(R.id.actionCamera);
        View actionGalleryImage = view.findViewById(R.id.actionGalleryImage);
        
        // Hide video options
        View actionRecordVideo = view.findViewById(R.id.actionRecordVideo);
        View actionGalleryVideo = view.findViewById(R.id.actionGalleryVideo);
        if (actionRecordVideo != null) actionRecordVideo.setVisibility(View.GONE);
        if (actionGalleryVideo != null) actionGalleryVideo.setVisibility(View.GONE);

        actionCamera.setVisibility(View.VISIBLE);
        actionGalleryImage.setVisibility(View.VISIBLE);

        actionCamera.setOnClickListener(v -> {
            dialog.dismiss();
            updatePostType("image");
            checkPermissionsAndOpenCamera(true);
        });

        actionGalleryImage.setOnClickListener(v -> {
            dialog.dismiss();
            updatePostType("image");
            openGallery(true);
        });

        View actionTextOnly = view.findViewById(R.id.actionTextOnly);
        if (actionTextOnly != null) {
            actionTextOnly.setOnClickListener(v -> {
                dialog.dismiss();
                updatePostType("text");
                clearSelectedMedia();
            });
        }

        dialog.show();
    }

    private void clearSelectedMedia() {
        currentMediaUri = null;
        currentMediaPath = null;
        uploadedImageUrl = null;
        
        CreatePostWizardActivity activity = (CreatePostWizardActivity) getActivity();
        if (activity != null) {
            activity.setSelectedMediaUri(null);
            activity.setSelectedMediaPath(null);
        }
        
        tvEmptyState.setText(R.string.text_only_selected);
        tvEmptyState.setVisibility(View.VISIBLE);
        containerImage.setVisibility(View.GONE);
        tvFileInfo.setVisibility(View.GONE);
    }

    private void updatePostType(String newType) {
        this.postType = newType;
        CreatePostWizardActivity activity = (CreatePostWizardActivity) getActivity();
        if (activity != null) {
            activity.setPostType(newType);
        }
        
        containerImage.setVisibility(View.VISIBLE);
    }
    
    private void checkPermissionsAndOpenCamera(boolean isImage) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS);
        } else {
            openCamera(isImage);
        }
    }
    
    private void openCamera(boolean isImage) {
        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            
            if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
                File mediaFile = createMediaFile(isImage);
                if (mediaFile != null) {
                    Uri mediaUri = FileProvider.getUriForFile(requireContext(),
                            requireContext().getPackageName() + ".fileprovider",
                            mediaFile);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, mediaUri);
                    startActivityForResult(intent, REQUEST_CAMERA_IMAGE);
                }
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Failed to open camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void openGallery(boolean isImage) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_GALLERY_IMAGE);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera(true);
            } else {
                Toast.makeText(requireContext(), R.string.camera_permissions_required, Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == Activity.RESULT_OK) {
            Uri mediaUri = null;
            boolean isImage = true;
            
            switch (requestCode) {
                case REQUEST_CAMERA_IMAGE:
                case REQUEST_GALLERY_IMAGE:
                    if (requestCode == REQUEST_GALLERY_IMAGE && data != null) {
                        mediaUri = data.getData();
                    } else {
                        mediaUri = currentMediaUri;
                    }
                    break;
            }
            
            if (mediaUri != null) {
                processSelectedMedia(mediaUri, isImage);
            }
        }
    }
    
    private void processSelectedMedia(Uri mediaUri, boolean isImage) {
        try {
            // Copy file to app storage
            String filePath = copyMediaToAppStorage(mediaUri, isImage);
            
            if (filePath == null) {
                Toast.makeText(requireContext(), R.string.failed_to_process_media, Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Validate file
            File file = new File(filePath);
            if (!file.exists()) {
                Toast.makeText(requireContext(), R.string.file_not_found, Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Check file size
            long fileSize = file.length();
            if (fileSize > MAX_FILE_SIZE) {
                Toast.makeText(requireContext(), R.string.file_size_exceeds_limit, Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Store in activity
            CreatePostWizardActivity activity = (CreatePostWizardActivity) requireActivity();
            activity.setSelectedMediaUri(mediaUri);
            activity.setSelectedMediaPath(filePath);
            
            // Update UI
            currentMediaUri = mediaUri;
            currentMediaPath = filePath;
            
            displayPreview(mediaUri, isImage, fileSize);
            
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error processing media: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void displayPreview(Uri mediaUri, boolean isImage, long fileSize) {
        tvEmptyState.setVisibility(View.GONE);
        containerImage.setVisibility(View.VISIBLE);
        
        try {
            if (mediaUri != null) {
                String uriString = mediaUri.toString();
                if (uriString.startsWith("file://") || uriString.startsWith("/")) {
                    String filePath = uriString.startsWith("file://") 
                        ? uriString.substring(7) 
                        : uriString;
                    File imageFile = new File(filePath);
                    if (imageFile.exists()) {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = 2; // Reduce memory usage
                        android.graphics.Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
                        if (bitmap != null) {
                            ivPreviewImage.setImageBitmap(bitmap);
                        } else {
                            ivPreviewImage.setImageURI(mediaUri);
                        }
                    } else {
                        ivPreviewImage.setImageURI(mediaUri);
                    }
                } else {
                    ivPreviewImage.setImageURI(mediaUri);
                }
            }
        } catch (Exception e) {
            ivPreviewImage.setImageURI(mediaUri);
        }
        
        String sizeText = formatFileSize(fileSize);
        tvFileInfo.setText("File: " + sizeText);
        tvFileInfo.setVisibility(View.VISIBLE);
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return (bytes / (1024 * 1024)) + " MB";
    }
    
    private File createMediaFile(boolean isImage) {
        try {
            File mediaDir = new File(requireContext().getFilesDir(), "article_media");
            if (!mediaDir.exists()) mediaDir.mkdirs();
            
            String fileName = "IMG_" + System.currentTimeMillis() + ".jpg";
            File mediaFile = new File(mediaDir, fileName);
            currentMediaUri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    mediaFile);
            return mediaFile;
        } catch (Exception e) {
            return null;
        }
    }
    
    private String copyMediaToAppStorage(Uri sourceUri, boolean isImage) {
        try {
            File mediaDir = new File(requireContext().getFilesDir(), "article_media");
            if (!mediaDir.exists()) mediaDir.mkdirs();
            
            String fileName = "IMG_" + System.currentTimeMillis() + ".jpg";
            File destFile = new File(mediaDir, fileName);
            
            InputStream inputStream = requireContext().getContentResolver().openInputStream(sourceUri);
            OutputStream outputStream = new FileOutputStream(destFile);
            
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            
            inputStream.close();
            outputStream.close();
            
            return destFile.getAbsolutePath();
        } catch (IOException e) {
            return null;
        }
    }
    
    public boolean validateStep() {
        if ("text".equals(postType)) {
            return true; // No image required for text type
        }
        
        if (currentMediaPath == null || currentMediaPath.isEmpty()) {
            Toast.makeText(requireContext(), R.string.please_select_image_or_text_only, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
    
    public void collectData(ArticlePostEntity post) {
        CreatePostWizardActivity activity = (CreatePostWizardActivity) requireActivity();
        if (currentMediaPath != null) {
            activity.setSelectedMediaPath(currentMediaPath);
            post.setImagePath(currentMediaPath);
        }
        
        if (uploadedImageUrl != null && !uploadedImageUrl.isEmpty()) {
            post.setImageUrl(uploadedImageUrl);
        }
    }
    
    public void uploadMediaToCloudinary(CloudinaryUploadHelper.OnFileUploadListener listener) {
        if (currentMediaUri == null) {
            if (listener != null) listener.onUploadError("No image selected");
            return;
        }
        
        if (isUploading) {
            if (listener != null) listener.onUploadError("Upload already in progress");
            return;
        }
        
        isUploading = true;
        String fileName = "article_image_" + System.currentTimeMillis() + ".jpg";
        
        CloudinaryUploadHelper.uploadFile(
            requireContext(),
            currentMediaUri,
            "image",
            fileName,
            "article_posts",
            new CloudinaryUploadHelper.OnFileUploadListener() {
                @Override
                public void onUploadStart() {
                    if (listener != null) listener.onUploadStart();
                }
                
                @Override
                public void onUploadProgress(double progress) {
                    if (listener != null) listener.onUploadProgress(progress);
                }
                
                @Override
                public void onUploadSuccess(String downloadUrl, String fileName) {
                    isUploading = false;
                    uploadedImageUrl = downloadUrl;
                    if (listener != null) listener.onUploadSuccess(downloadUrl, fileName);
                }
                
                @Override
                public void onUploadError(String error) {
                    isUploading = false;
                    if (listener != null) listener.onUploadError(error);
                }
            }
        );
    }
    
    public boolean isMediaUploaded() {
        return uploadedImageUrl != null && !uploadedImageUrl.isEmpty();
    }
    
    public String getUploadedMediaUrl() {
        return uploadedImageUrl;
    }
}

