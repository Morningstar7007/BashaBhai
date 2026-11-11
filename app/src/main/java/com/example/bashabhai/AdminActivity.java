package com.example.bashabhai;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.bashabhai.databinding.ActivityAdminBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
            
            // Set Listings button to Active
            binding.btnTabListings.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
            binding.btnTabListings.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
            binding.btnTabListings.setTextColor(Color.WHITE);
            
            // Set Verifications button to Inactive
            binding.btnTabVerifications.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            binding.btnTabVerifications.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.text_secondary)));
            binding.btnTabVerifications.setTextColor(ContextCompat.getColor(this, R.color.on_surface));

            binding.rvPendingListings.setVisibility(View.VISIBLE);
            binding.rvPendingVerifications.setVisibility(View.GONE);
            fetchPendingListings();
        });

        binding.btnTabVerifications.setOnClickListener(v -> {
            isListingsTab = false;
            
            // Set Verifications button to Active
            binding.btnTabVerifications.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
            binding.btnTabVerifications.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
            binding.btnTabVerifications.setTextColor(Color.WHITE);
            
            // Set Listings button to Inactive
            binding.btnTabListings.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            binding.btnTabListings.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.text_secondary)));
            binding.btnTabListings.setTextColor(ContextCompat.getColor(this, R.color.on_surface));

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
                if (!NetworkUtils.isNetworkAvailable(AdminActivity.this)) {
                    Toast.makeText(AdminActivity.this, "No internet connection.", Toast.LENGTH_SHORT).show();
                    return;
                }
                approveVerification(request);
            }

            @Override
            public void onReject(VerificationRequest request) {
                promptRejectVerification(request);
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
                        // Only fetch verifications that are PENDING
                        String status = req.getStatus() != null ? req.getStatus() : "PENDING";
                        if ("PENDING".equalsIgnoreCase(status)) {
                            req.setUserId(postSnapshot.getKey());
                            requests.add(req);
                        }
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
        userRef.child("verificationStatus").setValue("APPROVED");
        userRef.child("verificationRejectionReason").setValue("").addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // INSTEAD of deleting it, we save it historically by marking it APPROVED
                verificationsRef.child(uid).child("status").setValue("APPROVED");
                
                // SEND NOTIFICATION
                sendNotificationToUser(uid, "Verification Approved! ✅", "Congratulations! Your profile has been verified and you can now publish listings.");
                
                Toast.makeText(AdminActivity.this, "Landlord Verified 🟢 successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(AdminActivity.this, "Failed to approve verification.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void promptRejectVerification(VerificationRequest request) {
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
                .setTitle("Reject Verification")
                .setMessage("Please provide feedback for the landlord:")
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
                        executeRejectVerification(request.getUserId(), reason);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeRejectVerification(String uid, String reason) {
        DatabaseReference userRef = FirebaseDb.getRef("Users").child(uid);
        
        userRef.child("verified").setValue(false);
        userRef.child("verificationStatus").setValue("REJECTED");
        userRef.child("verificationRejectionReason").setValue(reason).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // INSTEAD of deleting it, we save it historically by marking it REJECTED
                verificationsRef.child(uid).child("status").setValue("REJECTED");
                
                // SEND NOTIFICATION
                sendNotificationToUser(uid, "Verification Rejected 🔴", "Your document verification failed. Reason: " + reason);
                
                Toast.makeText(AdminActivity.this, "Verification Rejected 🔴.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(AdminActivity.this, "Failed to reject verification.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void sendNotificationToUser(String targetUid, String title, String message) {
        DatabaseReference notifRef = FirebaseDb.getRef("Notifications").child(targetUid);
        String notifId = notifRef.push().getKey();
        if (notifId != null) {
            AppNotification notif = new AppNotification(notifId, title, message, "SYSTEM", System.currentTimeMillis());
            notifRef.child(notifId).setValue(notif);
        }
    }
}
