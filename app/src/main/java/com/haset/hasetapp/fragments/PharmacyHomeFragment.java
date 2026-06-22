package com.haset.hasetapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.haset.hasetapp.R;
import com.haset.hasetapp.adapters.PharmacyBestsellerAdapter;
import com.haset.hasetapp.adapters.PharmacyCategoryAdapter;
import com.haset.hasetapp.models.PharmacyProduct;
import androidx.lifecycle.ViewModelProvider;
import com.haset.hasetapp.viewmodels.PharmacyViewModel;
import com.haset.hasetapp.utils.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class PharmacyHomeFragment extends Fragment {

    private RecyclerView rvCategories;
    private RecyclerView rvBestsellerProducts;
    private TextView tvSeeAllCategories;
    private TextView tvSeeAllProducts;
    private MaterialButton btnShopNow;

    private PharmacyCategoryAdapter categoryAdapter;
    private PharmacyBestsellerAdapter bestsellerAdapter;
    private List<PharmacyProduct> allProducts = new ArrayList<>();
    private PharmacyViewModel viewModel;
    private PreferenceManager preferenceManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pharmacy_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        preferenceManager = new PreferenceManager(requireContext());
        viewModel = new ViewModelProvider(requireActivity()).get(PharmacyViewModel.class);
        viewModel.setUserId(preferenceManager.getUserId());

        initViews(view);
        setupCategories();
        setupBestsellerProducts();
        setupObservers();
        setupClickListeners();
    }

    private void setupObservers() {
        viewModel.getBestsellers().observe(getViewLifecycleOwner(), products -> {
            if (products != null) {
                allProducts = products;
                bestsellerAdapter.updateProducts(products);
            }
        });

        viewModel.getSearchQuery().observe(getViewLifecycleOwner(), this::filterProducts);
    }

    private void initViews(View view) {
        rvCategories = view.findViewById(R.id.rvCategories);
        rvBestsellerProducts = view.findViewById(R.id.rvBestsellerProducts);
        tvSeeAllCategories = view.findViewById(R.id.tvSeeAllCategories);
        tvSeeAllProducts = view.findViewById(R.id.tvSeeAllProducts);
        btnShopNow = view.findViewById(R.id.btnShopNow);
    }

    private void setupCategories() {
        List<PharmacyCategoryAdapter.Category> categories = new ArrayList<>();
        categories.add(new PharmacyCategoryAdapter.Category(getString(R.string.medicines), R.drawable.placeholder_image, "medicine"));
        categories.add(new PharmacyCategoryAdapter.Category(getString(R.string.supplements), R.drawable.placeholder_image, "supplement"));
        categories.add(new PharmacyCategoryAdapter.Category(getString(R.string.health_devices), R.drawable.placeholder_image, "equipment"));
        categories.add(new PharmacyCategoryAdapter.Category(getString(R.string.personal_care), R.drawable.placeholder_image, "personal_care"));

        categoryAdapter = new PharmacyCategoryAdapter(categories, category -> {
            // Handle category click - could filter products or navigate
            // For now, just a placeholder
        });

        rvCategories.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvCategories.setAdapter(categoryAdapter);
    }

    private void setupBestsellerProducts() {
        bestsellerAdapter = new PharmacyBestsellerAdapter(new ArrayList<>(), product -> {
            // Handle product click
        });

        rvBestsellerProducts.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvBestsellerProducts.setAdapter(bestsellerAdapter);
    }

    private List<PharmacyProduct> createSampleProducts() {
        return new ArrayList<>();
    }

    private void setupClickListeners() {
        // Shop Now button
        btnShopNow.setOnClickListener(v -> {
            // Scroll to products
            rvBestsellerProducts.smoothScrollToPosition(0);
        });

        // See All Categories
        tvSeeAllCategories.setOnClickListener(v -> {
            // Could show all categories in a dialog or navigate
        });

        // See All Products
        tvSeeAllProducts.setOnClickListener(v -> {
            // Navigate to all products screen
            // Intent allProductsIntent = new Intent(requireContext(), AllProductsActivity.class);
            // startActivity(allProductsIntent);
        });
    }

    public void filterProducts(String query) {
        if (query == null || query.trim().isEmpty()) {
            // Show all bestsellers
            List<PharmacyProduct> bestsellers = new ArrayList<>();
            for (int i = 0; i < Math.min(5, allProducts.size()); i++) {
                bestsellers.add(allProducts.get(i));
            }
            bestsellerAdapter.updateProducts(bestsellers);
            return;
        }

        // Filter products by search query
        List<PharmacyProduct> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        for (PharmacyProduct product : allProducts) {
            if (product.getName().toLowerCase().contains(lowerQuery) ||
                (product.getManufacturer() != null && product.getManufacturer().toLowerCase().contains(lowerQuery))) {
                filtered.add(product);
            }
        }
        bestsellerAdapter.updateProducts(filtered);
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Clear adapters
        if (rvCategories != null) {
            rvCategories.setAdapter(null);
        }
        if (rvBestsellerProducts != null) {
            rvBestsellerProducts.setAdapter(null);
        }
        categoryAdapter = null;
        bestsellerAdapter = null;
        
        // Null out view references
        rvCategories = null;
        rvBestsellerProducts = null;
        tvSeeAllCategories = null;
        tvSeeAllProducts = null;
        btnShopNow = null;
    }
}

