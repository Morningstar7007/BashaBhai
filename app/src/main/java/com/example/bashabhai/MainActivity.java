package com.example.bashabhai;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.widget.Toast;

import com.example.bashabhai.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
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
            binding.fragmentContainer.setPadding(0, topPadding, 0, 0);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        setupAuthStateListener();

        // Initial fragment setup
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ListingsFragment())
                    .commit();
        }

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                selectedFragment = new ListingsFragment();
            } else if (itemId == R.id.nav_my_postings) {
                selectedFragment = new MyPostingsFragment();
            } else if (itemId == R.id.nav_favorites) {
                selectedFragment = new FavoritesFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
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
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User userProfile = snapshot.getValue(User.class);
                if (userProfile != null && userProfile.getRole() != null) {
                    currentUserRole = userProfile.getRole();
                } else if (user.getEmail() != null && AdminConfig.isAdmin(user.getEmail())) {
                    currentUserRole = "ADMIN";
                } else {
                    currentUserRole = "STUDENT";
                }
                updateNavMenu();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                updateNavMenu();
            }
        });
    }

    private void updateNavMenu() {
        Menu menu = binding.bottomNavigation.getMenu();
        MenuItem myPostingsItem = menu.findItem(R.id.nav_my_postings);
        
        if ("STUDENT".equalsIgnoreCase(currentUserRole)) {
            if (myPostingsItem != null) myPostingsItem.setVisible(false);
        } else {
            // LANDLORD or ADMIN
            if (myPostingsItem != null) myPostingsItem.setVisible(true);
        }
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
                    Exception exception = task.getException();
                    if (exception instanceof FirebaseAuthInvalidUserException) {
                        forceLogout("Account has been deleted or disabled.");
                    }
                    // If it is a Network exception, we just ignore it!
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
}
