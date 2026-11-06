package com.example.bashabhai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.bashabhai.databinding.ActivityFavoritesBinding;

import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {

    private ActivityFavoritesBinding binding;
    private ListingAdapter adapter;
    private FavoriteDao favoriteDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFavoritesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        favoriteDao = AppDatabase.getInstance(this).favoriteDao();
        setupRecyclerView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavorites();
    }

    private void setupRecyclerView() {
        adapter = new ListingAdapter(listing -> {
            Intent intent = new Intent(FavoritesActivity.this, ListingDetailActivity.class);
            intent.putExtra("LISTING", listing);
            startActivity(intent);
        });
        binding.rvFavorites.setLayoutManager(new LinearLayoutManager(this));
        binding.rvFavorites.setAdapter(adapter);
    }

    private void loadFavorites() {
        List<FavoriteListing> favs = favoriteDao.getAllFavorites();
        if (favs.isEmpty()) {
            binding.tvNoFavorites.setVisibility(View.VISIBLE);
            binding.rvFavorites.setVisibility(View.GONE);
        } else {
            binding.tvNoFavorites.setVisibility(View.GONE);
            binding.rvFavorites.setVisibility(View.VISIBLE);
            
            // Map FavoriteListing back to regular Listing for the adapter
            List<Listing> displayListings = new ArrayList<>();
            for (FavoriteListing f : favs) {
                Listing l = new Listing();
                l.setId(f.getId());
                l.setTitle(f.getTitle());
                l.setType(f.getType());
                l.setRent(f.getRent());
                l.setLocation(f.getLocation());
                l.setLandlordPhone(f.getLandlordPhone());
                displayListings.add(l);
            }
            adapter.setListings(displayListings);
        }
    }
}
