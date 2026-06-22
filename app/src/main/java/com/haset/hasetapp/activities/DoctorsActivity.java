package com.haset.hasetapp.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import android.widget.PopupWindow;
import androidx.appcompat.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.haset.hasetapp.R;
import com.haset.hasetapp.adapters.DoctorAdapter;
import com.haset.hasetapp.database.entities.UserEntity;
import com.haset.hasetapp.fragments.DoctorDetailsBottomSheet;
import com.haset.hasetapp.models.Doctor;
import com.haset.hasetapp.utils.Constants;
import com.haset.hasetapp.utils.FirebaseHelper;
import com.haset.hasetapp.utils.ShimmerHelper;
import com.facebook.shimmer.ShimmerFrameLayout;

import androidx.lifecycle.ViewModelProvider;
import com.haset.hasetapp.viewmodels.DoctorsViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DoctorsActivity extends BaseActivity implements DoctorAdapter.OnDoctorClickListener {
    private static final String TAG = "DoctorsActivity";
    
    private RecyclerView rvDoctors;
    private RecyclerView rvShimmer;
    private TextInputEditText etSearch;
    private ProgressBar progressBar;
    private View emptyStateLayout;
    private ImageView btnBack;
    private ImageView btnMore;
    private DoctorAdapter doctorAdapter;
    private DoctorsViewModel viewModel;
    private int currentLimit = 8;
    private boolean isCurrentlyLoadingMore = false;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefreshLayout;
    private List<Doctor> allDisplayDoctors = new ArrayList<>();
    private android.os.Handler loadMoreHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctors);
        
        initViews();
        setupRecyclerView();
        setupSearchListener();
        
        viewModel = new ViewModelProvider(this).get(DoctorsViewModel.class);
        setupObservers();
        
        // Initialize Handler for load more functionality
        loadMoreHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    }

    private void setupObservers() {
        showLoading(true);
        viewModel.getDoctors().observe(this, doctors -> {
            allDisplayDoctors = doctors != null ? doctors : new ArrayList<>();
            updateUI();
            showLoading(false);
        });
    }

    private void loadDoctorsFromFirebase() {
        if (swipeRefreshLayout == null || !swipeRefreshLayout.isRefreshing()) {
            showLoading(true);
        }
        // Trigger refresh through ViewModel
        viewModel.refreshDoctors();
    }

    private void initViews() {
        rvDoctors = findViewById(R.id.rvDoctors);
        rvShimmer = findViewById(R.id.rvShimmer);
        etSearch = findViewById(R.id.etSearch);
        progressBar = findViewById(R.id.progressBar);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        btnBack = findViewById(R.id.btnBack);
        btnMore = findViewById(R.id.btnMore);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        
        
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.green_primary));
            swipeRefreshLayout.setOnRefreshListener(this::loadDoctorsFromFirebase);
        }

        // Setup back button
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
        
        // Setup more button with popup menu
        if (btnMore != null) {
            btnMore.setOnClickListener(v -> showMoreOptionsMenu());
        }
        
        // Setup shimmer RecyclerView
        setupShimmerRecyclerView();
    }
    
    private void setupShimmerRecyclerView() {
        rvShimmer.setLayoutManager(new LinearLayoutManager(this));
        // Create a simple adapter that shows shimmer items
        RecyclerView.Adapter<RecyclerView.ViewHolder> shimmerAdapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
                ShimmerFrameLayout shimmerLayout = ShimmerHelper.createShimmerLayout(
                        DoctorsActivity.this, 
                        R.layout.shimmer_layout_doctor_item
                );
                return new RecyclerView.ViewHolder(shimmerLayout) {};
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                // Shimmer items don't need binding
            }

            @Override
            public int getItemCount() {
                return 6; // Show 6 shimmer items (3 rows x 2 columns)
            }
        };
        rvShimmer.setAdapter(shimmerAdapter);
    }

    private void setupRecyclerView() {
        doctorAdapter = new DoctorAdapter(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvDoctors.setLayoutManager(layoutManager);
        rvDoctors.setAdapter(doctorAdapter);

        // Implement Scroll Listener for Pagination
        rvDoctors.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // Trigger when we are close to the bottom (within 2 items)
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!isCurrentlyLoadingMore && currentLimit < allDisplayDoctors.size()) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2) {
                        loadMoreDoctors();
                    }
                }
            }
        });
    }

    private void loadMoreDoctors() {
        isCurrentlyLoadingMore = true;
        doctorAdapter.setLoading(true);

        // Simulate a small delay for the "loader" effect
        if (loadMoreHandler != null) {
            loadMoreHandler.postDelayed(() -> {
                if (!isFinishing()) {
                    currentLimit += 6;
                    isCurrentlyLoadingMore = false;
                    doctorAdapter.setLoading(false);
                    updateUI();
                }
            }, 1000); // 1 second delay to show the loader
        }
    }

    private void setupSearchListener() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentLimit = 8;
                viewModel.setSearchQuery(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    
    private void sortDoctors(String criterion) {
        currentLimit = 8;
        viewModel.setSortBy(criterion);
    }
    
    private void sortDoctors() {
        sortDoctors("name");
    }



    private void showLoading(boolean show) {
        if (show) {
            // Show shimmer, hide content and empty state
            if (rvShimmer != null) {
                rvShimmer.setVisibility(View.VISIBLE);
            }
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setVisibility(View.GONE);
            }
            if (emptyStateLayout != null) {
                emptyStateLayout.setVisibility(View.GONE);
            }
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
        } else {
            // Hide shimmer
            if (rvShimmer != null) {
                rvShimmer.setVisibility(View.GONE);
            }
            
            // Stop refresh spinner
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }

            // Data visibility is handled by updateUI() which is called before showLoading(false)
            // But we ensure that if we are NOT loading, at least one of (List or EmptyState) is shown
            if (allDisplayDoctors == null || allDisplayDoctors.isEmpty()) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setVisibility(View.GONE);
                if (emptyStateLayout != null) emptyStateLayout.setVisibility(View.VISIBLE);
            } else {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setVisibility(View.VISIBLE);
                if (emptyStateLayout != null) emptyStateLayout.setVisibility(View.GONE);
            }
            
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
        }
    }

    private void updateUI() {
        if (allDisplayDoctors.isEmpty()) {
            // Show empty state
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setVisibility(View.GONE);
            }
            if (emptyStateLayout != null) {
                emptyStateLayout.setVisibility(View.VISIBLE);
            }
        } else {
            // Show doctors list
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setVisibility(View.VISIBLE);
            }
            if (emptyStateLayout != null) {
                emptyStateLayout.setVisibility(View.GONE);
            }
        }
        
        // Only show items up to currentLimit
        List<Doctor> paginatedList = new ArrayList<>();
        int limit = Math.min(currentLimit, allDisplayDoctors.size());
        for (int i = 0; i < limit; i++) {
            paginatedList.add(allDisplayDoctors.get(i));
        }
        
        doctorAdapter.setDoctors(paginatedList);
    }

    @Override
    public void onBookClick(Doctor doctor) {
        // Handle book appointment click
        if (doctor != null) {
            Log.d(TAG, "Book appointment clicked for: " + doctor.getFullName());
            // You can navigate to BookAppointmentActivity here if needed
        }
    }

    @Override
    public void onDoctorClick(Doctor doctor) {
        // Handle doctor card click - open doctor details bottom sheet
        if (doctor != null) {
            Log.d(TAG, "Doctor clicked: " + doctor.getFullName());
            DoctorDetailsBottomSheet bottomSheet = DoctorDetailsBottomSheet.newInstance(doctor);
            bottomSheet.show(getSupportFragmentManager(), "doctor_details_bottom_sheet");
        }
    }
    
    private void showMoreOptionsMenu() {
        // Inflate custom popup layout
        View popupView = LayoutInflater.from(this).inflate(R.layout.dialog_doctors_menu, null);
        
        // Create PopupWindow
        final PopupWindow popupWindow = new PopupWindow(
                popupView, 
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 
                true
        );

        // Set elevation and animation
        popupWindow.setElevation(10);
        
        // Get views
        TextView tvSortName = popupView.findViewById(R.id.tvSortName);
        TextView tvSortRating = popupView.findViewById(R.id.tvSortRating);
        TextView tvSortExperience = popupView.findViewById(R.id.tvSortExperience);
        TextView tvRefresh = popupView.findViewById(R.id.tvRefresh);
        TextView tvFilterSpecialty = popupView.findViewById(R.id.tvFilterSpecialty);
        
        // Set click listeners
        tvSortName.setOnClickListener(v -> {
            sortDoctors("name");
            Toast.makeText(this, R.string.sorted_by_name, Toast.LENGTH_SHORT).show();
            popupWindow.dismiss();
        });
        
        tvSortRating.setOnClickListener(v -> {
            sortDoctors("rating");
            Toast.makeText(this, R.string.sorted_by_rating, Toast.LENGTH_SHORT).show();
            popupWindow.dismiss();
        });
        
        tvSortExperience.setOnClickListener(v -> {
            sortDoctors("experience");
            Toast.makeText(this, R.string.sorted_by_experience, Toast.LENGTH_SHORT).show();
            popupWindow.dismiss();
        });
        
        tvRefresh.setOnClickListener(v -> {
            loadDoctorsFromFirebase();
            Toast.makeText(this, R.string.refreshing_doctors, Toast.LENGTH_SHORT).show();
            popupWindow.dismiss();
        });
        
        tvFilterSpecialty.setOnClickListener(v -> {
            popupWindow.dismiss();
            showSpecialtyFilterDialog();
        });
        
        // Show popup anchored to the button
        popupWindow.showAsDropDown(btnMore, 0, 8);
    }
    
    private void showSpecialtyFilterDialog() {
        // Get all unique specialties from doctors
        Set<String> specialties = new HashSet<>();
        for (Doctor doctor : allDisplayDoctors) {
            if (doctor.getSpecialty() != null && !doctor.getSpecialty().isEmpty()) {
                specialties.add(doctor.getSpecialty());
            }
        }
        
        List<String> specialtyList = new ArrayList<>(specialties);
        Collections.sort(specialtyList);
        specialtyList.add(0, getString(R.string.all_specialties));
        
        String[] specialtyArray = specialtyList.toArray(new String[0]);
        int currentIndex = 0; // Default to all
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(R.string.filter_by_specialty);
        builder.setSingleChoiceItems(specialtyArray, currentIndex, (dialog, which) -> {
            String selected = which == 0 ? null : specialtyList.get(which);
            viewModel.setSpecialtyFilter(selected);
        });
        builder.setPositiveButton(R.string.apply, (dialog, which) -> {
            currentLimit = 8;
            Toast.makeText(this, R.string.filter_applied, Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.setNeutralButton("Clear", (dialog, which) -> {
            currentLimit = 8;
            viewModel.setSpecialtyFilter(null);
            Toast.makeText(this, R.string.filter_cleared, Toast.LENGTH_SHORT).show();
        });
        builder.show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up Handler to prevent memory leaks
        if (loadMoreHandler != null) {
            loadMoreHandler.removeCallbacksAndMessages(null);
            loadMoreHandler = null;
        }
    }
}
