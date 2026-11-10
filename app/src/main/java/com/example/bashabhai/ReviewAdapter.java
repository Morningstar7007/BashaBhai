package com.example.bashabhai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bashabhai.databinding.ItemReviewBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private List<Review> reviews = new ArrayList<>();
    private final boolean isAdmin;
    private final OnReviewDeleteListener deleteListener;

    public interface OnReviewDeleteListener {
        void onDeleteClick(Review review);
    }

    public ReviewAdapter(boolean isAdmin, OnReviewDeleteListener deleteListener) {
        this.isAdmin = isAdmin;
        this.deleteListener = deleteListener;
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReviewBinding binding = ItemReviewBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ReviewViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviews.get(position);
        holder.bind(review);
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    class ReviewViewHolder extends RecyclerView.ViewHolder {

        private final ItemReviewBinding binding;

        public ReviewViewHolder(ItemReviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Review review) {
            binding.tvReviewerName.setText(review.getStudentName());
            binding.tvReviewerInfo.setText(review.getStudentInfo());
            binding.tvReviewRating.setText("⭐ " + review.getRating());
            
            if (review.getReviewText() != null && !review.getReviewText().trim().isEmpty()) {
                binding.tvReviewText.setVisibility(View.VISIBLE);
                binding.tvReviewText.setText(review.getReviewText());
            } else {
                binding.tvReviewText.setVisibility(View.GONE);
            }
            
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            binding.tvReviewDate.setText(sdf.format(new Date(review.getTimestamp())));
            
            if (isAdmin) {
                binding.btnDeleteReview.setVisibility(View.VISIBLE);
                binding.btnDeleteReview.setOnClickListener(v -> {
                    if (deleteListener != null) {
                        deleteListener.onDeleteClick(review);
                    }
                });
            } else {
                binding.btnDeleteReview.setVisibility(View.GONE);
            }
        }
    }
}
