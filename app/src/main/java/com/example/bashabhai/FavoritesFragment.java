package com.example.bashabhai;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.bashabhai.databinding.FragmentFavoritesBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class FavoritesFragment extends Fragment {

    private FragmentFavoritesBinding binding;
    private ListingAdapter adapter;
    private FavoriteDao favoriteDao;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFavoritesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        mAuth = FirebaseAuth.getInstance();
        favoriteDao = AppDatabase.getInstance(requireContext()).favoriteDao();
        setupRecyclerView();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFavorites();
    }

    private void setupRecyclerView() {
        adapter = new ListingAdapter(listing -> {
            Intent intent = new Intent(requireActivity(), ListingDetailActivity.class);
            intent.putExtra("LISTING", listing);
            startActivity(intent);
        });
        binding.rvFavorites.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvFavorites.setAdapter(adapter);
    }

    private void loadFavorites() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        
        String currentUserId = currentUser.getUid();
        List<FavoriteListing> favs = favoriteDao.getAllFavorites(currentUserId);
        
        if (favs.isEmpty()) {
            binding.llNoFavorites.setVisibility(View.VISIBLE);
            binding.rvFavorites.setVisibility(View.GONE);
        } else {
            binding.llNoFavorites.setVisibility(View.GONE);
            binding.rvFavorites.setVisibility(View.VISIBLE);
            
            // Map FavoriteListing back to regular Listing for the adapter
            List<Listing> displayListings = new ArrayList<>();
            for (FavoriteListing f : favs) {
                Listing l = new Listing();
                l.setId(f.getId());
                l.setLandlordId(f.getLandlordId());
                l.setTitle(f.getTitle());
                l.setType(f.getType());
                l.setRent(f.getRent());
                l.setLocation(f.getLocation());
                l.setLandlordPhone(f.getLandlordPhone());
                l.setAmenities(f.getAmenities());
                l.setGenderPreference(f.getGenderPreference());
                l.setDescription(f.getDescription());
                l.setMapLink(f.getMapLink());
                l.setPostNumber(f.getPostNumber());
                l.setRejectionReason(f.getRejectionReason());
                l.setImageBase64List(f.getImageBase64List());
                displayListings.add(l);
            }
            adapter.setListings(displayListings);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
