package com.example.bashabhai;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.bashabhai.databinding.FragmentMyPostingsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MyPostingsFragment extends Fragment {

    private FragmentMyPostingsBinding binding;
    private ListingAdapter adapter;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMyPostingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDb.getRef("Listings");

        setupRecyclerView();
        
        binding.btnAddPost.setOnClickListener(v -> 
            startActivity(new Intent(requireActivity(), AddListingActivity.class))
        );

        fetchMyPostings();
    }

    private void setupRecyclerView() {
        adapter = new ListingAdapter(listing -> {
            Intent intent = new Intent(requireActivity(), ListingDetailActivity.class);
            intent.putExtra("LISTING", listing);
            startActivity(intent);
        });
        binding.rvMyPostings.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMyPostings.setAdapter(adapter);
    }

    private void fetchMyPostings() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        
        String currentUid = currentUser.getUid();
        
        databaseReference.orderByChild("landlordId").equalTo(currentUid)
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Listing> myListings = new ArrayList<>();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Listing listing = postSnapshot.getValue(Listing.class);
                    if (listing != null) {
                        myListings.add(listing);
                    }
                }
                
                if (binding == null) return;

                if (myListings.isEmpty()) {
                    binding.llNoPostings.setVisibility(View.VISIBLE);
                    binding.rvMyPostings.setVisibility(View.GONE);
                } else {
                    binding.llNoPostings.setVisibility(View.GONE);
                    binding.rvMyPostings.setVisibility(View.VISIBLE);
                    adapter.setListings(myListings);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load your postings.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
