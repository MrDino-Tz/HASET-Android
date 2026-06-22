package com.haset.hasetapp.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.haset.hasetapp.R;
import com.haset.hasetapp.adapters.PharmacyProductAdapter;
import com.haset.hasetapp.models.PharmacyProduct;
import androidx.lifecycle.ViewModelProvider;
import com.haset.hasetapp.viewmodels.PharmacyViewModel;

import java.util.ArrayList;
import java.util.List;

public class PharmacyTabFragment extends Fragment {

    private static final String ARG_TAB_TITLE = "tab_title";
    private static final String ARG_SEARCH_QUERY = "search_query";

    private RecyclerView rvProducts;
    private LinearLayout layoutEmptyState;
    private PharmacyProductAdapter adapter;
    private List<PharmacyProduct> allProducts = new ArrayList<>();
    private String currentCategory;
    private String searchQuery = "";
    private PharmacyViewModel viewModel;

    public static PharmacyTabFragment newInstance(String tabTitle) {
        PharmacyTabFragment fragment = new PharmacyTabFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TAB_TITLE, tabTitle);
        fragment.setArguments(args);
        return fragment;
    }

    public static PharmacyTabFragment newInstance(String tabTitle, String searchQuery) {
        PharmacyTabFragment fragment = new PharmacyTabFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TAB_TITLE, tabTitle);
        args.putString(ARG_SEARCH_QUERY, searchQuery);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pharmacy_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        rvProducts = view.findViewById(R.id.rvProducts);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        
        if (getArguments() != null) {
            currentCategory = getArguments().getString(ARG_TAB_TITLE);
            searchQuery = getArguments().getString(ARG_SEARCH_QUERY, "");
        }
        
        setupRecyclerView();
        
        viewModel = new ViewModelProvider(requireActivity()).get(PharmacyViewModel.class);
        setupObservers();
    }

    private void setupObservers() {
        viewModel.getProductsByCategory(currentCategory).observe(getViewLifecycleOwner(), products -> {
            if (products != null) {
                allProducts = products;
                filterAndDisplayProducts();
            }
        });

        viewModel.getSearchQuery().observe(getViewLifecycleOwner(), query -> {
            searchQuery = query != null ? query : "";
            filterAndDisplayProducts();
        });
    }

    private void setupRecyclerView() {
        adapter = new PharmacyProductAdapter(new ArrayList<>(), new PharmacyProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(PharmacyProduct product) {
                // Handle product click - could open product details
                // For now, just a placeholder
            }

            @Override
            public void onAddToCartClick(PharmacyProduct product) {
                viewModel.addToCart(product, 1);
                // Could show a snackbar confirmation
            }
        });
        
        rvProducts.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvProducts.setAdapter(adapter);
    }

    private void filterAndDisplayProducts() {
        List<PharmacyProduct> filteredProducts = new ArrayList<>();
        
        for (PharmacyProduct product : allProducts) {
            // Filter by search query
            boolean matchesSearch = TextUtils.isEmpty(searchQuery) ||
                    product.getName().toLowerCase().contains(searchQuery.toLowerCase()) ||
                    (product.getManufacturer() != null && product.getManufacturer().toLowerCase().contains(searchQuery.toLowerCase()));
            
            if (matchesSearch) {
                filteredProducts.add(product);
            }
        }
        
        adapter.updateProducts(filteredProducts);
        updateEmptyState(filteredProducts.isEmpty());
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            rvProducts.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvProducts.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }
}
