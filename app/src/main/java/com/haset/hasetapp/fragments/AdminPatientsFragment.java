package com.haset.hasetapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.haset.hasetapp.R;
import com.haset.hasetapp.adapters.UserAdapter;
import com.haset.hasetapp.database.entities.UserEntity;
import com.haset.hasetapp.utils.FirebaseHelper;
import com.haset.hasetapp.utils.ShimmerHelper;
import androidx.lifecycle.ViewModelProvider;
import com.haset.hasetapp.viewmodels.AdminUsersViewModel;

import java.util.ArrayList;
import java.util.List;

public class AdminPatientsFragment extends Fragment {
    private RecyclerView rvPatients;
    private UserAdapter userAdapter;
    private View rootView;
    private LinearLayout shimmerContainer;
    private AdminUsersViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;
        rvPatients = view.findViewById(R.id.rvAdminContent);
        shimmerContainer = view.findViewById(R.id.shimmerContainer);
        setupRecyclerView();
        
        viewModel = new ViewModelProvider(this).get(AdminUsersViewModel.class);
        setupObservers();
    }

    private void setupObservers() {
        showShimmerLoading();
        viewModel.getPatients().observe(getViewLifecycleOwner(), patients -> {
            hideShimmerLoading();
            if (patients != null) {
                userAdapter.setUsers(patients);
                if (patients.isEmpty()) {
                    Snackbar.make(rootView, R.string.no_patients_found, Snackbar.LENGTH_SHORT).show();
                }
            } else {
                Snackbar.make(rootView, R.string.failed_to_load_patients, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void setupRecyclerView() {
        userAdapter = new UserAdapter();
        userAdapter.setOnUserClickListener(user -> {
            UserDetailsBottomSheet bottomSheet = UserDetailsBottomSheet.newInstance(user, "Patients");
            bottomSheet.setOnUserActionListener(new UserDetailsBottomSheet.OnUserActionListener() {
                @Override
                public void onUserUpdated() {
                    // Observers handle updates
                }

                @Override
                public void onUserDeleted() {
                    // Observers handle updates
                }

                @Override
                public void onDoctorApprovalChanged() {
                    // Observers handle updates
                }
            });
            bottomSheet.show(getParentFragmentManager(), bottomSheet.getTag());
        });
        rvPatients.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPatients.setAdapter(userAdapter);
    }
    
    @Override
    public void onResume() {
        super.onResume();
    }

    private void loadPatients() {
        // Handled by setupObservers
    }

    private void showShimmerLoading() {
        shimmerContainer.setVisibility(View.VISIBLE);
        rvPatients.setVisibility(View.GONE);
        ShimmerHelper.showListShimmer(requireContext(), shimmerContainer, 5, R.layout.shimmer_layout_user_item);
    }

    private void hideShimmerLoading() {
        ShimmerHelper.hideListShimmer(shimmerContainer);
        shimmerContainer.setVisibility(View.GONE);
        rvPatients.setVisibility(View.VISIBLE);
    }
}

