package com.haset.hasetapp.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.haset.hasetapp.R;
import com.haset.hasetapp.adapters.HospitalAdapter;
import com.haset.hasetapp.models.Hospital;
import com.haset.hasetapp.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.List;

public class AdminManageHospitalsActivity extends AppCompatActivity {

    private RecyclerView rvAdminHospitals;
    private View layoutEmptyState;
    private HospitalAdapter adapter;
    private List<Hospital> hospitalList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_hospitals);
        
        rvAdminHospitals = findViewById(R.id.rvAdminHospitals);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
        
        FloatingActionButton fabAddHospital = findViewById(R.id.fabAddHospital);
        if (fabAddHospital != null) {
            fabAddHospital.setOnClickListener(v -> {
                com.haset.hasetapp.fragments.BottomSheetAddHospital bottomSheet = new com.haset.hasetapp.fragments.BottomSheetAddHospital();
                bottomSheet.show(getSupportFragmentManager(), "BottomSheetAddHospital");
            });
        }
        
        hospitalList = new ArrayList<>();
        adapter = new HospitalAdapter(hospitalList, hospital -> {
            new android.app.AlertDialog.Builder(this)
                .setTitle(hospital.getName())
                .setItems(new CharSequence[]{"Edit Hospital", "Delete Hospital"}, (dialog, which) -> {
                    if (which == 0) {
                        // Edit
                        com.haset.hasetapp.fragments.BottomSheetAddHospital bottomSheet = new com.haset.hasetapp.fragments.BottomSheetAddHospital();
                        bottomSheet.setExistingHospital(hospital);
                        bottomSheet.show(getSupportFragmentManager(), "BottomSheetEditHospital");
                    } else if (which == 1) {
                        // Delete
                        new android.app.AlertDialog.Builder(this)
                            .setTitle("Delete " + hospital.getName() + "?")
                            .setMessage("Are you sure you want to permanently delete this hospital? This cannot be undone.")
                            .setPositiveButton("Delete", (d, w) -> {
                                FirebaseHelper.getInstance().getDatabaseReference().child("Hospitals").child(hospital.getHospitalId()).removeValue()
                                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Hospital deleted", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                    }
                })
                .show();
        });
        
        rvAdminHospitals.setLayoutManager(new LinearLayoutManager(this));
        rvAdminHospitals.setAdapter(adapter);
        
        loadHospitals();
    }
    
    private void loadHospitals() {
        FirebaseHelper.getInstance().getDatabaseReference().child("Hospitals")
            .addValueEventListener(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                    hospitalList.clear();
                    if (snapshot.exists()) {
                        for (com.google.firebase.database.DataSnapshot ds : snapshot.getChildren()) {
                            Hospital hospital = ds.getValue(Hospital.class);
                            if (hospital != null) {
                                hospital.setHospitalId(ds.getKey());
                                hospitalList.add(hospital);
                            }
                        }
                    }
                    
                    adapter.notifyDataSetChanged();
                    
                    if (hospitalList.isEmpty()) {
                        layoutEmptyState.setVisibility(View.VISIBLE);
                        rvAdminHospitals.setVisibility(View.GONE);
                    } else {
                        layoutEmptyState.setVisibility(View.GONE);
                        rvAdminHospitals.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                    Toast.makeText(AdminManageHospitalsActivity.this, "Failed to load hospitals", Toast.LENGTH_SHORT).show();
                }
            });
    }
}
