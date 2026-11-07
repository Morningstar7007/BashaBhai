package com.example.bashabhai;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bashabhai.databinding.ActivityAddListingBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class AddListingActivity extends AppCompatActivity {

    private ActivityAddListingBinding binding;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    
    private boolean isEditMode = false;
    private String existingListingId = null;
    private String existingStatus = "PENDING";
    private Listing editingListingObj = null;
    private String currentUserRole = "LANDLORD";

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

        setupDropdowns();
        
        checkIfEditMode();
        fetchCurrentUserRoleAndDefaults();
        
        binding.btnSubmitListing.setOnClickListener(v -> submitListing());
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
                existingStatus = editingListingObj.getStatus() != null ? editingListingObj.getStatus() : "PENDING";
                
                binding.etTitle.setText(editingListingObj.getTitle());
                binding.actType.setText(editingListingObj.getType(), false);
                binding.etRent.setText(String.valueOf(editingListingObj.getRent()));
                binding.etLocation.setText(editingListingObj.getLocation());
                binding.etMapLink.setText(editingListingObj.getMapLink() != null ? editingListingObj.getMapLink() : "");
                binding.etPhone.setText(editingListingObj.getLandlordPhone());
                binding.etAmenities.setText(editingListingObj.getAmenities());
                binding.actGender.setText(editingListingObj.getGenderPreference(), false);
                binding.etDescription.setText(editingListingObj.getDescription());
                
                binding.btnSubmitListing.setText("Update Listing");
            }
        }
    }

    private void submitListing() {
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
                    saveListingToDatabase(listingId, landlordId, title, type, rent, location, description, amenities, gender, mapLink, phone, finalStatus, assignedNumber);
                }
            });
        } else {
            int currentNum = editingListingObj != null && editingListingObj.getPostNumber() > 0 ? editingListingObj.getPostNumber() : 1000;
            saveListingToDatabase(existingListingId, landlordId, title, type, rent, location, description, amenities, gender, mapLink, phone, finalStatus, currentNum);
        }
    }

    private void saveListingToDatabase(String listingId, String landlordId, String title, String type, double rent, String location, String description, String amenities, String gender, String mapLink, String phone, String finalStatus, int postNumber) {
        Listing newListing = new Listing(
                listingId, landlordId, title, type, rent, location, 
                description, amenities, gender, false, mapLink, phone, finalStatus, postNumber
        );

        if (listingId != null) {
            databaseReference.child(listingId).setValue(newListing)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (isEditMode) {
                                Toast.makeText(AddListingActivity.this, "Listing updated successfully!", Toast.LENGTH_SHORT).show();
                            } else if ("APPROVED".equalsIgnoreCase(finalStatus)) {
                                Toast.makeText(AddListingActivity.this, "Listing published", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(AddListingActivity.this, "Listing submitted! Pending Admin approval.", Toast.LENGTH_LONG).show();
                            }
                            finish();
                        } else {
                            binding.btnSubmitListing.setEnabled(true);
                            Toast.makeText(AddListingActivity.this, "Failed to save listing.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}
