package com.haset.hasetapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.haset.hasetapp.R;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.VH> {
    public static class Category {
        public int iconRes;
        public String name;
        public Category(int iconRes, String name) {
            this.iconRes = iconRes;
            this.name = name;
        }
    }
    public interface OnCategoryClickListener {
        void onCategoryClick(Category cat);
    }
    private final List<Category> list;
    private final OnCategoryClickListener listener;
    public CategoryAdapter(List<Category> list, OnCategoryClickListener listener) {
        this.list = list;
        this.listener = listener;
    }
    static class VH extends RecyclerView.ViewHolder {
        ImageView iv;
        TextView tv;
        public VH(View v) {
            super(v);
            iv = v.findViewById(R.id.ivCategoryIcon);
            tv = v.findViewById(R.id.tvCategoryName);
        }
    }
    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_card, parent, false));
    }
    @Override
    public void onBindViewHolder(@NonNull VH holder, int pos) {
        Category cat = list.get(pos);
        holder.iv.setImageResource(cat.iconRes);
        holder.tv.setText(cat.name);
        holder.itemView.setOnClickListener(v -> listener.onCategoryClick(cat));
    }
    @Override public int getItemCount() { return list.size(); }
}
