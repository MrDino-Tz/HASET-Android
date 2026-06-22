package com.haset.hasetapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.haset.hasetapp.R;
import com.haset.hasetapp.models.PharmacyProduct;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PharmacyProductAdapter extends RecyclerView.Adapter<PharmacyProductAdapter.ViewHolder> {

    private List<PharmacyProduct> productList;
    private OnProductClickListener listener;
    private DecimalFormat priceFormat;

    public interface OnProductClickListener {
        void onProductClick(PharmacyProduct product);
        void onAddToCartClick(PharmacyProduct product);
    }

    public PharmacyProductAdapter(List<PharmacyProduct> productList, OnProductClickListener listener) {
        this.productList = productList != null ? productList : new ArrayList<>();
        this.listener = listener;
        this.priceFormat = new DecimalFormat("#,###");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pharmacy_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PharmacyProduct product = productList.get(position);
        
        holder.tvProductName.setText(product.getName());
        holder.tvManufacturer.setText(product.getManufacturer() != null ? product.getManufacturer() : "");
        
        // Format price
        String priceText = priceFormat.format(product.getPrice()) + " TZS";
        holder.tvPrice.setText(priceText);
        
        // Set unit
        if (product.getUnit() != null && !product.getUnit().isEmpty()) {
            holder.tvUnit.setText("/" + product.getUnit());
            holder.tvUnit.setVisibility(View.VISIBLE);
        } else {
            holder.tvUnit.setVisibility(View.GONE);
        }
        
        // Stock status
        if (product.isInStock()) {
            holder.tvStockStatus.setText(holder.itemView.getContext().getString(R.string.in_stock));
            holder.tvStockStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.green_primary));
            holder.btnAddToCart.setEnabled(true);
        } else {
            holder.tvStockStatus.setText(holder.itemView.getContext().getString(R.string.out_of_stock));
            holder.tvStockStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary));
            holder.btnAddToCart.setEnabled(false);
        }
        holder.tvStockStatus.setVisibility(View.VISIBLE);
        
        // Load image with Glide
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(product.getImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(holder.ivProductImage);
        } else {
            holder.ivProductImage.setImageResource(R.drawable.placeholder_image);
        }
        
        // Click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductClick(product);
            }
        });
        
        holder.btnAddToCart.setOnClickListener(v -> {
            if (listener != null && product.isInStock()) {
                listener.onAddToCartClick(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public void updateProducts(List<PharmacyProduct> newProducts) {
        this.productList = newProducts != null ? newProducts : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void filterProducts(String query) {
        if (query == null || query.trim().isEmpty()) {
            // If query is empty, show all products (this will be handled by the fragment)
            return;
        }
        
        // Filtering will be handled by the fragment
        // This method is here for future use if needed
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProductImage;
        TextView tvProductName;
        TextView tvManufacturer;
        TextView tvPrice;
        TextView tvUnit;
        TextView tvStockStatus;
        MaterialButton btnAddToCart;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvManufacturer = itemView.findViewById(R.id.tvManufacturer);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvUnit = itemView.findViewById(R.id.tvUnit);
            tvStockStatus = itemView.findViewById(R.id.tvStockStatus);
            btnAddToCart = itemView.findViewById(R.id.btnAddToCart);
        }
    }
}

