package com.haset.hasetapp.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.haset.hasetapp.R;
import com.haset.hasetapp.adapters.HospitalAdapter;
import com.haset.hasetapp.models.Hospital;
import com.haset.hasetapp.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.List;

public class HospitalsActivity extends LocalizedAppCompatActivity {

    private RecyclerView rvHospitals;
    private View layoutEmptyState;
    private HospitalAdapter adapter;
    private List<Hospital> hospitalList;

    // Pagination variables
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private static final int PAGE_SIZE = 10;
    private String lastKey = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hospitals);
        
        rvHospitals = findViewById(R.id.rvHospitals);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
        
        hospitalList = new ArrayList<>();
        adapter = new HospitalAdapter(hospitalList, hospital -> {
            String query = hospital.getName();
            if (hospital.getAddress() != null && !hospital.getAddress().isEmpty()) {
                query += ", " + hospital.getAddress();
            }
            android.content.Intent mapIntent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q=" + android.net.Uri.encode(query)));
            mapIntent.setPackage("com.google.android.apps.maps");
            try {
                startActivity(mapIntent);
            } catch (Exception e) {
                startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=" + android.net.Uri.encode(query))));
            }
        });
        
        if (rvHospitals != null) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            rvHospitals.setLayoutManager(layoutManager);
            rvHospitals.setAdapter(adapter);
            
            // Add scroll listener for pagination
            rvHospitals.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    
                    if (dy > 0) { // Scrolling down
                        int visibleItemCount = layoutManager.getChildCount();
                        int totalItemCount = layoutManager.getItemCount();
                        int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                        if (!isLoading && !isLastPage) {
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                loadMoreHospitals();
                            }
                        }
                    }
                }
            });
        }
        
        loadInitialHospitals();
    }
    
    private void loadInitialHospitals() {
        isLoading = true;
        FirebaseHelper.getInstance().getDatabaseReference().child("Hospitals")
            .orderByKey().limitToFirst(PAGE_SIZE)
            .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                    hospitalList.clear();
                    
                    if (snapshot.exists()) {
                        long count = 0;
                        for (com.google.firebase.database.DataSnapshot ds : snapshot.getChildren()) {
                            Hospital hospital = ds.getValue(Hospital.class);
                            if (hospital != null) {
                                hospital.setHospitalId(ds.getKey());
                                hospitalList.add(hospital);
                                lastKey = ds.getKey();
                            }
                            count++;
                        }
                        
                        if (count < PAGE_SIZE) {
                            isLastPage = true;
                        }
                    } else {
                        isLastPage = true;
                    }
                    
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                    isLoading = false;
                }

                @Override
                public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                    isLoading = false;
                    Toast.makeText(HospitalsActivity.this, "Failed to load hospitals", Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private void loadMoreHospitals() {
        if (lastKey == null) return;
        
        isLoading = true;
        // Firebase realtime db pagination via startAfter requires limitToFirst + 1 and dropping the first item which is overlapping
        FirebaseHelper.getInstance().getDatabaseReference().child("Hospitals")
            .orderByKey().startAfter(lastKey).limitToFirst(PAGE_SIZE)
            .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        long count = 0;
                        for (com.google.firebase.database.DataSnapshot ds : snapshot.getChildren()) {
                            Hospital hospital = ds.getValue(Hospital.class);
                            if (hospital != null) {
                                hospital.setHospitalId(ds.getKey());
                                hospitalList.add(hospital);
                                lastKey = ds.getKey();
                            }
                            count++;
                        }
                        
                        adapter.notifyDataSetChanged();
                        
                        // If we received fewer items than PAGE_SIZE, we hit the end
                        if (count < PAGE_SIZE) {
                            isLastPage = true;
                        }
                    } else {
                        isLastPage = true;
                    }
                    isLoading = false;
                }

                @Override
                public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                    isLoading = false;
                    Toast.makeText(HospitalsActivity.this, "Failed to load more hospitals", Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private void updateEmptyState() {
        if (hospitalList.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            if (rvHospitals != null) rvHospitals.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            if (rvHospitals != null) rvHospitals.setVisibility(View.VISIBLE);
        }
    }
}
