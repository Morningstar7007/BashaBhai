package com.example.bashabhai;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bashabhai.databinding.ActivityWelcomeBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class WelcomeActivity extends AppCompatActivity {

    private ActivityWelcomeBinding binding;
    private FirebaseAuth mAuth;
    
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            int imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), Math.max(imeHeight, systemBars));
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            if (mAuth.getCurrentUser().isEmailVerified()) {
                navigateToMain();
                return;
            } else {
                mAuth.signOut();
            }
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            if (account != null) {
                                firebaseAuthWithGoogle(account);
                            }
                        } catch (ApiException e) {
                            showMessage("Google sign in failed: " + e.getMessage(), true);
                        }
                    }
                });

        binding.btnGetStarted.setOnClickListener(v -> 
            startActivity(new Intent(WelcomeActivity.this, RegisterActivity.class))
        );
        
        binding.tvSignInLabel.setOnClickListener(v -> 
            startActivity(new Intent(WelcomeActivity.this, LoginActivity.class))
        );

        binding.btnGoogleSignInWelcome.setOnClickListener(v -> {
            mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            });
        });
    }

    private void showMessage(String text, boolean isError) {
        binding.tvMessage.setVisibility(View.VISIBLE);
        binding.tvMessage.setText(text);
        if (isError) {
            binding.tvMessage.setTextColor(Color.parseColor("#D32F2F"));
        } else {
            binding.tvMessage.setTextColor(Color.parseColor("#388E3C"));
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid();
                            DatabaseReference userRef = FirebaseDb.getRef("Users").child(userId);
                            
                            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snapshot) {
                                    if (snapshot.exists() && snapshot.child("role").getValue() != null) {
                                        // Existing user with role set
                                        navigateToMain();
                                    } else {
                                        // New user via Google Sign-In
                                        if (acct.getEmail() != null && acct.getEmail().equalsIgnoreCase("admin@bashabhai.com")) {
                                            User adminUser = new User(userId, acct.getDisplayName(), acct.getEmail(), "ADMIN");
                                            userRef.setValue(adminUser);
                                            navigateToMain();
                                        } else {
                                            promptRoleSelectionForGoogleUser(userId, acct.getDisplayName(), acct.getEmail());
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError error) {
                                    navigateToMain();
                                }
                            });
                        } else {
                            navigateToMain();
                        }
                    } else {
                        showMessage("Authentication Failed.", true);
                    }
                });
    }

    private void promptRoleSelectionForGoogleUser(String userId, String name, String email) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_google_role, null);
        RadioButton rbLandlord = dialogView.findViewById(R.id.rbDialogLandlord);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Welcome to BashaBhai!")
                .setMessage("Please select your account role to get started:")
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("Continue", (dialog, which) -> {
                    String role = (rbLandlord != null && rbLandlord.isChecked()) ? "LANDLORD" : "STUDENT";
                    DatabaseReference userRef = FirebaseDb.getRef("Users").child(userId);
                    User newUser = new User(userId, name, email, role);
                    userRef.setValue(newUser);
                    navigateToMain();
                })
                .show();
    }

    private void navigateToMain() {
        Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
