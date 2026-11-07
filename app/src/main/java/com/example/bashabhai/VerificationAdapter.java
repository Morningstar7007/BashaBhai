package com.example.bashabhai;

import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bashabhai.databinding.ItemVerificationRequestBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class VerificationAdapter extends RecyclerView.Adapter<VerificationAdapter.VerificationViewHolder> {

    private List<VerificationRequest> requests = new ArrayList<>();
    private final OnActionListener listener;

    public interface OnActionListener {
        void onApprove(VerificationRequest request);
        void onReject(VerificationRequest request);
    }

    public VerificationAdapter(OnActionListener listener) {
        this.listener = listener;
    }

    public void setRequests(List<VerificationRequest> requests) {
        this.requests = requests;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VerificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemVerificationRequestBinding binding = ItemVerificationRequestBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VerificationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VerificationViewHolder holder, int position) {
        VerificationRequest req = requests.get(position);
        holder.bind(req);
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    class VerificationViewHolder extends RecyclerView.ViewHolder {

        private final ItemVerificationRequestBinding binding;

        public VerificationViewHolder(ItemVerificationRequestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(VerificationRequest req) {
            // Fetch User details to show Name and Email
            FirebaseDb.getRef("Users").child(req.getUserId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        binding.tvAdminLandlordName.setText(user.getName() != null ? user.getName() : "Landlord");
                        binding.tvAdminLandlordEmail.setText(user.getEmail() != null ? user.getEmail() : "");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });

            // Load NID Photo (Base64 or URL)
            displayImage(req.getNidBase64(), req.getNidPhotoUrl(), binding.ivAdminNidPhoto, null);

            // Load Bill Photo if available
            if ((req.getBillBase64() != null && !req.getBillBase64().isEmpty()) || 
                (req.getBillPhotoUrl() != null && !req.getBillPhotoUrl().isEmpty())) {
                displayImage(req.getBillBase64(), req.getBillPhotoUrl(), binding.ivAdminBillPhoto, binding.tvAdminBillLabel);
            } else {
                binding.tvAdminBillLabel.setVisibility(View.GONE);
                binding.ivAdminBillPhoto.setVisibility(View.GONE);
            }

            binding.btnApproveVerification.setOnClickListener(v -> {
                if (listener != null) listener.onApprove(req);
            });

            binding.btnRejectVerification.setOnClickListener(v -> {
                if (listener != null) listener.onReject(req);
            });
        }

        private void displayImage(String base64Str, String urlStr, ImageView imageView, View labelView) {
            if (base64Str != null && !base64Str.isEmpty()) {
                try {
                    byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
                    imageView.setVisibility(View.VISIBLE);
                    if (labelView != null) labelView.setVisibility(View.VISIBLE);
                    Glide.with(itemView.getContext())
                            .load(decodedBytes)
                            .into(imageView);
                } catch (Exception e) {
                    imageView.setVisibility(View.GONE);
                    if (labelView != null) labelView.setVisibility(View.GONE);
                }
            } else if (urlStr != null && !urlStr.isEmpty()) {
                imageView.setVisibility(View.VISIBLE);
                if (labelView != null) labelView.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext())
                        .load(urlStr)
                        .into(imageView);
            } else {
                imageView.setVisibility(View.GONE);
                if (labelView != null) labelView.setVisibility(View.GONE);
            }
        }
    }
}
