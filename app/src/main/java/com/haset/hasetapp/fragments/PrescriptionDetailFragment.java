package com.haset.hasetapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.haset.hasetapp.R;
import com.haset.hasetapp.models.Prescription;
import com.haset.hasetapp.utils.PreferenceManager;
import com.haset.hasetapp.utils.PrescriptionHelper;
import com.haset.hasetapp.viewmodels.PrescriptionViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Fragment to display prescription details
 */
public class PrescriptionDetailFragment extends Fragment {
    
    private ImageView ivPrescriptionImage;
    private TextView tvDoctorName, tvDate, tvInstructions;
    private RecyclerView recyclerViewMedicines;
    private MaterialButton btnDelete;
    
    private Prescription prescription;
    private PrescriptionViewModel viewModel;
    private PreferenceManager preferenceManager;
    private String prescriptionId;

    public static PrescriptionDetailFragment newInstance(String prescriptionId) {
        PrescriptionDetailFragment fragment = new PrescriptionDetailFragment();
        Bundle args = new Bundle();
        args.putString("prescription_id", prescriptionId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_prescription_detail, container, false);
        
        viewModel = new androidx.lifecycle.ViewModelProvider(this).get(PrescriptionViewModel.class);
        updateToolbar();
        initializeViews(view);
        loadPrescriptionData();
        
        return view;
    }
    
    private void initializeViews(View view) {
        ivPrescriptionImage = view.findViewById(R.id.ivPrescriptionImage);
        tvDoctorName = view.findViewById(R.id.tvDoctorName);
        tvDate = view.findViewById(R.id.tvDate);
        tvInstructions = view.findViewById(R.id.tvInstructions);
        recyclerViewMedicines = view.findViewById(R.id.recyclerViewMedicines);
        btnDelete = view.findViewById(R.id.btnDelete);
        
        preferenceManager = new PreferenceManager(requireContext());
        
        // Get prescription from arguments
        if (getArguments() != null) {
            prescription = (Prescription) getArguments().getSerializable("prescription");
            prescriptionId = getArguments().getString("prescription_id");
            if (prescription != null && prescriptionId == null) {
                prescriptionId = prescription.getPrescriptionId();
            }
        }
        
        // Show delete button only for doctors
        String userRole = preferenceManager.getUserRole();
        if ("doctor".equalsIgnoreCase(userRole)) {
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> showDeleteConfirmation());
        }
    }
    
    
    private void loadPrescriptionData() {
        if (prescriptionId == null) return;
        
        // Load from repository to get latest updates if any
        viewModel.getPrescriptionById(prescriptionId).observe(getViewLifecycleOwner(), p -> {
            if (p != null) {
                this.prescription = p;
                displayPrescription(p);
                updateToolbar();
            }
        });
    }

    private void displayPrescription(Prescription prescription) {
        // Load image
        if (prescription.getImageUrl() != null && !prescription.getImageUrl().isEmpty()) {
            Glide.with(this)
                .load(prescription.getImageUrl())
                .placeholder(R.drawable.ic_prescription)
                .into(ivPrescriptionImage);
        }
        
        // Set doctor name
        tvDoctorName.setText(prescription.getDoctorName());
        
        // Format and set date
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        String formattedDate = dateFormat.format(new Date(prescription.getCreatedAt()));
        tvDate.setText(formattedDate);
        
        // Set instructions
        if (prescription.getInstructions() != null && !prescription.getInstructions().isEmpty()) {
            tvInstructions.setText(prescription.getInstructions());
        } else {
            tvInstructions.setText(R.string.na);
        }
        
        // Setup medicines RecyclerView
        if (prescription.getMedicines() != null && !prescription.getMedicines().isEmpty()) {
            recyclerViewMedicines.setLayoutManager(new LinearLayoutManager(requireContext()));
            com.haset.hasetapp.adapters.MedicineAdapter adapter = new com.haset.hasetapp.adapters.MedicineAdapter(prescription.getMedicines());
            recyclerViewMedicines.setAdapter(adapter);
        }
    }
    
    private void showDeleteConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_prescription)
            .setMessage(R.string.confirm_delete_prescription)
            .setPositiveButton(R.string.delete, (dialog, which) -> deletePrescription())
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    private void deletePrescription() {
        if (prescription == null) return;
        
        viewModel.deletePrescription(prescription.getPrescriptionId(), new com.haset.hasetapp.utils.FirebaseHelper.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (isAdded()) {
                    com.google.android.material.snackbar.Snackbar.make(requireView(), R.string.prescription_deleted, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                    requireActivity().onBackPressed();
                }
            }
            
            @Override
            public void onError(String error) {
                if (isAdded()) {
                    com.google.android.material.snackbar.Snackbar.make(requireView(), error, com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void updateToolbar() {
        if (requireActivity() instanceof com.haset.hasetapp.activities.PrescriptionActivity) {
            com.haset.hasetapp.activities.PrescriptionActivity activity = (com.haset.hasetapp.activities.PrescriptionActivity) requireActivity();
            activity.setToolbarTitle(getString(R.string.prescription_details));
            
            // Setup download button if there is an image
            if (prescription != null && prescription.getImageUrl() != null && !prescription.getImageUrl().isEmpty()) {
                activity.setDownloadButtonVisible(true, v -> downloadPrescriptionImage());
            } else {
                activity.setDownloadButtonVisible(false, null);
            }
        }
    }

    private void downloadPrescriptionImage() {
        if (prescription == null || prescription.getImageUrl() == null || prescription.getImageUrl().isEmpty()) {
            com.google.android.material.snackbar.Snackbar.make(requireView(), "No image to download", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
            return;
        }

        String url = prescription.getImageUrl();
        String fileName = "Prescription_" + prescription.getPrescriptionId() + ".jpg";

        android.app.DownloadManager.Request request = new android.app.DownloadManager.Request(android.net.Uri.parse(url));
        request.setTitle(getString(R.string.downloading_prescription));
        request.setDescription("Saving prescription image to gallery");
        request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName);
        request.setAllowedOverMetered(true);
        request.setAllowedOverRoaming(true);

        android.app.DownloadManager downloadManager = (android.app.DownloadManager) requireContext().getSystemService(android.content.Context.DOWNLOAD_SERVICE);
        if (downloadManager != null) {
            downloadManager.enqueue(request);
            com.google.android.material.snackbar.Snackbar.make(requireView(), "Download started...", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
        } else {
            com.google.android.material.snackbar.Snackbar.make(requireView(), "Download failed: Service unavailable", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Hide download button when leaving this fragment
        if (requireActivity() instanceof com.haset.hasetapp.activities.PrescriptionActivity) {
            ((com.haset.hasetapp.activities.PrescriptionActivity) requireActivity()).setDownloadButtonVisible(false, null);
            ((com.haset.hasetapp.activities.PrescriptionActivity) requireActivity()).setToolbarTitle(getString(R.string.prescriptions));
        }
    }
}
