package com.haset.hasetapp.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.haset.hasetapp.R;
// DISABLED FOR V1 - RATING SYSTEM COMING IN VERSION 2.0
// import com.haset.hasetapp.database.entities.DoctorRatingEntity;

import java.util.ArrayList;
import java.util.List;

/* DISABLED FOR V1 - RATING SYSTEM COMING IN VERSION 2.0
public class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder> {

    private Context context;
    private List<DoctorRatingEntity> reviews;

    public ReviewsAdapter(Context context) {
        this.context = context;
        this.reviews = new ArrayList<>();
    }

    public void setReviews(List<DoctorRatingEntity> reviews) {
        this.reviews = reviews;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        DoctorRatingEntity review = reviews.get(position);
        holder.bind(review);
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView tvPatientInitial, tvPatientName, tvDate, tvRating, tvComment;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPatientInitial = itemView.findViewById(R.id.tvPatientInitial);
            tvPatientName = itemView.findViewById(R.id.tvPatientName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvComment = itemView.findViewById(R.id.tvComment);
        }

        public void bind(DoctorRatingEntity review) {
            String name = review.getPatientName();
            if (TextUtils.isEmpty(name)) {
                name = "Anonymous";
            }
            
            tvPatientName.setText(name);
            tvPatientInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
            
            // Format time ago (e.g., "2 days ago")
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                review.getCreatedAt(),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            );
            tvDate.setText(timeAgo);
            
            tvRating.setText(String.format("%.1f", review.getRating()));
            
            if (!TextUtils.isEmpty(review.getComment())) {
                tvComment.setText(review.getComment());
                tvComment.setVisibility(View.VISIBLE);
            } else {
                tvComment.setVisibility(View.GONE);
            }
        }
    }
}
*/

// Placeholder class - Rating system disabled for V1
public class ReviewsAdapter {
    public ReviewsAdapter(Context context) {}
}
