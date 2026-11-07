package com.example.bashabhai;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFavorite(FavoriteListing listing);

    @Delete
    void deleteFavorite(FavoriteListing listing);

    @Query("SELECT * FROM favorites WHERE userId = :userId")
    List<FavoriteListing> getAllFavorites(String userId);

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :id AND userId = :userId LIMIT 1)")
    boolean isFavorite(String id, String userId);
}
