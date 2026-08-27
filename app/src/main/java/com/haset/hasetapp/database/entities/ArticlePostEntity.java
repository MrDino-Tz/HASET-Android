package com.haset.hasetapp.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "article_posts")
public class ArticlePostEntity {
    @PrimaryKey
    @NonNull
    private String postId;
    private String type; // "image", "text"
    private String title;
    private String description;
    @androidx.room.Ignore
    private String titleSw;
    @androidx.room.Ignore
    private String descriptionSw;
    private String profileName;
    private String tags;
    private String imagePath; // Local file path for image (legacy, kept for backward compatibility)
    private String imageUrl; // Cloudinary URL for image (or any cloud storage URL)
    private String status; // "published", "draft"
    private int likes;
    private int comments;
    private int shares;
    private int views;
    private long createdAt;
    private long updatedAt;

    private String authorId; // ID of the author (user/doctor) who created the post

    @androidx.room.Ignore // Mark this constructor to be ignored by Room
    public ArticlePostEntity() {
    }

    // New constructor with all fields
    public ArticlePostEntity(@NonNull String postId, String type, String title, String description,
                         String profileName, String imagePath,
                         String status, int likes, int comments, int shares, int views,
                         String tags, long createdAt, long updatedAt) {
        this.postId = postId;
        this.type = type;
        this.title = title;
        this.description = description;
        this.profileName = profileName;
        this.imagePath = imagePath;
        this.status = status;
        this.likes = likes;
        this.comments = comments;
        this.shares = shares;
        this.views = views;
        this.tags = tags;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @androidx.room.Ignore
    public ArticlePostEntity(@NonNull String postId, String type, String title, String description,
                         String profileName, String tags, String status) {
        this.postId = postId;
        this.type = type;
        this.title = title;
        this.description = description;
        this.profileName = profileName;
        this.tags = tags;
        this.status = status;
        this.likes = 0;
        this.comments = 0;
        this.shares = 0;
        this.views = 0;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    @NonNull
    public String getPostId() { return postId; }
    public void setPostId(@NonNull String postId) { this.postId = postId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getTitleSw() { return titleSw; }
    public void setTitleSw(String titleSw) { this.titleSw = titleSw; }

    public String getLocalizedTitle(String language) {
        return "sw".equalsIgnoreCase(language) && titleSw != null && !titleSw.trim().isEmpty()
                ? titleSw : title;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDescriptionSw() { return descriptionSw; }
    public void setDescriptionSw(String descriptionSw) { this.descriptionSw = descriptionSw; }

    public String getLocalizedDescription(String language) {
        return "sw".equalsIgnoreCase(language) && descriptionSw != null && !descriptionSw.trim().isEmpty()
                ? descriptionSw : description;
    }

    public String getProfileName() { return profileName; }
    public void setProfileName(String profileName) { this.profileName = profileName; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }

    public int getComments() { return comments; }
    public void setComments(int comments) { this.comments = comments; }

    public int getShares() { return shares; }
    public void setShares(int shares) { this.shares = shares; }

    public int getViews() { return views; }
    public void setViews(int views) { this.views = views; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
