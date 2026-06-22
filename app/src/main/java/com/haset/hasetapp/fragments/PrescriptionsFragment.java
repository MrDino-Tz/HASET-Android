package com.haset.hasetapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.haset.hasetapp.R;
import com.haset.hasetapp.adapters.PrescriptionAdapter;
import com.haset.hasetapp.models.Prescription;
import com.haset.hasetapp.utils.AddPrescriptionBottomSheet;
import com.haset.hasetapp.utils.PreferenceManager;
import com.haset.hasetapp.utils.PrescriptionHelper;
import com.haset.hasetapp.viewmodels.PrescriptionViewModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Fragment to display list of prescriptions
 */
public class PrescriptionsFragment extends Fragment {
    
    private RecyclerView recyclerView;
    private PrescriptionAdapter adapter;
    private ProgressBar progressBar;
    private View emptyState;
    private FloatingActionButton fabAdd;
    
    private PrescriptionViewModel viewModel;
    private PreferenceManager preferenceManager;
    private String userId;
    private String userRole;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_prescriptions, container, false);
        
        initializeViews(view);
        setupRecyclerView();
        
        viewModel = new androidx.lifecycle.ViewModelProvider(this).get(PrescriptionViewModel.class);
        setupObservers();
        
        return view;
    }
    
    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewPrescriptions);
        progressBar = view.findViewById(R.id.progressBar);
        emptyState = view.findViewById(R.id.emptyState);
        fabAdd = view.findViewById(R.id.fabAddPrescription);
        
        preferenceManager = new PreferenceManager(requireContext());
        userId = preferenceManager.getUserId();
        userRole = preferenceManager.getUserRole();
        
        // Show FAB only for doctors
        if ("doctor".equalsIgnoreCase(userRole)) {
            fabAdd.setVisibility(View.VISIBLE);
            fabAdd.setOnClickListener(v -> showUploadBottomSheet());
        }
    }
    
    private void setupRecyclerView() {
        adapter = new PrescriptionAdapter(new ArrayList<>(), prescription -> {
            // Navigate to detail fragment
            navigateToDetail(prescription);
        });
        
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }
    
    private void setupObservers() {
        progressBar.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        
        viewModel.setUserInfo(userId, userRole);
        viewModel.getPrescriptions().observe(getViewLifecycleOwner(), prescriptions -> {
            progressBar.setVisibility(View.GONE);
            if (prescriptions == null || prescriptions.isEmpty()) {
                emptyState.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyState.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                adapter.updatePrescriptions(prescriptions);
            }
        });
    }
    
    
    private void showUploadBottomSheet() {
        AddPrescriptionBottomSheet bottomSheet = new AddPrescriptionBottomSheet();
        bottomSheet.show(getChildFragmentManager(), "AddPrescriptionBottomSheet");
    }
    
    private void navigateToDetail(Prescription prescription) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("prescription", (Serializable) prescription);
        
        PrescriptionDetailFragment fragment = new PrescriptionDetailFragment();
        fragment.setArguments(bundle);
        
        requireActivity().getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit();
    }
}
