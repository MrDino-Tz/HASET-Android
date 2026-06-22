package com.haset.hasetapp.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.material.card.MaterialCardView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.haset.hasetapp.R;

public class FileAttachmentBottomSheet extends BottomSheetDialogFragment {

    public interface OnFileAttachmentSelectedListener {
        void onDocumentSelected();
        void onImageSelected();
        void onVideoSelected();
        void onPrescriptionSelected();
        void onServiceSelected();
    }

    private OnFileAttachmentSelectedListener listener;

    public void setOnFileAttachmentSelectedListener(OnFileAttachmentSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_file_attachment, container, false);

        // Initialize views
        MaterialCardView llDocumentOption = view.findViewById(R.id.llDocumentOption);
        MaterialCardView llImageOption = view.findViewById(R.id.llImageOption);
        MaterialCardView llVideoOption = view.findViewById(R.id.llVideoOption);

        // Set click listeners
        llDocumentOption.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDocumentSelected();
            }
            dismiss();
        });

        llImageOption.setOnClickListener(v -> {
            if (listener != null) {
                listener.onImageSelected();
            }
            dismiss();
        });

        llVideoOption.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVideoSelected();
            }
            dismiss();
        });

        com.google.android.material.button.MaterialButton btnPrescriptionOption = view.findViewById(R.id.btnPrescriptionOption);
        if (getArguments() != null && getArguments().getBoolean("showPrescription", false)) {
            btnPrescriptionOption.setVisibility(View.VISIBLE);
        } else {
            btnPrescriptionOption.setVisibility(View.GONE);
        }

        btnPrescriptionOption.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPrescriptionSelected();
            }
            dismiss();
        });

        // Service Payment button
        com.google.android.material.button.MaterialButton btnServiceOption = view.findViewById(R.id.btnServiceOption);
        if (getArguments() != null && getArguments().getBoolean("showService", false)) {
            btnServiceOption.setVisibility(View.VISIBLE);
        } else {
            btnServiceOption.setVisibility(View.GONE);
        }

        btnServiceOption.setOnClickListener(v -> {
            if (listener != null) {
                listener.onServiceSelected();
            }
            dismiss();
        });

        return view;
    }

    // Helper methods to handle file selection
    public void openDocumentPicker(Activity activity, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain"
        };
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        activity.startActivityForResult(Intent.createChooser(intent, activity.getString(R.string.select_document_picker)), requestCode);
    }

    public void openImagePicker(Activity activity, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        activity.startActivityForResult(Intent.createChooser(intent, activity.getString(R.string.select_image_picker)), requestCode);
    }

    public void openVideoPicker(Activity activity, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        activity.startActivityForResult(Intent.createChooser(intent, activity.getString(R.string.select_video_picker)), requestCode);
    }
}
