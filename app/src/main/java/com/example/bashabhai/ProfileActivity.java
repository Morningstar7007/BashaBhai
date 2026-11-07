package com.example.bashabhai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bashabhai.databinding.ActivityProfileBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            userRef = FirebaseDb.getRef("Users").child(userId);
            fetchUserData();
        } else {
            logout();
        }

        binding.btnAdminPanel.setOnClickListener(v -> 
            startActivity(new Intent(ProfileActivity.this, AdminActivity.class))
        );

        binding.btnLogout.setOnClickListener(v -> logout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkUserStatus();
    }

    private void checkUserStatus() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            logout();
        } else {
            currentUser.reload().addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Toast.makeText(ProfileActivity.this, "Account has been deleted or disabled.", Toast.LENGTH_LONG).show();
                    logout();
                }
            });
        }
    }

    private void fetchUserData() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                FirebaseUser currentUser = mAuth.getCurrentUser();
                
                String name = user != null && user.getName() != null ? user.getName() : 
                        (currentUser != null && currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User");
                String email = user != null && user.getEmail() != null ? user.getEmail() : 
                        (currentUser != null ? currentUser.getEmail() : "");
                String role = user != null && user.getRole() != null ? user.getRole() : "STUDENT";

                if (email != null && email.equalsIgnoreCase("admin@bashabhai.com")) {
                    role = "ADMIN";
                }

                binding.tvProfileName.setText(name);
                binding.tvProfileEmail.setText(email);
                binding.tvProfileRole.setText(role);

                if ("ADMIN".equalsIgnoreCase(role)) {
                    binding.btnAdminPanel.setVisibility(View.VISIBLE);
                } else {
                    binding.btnAdminPanel.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void logout() {
        mAuth.signOut();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
        googleSignInClient.signOut();
        
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
