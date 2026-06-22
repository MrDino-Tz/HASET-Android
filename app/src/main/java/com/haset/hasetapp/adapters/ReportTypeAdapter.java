package com.haset.hasetapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.haset.hasetapp.R;

import java.util.List;

public class ReportTypeAdapter extends RecyclerView.Adapter<ReportTypeAdapter.ReportTypeViewHolder> {
    private List<ReportType> reportTypes;
    private OnReportTypeClickListener listener;
    private int selectedPosition = -1;

    public interface OnReportTypeClickListener {
        void onReportTypeClick(ReportType reportType);
    }

    public static class ReportType {
        private String title;
        private String description;
        private int iconRes;

        public ReportType(String title, String description) {
            this.title = title;
            this.description = description;
            this.iconRes = R.drawable.ic_document;
        }

        public ReportType(String title, String description, int iconRes) {
            this.title = title;
            this.description = description;
            this.iconRes = iconRes;
        }

        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public int getIconRes() { return iconRes; }
    }

    public ReportTypeAdapter(List<ReportType> reportTypes, OnReportTypeClickListener listener) {
        this.reportTypes = reportTypes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReportTypeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report_list, parent, false);
        return new ReportTypeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportTypeViewHolder holder, int position) {
        ReportType reportType = reportTypes.get(position);
        holder.bind(reportType, position == selectedPosition);
        
        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = position;
            if (previousSelected >= 0) {
                notifyItemChanged(previousSelected);
            }
            notifyItemChanged(selectedPosition);
            if (listener != null) {
                listener.onReportTypeClick(reportType);
            }
        });
    }

    @Override
    public int getItemCount() {
        return reportTypes.size();
    }

    static class ReportTypeViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout layoutReportItem;
        private ImageView ivIcon;
        private TextView tvTitle;
        private TextView tvDescription;

        public ReportTypeViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutReportItem = itemView.findViewById(R.id.layoutReportItem);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvTitle = itemView.findViewById(R.id.tvReportTitle);
            tvDescription = itemView.findViewById(R.id.tvReportDescription);
        }

        public void bind(ReportType reportType, boolean isSelected) {
            tvTitle.setText(reportType.getTitle());
            tvDescription.setText(reportType.getDescription());
            ivIcon.setImageResource(reportType.getIconRes());
            
            if (isSelected) {
                layoutReportItem.setBackgroundResource(R.drawable.bg_selected_item);
            } else {
                layoutReportItem.setBackgroundResource(R.drawable.ripple_effect_rounded);
            }
        }
    }
}
