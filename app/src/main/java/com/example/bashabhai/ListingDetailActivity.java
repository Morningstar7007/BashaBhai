package com.example.bashabhai;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.bashabhai.databinding.ActivityListingDetailBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ListingDetailActivity extends AppCompatActivity {

    private ActivityListingDetailBinding binding;
    private Listing listing;
    private FavoriteDao favoriteDao;
    private boolean isFavorite = false;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    
    private ReviewAdapter reviewAdapter;
    private final List<Review> allReviews = new ArrayList<>();
    private boolean isAdminGlobal = false;
    private boolean isCurrentUserStudent = false;
    private String currentStudentName = "";
    private String currentStudentInfo = "";
    private List<String> loadedImages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityListingDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        if (getIntent().hasExtra("LISTING")) {
            listing = (Listing) getIntent().getSerializableExtra("LISTING");
            
            favoriteDao = AppDatabase.getInstance(this).favoriteDao();
            checkIfFavorite();
            
            populateUI();
            fetchImagesFromFirestore();
            setupClickListeners();
            setupRoleControls();
        } else {
            Toast.makeText(this, "Error loading listing details", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void fetchImagesFromFirestore() {
        if (listing == null || listing.getId() == null) return;
        
        firestore.collection("ListingImages").document(listing.getId()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("images")) {
                        Object imagesObj = documentSnapshot.get("images");
                        if (imagesObj instanceof List) {
                            loadedImages = (List<String>) imagesObj;
                        }
                        if (loadedImages != null && !loadedImages.isEmpty()) {
                            binding.flCarouselContainer.setVisibility(View.VISIBLE);
                            ImageCarouselAdapter carouselAdapter = new ImageCarouselAdapter(loadedImages);
                            binding.viewPagerCarousel.setAdapter(carouselAdapter);
                            
                            binding.tvCarouselIndicator.setText(getString(R.string.carousel_indicator_placeholder)
                                    .replace("1", "1")
                                    .replace("3", String.valueOf(loadedImages.size())));
                            
                            binding.viewPagerCarousel.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                                @Override
                                public void onPageSelected(int position) {
                                    binding.tvCarouselIndicator.setText(getString(R.string.carousel_indicator_placeholder)
                                            .replace("1", String.valueOf(position + 1))
                                            .replace("3", String.valueOf(loadedImages.size())));
                                }
                            });
                        }
                    } else {
                        binding.flCarouselContainer.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> binding.flCarouselContainer.setVisibility(View.GONE));
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
        
        if ("REJECTED".equalsIgnoreCase(listing.getStatus()) && listing.getRejectionReason() != null && !listing.getRejectionReason().isEmpty()) {
            binding.tvDetailRejectionReason.setVisibility(View.VISIBLE);
            binding.tvDetailRejectionReason.setText(getString(R.string.rejection_reason_placeholder).replace("…", listing.getRejectionReason()));
        } else {
            binding.tvDetailRejectionReason.setVisibility(View.GONE);
        }

        binding.tvDetailPostNumber.setText(listing.getFormattedPostNumber());
        binding.tvDetailTitle.setText(listing.getTitle());
        binding.tvDetailRent.setText("৳" + listing.getRent() + " / month");
        binding.tvDetailTypeGender.setText(listing.getType() + " • " + listing.getGenderPreference() + " Only");
        binding.tvDetailLocation.setText(listing.getLocation());
        binding.tvDetailAmenities.setText(listing.getAmenities());
        binding.tvDetailDescription.setText(listing.getDescription());

        // Fetch Landlord Details (Name, Verification Badge & Avatar, Rating)
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

                        // Load Rating
                        if (!isAdminUser && landlord.getTotalReviews() > 0) {
                            double avg = landlord.getRatingSum() / landlord.getTotalReviews();
                            binding.tvDetailLandlordRating.setVisibility(View.VISIBLE);
                            binding.tvDetailLandlordRating.setText(String.format("⭐ %.1f (%d)", avg, landlord.getTotalReviews()));
                        } else {
                            binding.tvDetailLandlordRating.setVisibility(View.GONE);
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
                
                isAdminGlobal = "ADMIN".equalsIgnoreCase(role);
                isCurrentUserStudent = "STUDENT".equalsIgnoreCase(role);
                
                if (userProfile != null && isCurrentUserStudent) {
                    currentStudentName = userProfile.getName() != null ? userProfile.getName() : "Student";
                    String dept = userProfile.getDepartment() != null ? userProfile.getDepartment() : "";
                    String batch = userProfile.getBatch() != null ? userProfile.getBatch() : "";
                    if (!dept.isEmpty() || !batch.isEmpty()) {
                        currentStudentInfo = dept + (dept.isEmpty() || batch.isEmpty() ? "" : " - ") + batch;
                    }
                }

                boolean isOwner = currentUid.equals(listing.getLandlordId());

                if (isOwner) {
                    binding.llOwnerControls.setVisibility(View.VISIBLE);
                    binding.btnEditListing.setVisibility(View.VISIBLE);
                    binding.btnDeleteListing.setVisibility(View.VISIBLE);
                    
                    // Allow editing for REJECTED posts so landlord can fix them!
                    if ("PENDING".equalsIgnoreCase(listing.getStatus()) || "REJECTED".equalsIgnoreCase(listing.getStatus())) {
                        binding.btnToggleRented.setVisibility(View.GONE);
                    } else {
                        binding.btnToggleRented.setVisibility(View.VISIBLE);
                        updateRentedButtonText();
                    }
                } else if (isAdminGlobal) {
                    binding.llOwnerControls.setVisibility(View.VISIBLE);
                    binding.btnEditListing.setVisibility(View.GONE);
                    binding.btnToggleRented.setVisibility(View.GONE);
                    binding.btnDeleteListing.setVisibility(View.VISIBLE);
                } else {
                    binding.llOwnerControls.setVisibility(View.GONE);
                }

                if (isAdminGlobal && "PENDING".equalsIgnoreCase(listing.getStatus())) {
                    binding.llAdminControls.setVisibility(View.VISIBLE);
                } else {
                    binding.llAdminControls.setVisibility(View.GONE);
                }
                
                // Initialize Reviews Section AFTER we know the user's role
                setupReviews();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateRentedButtonText() {
        if (listing.isRented() || "RENTED".equalsIgnoreCase(listing.getStatus())) {
            binding.btnToggleRented.setText(R.string.mark_as_rented);
        } else {
            binding.btnToggleRented.setText(R.string.mark_as_rented); // Assuming you want a toggle text here, keeping as is but using string res
        }
    }
    
    private void setupReviews() {
        if (listing.getLandlordId() == null) return;
        
        binding.rvReviews.setLayoutManager(new LinearLayoutManager(this));
        reviewAdapter = new ReviewAdapter(isAdminGlobal, this::deleteReview);
        binding.rvReviews.setAdapter(reviewAdapter);
        
        binding.btnViewAllReviews.setOnClickListener(v -> showAllReviewsDialog());
        binding.btnWriteReview.setOnClickListener(v -> promptWriteReview());
        
        DatabaseReference reviewsRef = FirebaseDb.getRef("Reviews").child(listing.getLandlordId());
        reviewsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allReviews.clear();
                boolean currentUserHasReviewed = false;
                String currentUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
                
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Review review = postSnapshot.getValue(Review.class);
                    if (review != null) {
                        allReviews.add(review);
                        if (review.getStudentId() != null && review.getStudentId().equals(currentUid)) {
                            currentUserHasReviewed = true;
                        }
                    }
                }
                
                if (allReviews.isEmpty()) {
                    binding.tvNoReviews.setVisibility(View.VISIBLE);
                    binding.rvReviews.setVisibility(View.GONE);
                    binding.btnViewAllReviews.setVisibility(View.GONE);
                } else {
                    binding.tvNoReviews.setVisibility(View.GONE);
                    binding.rvReviews.setVisibility(View.VISIBLE);
                    
                    if (allReviews.size() > 2) {
                        reviewAdapter.setReviews(allReviews.subList(allReviews.size() - 2, allReviews.size())); // Show latest 2
                        binding.btnViewAllReviews.setVisibility(View.VISIBLE);
                    } else {
                        reviewAdapter.setReviews(allReviews);
                        binding.btnViewAllReviews.setVisibility(View.GONE);
                    }
                }
                
                // Only show Write Review button if they are a student and haven't reviewed yet
                if (isCurrentUserStudent && !currentUserHasReviewed) {
                    binding.btnWriteReview.setVisibility(View.VISIBLE);
                } else {
                    binding.btnWriteReview.setVisibility(View.GONE);
                }
                
                // Show entire section if there are reviews OR if the user can write one
                if (allReviews.isEmpty() && (!isCurrentUserStudent || currentUserHasReviewed)) {
                    binding.llReviewsSection.setVisibility(View.GONE);
                } else {
                    binding.llReviewsSection.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    
    private void showAllReviewsDialog() {
        if (allReviews.isEmpty()) return;
        
        androidx.recyclerview.widget.RecyclerView rv = new androidx.recyclerview.widget.RecyclerView(this);
        rv.setLayoutManager(new LinearLayoutManager(this));
        ReviewAdapter fullAdapter = new ReviewAdapter(isAdminGlobal, this::deleteReview);
        fullAdapter.setReviews(allReviews);
        rv.setAdapter(fullAdapter);
        
        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 30;
        params.rightMargin = 30;
        params.topMargin = 20;
        params.bottomMargin = 20;
        rv.setLayoutParams(params);
        container.addView(rv);
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Landlord Reviews")
                .setView(container)
                .setPositiveButton("Close", null)
                .show();
    }
    
    private void promptWriteReview() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_write_review, null);
        android.widget.RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
        EditText etReviewText = dialogView.findViewById(R.id.etReviewText);
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Write a Review")
                .setView(dialogView)
                .setPositiveButton("Submit", (dialog, which) -> {
                    if (!NetworkUtils.isNetworkAvailable(this)) {
                        Toast.makeText(this, "No internet connection.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    float rating = ratingBar.getRating();
                    String text = etReviewText.getText().toString().trim();
                    submitReview(rating, text);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void submitReview(float rating, String text) {
        if (mAuth.getCurrentUser() == null || listing.getLandlordId() == null) return;
        
        String uid = mAuth.getCurrentUser().getUid();
        DatabaseReference reviewsRef = FirebaseDb.getRef("Reviews").child(listing.getLandlordId());
        String reviewId = reviewsRef.push().getKey();
        
        if (reviewId == null) return;
        
        Review review = new Review(reviewId, listing.getLandlordId(), uid, currentStudentName, currentStudentInfo, rating, text, System.currentTimeMillis());
        
        reviewsRef.child(reviewId).setValue(review).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Update aggregate ratings via Transaction on the Landlord's User profile
                updateLandlordAggregateRating(listing.getLandlordId(), rating, 1);
                
                // SEND NOTIFICATION TO LANDLORD
                sendNotificationToUser(listing.getLandlordId(), "New Review Received! ⭐", "A student just left a " + rating + "-star review on your profile.");
                
                Toast.makeText(this, "Review submitted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to submit review.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void deleteReview(Review review) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Review")
                .setMessage("Are you sure you want to delete this review?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (!NetworkUtils.isNetworkAvailable(this)) {
                        Toast.makeText(this, "No internet connection.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    DatabaseReference reviewsRef = FirebaseDb.getRef("Reviews").child(review.getLandlordId()).child(review.getReviewId());
                    reviewsRef.removeValue().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Decrement aggregate ratings
                            updateLandlordAggregateRating(review.getLandlordId(), -review.getRating(), -1);
                            Toast.makeText(this, "Review deleted.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Failed to delete review.", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void updateLandlordAggregateRating(String landlordId, double ratingDelta, int countDelta) {
        DatabaseReference userRef = FirebaseDb.getRef("Users").child(landlordId);
        userRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                User u = currentData.getValue(User.class);
                if (u == null) {
                    return Transaction.success(currentData);
                }
                
                double newRatingSum = u.getRatingSum() + ratingDelta;
                int newTotalReviews = u.getTotalReviews() + countDelta;
                
                // Guard against negative values due to floating point math issues or orphaned data
                if (newTotalReviews <= 0 || newRatingSum < 0) {
                    newRatingSum = 0;
                    newTotalReviews = 0;
                }
                
                u.setRatingSum(newRatingSum);
                u.setTotalReviews(newTotalReviews);
                currentData.setValue(u);
                
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {}
        });
    }

    private void setupClickListeners() {
        if (listing == null) return;
        
        binding.ivFavorite.setOnClickListener(v -> toggleFavorite());

        binding.btnToggleRented.setOnClickListener(v -> {
            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "No internet connection.", Toast.LENGTH_SHORT).show();
                return;
            }
            boolean newRentedState = !listing.isRented();
            listing.setRented(newRentedState);
            listing.setStatus(newRentedState ? "RENTED" : "APPROVED");
            
            DatabaseReference ref = FirebaseDb.getRef("Listings").child(listing.getId());
            ref.child("rented").setValue(newRentedState);
            ref.child("status").setValue(newRentedState ? "RENTED" : "APPROVED").addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    updateRentedButtonText();
                    Toast.makeText(this, newRentedState ? "Marked as Rented" : "Marked as Available", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show();
                }
            });
        });
        
        binding.btnEditListing.setOnClickListener(v -> {
            // Re-fetch listing images from Firestore before editing so they are passed over
            Intent intent = new Intent(ListingDetailActivity.this, AddListingActivity.class);
            listing.setImageBase64List(loadedImages); // Inject fetched images
            intent.putExtra("EDIT_LISTING", listing);
            startActivity(intent);
            finish();
        });

        binding.btnDeleteListing.setOnClickListener(v -> {
            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "No internet connection.", Toast.LENGTH_SHORT).show();
                return;
            }
            DatabaseReference ref = FirebaseDb.getRef("Listings").child(listing.getId());
            ref.removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Optional: Clean up images in Firestore
                    firestore.collection("ListingImages").document(listing.getId()).delete();
                    Toast.makeText(this, "Listing deleted", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Failed to delete listing", Toast.LENGTH_SHORT).show();
                }
            });
        });

        binding.btnApproveListing.setOnClickListener(v -> {
            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "No internet connection.", Toast.LENGTH_SHORT).show();
                return;
            }
            listing.setStatus("APPROVED");
            DatabaseReference ref = FirebaseDb.getRef("Listings").child(listing.getId());
            ref.child("status").setValue("APPROVED");
            ref.child("rejectionReason").setValue("").addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // SEND NOTIFICATION
                    sendNotificationToUser(listing.getLandlordId(), "Listing Approved! ✅", "Your property '" + listing.getTitle() + "' is now live on the feed.");
                    
                    Toast.makeText(this, "Listing Approved!", Toast.LENGTH_SHORT).show();
                    binding.llAdminControls.setVisibility(View.GONE);
                } else {
                    Toast.makeText(this, "Failed to approve listing.", Toast.LENGTH_SHORT).show();
                }
            });
        });
        
        binding.btnRejectListing.setOnClickListener(v -> promptRejectListing());

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
                String formattedPhone = phone.replaceAll("[^0-9]", ""); 
                if (formattedPhone.startsWith("01")) {
                    formattedPhone = "88" + formattedPhone;
                }

                String message = "Hi, I am interested in your listing on BashaBhai: " + listing.getTitle() + " (" + listing.getFormattedPostNumber() + ")";
                
                try {
                    Intent whatsappIntent = new Intent(Intent.ACTION_VIEW);
                    whatsappIntent.setData(Uri.parse("https://wa.me/" + formattedPhone + "?text=" + Uri.encode(message)));
                    startActivity(whatsappIntent);
                } catch (ActivityNotFoundException e) {
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
    
    private void promptRejectListing() {
        EditText input = new EditText(this);
        input.setHint("Enter reason for rejection...");
        
        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 50;
        params.rightMargin = 50;
        input.setLayoutParams(params);
        container.addView(input);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Reject Listing")
                .setMessage("Please provide feedback for the landlord so they can fix their post:")
                .setView(container)
                .setPositiveButton("Reject", (dialog, which) -> {
                    if (!NetworkUtils.isNetworkAvailable(this)) {
                        Toast.makeText(this, "No internet connection.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String reason = input.getText().toString().trim();
                    if (reason.isEmpty()) {
                        Toast.makeText(this, "A reason is required to reject!", Toast.LENGTH_SHORT).show();
                    } else {
                        executeRejectListing(reason);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void executeRejectListing(String reason) {
        listing.setStatus("REJECTED");
        listing.setRejectionReason(reason);
        
        DatabaseReference ref = FirebaseDb.getRef("Listings").child(listing.getId());
        ref.child("status").setValue("REJECTED");
        ref.child("rejectionReason").setValue(reason).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // SEND NOTIFICATION
                sendNotificationToUser(listing.getLandlordId(), "Listing Rejected 🔴", "Your property '" + listing.getTitle() + "' was rejected. Reason: " + reason);
                
                Toast.makeText(this, "Listing Rejected 🔴.", Toast.LENGTH_SHORT).show();
                binding.llAdminControls.setVisibility(View.GONE);
                
                binding.tvDetailRejectionReason.setVisibility(View.VISIBLE);
                binding.tvDetailRejectionReason.setText(getString(R.string.rejection_reason_placeholder).replace("…", reason));
            } else {
                Toast.makeText(this, "Failed to reject listing.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void sendNotificationToUser(String targetUid, String title, String message) {
        if (targetUid == null) return;
        DatabaseReference notifRef = FirebaseDb.getRef("Notifications").child(targetUid);
        String notifId = notifRef.push().getKey();
        if (notifId != null) {
            AppNotification notif = new AppNotification(notifId, title, message, "SYSTEM", System.currentTimeMillis());
            notifRef.child(notifId).setValue(notif);
        }
    }
    
    private void toggleFavorite() {
        if (listing == null || listing.getId() == null || mAuth.getCurrentUser() == null) return;
        
        String currentUserId = mAuth.getCurrentUser().getUid();
        
        FavoriteListing fav = new FavoriteListing(
                listing.getId(), currentUserId, listing.getLandlordId(), listing.getTitle(), listing.getType(),
                listing.getRent(), listing.getLocation(), listing.getLandlordPhone(),
                listing.getAmenities(), listing.getGenderPreference(), listing.getDescription(),
                listing.getMapLink(), listing.getPostNumber(), listing.getRejectionReason(), loadedImages
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
