package com.haset.hasetapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
        holder.bind(tips.get(position), position);
    }

    @Override
    public int getItemCount() { return tips.size(); }

    static class HealthTipViewHolder extends RecyclerView.ViewHolder {
        private static final int[] TIP_ICONS = {
                R.drawable.ic_tips_icon,
                R.drawable.ic_tips_icon2,
                R.drawable.ic_tips_icon3,
                R.drawable.ic_tips_icon4
        };

        private final ImageView tipIcon;
        private final TextView tipText;
        private final TextView tipAuthor;
        private final TextView timeAgo;

        HealthTipViewHolder(@NonNull View itemView) {
            super(itemView);
            tipIcon = itemView.findViewById(R.id.ivHealthTipIcon);
            tipText = itemView.findViewById(R.id.tvHealthTipText);
            tipAuthor = itemView.findViewById(R.id.tvHealthTipAuthor);
            timeAgo = itemView.findViewById(R.id.tvTimeAgo);
        }

        void bind(HealthTip tip, int position) {
            tipIcon.setImageResource(TIP_ICONS[position % TIP_ICONS.length]);
            tipText.setText(tip.getText());
            if (tip.getAuthor() != null && !tip.getAuthor().isEmpty()) {
                tipAuthor.setText(tip.getAuthor());
                tipAuthor.setVisibility(View.VISIBLE);
            } else {
                tipAuthor.setVisibility(View.GONE);
            }
            timeAgo.setText(com.haset.hasetapp.utils.DateTimeUtils.getTimeAgo(tip.getTimestamp()));
        }
    }
}
