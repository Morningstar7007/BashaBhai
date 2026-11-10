package com.example.bashabhai;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bashabhai.databinding.ItemListingBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ListingAdapter extends RecyclerView.Adapter<ListingAdapter.ListingViewHolder> {

    private List<Listing> listings = new ArrayList<>();
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Listing listing);
    }

    public ListingAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setListings(List<Listing> listings) {
        this.listings = listings;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ListingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemListingBinding binding = ItemListingBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ListingViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ListingViewHolder holder, int position) {
        Listing listing = listings.get(position);
        holder.bind(listing);
    }

    @Override
    public int getItemCount() {
        return listings.size();
    }

    class ListingViewHolder extends RecyclerView.ViewHolder {

        private final ItemListingBinding binding;

        public ListingViewHolder(ItemListingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(listings.get(position));
                }
            });
        }

        public void bind(Listing listing) {
            binding.tvPostNumber.setText(listing.getFormattedPostNumber());
            binding.tvTitle.setText(listing.getTitle());
            binding.tvLocation.setText(listing.getLocation());
            binding.tvType.setText(listing.getType());
            binding.tvGender.setText(listing.getGenderPreference());
            binding.tvRent.setText("৳" + listing.getRent());

            // Fetch Landlord Details (Name, Rating & Verification Badge)
            if (listing.getLandlordId() != null) {
                FirebaseDb.getRef("Users").child(listing.getLandlordId())
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            boolean isAdminUser = "ADMIN".equalsIgnoreCase(user.getRole()) || 
                                    (user.getEmail() != null && AdminConfig.isAdmin(user.getEmail()));

                            String name = isAdminUser ? "ADMIN" : 
                                    (user.getName() != null && !user.getName().isEmpty() ? user.getName() : "Landlord");

                            binding.tvLandlordName.setVisibility(View.VISIBLE);
                            binding.tvLandlordName.setText("Posted by: " + name);

                            // Setup Rating Badge
                            if (!isAdminUser && user.getTotalReviews() > 0) {
                                double avg = user.getRatingSum() / user.getTotalReviews();
                                binding.tvRating.setVisibility(View.VISIBLE);
                                binding.tvRating.setText(String.format("⭐ %.1f (%d)", avg, user.getTotalReviews()));
                            } else {
                                binding.tvRating.setVisibility(View.GONE);
                            }

                            // Admin posts are ALWAYS verified 🟢
                            if (isAdminUser || user.isVerified() || "APPROVED".equalsIgnoreCase(user.getVerificationStatus())) {
                                binding.tvVerifiedBadge.setVisibility(View.VISIBLE);
                            } else {
                                binding.tvVerifiedBadge.setVisibility(View.GONE);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            } else {
                binding.tvLandlordName.setVisibility(View.GONE);
                binding.tvVerifiedBadge.setVisibility(View.GONE);
                binding.tvRating.setVisibility(View.GONE);
            }

            if (listing.isRented() || "RENTED".equalsIgnoreCase(listing.getStatus())) {
                binding.tvStatus.setVisibility(View.VISIBLE);
                binding.tvStatus.setText("STATUS: RENTED");
                binding.tvStatus.setTextColor(Color.parseColor("#D32F2F"));
            } else if ("PENDING".equalsIgnoreCase(listing.getStatus())) {
                binding.tvStatus.setVisibility(View.VISIBLE);
                binding.tvStatus.setText("STATUS: PENDING ADMIN APPROVAL");
                binding.tvStatus.setTextColor(Color.parseColor("#FF9800"));
            } else if ("REJECTED".equalsIgnoreCase(listing.getStatus())) {
                binding.tvStatus.setVisibility(View.VISIBLE);
                
                String text = "STATUS: REJECTED";
                if (listing.getRejectionReason() != null && !listing.getRejectionReason().isEmpty()) {
                    text += "\nReason: " + listing.getRejectionReason();
                }
                
                binding.tvStatus.setText(text);
                binding.tvStatus.setTextColor(Color.parseColor("#D32F2F"));
            } else {
                binding.tvStatus.setVisibility(View.GONE);
            }
        }
    }
}
