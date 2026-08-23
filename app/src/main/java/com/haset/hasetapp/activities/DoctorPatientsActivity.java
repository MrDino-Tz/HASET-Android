package com.haset.hasetapp.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.haset.hasetapp.R;
import com.haset.hasetapp.database.entities.AppointmentEntity;
import com.haset.hasetapp.utils.Constants;
import com.haset.hasetapp.utils.FirebaseHelper;
import com.haset.hasetapp.utils.PreferenceManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DoctorPatientsActivity extends AppCompatActivity {
    private ListView patientList;
    private ProgressBar progressBar;
    private TextView emptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_patients);

        ImageView backButton = findViewById(R.id.btnBack);
        patientList = findViewById(R.id.patientList);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);
        backButton.setOnClickListener(v -> finish());

        loadPatients();
    }

    private void loadPatients() {
        String doctorId = new PreferenceManager(this).getUserId();
        if (doctorId == null || doctorId.isEmpty()) {
            showError(getString(R.string.failed_to_load_patients));
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        FirebaseHelper.getAppointmentsByUser(doctorId, Constants.ROLE_DOCTOR,
                new FirebaseHelper.OnCompleteListener<List<AppointmentEntity>>() {
                    @Override
                    public void onSuccess(List<AppointmentEntity> appointments) {
                        if (isFinishing() || isDestroyed()) return;
                        progressBar.setVisibility(View.GONE);

                        Map<String, PatientRow> patients = new LinkedHashMap<>();
                        if (appointments == null) appointments = new ArrayList<>();
                        for (AppointmentEntity appointment : appointments) {
                            String patientId = appointment.getPatientId();
                            if (patientId == null || patientId.trim().isEmpty()) continue;
                            PatientRow row = patients.get(patientId);
                            if (row == null) {
                                String name = appointment.getPatientName();
                                row = new PatientRow(name == null || name.trim().isEmpty()
                                        ? getString(R.string.patient) : name.trim());
                                patients.put(patientId, row);
                            }
                            row.appointmentCount++;
                        }

                        List<String> rows = new ArrayList<>();
                        for (PatientRow patient : patients.values()) {
                            rows.add(patient.name + "\n" + getResources().getQuantityString(
                                    R.plurals.patient_appointment_count,
                                    patient.appointmentCount,
                                    patient.appointmentCount));
                        }
                        rows.sort(String.CASE_INSENSITIVE_ORDER);

                        emptyState.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
                        patientList.setVisibility(rows.isEmpty() ? View.GONE : View.VISIBLE);
                        patientList.setAdapter(new ArrayAdapter<>(DoctorPatientsActivity.this,
                                android.R.layout.simple_list_item_1, rows));
                    }

                    @Override
                    public void onError(String error) {
                        if (error != null) com.haset.hasetapp.utils.ErrorLogger.log(error, error);
                        if (!isFinishing() && !isDestroyed()) {
                            showError(getString(R.string.failed_to_load_patients));
                        }
                    }
                });
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        patientList.setVisibility(View.GONE);
        emptyState.setText(message);
        emptyState.setVisibility(View.VISIBLE);
    }

    private static final class PatientRow {
        final String name;
        int appointmentCount;

        PatientRow(String name) {
            this.name = name;
        }
    }
}
