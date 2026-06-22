package com.haset.hasetapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.haset.hasetapp.R;
import com.haset.hasetapp.adapters.CartItemAdapter;
import com.haset.hasetapp.models.CartItem;
import com.haset.hasetapp.viewmodels.PharmacyViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class PharmacyCartFragment extends Fragment {

    private RecyclerView rvCartItems;
    private LinearLayout layoutEmptyCart;
    private MaterialCardView cardCartSummary;
    private TextView tvSubtotal;
    private TextView tvTotal;
    private MaterialButton btnCheckout;
    private MaterialButton btnStartShopping;

    private DecimalFormat priceFormat;
    private CartItemAdapter adapter;
    private PharmacyViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pharmacy_cart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        
        viewModel = new ViewModelProvider(requireActivity()).get(PharmacyViewModel.class);
        setupObservers();
        setupClickListeners();
    }

    private void setupObservers() {
        viewModel.getCartItems().observe(getViewLifecycleOwner(), items -> {
            if (items != null) {
                if (items.isEmpty()) {
                    showEmptyState();
                } else {
                    showCartItems();
                    adapter.updateItems(items);
                }
            }
        });

        viewModel.getCartSubtotal().observe(getViewLifecycleOwner(), subtotal -> {
            if (subtotal != null) {
                tvSubtotal.setText(priceFormat.format(subtotal) + " TZS");
                tvTotal.setText(priceFormat.format(subtotal) + " TZS"); // Assuming no additional fees for now
            }
        });
    }

    private void initViews(View view) {
        rvCartItems = view.findViewById(R.id.rvCartItems);
        layoutEmptyCart = view.findViewById(R.id.layoutEmptyCart);
        cardCartSummary = view.findViewById(R.id.cardCartSummary);
        tvSubtotal = view.findViewById(R.id.tvSubtotal);
        tvTotal = view.findViewById(R.id.tvTotal);
        btnCheckout = view.findViewById(R.id.btnCheckout);
        btnStartShopping = view.findViewById(R.id.btnStartShopping);
        priceFormat = new DecimalFormat("#,###");
    }

    private void setupRecyclerView() {
        adapter = new CartItemAdapter(new ArrayList<>(), new CartItemAdapter.OnCartItemChangeListener() {
            @Override
            public void onQuantityChange(CartItem item, int newQuantity) {
                viewModel.updateQuantity(item.getProductId(), newQuantity);
            }

            @Override
            public void onRemoveItem(CartItem item) {
                viewModel.removeFromCart(item.getProductId());
            }
        });
        rvCartItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCartItems.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnStartShopping.setOnClickListener(v -> {
            // Navigate back to home or switch to home fragment
            if (getActivity() instanceof com.haset.hasetapp.activities.PharmacyActivity) {
                com.haset.hasetapp.activities.PharmacyActivity activity = 
                    (com.haset.hasetapp.activities.PharmacyActivity) getActivity();
                activity.switchToHomeFragment();
            }
        });

        btnCheckout.setOnClickListener(v -> {
            // Navigate to checkout
            // Intent checkoutIntent = new Intent(requireContext(), CheckoutActivity.class);
            // startActivity(checkoutIntent);
        });
    }

    private void loadCartItems() {
        // Handled by setupObservers
    }

    private void showEmptyState() {
        layoutEmptyCart.setVisibility(View.VISIBLE);
        rvCartItems.setVisibility(View.GONE);
        cardCartSummary.setVisibility(View.GONE);
    }

    private void showCartItems() {
        layoutEmptyCart.setVisibility(View.GONE);
        rvCartItems.setVisibility(View.VISIBLE);
        cardCartSummary.setVisibility(View.VISIBLE);
    }

    private void updateCartSummary(ArrayList<Object> cartItems) {
        // Handled by setupObservers
    }
}

