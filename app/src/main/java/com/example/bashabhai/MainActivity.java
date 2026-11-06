package com.example.bashabhai;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.bashabhai.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ListingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupRecyclerView();

        binding.fabAddListing.setOnClickListener(v -> {
            Toast.makeText(this, "Add listing clicked", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to AddListingActivity in Step 3
        });
        
        // Dummy data for Step 2 testing
        loadDummyData();
    }

    private void setupRecyclerView() {
        adapter = new ListingAdapter(listing -> {
            Toast.makeText(this, "Clicked: " + listing.getTitle(), Toast.LENGTH_SHORT).show();
            // TODO: Navigate to ListingDetailActivity in Step 4
        });
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
    }
    
    private void loadDummyData() {
        List<Listing> dummies = new ArrayList<>();
        dummies.add(new Listing("1", "landlord1", "Spacious Room Near Campus", "Room", 3500.0, "Dhanmondi 27", "Good room", "WiFi, Water", "Male", false, 0.0, 0.0, "01700000000"));
        dummies.add(new Listing("2", "landlord2", "2-Bed Flat", "Flat", 15000.0, "Panthapath", "Nice flat", "Gas, Water", "Any", false, 0.0, 0.0, "01800000000"));
        dummies.add(new Listing("3", "landlord3", "Mess Seat Available", "Mess Seat", 2000.0, "Farmgate", "Student mess", "Meal, WiFi", "Female", true, 0.0, 0.0, "01900000000"));
        
        adapter.setListings(dummies);
    }
}
