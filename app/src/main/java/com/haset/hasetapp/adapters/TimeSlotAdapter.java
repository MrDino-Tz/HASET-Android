package com.haset.hasetapp.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.haset.hasetapp.R;

import java.util.List;

public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.TimeSlotViewHolder> {
    private List<String> timeSlots;
    private OnTimeSlotClickListener listener;
    private int selectedPosition = -1;

    public interface OnTimeSlotClickListener {
        void onTimeSlotClick(String time);
    }

    public TimeSlotAdapter(List<String> timeSlots, OnTimeSlotClickListener listener) {
        this.timeSlots = timeSlots;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TimeSlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_time_slot, parent, false);
        return new TimeSlotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeSlotViewHolder holder, int position) {
        String timeSlot = timeSlots.get(position);
        holder.bind(timeSlot, position);
    }

    @Override
    public int getItemCount() {
        return timeSlots.size();
    }

    class TimeSlotViewHolder extends RecyclerView.ViewHolder {
        private CardView cardTimeSlot;
        private TextView tvTimeSlot;

        public TimeSlotViewHolder(@NonNull View itemView) {
            super(itemView);
            cardTimeSlot = itemView.findViewById(R.id.cardTimeSlot);
            tvTimeSlot = itemView.findViewById(R.id.tvTimeSlot);
        }

        public void bind(String timeSlot, int position) {
            tvTimeSlot.setText(timeSlot);

            if (selectedPosition == position) {
                cardTimeSlot.setCardBackgroundColor(Color.parseColor("#008800"));
                tvTimeSlot.setTextColor(Color.WHITE);
            } else {
                cardTimeSlot.setCardBackgroundColor(Color.WHITE);
                tvTimeSlot.setTextColor(Color.parseColor("#212121"));
            }

            itemView.setOnClickListener(v -> {
                int previousPosition = selectedPosition;
                selectedPosition = position;
                notifyItemChanged(previousPosition);
                notifyItemChanged(selectedPosition);
                
                if (listener != null) {
                    listener.onTimeSlotClick(timeSlot);
                }
            });
        }
    }
}
