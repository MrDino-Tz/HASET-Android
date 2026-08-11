package com.haset.hasetapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.haset.hasetapp.R;
import com.haset.hasetapp.models.HealthTip;

import java.util.ArrayList;
import java.util.List;

public class HealthTipAdapter extends RecyclerView.Adapter<HealthTipAdapter.HealthTipViewHolder> {
    private final List<HealthTip> tips = new ArrayList<>();

    public void setTips(List<HealthTip> newTips) {
        tips.clear();
        if (newTips != null) tips.addAll(newTips);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HealthTipViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_health_tip, parent, false);
        return new HealthTipViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HealthTipViewHolder holder, int position) {
        holder.bind(tips.get(position));
    }

    @Override
    public int getItemCount() { return tips.size(); }

    static class HealthTipViewHolder extends RecyclerView.ViewHolder {
        private final TextView tipText;
        private final TextView tipAuthor;

        HealthTipViewHolder(@NonNull View itemView) {
            super(itemView);
            tipText = itemView.findViewById(R.id.tvHealthTipText);
            tipAuthor = itemView.findViewById(R.id.tvHealthTipAuthor);
        }

        void bind(HealthTip tip) {
            tipText.setText(tip.getText());
            tipAuthor.setText(itemView.getContext().getString(
                    R.string.health_tip_author_format, tip.getAuthor()));
        }
    }
}
