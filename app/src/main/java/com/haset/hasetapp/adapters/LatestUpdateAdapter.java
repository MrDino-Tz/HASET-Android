package com.haset.hasetapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.haset.hasetapp.R;

import java.util.List;

public class LatestUpdateAdapter extends RecyclerView.Adapter<LatestUpdateAdapter.UpdateViewHolder> {

    private List<LatestUpdate> updates;
    private OnUpdateClickListener listener;

    public LatestUpdateAdapter(List<LatestUpdate> updates, OnUpdateClickListener listener) {
        this.updates = updates;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UpdateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_latest_update, parent, false);
        return new UpdateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UpdateViewHolder holder, int position) {
        LatestUpdate update = updates.get(position);
        holder.ivUpdateImage.setImageResource(update.getImageResId());
        holder.tvUpdateTitle.setText(update.getTitle());
        holder.tvUpdateDescription.setText(update.getDescription());
        holder.btnAction.setText(update.getActionText());

        holder.itemView.setOnClickListener(v -> listener.onUpdateClick(update));
        holder.btnAction.setOnClickListener(v -> listener.onUpdateClick(update));
    }

    @Override
    public int getItemCount() {
        return updates.size();
    }

    static class UpdateViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUpdateImage;
        TextView tvUpdateTitle;
        TextView tvUpdateDescription;
        MaterialButton btnAction;

        public UpdateViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUpdateImage = itemView.findViewById(R.id.ivUpdateImage);
            tvUpdateTitle = itemView.findViewById(R.id.tvUpdateTitle);
            tvUpdateDescription = itemView.findViewById(R.id.tvUpdateDescription);
            btnAction = itemView.findViewById(R.id.btnAction);
        }
    }

    public interface OnUpdateClickListener {
        void onUpdateClick(LatestUpdate update);
    }

    public static class LatestUpdate {
        private int imageResId;
        private String title;
        private String description;
        private String actionText;

        public LatestUpdate(int imageResId, String title, String description, String actionText) {
            this.imageResId = imageResId;
            this.title = title;
            this.description = description;
            this.actionText = actionText;
        }

        public int getImageResId() {
            return imageResId;
        }

        public String getTitle() {
            return title;
        }
        
        public String getDescription() {
            return description;
        }

        public String getActionText() {
            return actionText;
        }
    }
}
