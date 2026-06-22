package com.haset.hasetapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.haset.hasetapp.R;
import com.haset.hasetapp.models.Prescription;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying prescriptions in RecyclerView
 */
public class PrescriptionAdapter extends RecyclerView.Adapter<PrescriptionAdapter.PrescriptionViewHolder> {
    
    private List<Prescription> prescriptions;
    private OnPrescriptionClickListener listener;
    
    public interface OnPrescriptionClickListener {
        void onPrescriptionClick(Prescription prescription);
    }
    
    public PrescriptionAdapter(List<Prescription> prescriptions, OnPrescriptionClickListener listener) {
        this.prescriptions = prescriptions;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public PrescriptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_prescription, parent, false);
        return new PrescriptionViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull PrescriptionViewHolder holder, int position) {
        Prescription prescription = prescriptions.get(position);
        holder.bind(prescription);
    }
    
    @Override
    public int getItemCount() {
        return prescriptions != null ? prescriptions.size() : 0;
    }
    
    public void updatePrescriptions(List<Prescription> newPrescriptions) {
        this.prescriptions = newPrescriptions != null ? newPrescriptions : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    class PrescriptionViewHolder extends RecyclerView.ViewHolder {
        
        private TextView tvDoctorName, tvMedicines, tvDate;
        
        public PrescriptionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDoctorName = itemView.findViewById(R.id.tvDoctorName);
            tvMedicines = itemView.findViewById(R.id.tvMedicines);
            tvDate = itemView.findViewById(R.id.tvDate);
            
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null && prescriptions != null && position < prescriptions.size()) {
                    listener.onPrescriptionClick(prescriptions.get(position));
                }
            });
        }
        
        public void bind(Prescription prescription) {
            // Set doctor name
            if (prescription.getDoctorName() != null) {
                tvDoctorName.setText(prescription.getDoctorName());
            } else {
                tvDoctorName.setText(R.string.na);
            }
            
            // Set medicines summary
            if (prescription.getMedicines() != null && !prescription.getMedicines().isEmpty()) {
                int medicineCount = prescription.getMedicines().size();
                String firstMedicine = prescription.getMedicines().get(0).getName();
                
                if (medicineCount == 1) {
                    tvMedicines.setText(firstMedicine);
                } else {
                    String summary = firstMedicine + " + " + (medicineCount - 1) + " more";
                    tvMedicines.setText(summary);
                }
            } else {
                tvMedicines.setText(R.string.na);
            }
            
            // Format and set date
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                String formattedDate = dateFormat.format(new Date(prescription.getCreatedAt()));
                tvDate.setText(formattedDate);
            } catch (Exception e) {
                tvDate.setText("");
            }
        }
    }
}
