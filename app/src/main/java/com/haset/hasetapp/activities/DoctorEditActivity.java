package com.haset.hasetapp.activities;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import android.location.Geocoder;
import android.location.Address;
import android.Manifest;
import androidx.core.app.ActivityCompat;
import com.haset.hasetapp.R;
import com.haset.hasetapp.database.entities.DoctorEntity;
import com.haset.hasetapp.models.Doctor;
import com.haset.hasetapp.utils.FirebaseHelper;
import com.haset.hasetapp.utils.PreferenceManager;
import com.haset.hasetapp.utils.ProfilePhotoHelper;

import androidx.lifecycle.ViewModelProvider;
import com.haset.hasetapp.viewmodels.DoctorEditViewModel;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.io.IOException;

import de.hdodenhof.circleimageview.CircleImageView;

public class DoctorEditActivity extends AppCompatActivity {
    private TextInputEditText etSpecialty, etConsultationFee, etFromTime, etToTime, etLocation, etBio, etRegNo;
    private MaterialButton btnSave, btnCancel;
    private LinearLayout layoutProgress;
    private ProgressBar progressBar;
    private MaterialSwitch switchOnlineStatus;
    
    private PreferenceManager preferenceManager;
    private DoctorEntity currentDoctorEntity;
    private ProfilePhotoHelper profilePhotoHelper;
    private DoctorEditViewModel viewModel;
    private String currentProfileImagePath; // To store the selected image path
    private final android.os.Handler safeHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    
    private int fromHour = 9;
    private int fromMinute = 0;
    private int toHour = 17;
    private int toMinute = 0;
    
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_edit);

        preferenceManager = new PreferenceManager(this);

        initViews();
        viewModel = new ViewModelProvider(this).get(DoctorEditViewModel.class);
        setupObservers();
        
        // Set default times initially
        etFromTime.setText(String.format(Locale.getDefault(), "%02d:%02d", fromHour, fromMinute));
        etToTime.setText(String.format(Locale.getDefault(), "%02d:%02d", toHour, toMinute));
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        loadDoctorData();
        setupClickListeners();
    }

    private void setupObservers() {
        viewModel.getLoading().observe(this, this::showProgress);
        
        viewModel.getError().observe(this, error -> {
            if (error != null) {
                com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), error, com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show();
            }
        });

        viewModel.getSaveSuccess().observe(this, success -> {
            if (success != null && success) {
                com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), "Professional information updated successfully!", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                safeHandler.postDelayed(() -> {
                    if (!isFinishing()) finish();
                }, 1000);
            }
        });
    }

    private void initViews() {
        etSpecialty = findViewById(R.id.etSpecialty);
        etConsultationFee = findViewById(R.id.etConsultationFee);
        etLocation = findViewById(R.id.etLocation);
        etFromTime = findViewById(R.id.etFromTime);
        etToTime = findViewById(R.id.etToTime);
        etBio = findViewById(R.id.etBio);
        etRegNo = findViewById(R.id.etRegNo);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        layoutProgress = findViewById(R.id.layoutProgress);
        progressBar = findViewById(R.id.progressBar);
        switchOnlineStatus = findViewById(R.id.switchOnlineStatus);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        // Setup time pickers
        setupTimePickers();
    }

    private void setupTimePickers() {
        etFromTime.setOnClickListener(v -> showTimePicker(true));
        etToTime.setOnClickListener(v -> showTimePicker(false));
    }
    
    private void showTimePicker(boolean isFromTime) {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
            this,
            (view, hourOfDay, minute) -> {
                if (isFromTime) {
                    fromHour = hourOfDay;
                    fromMinute = minute;
                    etFromTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
                } else {
                    toHour = hourOfDay;
                    toMinute = minute;
                    etToTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
                }
            },
            isFromTime ? fromHour : toHour,
            isFromTime ? fromMinute : toMinute,
            true // 24-hour format
        );
        timePickerDialog.show();
    }

    private void loadDoctorData() {
        String doctorId = preferenceManager.getUserId();
        if (doctorId == null || doctorId.isEmpty()) return;

        viewModel.getDoctorEntity(doctorId).observe(this, doctorEntity -> {
            currentDoctorEntity = doctorEntity;
            if (doctorEntity != null) {
                // Load existing data
                etSpecialty.setText(doctorEntity.getSpecialty() != null ? doctorEntity.getSpecialty() : "");
                etConsultationFee.setText(String.format(Locale.getDefault(), "%.0f", doctorEntity.getConsultationFee()));
                etLocation.setText(doctorEntity.getLocation() != null ? doctorEntity.getLocation() : "");
                etBio.setText(doctorEntity.getAbout() != null ? doctorEntity.getAbout() : "");
                if (etRegNo != null) {
                    etRegNo.setText(doctorEntity.getRegNo() != null ? doctorEntity.getRegNo() : "");
                }
                
                // Load online status
                if (switchOnlineStatus != null) {
                    switchOnlineStatus.setChecked(doctorEntity.isOnline());
                }
                
                // Load available times
                if (doctorEntity.getAvailableTimes() != null && !doctorEntity.getAvailableTimes().isEmpty()) {
                    String availableTimesStr = doctorEntity.getAvailableTimes();
                    List<String> availableTimes;
                    if (availableTimesStr.contains(",")) {
                        String[] timesArray = availableTimesStr.split(", ");
                        availableTimes = new ArrayList<>();
                        for (String time : timesArray) {
                            if (!time.trim().isEmpty()) {
                                availableTimes.add(time.trim());
                            }
                        }
                    } else if (availableTimesStr.contains("-")) {
                        availableTimes = new ArrayList<>();
                        String[] range = availableTimesStr.split("-");
                        if (range.length == 2) {
                            availableTimes.add(range[0].trim());
                            availableTimes.add(range[1].trim());
                        }
                    } else {
                        availableTimes = new ArrayList<>();
                        availableTimes.add(availableTimesStr);
                    }
                    
                    if (availableTimes.size() >= 2) {
                        try {
                            String[] firstTime = availableTimes.get(0).split(":");
                            fromHour = Integer.parseInt(firstTime[0]);
                            fromMinute = Integer.parseInt(firstTime[1]);
                            
                            String[] lastTime = availableTimes.get(availableTimes.size() - 1).split(":");
                            toHour = Integer.parseInt(lastTime[0]);
                            toMinute = Integer.parseInt(lastTime[1]);
                            
                            etFromTime.setText(String.format(Locale.getDefault(), "%02d:%02d", fromHour, fromMinute));
                            etToTime.setText(String.format(Locale.getDefault(), "%02d:%02d", toHour, toMinute));
                        } catch (Exception e) {
                            Log.e("DoctorEditActivity", "Error parsing times", e);
                        }
                    }
                }
            }
        });
    }

    private void setupClickListeners() {
        btnSave.setOnClickListener(v -> saveDoctorInfo());
        btnCancel.setOnClickListener(v -> finish());
        
        TextInputLayout tilLocation = findViewById(R.id.tilLocation);
        if (tilLocation != null) {
            tilLocation.setEndIconOnClickListener(v -> checkLocationPermissionAndFetch());
        }
    }

    private void checkLocationPermissionAndFetch() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchCurrentLocation();
        }
    }

    private void fetchCurrentLocation() {
        showProgress(true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            showProgress(false);
            return;
        }
        
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                reverseGeocode(location.getLatitude(), location.getLongitude());
            } else {
                showProgress(false);
                com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), "Could not get location. Make sure GPS is on.", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            showProgress(false);
            com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), "Error: " + e.getMessage(), com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
        });
    }

    private void reverseGeocode(double lat, double lng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                
                // Get thoroughfare (street), feature name (building), subLocality (neighborhood)
                if (address.getFeatureName() != null) sb.append(address.getFeatureName()).append(", ");
                if (address.getSubLocality() != null) sb.append(address.getSubLocality()).append(", ");
                if (address.getLocality() != null) sb.append(address.getLocality());
                
                String locationName = sb.toString();
                if (locationName.endsWith(", ")) {
                    locationName = locationName.substring(0, locationName.length() - 2);
                }
                
                if (TextUtils.isEmpty(locationName)) {
                    locationName = address.getAddressLine(0);
                }
                
                etLocation.setText(locationName);
                com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), "Location updated!", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e("DoctorEditActivity", "Geocoding failed", e);
            com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), "Could not resolve address. Try manual entry.", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
        } finally {
            showProgress(false);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchCurrentLocation();
            } else {
                com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), "Location permission denied", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void saveDoctorInfo() {
        String specialty = etSpecialty.getText() != null ? etSpecialty.getText().toString().trim() : "";
        String feeStr = etConsultationFee.getText() != null ? etConsultationFee.getText().toString().trim() : "";
        String location = etLocation.getText() != null ? etLocation.getText().toString().trim() : "";
        String about = etBio.getText() != null ? etBio.getText().toString().trim() : "";
        String regNo = (etRegNo != null && etRegNo.getText() != null) ? etRegNo.getText().toString().trim() : "";
        
        // Validation
        if (TextUtils.isEmpty(specialty)) {
            etSpecialty.setError(getString(R.string.error_specialty_required));
            etSpecialty.requestFocus();
            return;
        }
        
        if (TextUtils.isEmpty(feeStr)) {
            etConsultationFee.setError(getString(R.string.error_consultation_fee_required));
            etConsultationFee.requestFocus();
            return;
        }
        
        double consultationFee;
        try {
            consultationFee = Double.parseDouble(feeStr);
            if (consultationFee < 0) {
                etConsultationFee.setError(getString(R.string.error_fee_must_be_positive));
                etConsultationFee.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            etConsultationFee.setError(getString(R.string.error_valid_number));
            etConsultationFee.requestFocus();
            return;
        }
        
        if (TextUtils.isEmpty(location)) {
            etLocation.setError(getString(R.string.error_location_required));
            etLocation.requestFocus();
            return;
        }
        
        // Get time range
        String fromTimeStr = etFromTime.getText() != null ? etFromTime.getText().toString().trim() : "";
        String toTimeStr = etToTime.getText() != null ? etToTime.getText().toString().trim() : "";
        
        if (TextUtils.isEmpty(fromTimeStr)) {
            etFromTime.setError(getString(R.string.error_from_time_required));
            etFromTime.requestFocus();
            return;
        }
        
        if (TextUtils.isEmpty(toTimeStr)) {
            etToTime.setError(getString(R.string.error_to_time_required));
            etToTime.requestFocus();
            return;
        }
        
        // Validate time format
        if (!fromTimeStr.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
            etFromTime.setError(getString(R.string.error_invalid_time_format));
            etFromTime.requestFocus();
            return;
        }
        
        if (!toTimeStr.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
            etToTime.setError(getString(R.string.error_invalid_time_format));
            etToTime.requestFocus();
            return;
        }
        
        // Validate that "to" time is after "from" time
        try {
            Calendar fromCal = Calendar.getInstance();
            Calendar toCal = Calendar.getInstance();
            String[] fromParts = fromTimeStr.split(":");
            String[] toParts = toTimeStr.split(":");
            
            fromCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(fromParts[0]));
            fromCal.set(Calendar.MINUTE, Integer.parseInt(fromParts[1]));
            toCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(toParts[0]));
            toCal.set(Calendar.MINUTE, Integer.parseInt(toParts[1]));
            
            if (!toCal.after(fromCal)) {
                etToTime.setError(getString(R.string.error_end_time_after_start));
                etToTime.requestFocus();
                return;
            }
        } catch (Exception e) {
            etToTime.setError(getString(R.string.error_invalid_time_format));
            etToTime.requestFocus();
            return;
        }
        
        // Format as "HH:mm-HH:mm"
        String availableTimes = fromTimeStr + "-" + toTimeStr;
        
        // Show progress
        showProgress(true);
        
        String doctorId = preferenceManager.getUserId();
        
        // Create or update DoctorEntity
        DoctorEntity doctorEntity;
        if (currentDoctorEntity != null) {
            // Preserve existing entity and update fields
            doctorEntity = currentDoctorEntity;
            // Handle profile image preservation
            if (currentProfileImagePath == null) {
                currentProfileImagePath = currentDoctorEntity.getProfileImage();
            }
        } else {
            // New entity - default to not approved (admin must approve first time)
            doctorEntity = new DoctorEntity();
            doctorEntity.setDoctorId(doctorId);
            doctorEntity.setApproved(false); // New doctors need initial approval
        }
        
        // Update the fields
        doctorEntity.setDoctorId(doctorId);
        doctorEntity.setSpecialty(specialty);
        doctorEntity.setConsultationFee(consultationFee);
        doctorEntity.setAvailableTimes(availableTimes);
        doctorEntity.setLocation(location); // Set location
        doctorEntity.setAbout(about); // Set bio
        doctorEntity.setRegNo(regNo); // Set MCT reg no
        doctorEntity.setProfileImage(currentProfileImagePath); // Save profile image path
        doctorEntity.setLastUpdated(System.currentTimeMillis());
        
        // Set online status
        if (switchOnlineStatus != null) {
            doctorEntity.setOnline(switchOnlineStatus.isChecked());
            doctorEntity.setOnlineStatus(switchOnlineStatus.isChecked() ? "online" : "offline");
        }
        
        viewModel.saveDoctorProfile(doctorEntity);
    }

    private void showProgress(boolean show) {
        if (layoutProgress != null) {
            layoutProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        btnSave.setEnabled(!show);
        btnCancel.setEnabled(!show);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        safeHandler.removeCallbacksAndMessages(null);
    }
}
