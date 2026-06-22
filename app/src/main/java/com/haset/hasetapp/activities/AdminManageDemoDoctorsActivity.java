package com.haset.hasetapp.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.haset.hasetapp.R;
import com.haset.hasetapp.database.entities.DoctorEntity;
import com.haset.hasetapp.utils.FirebaseHelper;
import com.haset.hasetapp.utils.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdminManageDemoDoctorsActivity extends AppCompatActivity {

    private RecyclerView rvDemoDoctors;
    private TextView tvEmpty;
    private ProgressBar progressBar;
    private SwitchMaterial switchGlobalVisibility;
    
    private List<DoctorEntity> demoDoctors = new ArrayList<>();
    private DemoDoctorAdapter adapter;
    private PreferenceManager preferenceManager;
    
    private static final String DEMO_VISIBILITY_KEY = "demo_doctors_visible";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_demo_doctors);
        
        preferenceManager = new PreferenceManager(this);
        
        initViews();
        loadGlobalVisibility();
        loadDemoDoctors();
    }

    private void initViews() {
        rvDemoDoctors = findViewById(R.id.rvDemoDoctors);
        tvEmpty = findViewById(R.id.tvEmpty);
        progressBar = findViewById(R.id.progressBar);
        switchGlobalVisibility = findViewById(R.id.switchGlobalVisibility);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        adapter = new DemoDoctorAdapter(demoDoctors);
        rvDemoDoctors.setLayoutManager(new LinearLayoutManager(this));
        rvDemoDoctors.setAdapter(adapter);
        
        switchGlobalVisibility.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveGlobalVisibility(isChecked);
        });
    }

    private void loadGlobalVisibility() {
        FirebaseHelper.getInstance().getAppSettingsRef()
            .child(DEMO_VISIBILITY_KEY)
            .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                    Boolean isVisible = snapshot.getValue(Boolean.class);
                    switchGlobalVisibility.setChecked(isVisible == null || isVisible);
                }

                @Override
                public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                    switchGlobalVisibility.setChecked(true);
                }
            });
    }

    private void saveGlobalVisibility(boolean isVisible) {
        FirebaseHelper.getInstance().getAppSettingsRef()
            .child(DEMO_VISIBILITY_KEY)
            .setValue(isVisible)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, isVisible ? 
                    R.string.demo_doctors_shown : R.string.demo_doctors_hidden, Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, R.string.error_saving, Toast.LENGTH_SHORT).show();
            });
    }

    private void loadDemoDoctors() {
        progressBar.setVisibility(View.VISIBLE);
        
        FirebaseHelper.getInstance().getDoctorsNodeRef()
            .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                    progressBar.setVisibility(View.GONE);
                    demoDoctors.clear();
                    if (snapshot.exists()) {
                        for (com.google.firebase.database.DataSnapshot ds : snapshot.getChildren()) {
                            DoctorEntity doctor = ds.getValue(DoctorEntity.class);
                            if (doctor != null && doctor.isDemo()) {
                                doctor.setDoctorId(ds.getKey());
                                demoDoctors.add(doctor);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                }

                @Override
                public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AdminManageDemoDoctorsActivity.this, 
                        R.string.error_loading, Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void updateEmptyState() {
        if (demoDoctors.isEmpty()) {
            rvDemoDoctors.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            rvDemoDoctors.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void toggleDoctorVisibility(DoctorEntity doctor, boolean isVisible, int position) {
        FirebaseHelper.getInstance().getDoctorsNodeRef()
            .child(doctor.getDoctorId())
            .child("isVisible")
            .setValue(isVisible)
            .addOnSuccessListener(aVoid -> {
                doctor.setDemo(isVisible);
                Toast.makeText(this, isVisible ? 
                    R.string.doctor_now_visible : R.string.doctor_now_hidden, Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                adapter.notifyItemChanged(position);
                Toast.makeText(this, R.string.error_saving, Toast.LENGTH_SHORT).show();
            });
    }

    private class DemoDoctorAdapter extends RecyclerView.Adapter<DemoDoctorAdapter.ViewHolder> {
        private List<DoctorEntity> doctors;

        DemoDoctorAdapter(List<DoctorEntity> doctors) {
            this.doctors = doctors;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_demo_doctor_toggle, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DoctorEntity doctor = doctors.get(position);
            holder.bind(doctor, position);
        }

        @Override
        public int getItemCount() {
            return doctors.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            CircleImageView ivProfile;
            TextView tvName, tvSpecialty;
            SwitchMaterial switchVisible;

            ViewHolder(View itemView) {
                super(itemView);
                ivProfile = itemView.findViewById(R.id.ivProfile);
                tvName = itemView.findViewById(R.id.tvName);
                tvSpecialty = itemView.findViewById(R.id.tvSpecialty);
                switchVisible = itemView.findViewById(R.id.switchVisible);
            }

            void bind(DoctorEntity doctor, int position) {
                tvName.setText(doctor.getAbout() != null ? doctor.getAbout() : "Demo Doctor");
                tvSpecialty.setText(doctor.getSpecialty() != null ? doctor.getSpecialty() : "General");
                
                Boolean isVisible = doctor.isDemo();
                switchVisible.setOnCheckedChangeListener(null);
                switchVisible.setChecked(isVisible == null || isVisible);
                
                switchVisible.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    toggleDoctorVisibility(doctor, isChecked, position);
                });
            }
        }
    }
}
