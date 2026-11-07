package com.example.bashabhai;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.RadioButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bashabhai.databinding.ActivityRegisterBinding;
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

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth mAuth;

    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    private boolean isSubmitting = false;
    private Handler verificationHandler = new Handler(Looper.getMainLooper());
    private Runnable verificationRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            int imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), Math.max(imeHeight, systemBars));
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

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
                            } else {
                                setLoadingState(false, null);
                            }
                        } catch (ApiException e) {
                            setLoadingState(false, null);
                            showMessage("Google sign in failed: " + e.getMessage(), true);
                        }
                    } else {
                        setLoadingState(false, null);
                    }
                });

        setupTextWatchers();

        binding.btnRegister.setOnClickListener(v -> {
            if (!isSubmitting) {
                handleRegister();
            }
        });

        binding.tvLoginLabel.setOnClickListener(v -> {
            stopVerificationCheck();
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });

        binding.btnGoogleSignUp.setOnClickListener(v -> {
            if (!isSubmitting) {
                setLoadingState(true, "Connecting...");
                mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                    Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                    googleSignInLauncher.launch(signInIntent);
                });
            }
        });
    }

    private void setupTextWatchers() {
        binding.etName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tilName.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.etEmail.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tilEmail.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tilPassword.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void handleRegister() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        boolean isValid = true;

        if (name.isEmpty()) {
            binding.tilName.setError("Full Name is required");
            isValid = false;
        }

        if (email.isEmpty()) {
            binding.tilEmail.setError("Email is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Please enter a valid email address");
            isValid = false;
        }

        if (password.isEmpty()) {
            binding.tilPassword.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            binding.tilPassword.setError("Password should be at least 6 characters");
            isValid = false;
        }

        if (!isValid) return;

        // Determine user role
        String role = "STUDENT";
        if (email.equalsIgnoreCase("admin@bashabhai.com")) {
            role = "ADMIN";
        } else if (binding.rbLandlord.isChecked()) {
            role = "LANDLORD";
        }

        final String userRole = role;

        setLoadingState(true, "Creating Account...");

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid();
                            User newUser = new User(userId, name, email, userRole);
                            DatabaseReference usersRef = FirebaseDb.getRef("Users");
                            usersRef.child(userId).setValue(newUser);

                            // Send verification email
                            firebaseUser.sendEmailVerification().addOnCompleteListener(emailTask -> {
                                if (emailTask.isSuccessful()) {
                                    showMessage("Verification email sent! Checking for verification in background...", false);
                                    startEmailVerificationCheck(firebaseUser);
                                } else {
                                    setLoadingState(false, null);
                                    showMessage("Failed to send verification email.", true);
                                }
                            });
                        }
                    } else {
                        setLoadingState(false, null);
                        String err = task.getException() != null ? task.getException().getMessage() : "Registration failed.";
                        showMessage(err, true);
                    }
                });
    }

    private void startEmailVerificationCheck(FirebaseUser firebaseUser) {
        verificationRunnable = new Runnable() {
            @Override
            public void run() {
                if (firebaseUser == null) return;
                firebaseUser.reload().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && firebaseUser.isEmailVerified()) {
                        stopVerificationCheck();
                        showMessage("Email verified successfully! Redirecting to Sign In...", false);
                        mAuth.signOut();
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                        }, 1500);
                    } else {
                        verificationHandler.postDelayed(this, 3000);
                    }
                });
            }
        };
        verificationHandler.postDelayed(verificationRunnable, 3000);
    }

    private void stopVerificationCheck() {
        if (verificationHandler != null && verificationRunnable != null) {
            verificationHandler.removeCallbacks(verificationRunnable);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopVerificationCheck();
    }

    private void setLoadingState(boolean isLoading, String text) {
        isSubmitting = isLoading;
        if (isLoading) {
            binding.btnRegister.setText(text != null ? text : "Creating Account...");
        } else {
            binding.btnRegister.setText("Create Account");
        }
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
                                        // Existing user with role
                                        navigateToMain();
                                    } else {
                                        // New Google user
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
                        setLoadingState(false, null);
                        showMessage("Authentication Failed.", true);
                    }
                });
    }

    private void promptRoleSelectionForGoogleUser(String userId, String name, String email) {
        setLoadingState(false, null);

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
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
