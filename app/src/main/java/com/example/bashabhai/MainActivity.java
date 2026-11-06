package com.example.bashabhai;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;

import com.example.bashabhai.databinding.ActivityMainBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ListingAdapter adapter;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupRecyclerView();

        databaseReference = FirebaseDatabase.getInstance().getReference("Listings");

        binding.fabAddListing.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AddListingActivity.class));
        });
        
        fetchListingsFromDatabase();
    }

    private void setupRecyclerView() {
        adapter = new ListingAdapter(listing -> {
            Toast.makeText(this, "Clicked: " + listing.getTitle(), Toast.LENGTH_SHORT).show();
            // TODO: Navigate to ListingDetailActivity in Step 4
        });
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
    }
    
    private void fetchListingsFromDatabase() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Listing> fetchedListings = new ArrayList<>();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Listing listing = postSnapshot.getValue(Listing.class);
                    if (listing != null) {
                        fetchedListings.add(listing);
                    }
                }
                adapter.setListings(fetchedListings);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Failed to load listings.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
