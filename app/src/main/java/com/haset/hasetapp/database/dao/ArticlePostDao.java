package com.haset.hasetapp.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.haset.hasetapp.database.entities.ArticlePostEntity;

import java.util.List;

@Dao
public interface ArticlePostDao {
    
    @Insert
    void insert(ArticlePostEntity post);
    
    @Update
    void update(ArticlePostEntity post);
    
    @Delete
    void delete(ArticlePostEntity post);
    
    @Query("SELECT * FROM article_posts WHERE postId = :postId LIMIT 1")
    ArticlePostEntity getPostById(String postId);
    
    @Query("SELECT * FROM article_posts WHERE type = :type ORDER BY createdAt DESC")
    List<ArticlePostEntity> getPostsByType(String type);
    
    @Query("SELECT * FROM article_posts WHERE status = :status ORDER BY createdAt DESC")
    List<ArticlePostEntity> getPostsByStatus(String status);
    
    @Query("SELECT * FROM article_posts WHERE type = :type AND status = :status ORDER BY createdAt DESC")
    List<ArticlePostEntity> getPostsByTypeAndStatus(String type, String status);
    
    @Query("SELECT * FROM article_posts ORDER BY createdAt DESC")
    List<ArticlePostEntity> getAllPosts();
    
    @Query("SELECT * FROM article_posts WHERE status = 'published' ORDER BY createdAt DESC")
    List<ArticlePostEntity> getPublishedPosts();
    
    @Query("SELECT * FROM article_posts WHERE status = 'draft' ORDER BY updatedAt DESC")
    List<ArticlePostEntity> getDraftPosts();
    
    @Query("DELETE FROM article_posts WHERE postId = :postId")
    void deletePostById(String postId);
    
    @Query("DELETE FROM article_posts")
    void deleteAll();
}

