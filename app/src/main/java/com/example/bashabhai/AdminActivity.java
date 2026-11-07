package com.example.bashabhai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

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
    private ListingAdapter adapter;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        databaseReference = FirebaseDb.getRef("Listings");

        setupRecyclerView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchPendingListings();
    }

    private void setupRecyclerView() {
        adapter = new ListingAdapter(listing -> {
            Intent intent = new Intent(AdminActivity.this, ListingDetailActivity.class);
            intent.putExtra("LISTING", listing);
            startActivity(intent);
        });
        binding.rvPendingListings.setLayoutManager(new LinearLayoutManager(this));
        binding.rvPendingListings.setAdapter(adapter);
    }

    private void fetchPendingListings() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
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
                    binding.tvNoPending.setVisibility(View.VISIBLE);
                    binding.rvPendingListings.setVisibility(View.GONE);
                } else {
                    binding.tvNoPending.setVisibility(View.GONE);
                    binding.rvPendingListings.setVisibility(View.VISIBLE);
                    adapter.setListings(pendingListings);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(AdminActivity.this, "Failed to load pending listings.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
