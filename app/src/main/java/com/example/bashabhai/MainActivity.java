package com.example.bashabhai;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;

import com.example.bashabhai.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ListingAdapter adapter;
    private DatabaseReference databaseReference;
    private NetworkChangeReceiver networkReceiver;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private String currentUserRole = "STUDENT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            int topPadding = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            binding.recyclerView.setPadding(
                binding.recyclerView.getPaddingLeft(),
                topPadding,
                binding.recyclerView.getPaddingRight(),
                binding.recyclerView.getPaddingBottom()
            );
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        setupAuthStateListener();

        setupRecyclerView();

        databaseReference = FirebaseDb.getRef("Listings");

        binding.fabAddListing.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AddListingActivity.class));
        });

        // Setup Bottom Navigation
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_favorites) {
                startActivity(new Intent(MainActivity.this, FavoritesActivity.class));
                return false;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                return false;
            }
            return false;
        });
        
        fetchUserRoleAndConfigureUI();
        
        // Topic 10: Broadcast Receiver for network changes
        networkReceiver = new NetworkChangeReceiver();
        registerReceiver(networkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        
        // Topic 10: Service for checking new listings
        checkNotificationPermissionAndStartService();
    }
    
    private void fetchUserRoleAndConfigureUI() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        DatabaseReference userRef = FirebaseDb.getRef("Users").child(user.getUid());
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                User userProfile = snapshot.getValue(User.class);
                if (userProfile != null && userProfile.getRole() != null) {
                    currentUserRole = userProfile.getRole();
                } else if (user.getEmail() != null && user.getEmail().equalsIgnoreCase("admin@bashabhai.com")) {
                    currentUserRole = "ADMIN";
                } else {
                    currentUserRole = "STUDENT";
                }

                applyRoleBasedUI();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                applyRoleBasedUI();
            }
        });
    }

    private void applyRoleBasedUI() {
        if ("STUDENT".equalsIgnoreCase(currentUserRole)) {
            binding.fabAddListing.setVisibility(View.GONE);
        } else {
            // LANDLORD or ADMIN
            binding.fabAddListing.setVisibility(View.VISIBLE);
        }
        fetchListingsFromDatabase();
    }

    private void checkNotificationPermissionAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
                return;
            }
        }
        startService(new Intent(this, NewListingService.class));
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (mAuth != null && authStateListener != null) {
            mAuth.addAuthStateListener(authStateListener);
        }
        checkUserStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuth != null && authStateListener != null) {
            mAuth.removeAuthStateListener(authStateListener);
        }
    }

    private void setupAuthStateListener() {
        authStateListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user == null) {
                forceLogout(null);
            }
        };
    }

    private void checkUserStatus() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            forceLogout(null);
        } else {
            user.reload().addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    forceLogout("Account has been deleted or disabled.");
                }
            });
        }
    }

    private void forceLogout(String reason) {
        if (reason != null) {
            Toast.makeText(this, reason, Toast.LENGTH_LONG).show();
        }
        mAuth.signOut();
        Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkReceiver != null) {
            unregisterReceiver(networkReceiver);
        }
    }

    private void setupRecyclerView() {
        adapter = new ListingAdapter(listing -> {
            Intent intent = new Intent(MainActivity.this, ListingDetailActivity.class);
            intent.putExtra("LISTING", listing);
            startActivity(intent);
        });
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
    }
    
    private void fetchListingsFromDatabase() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Listing> fetchedListings = new ArrayList<>();
                FirebaseUser currentUser = mAuth.getCurrentUser();
                String currentUid = currentUser != null ? currentUser.getUid() : "";

                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Listing listing = postSnapshot.getValue(Listing.class);
                    if (listing != null) {
                        String status = listing.getStatus() != null ? listing.getStatus() : "APPROVED";
                        
                        if ("ADMIN".equalsIgnoreCase(currentUserRole)) {
                            // Admin sees all listings
                            fetchedListings.add(listing);
                        } else if ("LANDLORD".equalsIgnoreCase(currentUserRole)) {
                            // Landlord sees approved listings + their own pending/rented listings
                            if ("APPROVED".equalsIgnoreCase(status) || currentUid.equals(listing.getLandlordId())) {
                                fetchedListings.add(listing);
                            }
                        } else {
                            // Student ONLY sees APPROVED listings
                            if ("APPROVED".equalsIgnoreCase(status)) {
                                fetchedListings.add(listing);
                            }
                        }
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
