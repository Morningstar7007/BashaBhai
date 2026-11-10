package com.example.bashabhai;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.bashabhai.databinding.ActivityAddListingBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddListingActivity extends AppCompatActivity {

    private ActivityAddListingBinding binding;
    private DatabaseReference databaseReference;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    
    private boolean isEditMode = false;
    private String existingListingId = null;
    private String existingStatus = "PENDING";
    private Listing editingListingObj = null;
    private String currentUserRole = "LANDLORD";
    
    private final List<String> selectedImagesBase64 = new ArrayList<>();
    
    private final ActivityResultLauncher<String> pickMultipleImagesLauncher = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris != null && !uris.isEmpty()) {
                    int limit = Math.min(uris.size(), 5 - selectedImagesBase64.size());
                    if (limit < uris.size()) {
                        Toast.makeText(this, "Can only add up to 5 photos total.", Toast.LENGTH_SHORT).show();
                    }
                    
                    for (int i = 0; i < limit; i++) {
                        String base64 = compressAndEncodeImage(uris.get(i));
                        if (base64 != null) {
                            selectedImagesBase64.add(base64);
                        }
                    }
                    updateImageThumbnails();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddListingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            int imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), Math.max(imeHeight, systemBars));
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDb.getRef("Listings");
        firestore = FirebaseFirestore.getInstance();

        setupDropdowns();
        
        checkIfEditMode();
        fetchCurrentUserRoleAndDefaults();
        
        binding.btnAddPhotos.setOnClickListener(v -> {
            if (selectedImagesBase64.size() >= 5) {
                Toast.makeText(this, "Maximum 5 photos allowed.", Toast.LENGTH_SHORT).show();
                return;
            }
            pickMultipleImagesLauncher.launch("image/*");
        });
        
        binding.btnSubmitListing.setOnClickListener(v -> submitListing());
    }
    
    private String compressAndEncodeImage(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();
            
            if (bitmap == null) return null;

            int maxSize = 600; 
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float ratio = (float) width / (float) height;
            if (ratio > 1) {
                width = maxSize;
                height = (int) (width / ratio);
            } else {
                height = maxSize;
                width = (int) (height * ratio);
            }
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos); 
            byte[] imageBytes = baos.toByteArray();
            return Base64.encodeToString(imageBytes, Base64.DEFAULT);
        } catch (Exception e) {
            return null;
        }
    }
    
    private void updateImageThumbnails() {
        binding.llImageThumbnails.removeAllViews();
        binding.llImageThumbnails.addView(binding.btnAddPhotos);
        
        if (selectedImagesBase64.size() >= 5) {
            binding.btnAddPhotos.setVisibility(View.GONE);
        } else {
            binding.btnAddPhotos.setVisibility(View.VISIBLE);
        }
        
        for (int i = 0; i < selectedImagesBase64.size(); i++) {
            final int index = i;
            String base64 = selectedImagesBase64.get(i);
            
            View thumbView = LayoutInflater.from(this).inflate(R.layout.item_carousel_image, binding.llImageThumbnails, false);
            ImageView iv = thumbView.findViewById(R.id.ivCarousel);
            
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(300, 300);
            params.setMarginEnd(24);
            thumbView.setLayoutParams(params);
            
            try {
                byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                Glide.with(this).load(bitmap).centerCrop().into(iv);
            } catch (Exception e) {
                iv.setImageResource(R.drawable.bashabhai_logo);
            }
            
            // Long press to remove photo
            thumbView.setOnLongClickListener(v -> {
                selectedImagesBase64.remove(index);
                updateImageThumbnails();
                Toast.makeText(this, "Photo removed", Toast.LENGTH_SHORT).show();
                return true;
            });
            
            binding.llImageThumbnails.addView(thumbView, binding.llImageThumbnails.getChildCount() - 1);
        }
    }
    
    private void fetchCurrentUserRoleAndDefaults() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        
        DatabaseReference userRef = FirebaseDb.getRef("Users").child(currentUser.getUid());
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User userProfile = snapshot.getValue(User.class);
                if (userProfile != null) {
                    if (userProfile.getRole() != null) {
                        currentUserRole = userProfile.getRole();
                    }
                    if (!isEditMode) {
                        if (userProfile.getDefaultPhone() != null && !userProfile.getDefaultPhone().isEmpty()) {
                            binding.etPhone.setText(userProfile.getDefaultPhone());
                        }
                        if (userProfile.getDefaultAddress() != null && !userProfile.getDefaultAddress().isEmpty()) {
                            binding.etLocation.setText(userProfile.getDefaultAddress());
                        }
                    }
                }
                
                if (currentUser.getEmail() != null && AdminConfig.isAdmin(currentUser.getEmail())) {
                    currentUserRole = "ADMIN";
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupDropdowns() {
        String[] types = new String[]{"Room", "Flat", "Mess Seat"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                types
        );
        binding.actType.setAdapter(typeAdapter);

        String[] genders = new String[]{"Male", "Female", "Any"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                genders
        );
        binding.actGender.setAdapter(genderAdapter);
    }
    
    private void checkIfEditMode() {
        if (getIntent().hasExtra("EDIT_LISTING")) {
            editingListingObj = (Listing) getIntent().getSerializableExtra("EDIT_LISTING");
            if (editingListingObj != null) {
                isEditMode = true;
                existingListingId = editingListingObj.getId();
                
                if ("REJECTED".equalsIgnoreCase(editingListingObj.getStatus())) {
                    existingStatus = "PENDING";
                } else {
                    existingStatus = editingListingObj.getStatus() != null ? editingListingObj.getStatus() : "PENDING";
                }
                
                binding.etTitle.setText(editingListingObj.getTitle());
                binding.actType.setText(editingListingObj.getType(), false);
                binding.etRent.setText(String.valueOf(editingListingObj.getRent()));
                binding.etLocation.setText(editingListingObj.getLocation());
                binding.etMapLink.setText(editingListingObj.getMapLink() != null ? editingListingObj.getMapLink() : "");
                binding.etPhone.setText(editingListingObj.getLandlordPhone());
                binding.etAmenities.setText(editingListingObj.getAmenities());
                binding.actGender.setText(editingListingObj.getGenderPreference(), false);
                binding.etDescription.setText(editingListingObj.getDescription());
                
                // Fetch images from Firestore if editing
                firestore.collection("ListingImages").document(existingListingId).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists() && documentSnapshot.contains("images")) {
                                List<String> fetchedImages = (List<String>) documentSnapshot.get("images");
                                if (fetchedImages != null) {
                                    selectedImagesBase64.addAll(fetchedImages);
                                    updateImageThumbnails();
                                }
                            }
                        });
                
                binding.btnSubmitListing.setText("Update Listing");
            }
        }
    }

    private void submitListing() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No internet connection.", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = binding.etTitle.getText().toString().trim();
        String type = binding.actType.getText().toString().trim();
        String rentStr = binding.etRent.getText().toString().trim();
        String location = binding.etLocation.getText().toString().trim();
        String mapLink = binding.etMapLink.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String amenities = binding.etAmenities.getText().toString().trim();
        String gender = binding.actGender.getText().toString().trim();
        String description = binding.etDescription.getText().toString().trim();

        if (title.isEmpty() || type.isEmpty() || rentStr.isEmpty() || location.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill in all mandatory fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double rent = Double.parseDouble(rentStr);
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String landlordId = currentUser != null ? currentUser.getUid() : "Unknown";
        String userEmail = currentUser != null ? currentUser.getEmail() : "";
        
        boolean isAdminUser = "ADMIN".equalsIgnoreCase(currentUserRole) || AdminConfig.isAdmin(userEmail);
        
        String listingId = isEditMode ? existingListingId : databaseReference.push().getKey();
        String finalStatus = isEditMode ? existingStatus : (isAdminUser ? "APPROVED" : "PENDING");

        if (!isEditMode) {
            binding.btnSubmitListing.setEnabled(false);
            DatabaseReference counterRef = FirebaseDb.getRef("Counters").child("postNumber");
            counterRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
                @NonNull
                @Override
                public com.google.firebase.database.Transaction.Result doTransaction(@NonNull com.google.firebase.database.MutableData currentData) {
                    Integer current = currentData.getValue(Integer.class);
                    if (current == null) {
                        currentData.setValue(1001);
                    } else {
                        currentData.setValue(current + 1);
                    }
                    return com.google.firebase.database.Transaction.success(currentData);
                }

                @Override
                public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot snapshot) {
                    int assignedNumber = 1001;
                    if (committed && snapshot != null && snapshot.getValue(Integer.class) != null) {
                        assignedNumber = snapshot.getValue(Integer.class);
                    }
                    saveListingToDatabases(listingId, landlordId, title, type, rent, location, description, amenities, gender, mapLink, phone, finalStatus, assignedNumber);
                }
            });
        } else {
            int currentNum = editingListingObj != null && editingListingObj.getPostNumber() > 0 ? editingListingObj.getPostNumber() : 1000;
            saveListingToDatabases(existingListingId, landlordId, title, type, rent, location, description, amenities, gender, mapLink, phone, finalStatus, currentNum);
        }
    }

    private void saveListingToDatabases(String listingId, String landlordId, String title, String type, double rent, String location, String description, String amenities, String gender, String mapLink, String phone, String finalStatus, int postNumber) {
        if (listingId == null) return;
        
        // 1. Prepare Text Data for Realtime Database (Images are Excluded from this object!)
        Listing newListing = new Listing(
                listingId, landlordId, title, type, rent, location, 
                description, amenities, gender, false, mapLink, phone, finalStatus, postNumber, new ArrayList<>()
        );
        newListing.setRejectionReason("");

        // 2. Prepare Image Data for Firestore
        Map<String, Object> firestoreData = new HashMap<>();
        firestoreData.put("images", selectedImagesBase64);

        // Execute both saves simultaneously
        databaseReference.child(listingId).setValue(newListing).addOnCompleteListener(task1 -> {
            if (task1.isSuccessful()) {
                
                firestore.collection("ListingImages").document(listingId).set(firestoreData).addOnCompleteListener(task2 -> {
                    if (task2.isSuccessful()) {
                        if (isEditMode) {
                            if ("PENDING".equalsIgnoreCase(finalStatus)) {
                                Toast.makeText(AddListingActivity.this, "Listing updated and sent for Admin review!", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(AddListingActivity.this, "Listing updated successfully!", Toast.LENGTH_SHORT).show();
                            }
                        } else if ("APPROVED".equalsIgnoreCase(finalStatus)) {
                            Toast.makeText(AddListingActivity.this, "Listing published", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(AddListingActivity.this, "Listing submitted! Pending Admin approval.", Toast.LENGTH_LONG).show();
                        }
                        finish();
                    } else {
                        binding.btnSubmitListing.setEnabled(true);
                        Toast.makeText(AddListingActivity.this, "Text saved, but failed to save images.", Toast.LENGTH_SHORT).show();
                    }
                });
                
            } else {
                binding.btnSubmitListing.setEnabled(true);
                Toast.makeText(AddListingActivity.this, "Failed to save listing.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
