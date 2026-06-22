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

import java.util.ArrayList;
import java.util.List;

public class PharmacyCategoryAdapter extends RecyclerView.Adapter<PharmacyCategoryAdapter.ViewHolder> {

    private List<Category> categories;
    private OnCategoryClickListener listener;

    public static class Category {
        public String name;
        public int iconRes;
        public String categoryKey; // "medicine", "supplement", "equipment", etc.

        public Category(String name, int iconRes, String categoryKey) {
            this.name = name;
            this.iconRes = iconRes;
            this.categoryKey = categoryKey;
        }
    }

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    public PharmacyCategoryAdapter(List<Category> categories, OnCategoryClickListener listener) {
        this.categories = categories != null ? categories : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pharmacy_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.tvCategoryName.setText(category.name);
        holder.ivCategoryIcon.setImageResource(category.iconRes);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCategoryClick(category);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCategoryIcon;
        TextView tvCategoryName;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
        }
    }
}

