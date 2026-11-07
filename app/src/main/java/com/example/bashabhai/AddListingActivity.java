package com.example.bashabhai;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bashabhai.databinding.ActivityAddListingBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;

public class AddListingActivity extends AppCompatActivity {

    private ActivityAddListingBinding binding;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;

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
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String landlordId = currentUser != null ? currentUser.getUid() : "Unknown";
        String userEmail = currentUser != null ? currentUser.getEmail() : "";
        
        // Initial status: APPROVED if Admin, PENDING if Landlord
        String initialStatus = (userEmail != null && userEmail.equalsIgnoreCase("admin@bashabhai.com")) ? "APPROVED" : "PENDING";

        // Generate a unique ID for the listing
        String listingId = databaseReference.push().getKey();

        Listing newListing = new Listing(
                listingId, landlordId, title, type, rent, location, 
                description, amenities, gender, false, 0.0, 0.0, phone, initialStatus
        );

        if (listingId != null) {
            databaseReference.child(listingId).setValue(newListing)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if ("APPROVED".equalsIgnoreCase(initialStatus)) {
                                Toast.makeText(AddListingActivity.this, "Listing published directly!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(AddListingActivity.this, "Listing submitted! Pending Admin approval.", Toast.LENGTH_LONG).show();
                            }
                            finish();
                        } else {
                            Toast.makeText(AddListingActivity.this, "Failed to add listing.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}
