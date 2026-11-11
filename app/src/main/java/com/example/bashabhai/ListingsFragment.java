package com.example.bashabhai;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.bashabhai.databinding.FragmentListingsBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ListingsFragment extends Fragment {

    private FragmentListingsBinding binding;
    private ListingAdapter adapter;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;

    private AlertDialog selectionDialog;
    
    // Master list holds all data fetched from Firebase based on User Role
    private final List<Listing> masterListingList = new ArrayList<>();
    
    private long currentAnnouncementTimestamp = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentListingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDb.getRef("Listings");

        setupRecyclerView();
        setupSearchAndFilters();
        setupSwipeToRefresh();
        setupAnnouncement();

        binding.btnCreatePost.setOnClickListener(v -> 
            startActivity(new Intent(requireActivity(), AddListingActivity.class))
        );
        
        binding.btnBroadcastMessage.setOnClickListener(v -> promptBroadcastMessage());

        fetchUserRoleAndListings();
    }

    private void promptBroadcastMessage() {
        if (getContext() == null) return;
        
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_broadcast_selection, null);
        View cvOptionBanner = dialogView.findViewById(R.id.cvOptionBanner);
        View cvOptionPush = dialogView.findViewById(R.id.cvOptionPush);
        
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext())
                .setView(dialogView)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
                
        selectionDialog = builder.create();
        
        cvOptionBanner.setOnClickListener(v -> {
            if (selectionDialog != null && selectionDialog.isShowing()) selectionDialog.dismiss();
            promptBannerBroadcast();
        });
        
        cvOptionPush.setOnClickListener(v -> {
            if (selectionDialog != null && selectionDialog.isShowing()) selectionDialog.dismiss();
            promptPushNotificationBroadcast();
        });
        
        selectionDialog.show();
    }
    
    private void promptPushNotificationBroadcast() {
        if (getContext() == null) return;
        
        EditText input = new EditText(getContext());
        input.setHint("Enter push notification message...");
        input.setLines(4);
        input.setMaxLines(6);
        
        FrameLayout container = new FrameLayout(getContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 50;
        params.rightMargin = 50;
        input.setLayoutParams(params);
        container.addView(input);

        new MaterialAlertDialogBuilder(getContext())
                .setTitle("🔔 Send Global Push")
                .setMessage("This will send a direct phone notification to every registered user (except you).")
                .setView(container)
                .setPositiveButton("Send", (dialog, which) -> {
                    if (!NetworkUtils.isNetworkAvailable(getContext())) {
                        Toast.makeText(getContext(), "No internet connection.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String message = input.getText().toString().trim();
                    if (!message.isEmpty()) {
                        executeGlobalPushBroadcast(message);
                    } else {
                        Toast.makeText(getContext(), "Message cannot be empty!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void executeGlobalPushBroadcast(String message) {
        if (getContext() == null) return;
        
        String myUid = mAuth.getUid();
        if (myUid == null) return;
        
        Toast.makeText(getContext(), "Sending notifications...", Toast.LENGTH_SHORT).show();
        
        DatabaseReference usersRef = FirebaseDb.getRef("Users");
        DatabaseReference notificationsRef = FirebaseDb.getRef("Notifications");
        
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int sentCount = 0;
                long timestamp = System.currentTimeMillis();
                
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String targetUid = userSnapshot.getKey();
                    
                    // Do not send the notification to the admin themselves
                    if (targetUid != null && !targetUid.equals(myUid)) {
                        String notifId = notificationsRef.child(targetUid).push().getKey();
                        if (notifId != null) {
                            AppNotification notif = new AppNotification(
                                    notifId, 
                                    "📢 Admin Announcement", 
                                    message, 
                                    "SYSTEM", 
                                    timestamp
                            );
                            notificationsRef.child(targetUid).child(notifId).setValue(notif);
                            sentCount++;
                        }
                    }
                }
                
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Successfully notified " + sentCount + " users!", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to fetch users for broadcast.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void promptBannerBroadcast() {
        if (getContext() == null) return;
        
        EditText input = new EditText(getContext());
        input.setHint("Enter banner message...");
        input.setLines(4);
        input.setMaxLines(6);
        
        FrameLayout container = new FrameLayout(getContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 50;
        params.rightMargin = 50;
        input.setLayoutParams(params);
        container.addView(input);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext())
                .setTitle("📢 In-App Banner")
                .setMessage(currentAnnouncementTimestamp > 0 ? "Send a global banner to all users, or clear the current one:" : "Send a global banner to all users:")
                .setView(container)
                .setPositiveButton("Send New", (dialog, which) -> {
                    if (!NetworkUtils.isNetworkAvailable(getContext())) {
                        Toast.makeText(getContext(), "No internet connection.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String message = input.getText().toString().trim();
                    if (!message.isEmpty()) {
                        DatabaseReference ref = FirebaseDb.getRef("System").child("latestAnnouncement");
                        ref.child("message").setValue(message);
                        ref.child("timestamp").setValue(System.currentTimeMillis()).addOnCompleteListener(task -> {
                            if (task.isSuccessful() && getContext() != null) {
                                Toast.makeText(getContext(), "Banner broadcasted!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Message cannot be empty!", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null);
                
        // Only show "Stop Current" if there is an active broadcast running in the database
        if (currentAnnouncementTimestamp > 0) {
            builder.setNeutralButton("Stop Current", (dialog, which) -> {
                if (!NetworkUtils.isNetworkAvailable(getContext())) {
                    Toast.makeText(getContext(), "No internet connection.", Toast.LENGTH_SHORT).show();
                    return;
                }
                DatabaseReference ref = FirebaseDb.getRef("System").child("latestAnnouncement");
                ref.removeValue().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && getContext() != null) {
                        Toast.makeText(getContext(), "Banner stopped.", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }
        
        builder.show();
    }

    private void setupAnnouncement() {
        binding.btnCloseAnnouncement.setOnClickListener(v -> {
            binding.cvAnnouncement.setVisibility(View.GONE);
            // Save the dismissed state tied specifically to THIS user's UID
            // This prevents a dismissal on one device from hiding it if another user logs into the same device
            if (getContext() != null && mAuth.getCurrentUser() != null) {
                String prefsKey = "dismissed_announcement_" + mAuth.getCurrentUser().getUid();
                getContext().getSharedPreferences("BashaBhaiPrefs", Context.MODE_PRIVATE)
                        .edit()
                        .putLong(prefsKey, currentAnnouncementTimestamp)
                        .apply();
            }
        });

        DatabaseReference sysRef = FirebaseDb.getRef("System").child("latestAnnouncement");
        sysRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;
                
                if (!snapshot.exists()) {
                    binding.cvAnnouncement.setVisibility(View.GONE);
                    currentAnnouncementTimestamp = 0; // Reset state if admin deleted it
                    return;
                }
                
                String message = snapshot.child("message").getValue(String.class);
                Long timestamp = snapshot.child("timestamp").getValue(Long.class);
                
                if (message != null && !message.isEmpty() && timestamp != null) {
                    currentAnnouncementTimestamp = timestamp;
                    
                    long dismissedTimestamp = 0;
                    if (getContext() != null && mAuth.getCurrentUser() != null) {
                        String prefsKey = "dismissed_announcement_" + mAuth.getCurrentUser().getUid();
                        dismissedTimestamp = getContext().getSharedPreferences("BashaBhaiPrefs", Context.MODE_PRIVATE)
                                .getLong(prefsKey, 0);
                    }
                    
                    if (timestamp > dismissedTimestamp) {
                        binding.tvAnnouncementMessage.setText(message);
                        binding.cvAnnouncement.setVisibility(View.VISIBLE);
                    } else {
                        binding.cvAnnouncement.setVisibility(View.GONE);
                    }
                } else {
                    binding.cvAnnouncement.setVisibility(View.GONE);
                    currentAnnouncementTimestamp = 0;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new ListingAdapter(listing -> {
            Intent intent = new Intent(requireActivity(), ListingDetailActivity.class);
            intent.putExtra("LISTING", listing);
            startActivity(intent);
        });
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);
    }
    
    private void setupSwipeToRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        );
        
        binding.swipeRefreshLayout.setOnRefreshListener(this::fetchListingsFromDatabase);
    }
    
    private void setupSearchAndFilters() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                applyFilters();
            }
        });

        binding.chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> applyFilters());
    }

    private void fetchUserRoleAndListings() {
        // Prevent programmatic flicker on initial launch since RTDB is cached
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            if (binding != null && binding.swipeRefreshLayout != null) {
                binding.swipeRefreshLayout.setRefreshing(false);
            }
            return;
        }

        DatabaseReference userRef = FirebaseDb.getRef("Users").child(user.getUid());
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User userProfile = snapshot.getValue(User.class);
                String currentUserRole = "STUDENT";
                if (userProfile != null && userProfile.getRole() != null) {
                    currentUserRole = userProfile.getRole();
                } 
                if (user.getEmail() != null && AdminConfig.isAdmin(user.getEmail())) {
                    currentUserRole = "ADMIN";
                }

                if (binding != null) {
                    if ("LANDLORD".equalsIgnoreCase(currentUserRole)) {
                        binding.btnCreatePost.setVisibility(View.VISIBLE);
                        binding.btnBroadcastMessage.setVisibility(View.GONE);
                    } else if ("ADMIN".equalsIgnoreCase(currentUserRole)) {
                        binding.btnCreatePost.setVisibility(View.VISIBLE);
                        binding.btnBroadcastMessage.setVisibility(View.VISIBLE);
                    } else {
                        binding.btnCreatePost.setVisibility(View.GONE);
                        binding.btnBroadcastMessage.setVisibility(View.GONE);
                    }
                }

                fetchListingsFromDatabase();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                fetchListingsFromDatabase();
            }
        });
    }

    private void fetchListingsFromDatabase() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                masterListingList.clear();

                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Listing listing = postSnapshot.getValue(Listing.class);
                    if (listing != null) {
                        String status = listing.getStatus() != null ? listing.getStatus() : "APPROVED";

                        // Global public feed ONLY displays APPROVED listings for all roles (Students, Landlords, and Admins)
                        if ("APPROVED".equalsIgnoreCase(status)) {
                            masterListingList.add(listing);
                        }
                    }
                }
                
                if (binding != null && binding.swipeRefreshLayout != null) {
                    binding.swipeRefreshLayout.setRefreshing(false);
                }
                
                applyFilters();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (binding != null && binding.swipeRefreshLayout != null) {
                    binding.swipeRefreshLayout.setRefreshing(false);
                }
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load listings.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    private void applyFilters() {
        if (adapter == null || binding == null) return;
        
        String query = binding.etSearch.getText() != null ? binding.etSearch.getText().toString().toLowerCase().trim() : "";
        int checkedChipId = binding.chipGroupFilter.getCheckedChipId();
        
        List<Listing> filteredList = new ArrayList<>();
        
        for (Listing listing : masterListingList) {
            boolean matchesSearch = false;
            boolean matchesChip = false;
            
            // Check Search Query (Title, Location, or Post Number)
            if (query.isEmpty()) {
                matchesSearch = true;
            } else {
                String title = listing.getTitle() != null ? listing.getTitle().toLowerCase() : "";
                String location = listing.getLocation() != null ? listing.getLocation().toLowerCase() : "";
                String postNumStr = String.valueOf(listing.getPostNumber());
                String formattedPostNum = listing.getFormattedPostNumber().toLowerCase();

                if (title.contains(query) || location.contains(query) || postNumStr.contains(query) || formattedPostNum.contains(query)) {
                    matchesSearch = true;
                }
            }
            
            // Check Chip Filter
            if (checkedChipId == binding.chipAll.getId() || checkedChipId == View.NO_ID) {
                matchesChip = true;
            } else if (checkedChipId == binding.chipRoom.getId()) {
                if (listing.getType() != null && listing.getType().toLowerCase().contains("room")) {
                    matchesChip = true;
                }
            } else if (checkedChipId == binding.chipFlat.getId()) {
                if (listing.getType() != null && listing.getType().toLowerCase().contains("flat")) {
                    matchesChip = true;
                }
            } else if (checkedChipId == binding.chipMess.getId()) {
                if (listing.getType() != null && listing.getType().toLowerCase().contains("mess")) {
                    matchesChip = true;
                }
            } else if (checkedChipId == binding.chipMale.getId()) {
                if (listing.getGenderPreference() != null && listing.getGenderPreference().toLowerCase().contains("male")) {
                    // Make sure it doesn't accidentally catch "female" which contains "male"
                    if (!listing.getGenderPreference().toLowerCase().contains("female")) {
                        matchesChip = true;
                    }
                }
            } else if (checkedChipId == binding.chipFemale.getId()) {
                if (listing.getGenderPreference() != null && listing.getGenderPreference().toLowerCase().contains("female")) {
                    matchesChip = true;
                }
            }
            
            if (matchesSearch && matchesChip) {
                filteredList.add(listing);
            }
        }
        
        adapter.setListings(filteredList);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
