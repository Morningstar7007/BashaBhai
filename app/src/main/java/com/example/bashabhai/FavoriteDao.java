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

    @Query("SELECT * FROM favorites")
    List<FavoriteListing> getAllFavorites();

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :id LIMIT 1)")
    boolean isFavorite(String id);
}
