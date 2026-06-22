package com.haset.hasetapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.haset.hasetapp.R;
import com.haset.hasetapp.models.Prescription;

import java.util.List;

public class MedicineAdapter extends RecyclerView.Adapter<MedicineAdapter.ViewHolder> {

    private final List<Prescription.Medicine> medicines;

    public MedicineAdapter(List<Prescription.Medicine> medicines) {
        this.medicines = medicines;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medicine, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Prescription.Medicine medicine = medicines.get(position);
        Context context = holder.itemView.getContext();

        holder.tvMedicineName.setText(medicine.getName());
        holder.tvDosage.setText(medicine.getDosage());
        holder.tvFrequency.setText(medicine.getFrequency());
        
        String durationText = medicine.getDuration() + " days";
        holder.tvDuration.setText(durationText);
    }

    @Override
    public int getItemCount() {
        return medicines.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMedicineName, tvDosage, tvFrequency, tvDuration;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMedicineName = itemView.findViewById(R.id.tvMedicineName);
            tvDosage = itemView.findViewById(R.id.tvDosage);
            tvFrequency = itemView.findViewById(R.id.tvFrequency);
            tvDuration = itemView.findViewById(R.id.tvDuration);
        }
    }
}
