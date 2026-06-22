package com.haset.hasetapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.haset.hasetapp.R;
import com.haset.hasetapp.models.Hospital;

import java.util.List;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;

public class HospitalAdapter extends RecyclerView.Adapter<HospitalAdapter.HospitalViewHolder> {

    private List<Hospital> hospitals;
    private OnHospitalClickListener listener;

    public interface OnHospitalClickListener {
        void onHospitalClick(Hospital hospital);
    }

    public HospitalAdapter(List<Hospital> hospitals, OnHospitalClickListener listener) {
        this.hospitals = hospitals;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HospitalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hospital, parent, false);
        return new HospitalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HospitalViewHolder holder, int position) {
        Hospital hospital = hospitals.get(position);
        holder.bind(hospital);
    }

    @Override
    public int getItemCount() {
        return hospitals.size();
    }

    public void updateData(List<Hospital> newHospitals) {
        this.hospitals = newHospitals;
        notifyDataSetChanged();
    }

    class HospitalViewHolder extends RecyclerView.ViewHolder {
        TextView tvHospitalName, tvHospitalAddress, tvHospitalDescription;
        ImageView ivHospitalImage;
        ShimmerFrameLayout shimmerImage;
        View viewShimmer;

        public HospitalViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHospitalName = itemView.findViewById(R.id.tvHospitalName);
            tvHospitalAddress = itemView.findViewById(R.id.tvHospitalAddress);
            tvHospitalDescription = itemView.findViewById(R.id.tvHospitalDescription);
            ivHospitalImage = itemView.findViewById(R.id.ivHospitalImage);
            shimmerImage = itemView.findViewById(R.id.shimmerImage);
            viewShimmer = itemView.findViewById(R.id.viewShimmer);
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        listener.onHospitalClick(hospitals.get(pos));
                    }
                }
            });
        }

        public void bind(Hospital hospital) {
            tvHospitalName.setText(hospital.getName());
            tvHospitalAddress.setText(hospital.getAddress() != null ? hospital.getAddress() : "No location provided");
            tvHospitalDescription.setText(hospital.getDescription() != null ? hospital.getDescription() : "");
            
            if (hospital.getDescription() == null || hospital.getDescription().isEmpty()) {
                tvHospitalDescription.setVisibility(View.GONE);
            } else {
                tvHospitalDescription.setVisibility(View.VISIBLE);
            }

            if (hospital.getImageUrl() != null && !hospital.getImageUrl().isEmpty()) {
                ivHospitalImage.setPadding(0, 0, 0, 0);
                ivHospitalImage.setAlpha(1.0f);
                ivHospitalImage.clearColorFilter();
                
                shimmerImage.startShimmer();
                shimmerImage.setVisibility(View.VISIBLE);
                viewShimmer.setVisibility(View.VISIBLE);

                Glide.with(itemView.getContext())
                        .load(hospital.getImageUrl())
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                shimmerImage.stopShimmer();
                                viewShimmer.setVisibility(View.GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                shimmerImage.stopShimmer();
                                viewShimmer.setVisibility(View.GONE);
                                return false;
                            }
                        })
                        .into(ivHospitalImage);
            } else {
                // Reset to default blank state
                shimmerImage.stopShimmer();
                viewShimmer.setVisibility(View.GONE);
                
                ivHospitalImage.setPadding(0, 0, 0, 0);
                ivHospitalImage.setImageDrawable(null);
                ivHospitalImage.clearColorFilter();
            }
        }
    }
}
