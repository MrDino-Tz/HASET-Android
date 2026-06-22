package com.haset.hasetapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.haset.hasetapp.R;
import com.haset.hasetapp.models.PharmacyProduct;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class PharmacyBestsellerAdapter extends RecyclerView.Adapter<PharmacyBestsellerAdapter.ViewHolder> {

    private List<PharmacyProduct> products;
    private OnProductClickListener listener;
    private DecimalFormat ratingFormat;

    public interface OnProductClickListener {
        void onProductClick(PharmacyProduct product);
    }

    public PharmacyBestsellerAdapter(List<PharmacyProduct> products, OnProductClickListener listener) {
        this.products = products != null ? products : new ArrayList<>();
        this.listener = listener;
        this.ratingFormat = new DecimalFormat("#.#");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pharmacy_product_bestseller, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PharmacyProduct product = products.get(position);

        holder.tvProductName.setText(product.getName());

        // Set product type/category
        String type = getCategoryDisplayName(product.getCategory());
        holder.tvProductType.setText(type);

        // Set rating (using a default rating for now, can be added to model later)
        float rating = 4.8f; // Default rating, can be added to PharmacyProduct model
        int reviewCount = 2200; // Default review count
        holder.tvRating.setText(ratingFormat.format(rating) + " (" + formatReviewCount(reviewCount) + ")");

        // Load image
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(product.getImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(holder.ivProductImage);
        } else {
            holder.ivProductImage.setImageResource(R.drawable.placeholder_image);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductClick(product);
            }
        });
    }

    private String getCategoryDisplayName(String category) {
        if (category == null) return "Product";
        switch (category.toLowerCase()) {
            case "medicine":
                return "Medicine";
            case "supplement":
                return "Supplements";
            case "equipment":
                return "Equipment";
            default:
                return "Product";
        }
    }

    private String formatReviewCount(int count) {
        if (count >= 1000) {
            return (count / 1000.0) + "k";
        }
        return String.valueOf(count);
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public void updateProducts(List<PharmacyProduct> newProducts) {
        this.products = newProducts != null ? newProducts : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProductImage;
        TextView tvProductName;
        TextView tvProductType;
        TextView tvRating;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductType = itemView.findViewById(R.id.tvProductType);
//            tvRating = itemView.findViewById(R.id.tvRating);
        }
    }
}

