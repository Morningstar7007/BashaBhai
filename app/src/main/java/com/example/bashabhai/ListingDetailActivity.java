package com.example.bashabhai;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bashabhai.databinding.ActivityListingDetailBinding;

public class ListingDetailActivity extends AppCompatActivity {

    private ActivityListingDetailBinding binding;
    private Listing listing;
    private FavoriteDao favoriteDao;
    private boolean isFavorite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityListingDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get Listing object passed via Explicit Intent
        if (getIntent().hasExtra("LISTING")) {
            listing = (Listing) getIntent().getSerializableExtra("LISTING");
            
            favoriteDao = AppDatabase.getInstance(this).favoriteDao();
            checkIfFavorite();
            
            populateUI();
            setupClickListeners();
        } else {
            Toast.makeText(this, "Error loading listing details", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void checkIfFavorite() {
        if (listing != null && listing.getId() != null) {
            isFavorite = favoriteDao.isFavorite(listing.getId());
            updateFavoriteIcon();
        }
    }
    
    private void updateFavoriteIcon() {
        if (isFavorite) {
            binding.ivFavorite.setImageResource(android.R.drawable.btn_star_big_on);
        } else {
            binding.ivFavorite.setImageResource(android.R.drawable.btn_star_big_off);
        }
    }

    private void populateUI() {
        if (listing == null) return;

        binding.tvDetailTitle.setText(listing.getTitle());
        binding.tvDetailRent.setText("৳" + listing.getRent() + " / month");
        binding.tvDetailTypeGender.setText(listing.getType() + " • " + listing.getGenderPreference() + " Only");
        binding.tvDetailLocation.setText(listing.getLocation());
        binding.tvDetailAmenities.setText(listing.getAmenities());
        binding.tvDetailDescription.setText(listing.getDescription());
    }

    private void setupClickListeners() {
        if (listing == null) return;
        
        binding.ivFavorite.setOnClickListener(v -> toggleFavorite());

        // Implicit Intent to Call
        binding.btnCall.setOnClickListener(v -> {
            String phone = listing.getLandlordPhone();
            if (phone != null && !phone.isEmpty()) {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + phone));
                startActivity(callIntent);
            } else {
                Toast.makeText(this, "No phone number available", Toast.LENGTH_SHORT).show();
            }
        });

        // Implicit Intent to SMS
        binding.btnSMS.setOnClickListener(v -> {
            String phone = listing.getLandlordPhone();
            if (phone != null && !phone.isEmpty()) {
                Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                smsIntent.setData(Uri.parse("smsto:" + phone));
                smsIntent.putExtra("sms_body", "Hi, I am interested in your listing: " + listing.getTitle());
                startActivity(smsIntent);
            } else {
                Toast.makeText(this, "No phone number available", Toast.LENGTH_SHORT).show();
            }
        });

        // Implicit Intent to Map
        binding.btnMap.setOnClickListener(v -> {
            String location = listing.getLocation();
            if (location != null && !location.isEmpty()) {
                Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(location));
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                
                // Verify that Google Maps is installed
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    // Fallback to general geo intent if Google Maps isn't specifically available
                    Intent generalMapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    startActivity(generalMapIntent);
                }
            } else {
                Toast.makeText(this, "Location not specified", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void toggleFavorite() {
        if (listing == null || listing.getId() == null) return;
        
        FavoriteListing fav = new FavoriteListing(
                listing.getId(), listing.getTitle(), listing.getType(),
                listing.getRent(), listing.getLocation(), listing.getLandlordPhone()
        );
        
        if (isFavorite) {
            favoriteDao.deleteFavorite(fav);
            isFavorite = false;
            Toast.makeText(this, "Removed from Favorites", Toast.LENGTH_SHORT).show();
        } else {
            favoriteDao.insertFavorite(fav);
            isFavorite = true;
            Toast.makeText(this, "Saved to Favorites", Toast.LENGTH_SHORT).show();
        }
        updateFavoriteIcon();
    }
}
