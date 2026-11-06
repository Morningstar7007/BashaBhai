package com.example.bashabhai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bashabhai.databinding.ItemListingBinding;

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
            binding.tvTitle.setText(listing.getTitle());
            binding.tvLocation.setText(listing.getLocation());
            binding.tvType.setText(listing.getType());
            binding.tvGender.setText(listing.getGenderPreference());
            binding.tvRent.setText("৳" + listing.getRent());
            
            if (listing.isRented()) {
                binding.tvStatus.setVisibility(View.VISIBLE);
            } else {
                binding.tvStatus.setVisibility(View.GONE);
            }
        }
    }
}
