package com.haset.hasetapp.utils;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.haset.hasetapp.R;
import com.haset.hasetapp.database.entities.AppointmentEntity;
import com.haset.hasetapp.models.Prescription;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddPrescriptionBottomSheet extends BottomSheetDialogFragment {

    private AutoCompleteTextView actvPatient, actvAppointment;
    private LinearLayout llMedicinesContainer;
    private TextInputEditText etInstructions;
    private ImageView ivPrescriptionPreview;
    private View llAddPhotoPrompt, progressOverlay, btnRemovePhoto;
    private TextView tvProgressStatus;
    
    private List<AppointmentEntity> allAppointments = new ArrayList<>();
    private Map<String, List<AppointmentEntity>> patientAppointmentsMap = new HashMap<>();
    private List<String> patientNames = new ArrayList<>();
    private String selectedPatientId;
    private String selectedAppointmentId;
    private String preferredAppointmentId;
    
    private Uri currentImageUri;
    private String currentImagePath;
    
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_upload_prescription, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        checkArguments();
        loadAppointments();
        addMedicineRow(); // Add first row by default
    }

    private void checkArguments() {
        if (getArguments() != null) {
            selectedPatientId = getArguments().getString("patientId");
            preferredAppointmentId = getArguments().getString("appointmentId");
            String patientName = getArguments().getString("patientName");
            if (selectedPatientId != null && patientName != null) {
                actvPatient.setText(patientName);
                actvPatient.setEnabled(false); // Lock it to this patient
            }
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        return dialog;
    }

    private void initViews(View view) {
        actvPatient = view.findViewById(R.id.actvPatient);
        actvAppointment = view.findViewById(R.id.actvAppointment);
        llMedicinesContainer = view.findViewById(R.id.llMedicinesContainer);
        etInstructions = view.findViewById(R.id.etInstructions);
        ivPrescriptionPreview = view.findViewById(R.id.ivPrescriptionPreview);
        llAddPhotoPrompt = view.findViewById(R.id.llAddPhotoPrompt);
        progressOverlay = view.findViewById(R.id.progressOverlay);
        tvProgressStatus = view.findViewById(R.id.tvProgressStatus);
        btnRemovePhoto = view.findViewById(R.id.btnRemovePhoto);

        view.findViewById(R.id.btnAddMedicine).setOnClickListener(v -> addMedicineRow());
        view.findViewById(R.id.cardAddPhoto).setOnClickListener(v -> showImagePickerDialog());
        view.findViewById(R.id.btnSubmitPrescription).setOnClickListener(v -> validateAndSubmit());
        btnRemovePhoto.setOnClickListener(v -> removePhoto());

        actvPatient.setOnItemClickListener((parent, v, position, id) -> {
            String selectedName = (String) parent.getItemAtPosition(position);
            onPatientSelected(selectedName);
        });
    }

    private void addMedicineRow() {
        View rowView = getLayoutInflater().inflate(R.layout.item_medicine_input, llMedicinesContainer, false);
        rowView.findViewById(R.id.btnRemoveMedicine).setOnClickListener(v -> {
            if (llMedicinesContainer.getChildCount() > 1) {
                llMedicinesContainer.removeView(rowView);
            } else {
                showSnackbar("At least one medicine is required");
            }
        });
        llMedicinesContainer.addView(rowView);
    }

    private void loadAppointments() {
        PreferenceManager pref = new PreferenceManager(requireContext());
        String doctorId = pref.getUserId();
        
        FirebaseHelper.getAppointmentsByUser(doctorId, Constants.ROLE_DOCTOR, new FirebaseHelper.OnCompleteListener<List<AppointmentEntity>>() {
            @Override
            public void onSuccess(List<AppointmentEntity> appointments) {
                allAppointments = appointments;
                processAppointments();
                
                // If we pre-selected a patient, we still need to filter their appointments for the dropdown
                if (selectedPatientId != null && actvPatient.getText() != null) {
                    onPatientSelected(actvPatient.getText().toString());
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    showSnackbar("Failed to load patients: " + error);
                }
            }
        });
    }

    private void processAppointments() {
        patientAppointmentsMap.clear();
        patientNames.clear();
        Map<String, String> nameToIdMap = new HashMap<>();

        for (AppointmentEntity appt : allAppointments) {
            String patientName = appt.getPatientName();
            if (!patientAppointmentsMap.containsKey(patientName)) {
                patientAppointmentsMap.put(patientName, new ArrayList<>());
                patientNames.add(patientName);
                nameToIdMap.put(patientName, appt.getPatientId());
            }
            patientAppointmentsMap.get(patientName).add(appt);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, patientNames);
        actvPatient.setAdapter(adapter);
    }

    private void onPatientSelected(String patientName) {
        List<AppointmentEntity> appointments = patientAppointmentsMap.get(patientName);
        if (appointments != null && !appointments.isEmpty()) {
            selectedPatientId = appointments.get(0).getPatientId();
            
            List<String> apptDisplay = new ArrayList<>();
            for (AppointmentEntity a : appointments) {
                apptDisplay.add(a.getDate() + " " + a.getTime() + " (" + a.getStatus() + ")");
            }
            
            ArrayAdapter<String> apptAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, apptDisplay);
            actvAppointment.setAdapter(apptAdapter);
            int selectedIndex = 0;
            if (preferredAppointmentId != null) {
                for (int i = 0; i < appointments.size(); i++) {
                    if (preferredAppointmentId.equals(appointments.get(i).getAppointmentId())) {
                        selectedIndex = i;
                        break;
                    }
                }
            }
            actvAppointment.setText(apptDisplay.get(selectedIndex), false);
            selectedAppointmentId = appointments.get(selectedIndex).getAppointmentId();
            
            actvAppointment.setOnItemClickListener((parent, view, position, id) -> {
                selectedAppointmentId = appointments.get(position).getAppointmentId();
            });
        }
    }

    private void showImagePickerDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        new AlertDialog.Builder(requireContext())
                .setTitle(requireContext().getString(R.string.select_prescription_image))
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        launchCamera();
                    } else {
                        launchGallery();
                    }
                })
                .show();
    }

    private static final int REQUEST_CAMERA_PERMISSION = 1002;

    private void launchCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            return;
        }
        openCamera();
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                showSnackbar("Error creating file");
            }
            if (photoFile != null) {
                currentImageUri = FileProvider.getUriForFile(requireContext(),
                        requireContext().getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentImageUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                showSnackbar("Camera permission is required to take photos");
            }
        }
    }

    private void launchGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireActivity().getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentImagePath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == android.app.Activity.RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                showPreview(currentImageUri);
            } else if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                currentImageUri = data.getData();
                showPreview(currentImageUri);
            }
        }
    }

    private void showPreview(Uri uri) {
        ivPrescriptionPreview.setImageURI(uri);
        ivPrescriptionPreview.setVisibility(View.VISIBLE);
        llAddPhotoPrompt.setVisibility(View.GONE);
        btnRemovePhoto.setVisibility(View.VISIBLE);
    }

    private void removePhoto() {
        currentImageUri = null;
        ivPrescriptionPreview.setVisibility(View.GONE);
        llAddPhotoPrompt.setVisibility(View.VISIBLE);
        btnRemovePhoto.setVisibility(View.GONE);
    }

    private void validateAndSubmit() {
        if (selectedPatientId == null) {
            showSnackbar("Please select a patient");
            return;
        }

        List<Prescription.Medicine> medicines = new ArrayList<>();
        for (int i = 0; i < llMedicinesContainer.getChildCount(); i++) {
            View row = llMedicinesContainer.getChildAt(i);
            TextInputEditText etName = row.findViewById(R.id.etMedicineName);
            TextInputEditText etDosage = row.findViewById(R.id.etDosage);
            TextInputEditText etFreq = row.findViewById(R.id.etFrequency);
            TextInputEditText etDur = row.findViewById(R.id.etDuration);

            String name = etName.getText().toString().trim();
            if (name.isEmpty()) continue;

            Prescription.Medicine med = new Prescription.Medicine();
            med.setName(name);
            med.setDosage(etDosage.getText().toString().trim());
            med.setFrequency(etFreq.getText().toString().trim());
            try {
                med.setDuration(Integer.parseInt(etDur.getText().toString().trim()));
            } catch (Exception e) {
                med.setDuration(0);
            }
            medicines.add(med);
        }

        if (medicines.isEmpty()) {
            showSnackbar("Please add at least one medicine");
            return;
        }

        submitPrescription(medicines);
    }

    private void submitPrescription(List<Prescription.Medicine> medicines) {
        progressOverlay.setVisibility(View.VISIBLE);
        tvProgressStatus.setText(R.string.issuing_prescription);
        
        PreferenceManager pref = new PreferenceManager(requireContext());
        Prescription prescription = new Prescription();
        prescription.setPatientId(selectedPatientId);
        prescription.setPatientName(actvPatient.getText().toString());
        prescription.setAppointmentId(selectedAppointmentId);
        prescription.setDoctorId(pref.getUserId());
        prescription.setDoctorName(pref.getUserName());
        prescription.setMedicines(medicines);
        prescription.setInstructions(etInstructions.getText().toString().trim());
        prescription.setCreatedAt(System.currentTimeMillis());

        PrescriptionHelper prescriptionHelper = new PrescriptionHelper(requireContext());

        if (currentImageUri != null) {
            tvProgressStatus.setText(R.string.uploading_attachment);
            prescriptionHelper.uploadPrescriptionImage(currentImageUri, new com.cloudinary.android.callback.UploadCallback() {
                @Override
                public void onStart(String requestId) {}

                @Override
                public void onProgress(String requestId, long bytes, long totalBytes) {
                    requireActivity().runOnUiThread(() -> {
                        int progress = (int) ((bytes * 100) / totalBytes);
                        tvProgressStatus.setText("Uploading: " + progress + "%");
                    });
                }

                @Override
                public void onSuccess(String requestId, Map resultData) {
                    String imageUrl = (String) resultData.get("secure_url");
                    prescription.setImageUrl(imageUrl);
                    savePrescriptionToFirebase(prescription, prescriptionHelper);
                }

                @Override
                public void onError(String requestId, com.cloudinary.android.callback.ErrorInfo error) {
                    requireActivity().runOnUiThread(() -> {
                        progressOverlay.setVisibility(View.GONE);
                        showSnackbar("Upload failed: " + error.getDescription());
                    });
                }

                @Override
                public void onReschedule(String requestId, com.cloudinary.android.callback.ErrorInfo error) {}
            });
        } else {
            savePrescriptionToFirebase(prescription, prescriptionHelper);
        }
    }

    private void savePrescriptionToFirebase(Prescription prescription, PrescriptionHelper helper) {
        requireActivity().runOnUiThread(() -> tvProgressStatus.setText(R.string.saving_data));
        helper.createPrescription(prescription, new PrescriptionHelper.PrescriptionCallback() {
            @Override
            public void onSuccess(Prescription result) {
                if (isAdded()) {
                    progressOverlay.setVisibility(View.GONE);
                    AuditLogger.getInstance(requireContext()).logPrescriptionIssued(prescription.getPrescriptionId(),
                            prescription.getPatientName());
                    sendPrescriptionChatMessage(result);
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    progressOverlay.setVisibility(View.GONE);
                    showSnackbar("Failed to save: " + error);
                }
            }
        });
    }

    private void sendPrescriptionChatMessage(Prescription prescription) {
        String doctorId = prescription.getDoctorId();
        String patientId = prescription.getPatientId();
        
        com.haset.hasetapp.models.ChatMessage message = new com.haset.hasetapp.models.ChatMessage(doctorId, patientId, "New Prescription Issued");
        message.setSenderName(prescription.getDoctorName());
        message.setReceiverName(prescription.getPatientName());
        message.setMessageType("prescription");
        message.setPrescriptionId(prescription.getPrescriptionId());
        message.setTimestamp(System.currentTimeMillis());
        message.setMessageStatus("sent");
        
        com.haset.hasetapp.utils.FirebaseHelper.sendPrescriptionMessage(message,
                new com.haset.hasetapp.utils.FirebaseHelper.OnCompleteListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> dismiss());
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() ->
                                    showSnackbar("Prescription saved, but chat delivery failed: " + error));
                        }
                    }
                });
    }

    private void showSnackbar(String message) {
        if (getView() != null) {
            com.google.android.material.snackbar.Snackbar.make(getView(), message, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
        } else if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
