package com.haset.hasetapp.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide; // Added Glide import
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.haset.hasetapp.R;
import com.haset.hasetapp.database.entities.UserEntity;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ProfilePhotoHelper {
    private static final String TAG = "ProfilePhotoHelper";
    private static final int REQUEST_CAMERA = 1001;
    private static final int REQUEST_GALLERY = 1002;
    private static final int REQUEST_PERMISSIONS = 1003;
    
    private Context context;
    private PreferenceManager preferenceManager;
    private Uri currentImageUri; // Used for camera capture
    private OnPhotoSelectedListener listener;
    private FirebaseAuth mAuth; // Firebase Auth instance
    private DatabaseReference usersRef; // Reference to Firebase Realtime Database users node
    private ActivityResultLauncher<Intent> galleryLauncher; // Activity Result Launcher for gallery
    
    public interface OnPhotoSelectedListener {
        void onPhotoSelected(Uri imageUri);
        void onPhotoError(String error);
        default void onLocalPhotoSelected(Uri localUri) {}
    }
    
    public ProfilePhotoHelper(Context context, OnPhotoSelectedListener listener) {
        this.context = context;
        this.listener = listener;
        this.preferenceManager = new PreferenceManager(context);
        this.mAuth = FirebaseHelper.getFirebaseAuth();
        this.usersRef = FirebaseHelper.getUsersRef();
        
        // Initialize Activity Result Launcher if context is a FragmentActivity
        if (context instanceof FragmentActivity) {
            FragmentActivity activity = (FragmentActivity) context;
            galleryLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImage = result.getData().getData();
                        if (selectedImage != null) {
                            if (listener != null) {
                                listener.onLocalPhotoSelected(selectedImage);
                            }
                            uploadProfilePhoto(selectedImage);
                        }
                    }
                }
            );
        }
    }
    
    /**
     * Show photo selection dialog
     */
    public void showPhotoSelectionDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.select_profile_photo));
        builder.setItems(new CharSequence[]{"Choose from Gallery", "Cancel"}, // Removed "Take Photo" for simplicity to avoid complexities with FileProvider and camera intents
                (dialog, which) -> {
                    switch (which) {
                        // case 0:
                        //    checkPermissionsAndOpenCamera();
                        //    break;
                        case 0:
                            openGallery();
                            break;
                        case 1:
                            dialog.dismiss();
                            break;
                    }
                });
        builder.show();
    }
    
    /**
     * Check permissions and open camera (removed for now)
     */
    private void checkPermissionsAndOpenCamera() {
        // if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
        //         ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
        //     
        //     if (context instanceof Activity) {
        //         ActivityCompat.requestPermissions((Activity) context,
        //                 new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
        //                 REQUEST_PERMISSIONS);
        //     }
        // } else {
        //     openCamera();
        // }
    }
    
    /**
     * Open camera to take photo (removed for now)
     */
    private void openCamera() {
        // try {
        //     Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //     if (cameraIntent.resolveActivity(context.getPackageManager()) != null) {
        //         File photoFile = createImageFile();
        //         if (photoFile != null) {
        //             currentImageUri = FileProvider.getUriForFile(context,
        //                     context.getPackageName() + ".fileprovider",
        //                     photoFile);
        //             cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentImageUri);
        //             
        //             if (context instanceof Activity) {
        //                 ((Activity) context).startActivityForResult(cameraIntent, REQUEST_CAMERA);
        //             }
        //         }
        //     }
        // } catch (Exception e) {
        //     Log.e(TAG, "Error opening camera", e);
        //     if (listener != null) {
        //         listener.onPhotoError("Failed to open camera");
        //     }
        // }
    }
    
    /**
     * Open gallery to select photo
     */
    private void openGallery() {
        try {
            Intent galleryIntent = new Intent(Intent.ACTION_PICK);
            galleryIntent.setType("image/*");
            
            // Use Activity Result Launcher if available (modern API)
            if (galleryLauncher != null && context instanceof FragmentActivity) {
                galleryLauncher.launch(galleryIntent);
            } else if (context instanceof Activity) {
                // Fallback to deprecated API for older Android versions
                ((Activity) context).startActivityForResult(galleryIntent, REQUEST_GALLERY);
            } else {
                Log.e(TAG, "Context is not an Activity or FragmentActivity");
                if (listener != null) {
                    listener.onPhotoError("Unable to open gallery");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening gallery", e);
            if (listener != null) {
                listener.onPhotoError("Failed to open gallery: " + e.getMessage());
            }
        }
    }
    
    /**
     * Handle activity result for photo selection
     */
    public void handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CAMERA:
                    if (currentImageUri != null) {
                        uploadProfilePhoto(currentImageUri);
                    }
                    break;
                case REQUEST_GALLERY:
                    if (data != null && data.getData() != null) {
                        Uri selectedImage = data.getData();
                        if (listener != null) listener.onLocalPhotoSelected(selectedImage);
                        uploadProfilePhoto(selectedImage);
                    }
                    break;
            }
        }
    }
    
    /**
     * Handle permission request result (camera permissions removed for now)
     */
    public void handlePermissionResult(int requestCode, int[] grantResults) {
        // if (requestCode == REQUEST_PERMISSIONS) {
        //     if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
        //             grantResults[1] == PackageManager.PERMISSION_GRANTED) {
        //         openCamera();
        //     } else {
        //         Toast.makeText(context, "Camera permissions required to take photos", Toast.LENGTH_SHORT).show();
        //     }
        // }
    }
    
    /**
     * Upload profile photo to Cloudinary and save URL to Firebase Realtime Database
     */
    private void uploadProfilePhoto(Uri imageUri) {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : preferenceManager.getUserId();
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "User ID is null or empty. Cannot upload photo.");
            if (listener != null) listener.onPhotoError("User not logged in.");
            return;
        }

        // Check if user is authenticated
        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "User is not authenticated in Firebase Auth.");
            if (listener != null) listener.onPhotoError("Please log in again to upload photos.");
            return;
        }

        if (imageUri == null) {
            Log.e(TAG, "Image URI is null.");
            if (listener != null) listener.onPhotoError("Invalid image selected.");
            return;
        }

        Log.d(TAG, "Starting profile photo upload to Cloudinary for user: " + userId);
        Log.d(TAG, "Image URI: " + imageUri.toString());

        if (listener != null) {
            listener.onLocalPhotoSelected(imageUri);
        }

        // Show progress toast
        Toast.makeText(context, R.string.uploading_profile_photo, Toast.LENGTH_SHORT).show();

        // Generate unique filename for profile photo
        String fileName = userId + "_profile.jpg";
        
        // Upload to Cloudinary
        CloudinaryUploadHelper.uploadFile(context, imageUri, "image", fileName, "profile_photos",
            new CloudinaryUploadHelper.OnFileUploadListener() {
                @Override
                public void onUploadStart() {
                    Log.d(TAG, "Profile photo upload started");
                }

                @Override
                public void onUploadProgress(double progress) {
                    Log.d(TAG, "Upload progress: " + progress + "%");
                }

                @Override
                public void onUploadSuccess(String downloadUrl, String uploadedFileName) {
                    Log.d(TAG, "=== PROFILE PHOTO UPLOAD SUCCESS ===");
                    Log.d(TAG, "Uploaded File Name: " + uploadedFileName);

                    // Save Cloudinary URL to Firebase Realtime Database
                    saveProfilePhotoUrlToDatabase(userId, downloadUrl);

                    if (listener != null) {
                        listener.onPhotoSelected(Uri.parse(downloadUrl));
                    }
                    Toast.makeText(context, R.string.profile_photo_updated, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onUploadError(String error) {
                    Log.e(TAG, "Failed to upload profile photo to Cloudinary: " + error);
                    String errorMsg = "Failed to upload photo";
                    if (error != null) {
                        if (error.contains("network") || error.contains("connection")) {
                            errorMsg = "Network error. Please check your internet connection.";
                        } else {
                            errorMsg = "Upload failed: " + error;
                        }
                    }
                    if (listener != null) listener.onPhotoError(errorMsg);
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show();
                }
            });
    }

    /**
     * Save profile photo URL to Realtime Database
     */
    private void saveProfilePhotoUrlToDatabase(String userId, String downloadUrl) {
        Log.d(TAG, "=== SAVING PROFILE PHOTO URL TO FIREBASE ===");
        Log.d(TAG, "URL to save: " + downloadUrl);
        
        // 1. Save to users node (primary source)
        usersRef.child(userId).child("profileImage").setValue(downloadUrl)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ SUCCESS: Profile image URL saved to users/" + userId);
                    
                    // Update local cache
                    preferenceManager.saveProfilePhotoPath(downloadUrl);
                    
                    // 2. Check if user is a doctor and update doctors node too
                    usersRef.child(userId).child("role").addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                            String role = snapshot.getValue(String.class);
                            if ("doctor".equals(role)) {
                                Log.d(TAG, "User is a doctor, syncing with doctors node...");
                                FirebaseHelper.getDoctorsNodeRef().child(userId).child("profileImage").setValue(downloadUrl)
                                        .addOnSuccessListener(v -> Log.d(TAG, "✅ SUCCESS: Profile image URL synced to doctors/" + userId))
                                        .addOnFailureListener(e -> Log.e(TAG, "❌ FAILED: Could not sync to doctors node: " + e.getMessage()));
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ FAILED: Could not save profile image URL to Firebase users node");
                    Toast.makeText(context, "Failed to save photo URL: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
    
    /**
     * Debug method to check if profile photo is saved in database
     */
    public static void debugCheckProfilePhoto(Context context) {
        PreferenceManager preferenceManager = new PreferenceManager(context);
        String userId = preferenceManager.getUserId();
        
        if (userId != null && !userId.isEmpty()) {
            FirebaseHelper.getUsersRef().child(userId).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                    UserEntity user = snapshot.getValue(UserEntity.class);
                    Log.d(TAG, "=== DEBUG CHECK ===");
                    Log.d(TAG, "UserId: " + userId);
                    Log.d(TAG, "User exists: " + (user != null));
                    if (user != null) {
                        Log.d(TAG, "ProfileImage in DB: " + user.getProfileImage());
                        Log.d(TAG, "ProfileImage is null: " + (user.getProfileImage() == null));
                        Log.d(TAG, "ProfileImage is empty: " + (user.getProfileImage() != null && user.getProfileImage().isEmpty()));
                    }
                    
                    // Also check preferences
                    String prefPath = preferenceManager.getProfilePhotoPath();
                    Log.d(TAG, "ProfileImage in Preferences: " + prefPath);
                    Log.d(TAG, "==================");
                }
                
                @Override
                public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                    Log.e(TAG, "DEBUG CHECK ERROR: " + error.getMessage());
                }
            });
        }
    }
    
    /**
     * Load profile photo into CircleImageView from Cloudinary (URL stored in Firebase Realtime Database) using Glide.
     * This method is suitable for loading any user's profile photo where the ID is known.
     * @param context Application context
     * @param userId The ID of the user whose profile photo is to be loaded
     * @param imageView CircleImageView to load photo into
     */
    public static void loadProfilePhoto(Context context, String userId, ImageView imageView) {
        loadProfilePhoto(context, userId, imageView, null);
    }
    
    /**
     * Load profile photo into CircleImageView with shimmer loading effect.
     * @param context Application context
     * @param userId The ID of the user whose profile photo is to be loaded
     * @param imageView CircleImageView to load photo into
     * @param shimmerLayout Optional ShimmerFrameLayout to show while loading (can be null)
     */
    public static void loadProfilePhoto(Context context, String userId, ImageView imageView, ShimmerFrameLayout shimmerLayout) {
        loadProfilePhoto(context, userId, imageView, shimmerLayout, null);
    }

    /**
     * Load profile photo into CircleImageView with shimmer loading effect and initials fallback.
     * @param context Application context
     * @param userId The ID of the user whose profile photo is to be loaded
     * @param imageView CircleImageView to load photo into
     * @param shimmerLayout Optional ShimmerFrameLayout to show while loading (can be null)
     * @param tvInitials Optional TextView to show initials if photo is missing (can be null)
     */
    public static void loadProfilePhoto(Context context, String userId, ImageView imageView, ShimmerFrameLayout shimmerLayout, TextView tvInitials) {
        Log.d(TAG, "=== LOADING PROFILE PHOTO ===");
        Log.d(TAG, "Context: " + context.getClass().getSimpleName());
        
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "⚠️ User ID is null or empty, setting default image");
            if (tvInitials != null) {
                tvInitials.setVisibility(View.VISIBLE);
                tvInitials.setText("?");
                imageView.setVisibility(View.GONE);
            } else {
                imageView.setVisibility(View.VISIBLE);
                imageView.setImageResource(R.drawable.profile_photo);
            }
            
            if (shimmerLayout != null) {
                shimmerLayout.stopShimmer();
                shimmerLayout.setVisibility(View.GONE);
            }
            return;
        }

        // Show shimmer if provided
        if (shimmerLayout != null) {
            shimmerLayout.setVisibility(View.VISIBLE);
            shimmerLayout.startShimmer();
        }

        Log.d(TAG, "Fetching user data from Firebase: users/" + userId);
        FirebaseHelper.getUsersRef().child(userId).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                Log.d(TAG, "Firebase data received. Snapshot exists: " + snapshot.exists());
                
                String fullName = snapshot.child("fullName").getValue(String.class);
                String profileImage = snapshot.child("profileImage").getValue(String.class);
                
                if (profileImage != null && !profileImage.isEmpty()) {
                    // Success case: Image exists
                    if (tvInitials != null) tvInitials.setVisibility(View.GONE);
                    imageView.setVisibility(View.VISIBLE);
                    
                    String imageUrl = profileImage;
                    Log.d(TAG, "  - Image found: " + imageUrl);
                    
                    // Load image with shimmer listener
                    ImageLoader.loadImageWithListener(context, imageUrl, imageView, 
                        new RequestListener<android.graphics.drawable.Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e, Object model, Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                                if (shimmerLayout != null) {
                                    shimmerLayout.stopShimmer();
                                    shimmerLayout.setVisibility(View.GONE);
                                }
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                                if (shimmerLayout != null) {
                                    shimmerLayout.stopShimmer();
                                    shimmerLayout.setVisibility(View.GONE);
                                }
                                return false;
                            }
                        });
                } else {
                    // Fallback case: No image
                    if (tvInitials != null && fullName != null) {
                        tvInitials.setVisibility(View.VISIBLE);
                        tvInitials.setText(getInitials(fullName));
                        imageView.setVisibility(View.GONE);
                    } else {
                        imageView.setVisibility(View.VISIBLE);
                        imageView.setImageResource(R.drawable.profile_photo);
                    }
                    
                    if (shimmerLayout != null) {
                        shimmerLayout.stopShimmer();
                        shimmerLayout.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                Log.e(TAG, "Firebase fetch cancelled: " + error.getMessage());
                imageView.setImageResource(R.drawable.profile_photo);
                if (shimmerLayout != null) {
                    shimmerLayout.stopShimmer();
                    shimmerLayout.setVisibility(View.GONE);
                }
            }
        });
    }

    public static String getInitials(String fullName) {
        if (fullName == null || fullName.isEmpty()) return "?";
        String[] parts = fullName.split(" ");
        StringBuilder initials = new StringBuilder();
        for (int i = 0; i < Math.min(parts.length, 2); i++) {
            if (!parts[i].isEmpty()) {
                initials.append(parts[i].charAt(0));
            }
        }
        return initials.toString().toUpperCase();
    }
    
    /**
     * Load profile photo into CircleImageView from a given image URL string using Glide.
     * This method is suitable when the image URL is directly available.
     * @param context Application context
     * @param imageUrl The URL string of the image to load
     * @param imageView CircleImageView to load photo into
     */
    public static void loadProfilePhotoFromUrl(Context context, String imageUrl, ImageView imageView) {
        loadProfilePhotoFromUrl(context, imageUrl, imageView, null);
    }
    
    /**
     * Load profile photo into CircleImageView from a given image URL string using Glide with shimmer.
     * @param context Application context
     * @param imageUrl The URL string of the image to load
     * @param imageView CircleImageView to load photo into
     * @param shimmerLayout Optional ShimmerFrameLayout to show while loading (can be null)
     */
    public static void loadProfilePhotoFromUrl(Context context, String imageUrl, ImageView imageView, ShimmerFrameLayout shimmerLayout) {
        // Show shimmer if provided
        if (shimmerLayout != null) {
            shimmerLayout.setVisibility(View.VISIBLE);
            shimmerLayout.startShimmer();
        }
        
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.profile_photo)
                    .error(R.drawable.profile_photo)
                    .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(@androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e, Object model, Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            // Hide shimmer on load failure
                            if (shimmerLayout != null) {
                                shimmerLayout.stopShimmer();
                                shimmerLayout.setVisibility(View.GONE);
                            }
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            // Hide shimmer when image is loaded
                            if (shimmerLayout != null) {
                                shimmerLayout.stopShimmer();
                                shimmerLayout.setVisibility(View.GONE);
                            }
                            return false;
                        }
                    })
                    .into(imageView);
            Log.d(TAG, "SUCCESS: Loaded profile photo from URL: " + imageUrl);
        } else {
            Log.d(TAG, "Image URL is null or empty, setting default image");
            imageView.setImageResource(R.drawable.profile_photo);
            // Hide shimmer if no image
            if (shimmerLayout != null) {
                shimmerLayout.stopShimmer();
                shimmerLayout.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Clear profile photo from preferences and database
     * Note: Cloudinary files are not deleted here. If needed, implement Cloudinary delete API.
     */
    public void clearProfilePhoto() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : preferenceManager.getUserId();
        if (userId == null || userId.isEmpty()) {
            if (listener != null) listener.onPhotoError("User not logged in.");
            return;
        }

        // Clear from preferences
        preferenceManager.saveProfilePhotoPath(null);

        // Clear from Realtime Database
        usersRef.child(userId).child("profileImage").removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Profile image URL cleared from Realtime Database for user: " + userId);
                    // Note: Cloudinary file deletion would require Cloudinary Admin API
                    // For now, we only clear the database reference

                    if (listener != null) {
                        listener.onPhotoSelected(null);
                    }
                    Toast.makeText(context, R.string.profile_photo_removed, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to clear profile image URL from Realtime Database: " + e.getMessage(), e);
                    if (listener != null) listener.onPhotoError("Failed to remove photo.");
                });
    }
}
