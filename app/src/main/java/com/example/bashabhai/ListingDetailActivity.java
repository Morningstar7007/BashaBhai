package com.example.bashabhai;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.bashabhai.databinding.ActivityListingDetailBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class ListingDetailActivity extends AppCompatActivity {

    private ActivityListingDetailBinding binding;
    private Listing listing;
    private FavoriteDao favoriteDao;
    private boolean isFavorite = false;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityListingDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        mAuth = FirebaseAuth.getInstance();

        // Get Listing object passed via Explicit Intent
        if (getIntent().hasExtra("LISTING")) {
            listing = (Listing) getIntent().getSerializableExtra("LISTING");
            
            favoriteDao = AppDatabase.getInstance(this).favoriteDao();
            checkIfFavorite();
            
            populateUI();
            setupClickListeners();
            setupRoleControls();
        } else {
            Toast.makeText(this, "Error loading listing details", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void checkIfFavorite() {
        if (listing != null && listing.getId() != null && mAuth.getCurrentUser() != null) {
            String currentUserId = mAuth.getCurrentUser().getUid();
            isFavorite = favoriteDao.isFavorite(listing.getId(), currentUserId);
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

        binding.tvDetailPostNumber.setText(listing.getFormattedPostNumber());
        binding.tvDetailTitle.setText(listing.getTitle());
        binding.tvDetailRent.setText("৳" + listing.getRent() + " / month");
        binding.tvDetailTypeGender.setText(listing.getType() + " • " + listing.getGenderPreference() + " Only");
        binding.tvDetailLocation.setText(listing.getLocation());
        binding.tvDetailAmenities.setText(listing.getAmenities());
        binding.tvDetailDescription.setText(listing.getDescription());

        // Fetch Landlord Details (Name, Verification Badge & Avatar)
        if (listing.getLandlordId() != null) {
            FirebaseDb.getRef("Users").child(listing.getLandlordId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User landlord = snapshot.getValue(User.class);
                    if (landlord != null) {
                        boolean isAdminUser = "ADMIN".equalsIgnoreCase(landlord.getRole()) || 
                                (landlord.getEmail() != null && AdminConfig.isAdmin(landlord.getEmail()));

                        String name = isAdminUser ? "ADMIN" : 
                                (landlord.getName() != null && !landlord.getName().isEmpty() ? landlord.getName() : "Landlord");

                        binding.tvDetailLandlordName.setText(name);

                        // Load Profile Picture if available (Keep app logo for ADMIN privacy)
                        String picBase64 = isAdminUser ? null : landlord.getProfilePicBase64();
                        if (picBase64 != null && !picBase64.isEmpty()) {
                            try {
                                byte[] bytes = Base64.decode(picBase64, Base64.DEFAULT);
                                Glide.with(ListingDetailActivity.this).load(bytes).into(binding.ivDetailLandlordAvatar);
                            } catch (Exception e) {
                                binding.ivDetailLandlordAvatar.setImageResource(R.drawable.bashabhai_logo);
                            }
                        } else {
                            binding.ivDetailLandlordAvatar.setImageResource(R.drawable.bashabhai_logo);
                        }

                        // Admin posts are ALWAYS verified 🟢
                        if (isAdminUser || landlord.isVerified() || "APPROVED".equalsIgnoreCase(landlord.getVerificationStatus())) {
                            binding.tvDetailLandlordVerified.setVisibility(View.VISIBLE);
                        } else {
                            binding.tvDetailLandlordVerified.setVisibility(View.GONE);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    private void setupRoleControls() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || listing == null) return;

        String currentUid = user.getUid();
        String currentEmail = user.getEmail() != null ? user.getEmail() : "";

        DatabaseReference userRef = FirebaseDb.getRef("Users").child(currentUid);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User userProfile = snapshot.getValue(User.class);
                String role = (userProfile != null && userProfile.getRole() != null) ? userProfile.getRole() : "STUDENT";
                if (AdminConfig.isAdmin(currentEmail)) {
                    role = "ADMIN";
                }

                boolean isOwner = currentUid.equals(listing.getLandlordId());
                boolean isAdmin = "ADMIN".equalsIgnoreCase(role);

                if (isOwner) {
                    binding.llOwnerControls.setVisibility(View.VISIBLE);
                    binding.btnEditListing.setVisibility(View.VISIBLE);
                    binding.btnDeleteListing.setVisibility(View.VISIBLE);
                    
                    // Hide "Mark as Rented" if the post is still pending Admin approval
                    if ("PENDING".equalsIgnoreCase(listing.getStatus())) {
                        binding.btnToggleRented.setVisibility(View.GONE);
                    } else {
                        binding.btnToggleRented.setVisibility(View.VISIBLE);
                        updateRentedButtonText();
                    }
                } else if (isAdmin) {
                    binding.llOwnerControls.setVisibility(View.VISIBLE);
                    binding.btnEditListing.setVisibility(View.GONE); // Admin CANNOT edit listings they don't own!
                    binding.btnToggleRented.setVisibility(View.GONE); // Admin CANNOT mark rented unless they own it!
                    binding.btnDeleteListing.setVisibility(View.VISIBLE); // Admin CAN delete for moderation
                } else {
                    binding.llOwnerControls.setVisibility(View.GONE);
                }

                if (isAdmin && "PENDING".equalsIgnoreCase(listing.getStatus())) {
                    binding.llAdminControls.setVisibility(View.VISIBLE);
                } else {
                    binding.llAdminControls.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateRentedButtonText() {
        if (listing.isRented() || "RENTED".equalsIgnoreCase(listing.getStatus())) {
            binding.btnToggleRented.setText("Mark as Available");
        } else {
            binding.btnToggleRented.setText("Mark as Rented");
        }
    }

    private void setupClickListeners() {
        if (listing == null) return;
        
        binding.ivFavorite.setOnClickListener(v -> toggleFavorite());

        binding.btnToggleRented.setOnClickListener(v -> {
            boolean newRentedState = !listing.isRented();
            listing.setRented(newRentedState);
            listing.setStatus(newRentedState ? "RENTED" : "APPROVED");
            
            DatabaseReference ref = FirebaseDb.getRef("Listings").child(listing.getId());
            ref.child("rented").setValue(newRentedState);
            ref.child("status").setValue(newRentedState ? "RENTED" : "APPROVED");
            
            updateRentedButtonText();
            Toast.makeText(this, newRentedState ? "Marked as Rented" : "Marked as Available", Toast.LENGTH_SHORT).show();
        });
        
        binding.btnEditListing.setOnClickListener(v -> {
            Intent intent = new Intent(ListingDetailActivity.this, AddListingActivity.class);
            intent.putExtra("EDIT_LISTING", listing);
            startActivity(intent);
            finish();
        });

        binding.btnDeleteListing.setOnClickListener(v -> {
            DatabaseReference ref = FirebaseDb.getRef("Listings").child(listing.getId());
            ref.removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Listing deleted", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Failed to delete listing", Toast.LENGTH_SHORT).show();
                }
            });
        });

        binding.btnApproveListing.setOnClickListener(v -> {
            listing.setStatus("APPROVED");
            DatabaseReference ref = FirebaseDb.getRef("Listings").child(listing.getId());
            ref.child("status").setValue("APPROVED").addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Listing Approved!", Toast.LENGTH_SHORT).show();
                    binding.llAdminControls.setVisibility(View.GONE);
                }
            });
        });

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

        // Implicit Intent to WhatsApp (Fallback to SMS)
        binding.btnSMS.setOnClickListener(v -> {
            String phone = listing.getLandlordPhone();
            if (phone != null && !phone.isEmpty()) {
                // Format BD phone number for WhatsApp
                String formattedPhone = phone.replaceAll("[^0-9]", ""); // Remove spaces/dashes
                if (formattedPhone.startsWith("01")) {
                    formattedPhone = "88" + formattedPhone;
                }

                String message = "Hi, I am interested in your listing on BashaBhai: " + listing.getTitle() + " (" + listing.getFormattedPostNumber() + ")";
                
                try {
                    // Try to open WhatsApp via wa.me intent
                    Intent whatsappIntent = new Intent(Intent.ACTION_VIEW);
                    whatsappIntent.setData(Uri.parse("https://wa.me/" + formattedPhone + "?text=" + Uri.encode(message)));
                    startActivity(whatsappIntent);
                } catch (ActivityNotFoundException e) {
                    // Fallback to standard SMS if WhatsApp is not installed or errors out
                    Toast.makeText(this, "WhatsApp not installed, falling back to SMS.", Toast.LENGTH_SHORT).show();
                    Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                    smsIntent.setData(Uri.parse("smsto:" + phone));
                    smsIntent.putExtra("sms_body", message);
                    startActivity(smsIntent);
                }
            } else {
                Toast.makeText(this, "No phone number available", Toast.LENGTH_SHORT).show();
            }
        });

        // Map Button Logic (Google Maps URL or text-search fallback)
        binding.btnMap.setOnClickListener(v -> {
            String mapLink = listing.getMapLink();
            if (mapLink != null && !mapLink.isEmpty() && (mapLink.startsWith("http://") || mapLink.startsWith("https://"))) {
                try {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mapLink));
                    startActivity(browserIntent);
                } catch (Exception e) {
                    Toast.makeText(this, "Invalid Map Link", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Fallback to text search
                String location = listing.getLocation();
                if (location != null && !location.isEmpty()) {
                    Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(location));
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    if (mapIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(mapIntent);
                    } else {
                        startActivity(new Intent(Intent.ACTION_VIEW, gmmIntentUri));
                    }
                } else {
                    Toast.makeText(this, "Location not specified", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    private void toggleFavorite() {
        if (listing == null || listing.getId() == null || mAuth.getCurrentUser() == null) return;
        
        String currentUserId = mAuth.getCurrentUser().getUid();
        
        FavoriteListing fav = new FavoriteListing(
                listing.getId(), currentUserId, listing.getLandlordId(), listing.getTitle(), listing.getType(),
                listing.getRent(), listing.getLocation(), listing.getLandlordPhone(),
                listing.getAmenities(), listing.getGenderPreference(), listing.getDescription(),
                listing.getMapLink(), listing.getPostNumber()
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
