package com.haset.hasetapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.haset.hasetapp.R;
// DISABLED FOR V1 - RATING SYSTEM COMING IN VERSION 2.0
// import com.haset.hasetapp.adapters.ReviewsAdapter;
// import com.haset.hasetapp.utils.FirebaseHelper;
// import androidx.lifecycle.ViewModelProvider;
// import com.haset.hasetapp.viewmodels.ReviewsViewModel;

import java.util.Collections;
import java.util.List;

/* DISABLED FOR V1 - RATING SYSTEM COMING IN VERSION 2.0
public class DoctorReviewsFragment extends Fragment {

    private static final String ARG_DOCTOR_ID = "doctor_id";
    private String doctorId;
    
    private RecyclerView rvReviews;
    private ReviewsAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout layoutEmpty;
    private ImageButton btnBack;
    private ReviewsViewModel viewModel;

    public static DoctorReviewsFragment newInstance(String doctorId) {
        DoctorReviewsFragment fragment = new DoctorReviewsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DOCTOR_ID, doctorId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            doctorId = getArguments().getString(ARG_DOCTOR_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_doctor_reviews, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(ReviewsViewModel.class);
        
        setupObservers();
    }

    private void initViews(View view) {
        rvReviews = view.findViewById(R.id.rvReviews);
        progressBar = view.findViewById(R.id.progressBar);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        btnBack = view.findViewById(R.id.btnBack);
        
        rvReviews.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ReviewsAdapter(requireContext());
        rvReviews.setAdapter(adapter);
        
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());
    }

    private void setupObservers() {
        if (doctorId == null) return;
        
        progressBar.setVisibility(View.VISIBLE);
        
        viewModel.getReviews(doctorId).observe(getViewLifecycleOwner(), reviews -> {
            progressBar.setVisibility(View.GONE);
            if (reviews != null && !reviews.isEmpty()) {
                adapter.setReviews(reviews);
                layoutEmpty.setVisibility(View.GONE);
                rvReviews.setVisibility(View.VISIBLE);
            } else if (reviews != null) {
                layoutEmpty.setVisibility(View.VISIBLE);
                rvReviews.setVisibility(View.GONE);
            }
        });
    }
}
*/

// Placeholder class - Rating system disabled for V1
public class DoctorReviewsFragment extends Fragment {
    public static DoctorReviewsFragment newInstance(String doctorId) {
        return new DoctorReviewsFragment();
    }
}
