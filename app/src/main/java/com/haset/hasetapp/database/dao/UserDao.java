package com.haset.hasetapp.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.haset.hasetapp.database.entities.UserEntity;

import java.util.List;

@Dao
public interface UserDao {
    
    @Insert
    void insert(UserEntity user);
    
    @Update
    void update(UserEntity user);
    
    @Delete
    void delete(UserEntity user);
    
    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    UserEntity getUserById(String userId);
    
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    UserEntity getUserByEmail(String email);
    
    @Query("SELECT * FROM users WHERE email = :email AND password = :password LIMIT 1")
    UserEntity login(String email, String password);
    
    @Query("SELECT * FROM users WHERE role = :role")
    List<UserEntity> getUsersByRole(String role);
    
    @Query("SELECT * FROM users")
    List<UserEntity> getAllUsers();
    
    @Query("DELETE FROM users")
    void deleteAll();
}
