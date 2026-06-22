package com.haset.hasetapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.haset.hasetapp.R;
import com.haset.hasetapp.models.Comment;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class PostCommentsBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_POST_ID = "post_id";

    private RecyclerView rvComments;
    private EditText etCommentInput;
    private ImageView ivSendComment;
    private CommentsAdapter commentsAdapter;
    private List<Comment> commentList;

    public static PostCommentsBottomSheet newInstance(String postId) {
        PostCommentsBottomSheet fragment = new PostCommentsBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_POST_ID, postId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_article_comments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvComments = view.findViewById(R.id.rvComments);
        etCommentInput = view.findViewById(R.id.etCommentInput);
        ivSendComment = view.findViewById(R.id.ivSendComment);

        commentList = new ArrayList<>();
        // TODO: Load comments for the given post ID from a database or API
        commentList.add(new Comment("1", getString(R.string.user_1), getString(R.string.dummy_comment_1), System.currentTimeMillis()));
        commentList.add(new Comment("2", getString(R.string.user_2), getString(R.string.dummy_comment_2), System.currentTimeMillis()));

        commentsAdapter = new CommentsAdapter(commentList);
        rvComments.setLayoutManager(new LinearLayoutManager(getContext()));
        rvComments.setAdapter(commentsAdapter);

        ivSendComment.setOnClickListener(v -> sendComment());
    }

    private void sendComment() {
        String commentText = etCommentInput.getText().toString().trim();
        if (!commentText.isEmpty()) {
            // TODO: Save comment to database/API and refresh list
            Comment newComment = new Comment(String.valueOf(commentList.size() + 1), getString(R.string.current_user), commentText, System.currentTimeMillis());
            commentList.add(newComment);
            commentsAdapter.notifyItemInserted(commentList.size() - 1);
            etCommentInput.setText("");
            rvComments.scrollToPosition(commentList.size() - 1);
            Toast.makeText(getContext(), R.string.comment_sent_msg, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), R.string.comment_empty_error, Toast.LENGTH_SHORT).show();
        }
    }

    static class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {
        private List<Comment> comments;

        public CommentsAdapter(List<Comment> comments) {
            this.comments = comments;
        }

        @NonNull
        @Override
        public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
            return new CommentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
            Comment comment = comments.get(position);
            holder.tvCommentAuthor.setText(comment.getAuthor());
            holder.tvCommentContent.setText(comment.getContent());
            // Format timestamp - you can use a helper here
            holder.tvCommentTimestamp.setText(holder.itemView.getContext().getString(R.string.some_time_ago));
            
            // Load user profile photo if userId is available in Comment model
            if (comment.getAuthor() != null) { // Assuming author name is used for demo, should be userId
                com.haset.hasetapp.utils.ProfilePhotoHelper.loadProfilePhoto(holder.itemView.getContext(), comment.getAuthor(), holder.ivCommentProfile, holder.shimmerCommentProfile);
            } else {
                holder.ivCommentProfile.setImageResource(R.drawable.profile_photo);
                if (holder.shimmerCommentProfile != null) {
                    holder.shimmerCommentProfile.stopShimmer();
                    holder.shimmerCommentProfile.setVisibility(View.GONE);
                }
            }
        }

        @Override
        public int getItemCount() {
            return comments.size();
        }

        static class CommentViewHolder extends RecyclerView.ViewHolder {
            CircleImageView ivCommentProfile;
            com.facebook.shimmer.ShimmerFrameLayout shimmerCommentProfile;
            TextView tvCommentAuthor, tvCommentContent, tvCommentTimestamp;

            public CommentViewHolder(@NonNull View itemView) {
                super(itemView);
                ivCommentProfile = itemView.findViewById(R.id.ivCommentProfile);
                shimmerCommentProfile = itemView.findViewById(R.id.shimmerCommentProfile);
                tvCommentAuthor = itemView.findViewById(R.id.tvCommentAuthor);
                tvCommentContent = itemView.findViewById(R.id.tvCommentContent);
                tvCommentTimestamp = itemView.findViewById(R.id.tvCommentTimestamp);
            }
        }
    }
}



