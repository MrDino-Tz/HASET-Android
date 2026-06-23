package com.haset.hasetapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.haset.hasetapp.R;
import com.haset.hasetapp.database.entities.UserEntity;
import com.haset.hasetapp.utils.ProfilePhotoHelper;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private List<UserEntity> users;
    private OnUserClickListener onUserClickListener;

    public interface OnUserClickListener {
        void onUserClick(UserEntity user);
    }

    public UserAdapter() {
        this.users = new ArrayList<>();
    }

    public void setOnUserClickListener(OnUserClickListener listener) {
        this.onUserClickListener = listener;
    }

    public void setUsers(List<UserEntity> users) {
        this.users = users != null ? users : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<UserEntity> getUsers() {
        return users;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_list, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserEntity user = users.get(position);
        holder.bind(user);
        
        holder.itemView.setOnClickListener(v -> {
            if (onUserClickListener != null) {
                onUserClickListener.onUserClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return users != null ? users.size() : 0;
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private CircleImageView ivUserImage;
        private com.facebook.shimmer.ShimmerFrameLayout shimmerUserImage;
        private TextView tvUserName, tvUserRole, tvUserEmail, tvRoleBadge, tvUserInitials;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserImage = itemView.findViewById(R.id.ivUserImage);
            shimmerUserImage = itemView.findViewById(R.id.shimmerUserImage);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserRole = itemView.findViewById(R.id.tvUserRole);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            tvRoleBadge = itemView.findViewById(R.id.tvRoleBadge);
            tvUserInitials = itemView.findViewById(R.id.tvUserInitials);
        }

        public void bind(UserEntity user) {
            String fullName = user.getFullName();
            String role = user.getRole();
            String email = user.getEmail();
            
            tvUserName.setText(fullName != null ? fullName : "Unknown");
            
            if (role != null && !role.isEmpty()) {
                tvUserRole.setText(role.substring(0, 1).toUpperCase() + role.substring(1));
                tvRoleBadge.setText(role.toUpperCase());
            } else {
                tvUserRole.setText("User");
                tvRoleBadge.setText("USER");
            }
            
            tvUserEmail.setText(email != null ? email : "");
            
            if (role != null) {
                switch (role.toLowerCase()) {
                    case "doctor":
                        tvRoleBadge.setBackgroundResource(R.drawable.bg_badge_red);
                        tvRoleBadge.setTextColor(itemView.getContext().getResources().getColor(R.color.badge_red_text));
                        break;
                    default:
                        tvRoleBadge.setBackgroundResource(R.drawable.bg_badge_teal);
                        tvRoleBadge.setTextColor(itemView.getContext().getResources().getColor(R.color.badge_teal_text));
                        break;
                }
            }
            
            // Load profile photo directly from URL if available
            String profileUrl = user.getProfileImage();
            if (profileUrl != null && !profileUrl.isEmpty()) {
                tvUserInitials.setVisibility(View.GONE);
                ivUserImage.setVisibility(View.VISIBLE);
                ProfilePhotoHelper.loadProfilePhotoFromUrl(itemView.getContext(), profileUrl, ivUserImage, shimmerUserImage);
            } else {
                // Show initials if no profile image
                tvUserInitials.setVisibility(View.VISIBLE);
                tvUserInitials.setText(ProfilePhotoHelper.getInitials(fullName));
                ivUserImage.setVisibility(View.GONE);
                if (shimmerUserImage != null) {
                    shimmerUserImage.stopShimmer();
                    shimmerUserImage.setVisibility(View.GONE);
                }
            }
        }
    }
}
