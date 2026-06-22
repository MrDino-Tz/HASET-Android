package com.haset.hasetapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.haset.hasetapp.R;
import com.haset.hasetapp.models.Doctor;
import com.haset.hasetapp.models.Article;
import com.haset.hasetapp.utils.ProfilePhotoHelper;

import java.util.ArrayList;
import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

    public static final int TYPE_DOCTOR = 0;
    public static final int TYPE_ARTICLE = 1;
    public static final int TYPE_DRUG = 2;
    public static final int TYPE_SUGGESTION = 3;

    private List<Object> results = new ArrayList<>();
    private List<SearchResult> searchResults = new ArrayList<>();
    private OnItemClickListener listener;
    private OnSearchResultClickListener searchListener;

    public interface OnItemClickListener {
        void onDoctorClick(Doctor doctor);
        void onArticleClick(Article article);
        void onServiceClick(ServiceItem service);
    }

    public static class ServiceItem {
        public String name;
        public int iconResId;
        public String actionId;
        public ServiceItem(String name, int iconResId, String actionId) {
            this.name = name; this.iconResId = iconResId; this.actionId = actionId;
        }
    }

    public interface OnSearchResultClickListener {
        void onSearchResultClick(SearchResult result);
    }

    public static class SearchResult {
        public static final int TYPE_DOCTOR = 0;
        public static final int TYPE_ARTICLE = 1;
        public static final int TYPE_DRUG = 2;
        public static final int TYPE_SUGGESTION = 3;

        private int type;
        private String title;
        private String subtitle;
        private Object data;

        public SearchResult(int type, String title, String subtitle, Object data) {
            this.type = type;
            this.title = title;
            this.subtitle = subtitle;
            this.data = data;
        }

        public int getType() { return type; }
        public String getTitle() { return title; }
        public String getSubtitle() { return subtitle; }
        public Object getData() { return data; }
    }

    public SearchResultAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public SearchResultAdapter(OnSearchResultClickListener listener) {
        this.searchListener = listener;
    }

    public void setResults(List<Object> results) {
        this.results = results;
        this.searchResults.clear();
        notifyDataSetChanged();
    }

    public void setSearchResults(List<SearchResult> results) {
        this.searchResults = results;
        this.results.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_result_m3, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (searchListener != null && !searchResults.isEmpty()) {
            SearchResult result = searchResults.get(position);
            holder.tvTitle.setText(result.getTitle());
            holder.tvSubtitle.setText(result.getSubtitle() != null ? result.getSubtitle() : "");
            
            switch (result.getType()) {
                case TYPE_DOCTOR:
                    holder.ivIcon.setImageResource(R.drawable.user_md_24);
                    break;
                case TYPE_ARTICLE:
                    holder.ivIcon.setImageResource(R.drawable.ic_news_paper);
                    break;
                case TYPE_DRUG:
                    holder.ivIcon.setImageResource(R.drawable.ic_medical);
                    break;
                case TYPE_SUGGESTION:
                    holder.ivIcon.setImageResource(R.drawable.ic_search);
                    break;
            }
            
            holder.itemView.setOnClickListener(v -> searchListener.onSearchResultClick(result));
        } else if (listener != null && !results.isEmpty()) {
            Object item = results.get(position);
            if (item instanceof Doctor) {
                Doctor doctor = (Doctor) item;
                holder.tvTitle.setText(doctor.getFullName());
                holder.tvSubtitle.setText(doctor.getSpecialty());
                holder.ivIcon.setVisibility(View.GONE);
                holder.ivDoctorProfile.setVisibility(View.VISIBLE);
                String profileUrl = doctor.getProfileImage();
                if (profileUrl != null && !profileUrl.isEmpty()) {
                    ProfilePhotoHelper.loadProfilePhotoFromUrl(holder.itemView.getContext(), profileUrl, holder.ivDoctorProfile);
                } else {
                    holder.ivDoctorProfile.setImageResource(R.drawable.profile_photo);
                }
                holder.itemView.setOnClickListener(v -> listener.onDoctorClick(doctor));
            } else if (item instanceof Article) {
                Article article = (Article) item;
                holder.tvTitle.setText(article.getTitle());
                holder.tvSubtitle.setText("Article • " + article.getCategory());
                holder.ivIcon.setImageResource(R.drawable.ic_news_paper);
                holder.ivIcon.setVisibility(View.VISIBLE);
                holder.ivDoctorProfile.setVisibility(View.GONE);
                holder.itemView.setOnClickListener(v -> listener.onArticleClick(article));
            } else if (item instanceof ServiceItem) {
                ServiceItem service = (ServiceItem) item;
                holder.tvTitle.setText(service.name);
                holder.tvSubtitle.setText("App Service");
                holder.ivIcon.setImageResource(service.iconResId);
                holder.ivIcon.setVisibility(View.VISIBLE);
                holder.ivDoctorProfile.setVisibility(View.GONE);
                holder.itemView.setOnClickListener(v -> listener.onServiceClick(service));
            }
        }
    }

    @Override
    public int getItemCount() {
        return searchListener != null ? searchResults.size() : results.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        de.hdodenhof.circleimageview.CircleImageView ivDoctorProfile;
        TextView tvTitle, tvSubtitle;

        ViewHolder(View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivResultIcon);
            ivDoctorProfile = itemView.findViewById(R.id.ivDoctorProfile);
            tvTitle = itemView.findViewById(R.id.tvResultTitle);
            tvSubtitle = itemView.findViewById(R.id.tvResultSubtitle);
        }
    }
}