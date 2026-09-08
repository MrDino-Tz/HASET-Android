package com.haset.hasetapp.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.haset.hasetapp.R;
import com.haset.hasetapp.models.Hospital;
import com.haset.hasetapp.utils.CloudinaryUploadHelper;
import com.haset.hasetapp.utils.FirebaseHelper;

public class BottomSheetAddHospital extends BottomSheetDialogFragment {

    private TextInputEditText etHospitalName, etHospitalAddress, etHospitalDescription;
    private MaterialButton btnSave;
    private ImageView ivHospitalPreview;
    private View llUploadPrompt;
    private ProgressBar pbImageUpload;
    
    private Hospital existingHospital = null;
    private Uri imageUri = null;
    private String uploadedImageUrl = null;
    
    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    imageUri = uri;
                    ivHospitalPreview.setImageURI(uri);
                    llUploadPrompt.setVisibility(View.GONE);
                    uploadImageToFirebase(uri);
                }
            }
    );
    
    public void setExistingHospital(Hospital hospital) {
        this.existingHospital = hospital;
    }

    public BottomSheetAddHospital() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_add_hospital, container, false);
        
        etHospitalName = view.findViewById(R.id.etHospitalName);
        etHospitalAddress = view.findViewById(R.id.etHospitalAddress);
        etHospitalDescription = view.findViewById(R.id.etHospitalDescription);
        btnSave = view.findViewById(R.id.btnSave);
        
        ivHospitalPreview = view.findViewById(R.id.ivHospitalPreview);
        llUploadPrompt = view.findViewById(R.id.llUploadPrompt);
        pbImageUpload = view.findViewById(R.id.pbImageUpload);
        
        view.findViewById(R.id.cardHospitalImage).setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        if (existingHospital != null) {
            etHospitalName.setText(existingHospital.getName());
            etHospitalAddress.setText(existingHospital.getAddress());
            etHospitalDescription.setText(existingHospital.getDescription());
            btnSave.setText("Update Hospital");
            
            if (existingHospital.getImageUrl() != null && !existingHospital.getImageUrl().isEmpty()) {
                uploadedImageUrl = existingHospital.getImageUrl();
                llUploadPrompt.setVisibility(View.GONE);
                if (getContext() != null) {
                    Glide.with(getContext()).load(uploadedImageUrl).into(ivHospitalPreview);
                }
            }
        }

        btnSave.setOnClickListener(v -> saveHospital());

        return view;
    }

    private void uploadImageToFirebase(Uri uri) {
        if (pbImageUpload != null) pbImageUpload.setVisibility(View.VISIBLE);
        if (btnSave != null) btnSave.setEnabled(false);
        
        CloudinaryUploadHelper.uploadFile(getContext(), uri, "image", "hospital_" + System.currentTimeMillis(), "hospitals", new CloudinaryUploadHelper.OnFileUploadListener() {
            @Override
            public void onUploadStart() {
                // Already handled before call
            }

            @Override
            public void onUploadProgress(double progress) {
                // Optional: update progress bar
            }

            @Override
            public void onUploadSuccess(String downloadUrl, String fileName) {
                uploadedImageUrl = downloadUrl;
                if (pbImageUpload != null) pbImageUpload.setVisibility(View.GONE);
                if (btnSave != null) {
                    btnSave.setEnabled(true);
                    btnSave.setText(existingHospital != null ? "Update Hospital" : "Save Hospital");
                }
                Toast.makeText(getContext(), "Image uploaded to Cloudinary!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUploadError(String error) {
                if (pbImageUpload != null) pbImageUpload.setVisibility(View.GONE);
                if (btnSave != null) btnSave.setEnabled(true);
                Toast.makeText(getContext(), "Cloudinary upload failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveHospital() {
        String name = etHospitalName.getText().toString().trim();
        String address = etHospitalAddress.getText().toString().trim();
        String description = etHospitalDescription.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etHospitalName.setError("Hospital Name is required");
            return;
        }

        if (TextUtils.isEmpty(address)) {
            etHospitalAddress.setError("Address is required");
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText(existingHospital != null ? "Updating..." : "Saving...");

        // Use existing ID if editing, otherwise generate a new one
        String newId = existingHospital != null ? existingHospital.getHospitalId() : FirebaseHelper.getInstance().getDatabaseReference().child("Hospitals").push().getKey();
        if (newId != null) {
            Hospital newHospital = new Hospital(newId, name, address);
            newHospital.setDescription(description);
            newHospital.setImageUrl(uploadedImageUrl);
            newHospital.setCreatedAt(existingHospital != null ? existingHospital.getCreatedAt() : System.currentTimeMillis());

            FirebaseHelper.getInstance().getDatabaseReference().child("Hospitals").child(newId).setValue(newHospital)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), existingHospital != null ? "Hospital updated!" : "Hospital saved successfully!", Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    btnSave.setText(existingHospital != null ? "Update Hospital" : "Save Hospital");
                    Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        }
    }
}
