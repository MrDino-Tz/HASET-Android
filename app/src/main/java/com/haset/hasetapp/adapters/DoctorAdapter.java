package com.haset.hasetapp.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.haset.hasetapp.R;
import com.haset.hasetapp.models.Doctor;
import com.haset.hasetapp.utils.ProfilePhotoHelper;

import java.util.ArrayList;
import java.util.List;




public class DoctorAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Doctor> doctors;
    private OnDoctorClickListener listener;
    private int selectedPosition = -1;
    private boolean isSimple = false;
    private static final int VIEW_TYPE_ITEM = 0;
    private static final int VIEW_TYPE_LOADING = 1;
    private boolean isLoading = false;

    public interface OnDoctorClickListener {
        void onBookClick(Doctor doctor);
        void onDoctorClick(Doctor doctor);
    }

    public DoctorAdapter(OnDoctorClickListener listener) {
        this.doctors = new ArrayList<>();
        this.listener = listener;
    }

    public DoctorAdapter(OnDoctorClickListener listener, boolean isSimple) {
        this.doctors = new ArrayList<>();
        this.listener = listener;
        this.isSimple = isSimple;
    }

    public void setLoading(boolean loading) {
        if (this.isLoading != loading) {
            this.isLoading = loading;
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isLoading && position == doctors.size()) {
            return VIEW_TYPE_LOADING;
        }
        return VIEW_TYPE_ITEM;
    }

    public void setDoctors(List<Doctor> doctors) {
        this.doctors = doctors;
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position) {
        int previousSelected = selectedPosition;
        selectedPosition = position;
        if (previousSelected != -1) {
            notifyItemChanged(previousSelected);
        }
        if (selectedPosition != -1) {
            notifyItemChanged(selectedPosition);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_LOADING) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_loading, parent, false);
            return new LoadingViewHolder(view);
        }
        int layoutId = isSimple ? R.layout.item_doctor_popular : R.layout.doctor_item;
        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false);
        return new DoctorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof DoctorViewHolder) {
            Doctor doctor = doctors.get(position);
            boolean isSelected = (position == selectedPosition);
            ((DoctorViewHolder) holder).bind(doctor, isSelected);
        }
    }

    @Override
    public int getItemCount() {
        return doctors.size() + (isLoading ? 1 : 0);
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    class DoctorViewHolder extends RecyclerView.ViewHolder {
        private View cardRoot;
        private TextView tvInitials;
        private ImageView ivProfileImage, ivVerified;
        private TextView tvName, tvSpecialty, tvRating, tvExperience, tvFee, tvStatus, tvAvailableTime;
        private ImageView ivAction;
        private com.facebook.shimmer.ShimmerFrameLayout shimmerProfile;
        private TextView tvDemoBadge;

        public DoctorViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.cardRoot);
            ivProfileImage = itemView.findViewById(R.id.ivProfileImage);
            ivVerified = itemView.findViewById(R.id.ivVerified);
            tvInitials = itemView.findViewById(R.id.tvInitials);
            tvName = itemView.findViewById(R.id.tvName);
            tvSpecialty = itemView.findViewById(R.id.tvSpecialty);
//            tvRating = itemView.findViewById(R.id.tvRating);
//            tvExperience = itemView.findViewById(R.id.tvExperience);
            tvFee = itemView.findViewById(R.id.tvFee);
            ivAction = itemView.findViewById(R.id.ivAction);
            shimmerProfile = itemView.findViewById(R.id.shimmerProfile);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvAvailableTime = itemView.findViewById(R.id.tvAvailableTime);
            tvDemoBadge = itemView.findViewById(R.id.tvDemoBadge);
        }

        public void bind(Doctor doctor, boolean isSelected) {
            // Set doctor name
            String fullName = doctor.getFullName();
            if (fullName != null && !fullName.isEmpty()) {
                tvName.setText(fullName.startsWith("Dr. ") ? fullName : "Dr. " + fullName);
            } else {
                tvName.setText(R.string.dr_unknown);
            }

            // Specialty and Location binding
            String specialty = doctor.getSpecialty();
            String location = doctor.getLocation();
            
            StringBuilder specialtyText = new StringBuilder();
            if (specialty != null && !specialty.isEmpty() && !specialty.equals("Specialty")) {
                specialtyText.append(specialty);
            } else {
                specialtyText.append("Medical Doctor");
            }
            
            if (location != null && !location.isEmpty() && !location.contains("City")) {
                specialtyText.append(" | ").append(location);
            }
            
            tvSpecialty.setText(specialtyText.toString());

            if (tvRating != null) {
                float rating = doctor.getRating();
                if (rating > 0) {
                    tvRating.setText(String.format("%.1f", rating));
                } else {
                    tvRating.setText("0.0");
                }
            }

            // Load doctor profile photo directly from URL (already loaded in Doctor model)
            String profileUrl = doctor.getProfileImage();
            if (profileUrl != null && !profileUrl.isEmpty()) {
                tvInitials.setVisibility(View.GONE);
                ivProfileImage.setVisibility(View.VISIBLE);
                ProfilePhotoHelper.loadProfilePhotoFromUrl(itemView.getContext(), profileUrl, ivProfileImage, shimmerProfile);
            } else {
                // Show initials if no profile image
                tvInitials.setVisibility(View.VISIBLE);
                tvInitials.setText(ProfilePhotoHelper.getInitials(doctor.getFullName()));
                ivProfileImage.setVisibility(View.GONE);
                if (shimmerProfile != null) {
                    shimmerProfile.stopShimmer();
                    shimmerProfile.setVisibility(View.GONE);
                }
            }

            // Set verified badge
            if (ivVerified != null) {
                ivVerified.setVisibility(doctor.isVerified() ? View.VISIBLE : View.GONE);
            }

            // Set online status
            if (tvStatus != null) {
                boolean isOnline = doctor.isOnline();
                if (isOnline) {
                    tvStatus.setText(R.string.status_online);
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.green_primary));
                    tvStatus.setBackgroundResource(R.drawable.bg_status_online);
                } else {
                    tvStatus.setText(R.string.status_offline);
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_hint));
                    tvStatus.setBackgroundResource(R.drawable.bg_status_offline);
                }
                tvStatus.setVisibility(View.VISIBLE);
            }

            // Set Fee
            if (tvFee != null) {
                double fee = doctor.getConsultationFee();
                if (fee > 0) {
                    tvFee.setText(String.format("%,.0f TSH", fee));
                    tvFee.setVisibility(View.VISIBLE);
                } else {
                    tvFee.setVisibility(View.INVISIBLE); // Keep layout stable
                }
            }
            
            // Set Demo Badge
            if (tvDemoBadge != null) {
                if (doctor.isDemo()) {
                    tvDemoBadge.setVisibility(View.VISIBLE);
                    if (tvFee != null) {
                        tvFee.setText("FREE");
                        tvFee.setVisibility(View.VISIBLE);
                    }
                } else {
                    tvDemoBadge.setVisibility(View.GONE);
                }
            }

            // Set Available Time
            if (tvAvailableTime != null) {
                List<String> availableTimes = doctor.getAvailableTimes();
                if (availableTimes != null && !availableTimes.isEmpty()) {
                    String timeRange;
                    if (availableTimes.size() >= 2) {
                        timeRange = availableTimes.get(0) + " - " + availableTimes.get(availableTimes.size() - 1);
                    } else {
                        timeRange = availableTimes.get(0);
                    }
                    tvAvailableTime.setText("🕒 " + timeRange);
                    tvAvailableTime.setVisibility(View.VISIBLE);
                } else {
                    tvAvailableTime.setText("🕒 " + itemView.getContext().getString(R.string.not_available));
                    tvAvailableTime.setVisibility(View.VISIBLE);
                }
            }

            // Handle card highlighting
            if (isSelected) {
                // Highlighted card - green background
                cardRoot.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.green_light));
                tvName.setTextColor(Color.WHITE);
                if (tvSpecialty != null) tvSpecialty.setTextColor(Color.WHITE);
                if (tvExperience != null) tvExperience.setTextColor(Color.WHITE);
                if (tvFee != null) tvFee.setTextColor(Color.WHITE);
                if (tvStatus != null) tvStatus.setTextColor(Color.WHITE);
                if (tvAvailableTime != null) tvAvailableTime.setTextColor(Color.WHITE);
                if (ivAction != null) ivAction.setColorFilter(Color.WHITE);
                // Also update initials colors for selected state
                if (tvInitials != null) {
                    tvInitials.setBackgroundColor(Color.WHITE); // White circle/bg
                    tvInitials.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.green_primary)); // Green text
                }
            } else {
                // Use theme attributes for background and text
                android.util.TypedValue typedValue = new android.util.TypedValue();
                itemView.getContext().getTheme().resolveAttribute(R.attr.colorCardBackground, typedValue, true);
                cardRoot.setBackgroundColor(typedValue.data);

                itemView.getContext().getTheme().resolveAttribute(R.attr.colorPrimaryText, typedValue, true);
                tvName.setTextColor(typedValue.data);

                if (tvSpecialty != null) tvSpecialty.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.green_light));

                itemView.getContext().getTheme().resolveAttribute(R.attr.colorSecondaryText, typedValue, true);
                if (tvExperience != null) tvExperience.setTextColor(typedValue.data);

                if (tvFee != null) tvFee.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.green_primary));

                if (ivAction != null) ivAction.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.green_light));

                // Reset initials colors
                if (tvInitials != null) {
                    tvInitials.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.badge_teal_text));
                    if (isSimple) {
                        tvInitials.setBackgroundResource(R.drawable.bg_circle_teal);
                    } else {
                        tvInitials.setBackgroundResource(R.color.badge_teal);
                    }
                }
            }

            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    setSelectedPosition(getAdapterPosition());
                    listener.onDoctorClick(doctor);
                }
            });

            if (ivAction != null) {
                ivAction.setOnClickListener(v -> {
                    if (listener != null) {
                        setSelectedPosition(getAdapterPosition());
                        listener.onDoctorClick(doctor);
                    }
                });
            }
        }
        
        private String getInitials(String fullName) {
            if (fullName == null || fullName.isEmpty()) return "DR";
            // Remove "Dr." prefix if present
            String name = fullName.replaceAll("(?i)^dr\\.\\s*", "").trim();
            if (name.isEmpty()) return "DR";
            
            String[] parts = name.split("\\s+");
            if (parts.length == 0) return "DR";
            
            String first = parts[0];
            if (parts.length == 1) {
                return first.length() > 1 ? first.substring(0, 2).toUpperCase() : first.toUpperCase();
            }
            
            String last = parts[parts.length - 1];
            return (first.substring(0, 1) + last.substring(0, 1)).toUpperCase();
        }
    }
}
