package com.example.bashabhai;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.bashabhai.databinding.ActivityAdminBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AdminActivity extends AppCompatActivity {

    private ActivityAdminBinding binding;
    private ListingAdapter listingsAdapter;
    private VerificationAdapter verificationsAdapter;
    private DatabaseReference listingsRef;
    private DatabaseReference verificationsRef;
    private boolean isListingsTab = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        listingsRef = FirebaseDb.getRef("Listings");
        verificationsRef = FirebaseDb.getRef("VerificationRequests");

        setupRecyclerViews();
        setupTabButtons();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isListingsTab) {
            fetchPendingListings();
        } else {
            fetchPendingVerifications();
        }
    }

    private void setupTabButtons() {
        binding.btnTabListings.setOnClickListener(v -> {
            isListingsTab = true;
            binding.btnTabListings.setBackgroundColor(getColor(R.color.primary));
            binding.btnTabListings.setTextColor(Color.WHITE);
            binding.btnTabVerifications.setBackgroundColor(Color.TRANSPARENT);
            binding.btnTabVerifications.setTextColor(getColor(R.color.on_surface));

            binding.rvPendingListings.setVisibility(View.VISIBLE);
            binding.rvPendingVerifications.setVisibility(View.GONE);
            fetchPendingListings();
        });

        binding.btnTabVerifications.setOnClickListener(v -> {
            isListingsTab = false;
            binding.btnTabVerifications.setBackgroundColor(getColor(R.color.primary));
            binding.btnTabVerifications.setTextColor(Color.WHITE);
            binding.btnTabListings.setBackgroundColor(Color.TRANSPARENT);
            binding.btnTabListings.setTextColor(getColor(R.color.on_surface));

            binding.rvPendingListings.setVisibility(View.GONE);
            binding.rvPendingVerifications.setVisibility(View.VISIBLE);
            fetchPendingVerifications();
        });
    }

    private void setupRecyclerViews() {
        listingsAdapter = new ListingAdapter(listing -> {
            Intent intent = new Intent(AdminActivity.this, ListingDetailActivity.class);
            intent.putExtra("LISTING", listing);
            startActivity(intent);
        });
        binding.rvPendingListings.setLayoutManager(new LinearLayoutManager(this));
        binding.rvPendingListings.setAdapter(listingsAdapter);

        verificationsAdapter = new VerificationAdapter(new VerificationAdapter.OnActionListener() {
            @Override
            public void onApprove(VerificationRequest request) {
                approveVerification(request);
            }

            @Override
            public void onReject(VerificationRequest request) {
                rejectVerification(request);
            }
        });
        binding.rvPendingVerifications.setLayoutManager(new LinearLayoutManager(this));
        binding.rvPendingVerifications.setAdapter(verificationsAdapter);
    }

    private void fetchPendingListings() {
        listingsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isListingsTab) return;
                List<Listing> pendingListings = new ArrayList<>();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Listing listing = postSnapshot.getValue(Listing.class);
                    if (listing != null) {
                        String status = listing.getStatus() != null ? listing.getStatus() : "APPROVED";
                        if ("PENDING".equalsIgnoreCase(status)) {
                            pendingListings.add(listing);
                        }
                    }
                }

                if (pendingListings.isEmpty()) {
                    binding.tvNoPending.setText("No pending listings awaiting approval.");
                    binding.tvNoPending.setVisibility(View.VISIBLE);
                    binding.rvPendingListings.setVisibility(View.GONE);
                } else {
                    binding.tvNoPending.setVisibility(View.GONE);
                    binding.rvPendingListings.setVisibility(View.VISIBLE);
                    listingsAdapter.setListings(pendingListings);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminActivity.this, "Failed to load pending listings.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchPendingVerifications() {
        verificationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isListingsTab) return;
                List<VerificationRequest> requests = new ArrayList<>();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    VerificationRequest req = postSnapshot.getValue(VerificationRequest.class);
                    if (req != null) {
                        req.setUserId(postSnapshot.getKey());
                        requests.add(req);
                    }
                }

                if (requests.isEmpty()) {
                    binding.tvNoPending.setText("No verification requests awaiting review.");
                    binding.tvNoPending.setVisibility(View.VISIBLE);
                    binding.rvPendingVerifications.setVisibility(View.GONE);
                } else {
                    binding.tvNoPending.setVisibility(View.GONE);
                    binding.rvPendingVerifications.setVisibility(View.VISIBLE);
                    verificationsAdapter.setRequests(requests);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminActivity.this, "Failed to load verification requests.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void approveVerification(VerificationRequest request) {
        String uid = request.getUserId();
        DatabaseReference userRef = FirebaseDb.getRef("Users").child(uid);
        
        userRef.child("verified").setValue(true);
        userRef.child("verificationStatus").setValue("APPROVED").addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                verificationsRef.child(uid).removeValue();
                Toast.makeText(AdminActivity.this, "Landlord Verified 🟢 successfully!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void rejectVerification(VerificationRequest request) {
        String uid = request.getUserId();
        DatabaseReference userRef = FirebaseDb.getRef("Users").child(uid);
        
        userRef.child("verified").setValue(false);
        userRef.child("verificationStatus").setValue("REJECTED").addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                verificationsRef.child(uid).removeValue();
                Toast.makeText(AdminActivity.this, "Verification Rejected 🔴.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
