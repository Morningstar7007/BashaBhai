package com.example.bashabhai;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bashabhai.databinding.ActivityAddListingBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AddListingActivity extends AppCompatActivity {

    private ActivityAddListingBinding binding;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddListingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        // Initialize Firebase Realtime Database
        databaseReference = FirebaseDatabase.getInstance().getReference("Listings");

        binding.btnSubmitListing.setOnClickListener(v -> submitListing());
    }

    private void submitListing() {
        String title = binding.etTitle.getText().toString().trim();
        String type = binding.etType.getText().toString().trim();
        String rentStr = binding.etRent.getText().toString().trim();
        String location = binding.etLocation.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String amenities = binding.etAmenities.getText().toString().trim();
        String gender = binding.etGender.getText().toString().trim();
        String description = binding.etDescription.getText().toString().trim();

        if (title.isEmpty() || type.isEmpty() || rentStr.isEmpty() || location.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill in all mandatory fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double rent = Double.parseDouble(rentStr);
        String landlordId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "Unknown";
        
        // Generate a unique ID for the listing
        String listingId = databaseReference.push().getKey();

        Listing newListing = new Listing(
                listingId, landlordId, title, type, rent, location, 
                description, amenities, gender, false, 0.0, 0.0, phone
        );

        if (listingId != null) {
            databaseReference.child(listingId).setValue(newListing)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(AddListingActivity.this, "Listing added successfully!", Toast.LENGTH_SHORT).show();
                            finish(); // Close activity and go back to MainActivity
                        } else {
                            Toast.makeText(AddListingActivity.this, "Failed to add listing.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}
