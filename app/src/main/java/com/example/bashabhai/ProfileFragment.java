package com.example.bashabhai;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.bashabhai.databinding.FragmentProfileBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private User currentUserProfile;

    // Variables for Verification Photo Uploads
    private Uri nidImageUri;
    private Uri billImageUri;
    private ImageView currentImageViewTarget;
    private View currentPlaceholderTarget;
    private AlertDialog verificationDialog;
    private LinearLayout llUploadProgress;
    
    // Photo Picker Launcher for Verification
    private final ActivityResultLauncher<Intent> photoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedImage = result.getData().getData();
                    if (selectedImage != null && currentImageViewTarget != null) {
                        currentImageViewTarget.setVisibility(View.VISIBLE);
                        currentPlaceholderTarget.setVisibility(View.GONE);
                        Glide.with(this).load(selectedImage).into(currentImageViewTarget);
                        
                        if (currentImageViewTarget.getId() == R.id.ivNidPreview) {
                            nidImageUri = selectedImage;
                        } else {
                            billImageUri = selectedImage;
                        }
                    }
                }
            }
    );

    // Photo Picker Launcher for Profile Picture
    private Uri profilePicUri;
    private final ActivityResultLauncher<Intent> profilePicPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedImage = result.getData().getData();
                    if (selectedImage != null) {
                        profilePicUri = selectedImage;
                        uploadProfilePicToFirebase();
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            userRef = FirebaseDb.getRef("Users").child(userId);
            fetchUserData();
            checkAuthProvider(currentUser);
        } else {
            logout();
        }

        binding.flProfilePicContainer.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            profilePicPickerLauncher.launch(intent);
        });

        binding.btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        binding.btnUpdatePassword.setOnClickListener(v -> showUpdatePasswordDialog());
        binding.btnRequestVerification.setOnClickListener(v -> showVerificationRequestDialog());

        binding.btnAdminPanel.setOnClickListener(v -> 
            startActivity(new Intent(requireActivity(), AdminActivity.class))
        );

        binding.btnLogout.setOnClickListener(v -> logout());
    }
    
    private void uploadProfilePicToFirebase() {
        if (profilePicUri == null || userRef == null) return;
        if (getContext() != null) {
            Toast.makeText(getContext(), "Updating profile photo...", Toast.LENGTH_SHORT).show();
        }
        
        new Thread(() -> {
            String base64Image = convertUriToBase64(profilePicUri);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (base64Image != null) {
                        userRef.child("profilePicBase64").setValue(base64Image).addOnCompleteListener(task -> {
                            if (task.isSuccessful() && getContext() != null) {
                                Toast.makeText(getContext(), "Profile photo updated!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        }).start();
    }
    
    private void checkAuthProvider(FirebaseUser user) {
        boolean isGoogleUser = false;
        for (UserInfo userInfo : user.getProviderData()) {
            if (userInfo.getProviderId().equals("google.com")) {
                isGoogleUser = true;
                break;
            }
        }
        if (isGoogleUser) {
            binding.btnUpdatePassword.setVisibility(View.GONE);
        } else {
            binding.btnUpdatePassword.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkUserStatus();
    }

    private void checkUserStatus() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            logout();
        } else {
            currentUser.reload().addOnCompleteListener(task -> {
                if (!task.isSuccessful() && getContext() != null) {
                    Toast.makeText(getContext(), "Account has been deleted or disabled.", Toast.LENGTH_LONG).show();
                    logout();
                }
            });
        }
    }

    private void fetchUserData() {
        if (userRef == null) return;
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;
                
                currentUserProfile = snapshot.getValue(User.class);
                FirebaseUser currentUser = mAuth.getCurrentUser();
                
                String name = currentUserProfile != null && currentUserProfile.getName() != null ? currentUserProfile.getName() : 
                        (currentUser != null && currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User");
                String email = currentUserProfile != null && currentUserProfile.getEmail() != null ? currentUserProfile.getEmail() : 
                        (currentUser != null && currentUser.getEmail() != null ? currentUser.getEmail() : "");
                String role = currentUserProfile != null && currentUserProfile.getRole() != null ? currentUserProfile.getRole() : "STUDENT";
                String vStatus = currentUserProfile != null && currentUserProfile.getVerificationStatus() != null ? currentUserProfile.getVerificationStatus() : "NONE";

                if (AdminConfig.isAdmin(email)) {
                    role = "ADMIN";
                }

                binding.tvProfileName.setText(name);
                binding.tvProfileEmail.setText(email);
                binding.tvProfileRole.setText(role);

                // Load Profile Picture
                String picBase64 = currentUserProfile != null ? currentUserProfile.getProfilePicBase64() : null;
                if (picBase64 != null && !picBase64.isEmpty()) {
                    try {
                        byte[] decodedBytes = Base64.decode(picBase64, Base64.DEFAULT);
                        Glide.with(ProfileFragment.this).load(decodedBytes).into(binding.ivProfilePic);
                    } catch (Exception e) {
                        binding.ivProfilePic.setImageResource(R.drawable.bashabhai_logo);
                    }
                } else {
                    binding.ivProfilePic.setImageResource(R.drawable.bashabhai_logo);
                }

                if ("ADMIN".equalsIgnoreCase(role)) {
                    binding.btnAdminPanel.setVisibility(View.VISIBLE);
                    binding.btnRequestVerification.setVisibility(View.GONE);
                    binding.tvVerificationBadge.setVisibility(View.GONE);
                    binding.llLandlordDetails.setVisibility(View.VISIBLE);

                    String phone = (currentUserProfile != null && currentUserProfile.getDefaultPhone() != null && !currentUserProfile.getDefaultPhone().isEmpty()) ?
                            currentUserProfile.getDefaultPhone() : "Not Set";
                            
                    binding.tvProfilePhone.setText(phone);
                } else if ("LANDLORD".equalsIgnoreCase(role)) {
                    binding.btnAdminPanel.setVisibility(View.GONE);
                    binding.tvVerificationBadge.setVisibility(View.VISIBLE);
                    binding.llLandlordDetails.setVisibility(View.VISIBLE);
                    
                    String phone = (currentUserProfile != null && currentUserProfile.getDefaultPhone() != null && !currentUserProfile.getDefaultPhone().isEmpty()) ?
                            currentUserProfile.getDefaultPhone() : "Not Set";
                            
                    binding.tvProfilePhone.setText(phone);
                    
                    if ("APPROVED".equalsIgnoreCase(vStatus) || (currentUserProfile != null && currentUserProfile.isVerified())) {
                        binding.tvVerificationBadge.setText("Verified 🟢");
                        binding.tvVerificationBadge.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                        binding.btnRequestVerification.setVisibility(View.GONE);
                    } else if ("PENDING".equalsIgnoreCase(vStatus)) {
                        binding.tvVerificationBadge.setText("Pending 🟡");
                        binding.tvVerificationBadge.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                        binding.btnRequestVerification.setVisibility(View.GONE);
                    } else {
                        binding.tvVerificationBadge.setText("Unverified 🔴");
                        binding.tvVerificationBadge.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        binding.btnRequestVerification.setVisibility(View.VISIBLE);
                    }
                } else {
                    binding.btnAdminPanel.setVisibility(View.GONE);
                    binding.btnRequestVerification.setVisibility(View.GONE);
                    binding.tvVerificationBadge.setVisibility(View.GONE);
                    binding.llLandlordDetails.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    private void showVerificationRequestDialog() {
        if (getContext() == null) return;
        
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_verification, null);
        View cvNidPhoto = dialogView.findViewById(R.id.cvNidPhoto);
        View cvBillPhoto = dialogView.findViewById(R.id.cvBillPhoto);
        
        ImageView ivNidPreview = dialogView.findViewById(R.id.ivNidPreview);
        View llNidPlaceholder = dialogView.findViewById(R.id.llNidPlaceholder);
        
        ImageView ivBillPreview = dialogView.findViewById(R.id.ivBillPreview);
        View llBillPlaceholder = dialogView.findViewById(R.id.llBillPlaceholder);
        
        llUploadProgress = dialogView.findViewById(R.id.llUploadProgress);
        
        nidImageUri = null;
        billImageUri = null;

        cvNidPhoto.setOnClickListener(v -> {
            currentImageViewTarget = ivNidPreview;
            currentPlaceholderTarget = llNidPlaceholder;
            openPhotoPicker();
        });
        
        cvBillPhoto.setOnClickListener(v -> {
            currentImageViewTarget = ivBillPreview;
            currentPlaceholderTarget = llBillPlaceholder;
            openPhotoPicker();
        });

        verificationDialog = new MaterialAlertDialogBuilder(getContext())
                .setTitle("Submit Verification Photos")
                .setMessage("Please provide clear photos of your real-world documents.")
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("Upload & Submit", null)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create();
                
        verificationDialog.setOnShowListener(dialog -> {
            verificationDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (nidImageUri == null) {
                    Toast.makeText(getContext(), "NID Photo is required!", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                verificationDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                verificationDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
                llUploadProgress.setVisibility(View.VISIBLE);
                
                uploadPhotosToFirebase();
            });
        });
        
        verificationDialog.show();
    }
    
    private void openPhotoPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        photoPickerLauncher.launch(intent);
    }
    
    private String convertUriToBase64(Uri uri) {
        if (getContext() == null) return null;
        try {
            InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();
            
            if (bitmap == null) return null;

            int maxSize = 800;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float ratio = (float) width / (float) height;
            if (ratio > 1) {
                width = maxSize;
                height = (int) (width / ratio);
            } else {
                height = maxSize;
                width = (int) (height * ratio);
            }
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
            byte[] imageBytes = baos.toByteArray();
            return Base64.encodeToString(imageBytes, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private void uploadPhotosToFirebase() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || nidImageUri == null) return;
        
        String uid = currentUser.getUid();
        
        new Thread(() -> {
            String nidBase64 = convertUriToBase64(nidImageUri);
            String billBase64 = billImageUri != null ? convertUriToBase64(billImageUri) : null;

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (nidBase64 == null) {
                        handleUploadFailure("Failed to process NID image.");
                        return;
                    }

                    DatabaseReference reqRef = FirebaseDb.getRef("VerificationRequests").child(uid);
                    reqRef.child("nidBase64").setValue(nidBase64);
                    if (billBase64 != null) {
                        reqRef.child("billBase64").setValue(billBase64);
                    }

                    userRef.child("verificationStatus").setValue("PENDING").addOnCompleteListener(task -> {
                        if (verificationDialog != null && verificationDialog.isShowing()) {
                            verificationDialog.dismiss();
                        }
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Verification documents submitted successfully!", Toast.LENGTH_LONG).show();
                        }
                    });
                });
            }
        }).start();
    }
    
    private void handleUploadFailure(String error) {
        if (getContext() != null) {
            Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
        }
        if (verificationDialog != null && verificationDialog.isShowing()) {
            verificationDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            verificationDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
            if (llUploadProgress != null) llUploadProgress.setVisibility(View.GONE);
        }
    }

    private void showEditProfileDialog() {
        if (getContext() == null || binding == null) return;

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        EditText etName = new EditText(getContext());
        etName.setHint("Full Name");
        etName.setText(binding.tvProfileName.getText());
        layout.addView(etName);

        EditText etPhone = null;

        FirebaseUser currentUser = mAuth.getCurrentUser();
        String currentEmail = currentUser != null ? currentUser.getEmail() : "";
        boolean hasPhoneField = currentUserProfile != null && 
                ("LANDLORD".equalsIgnoreCase(currentUserProfile.getRole()) || 
                 "ADMIN".equalsIgnoreCase(currentUserProfile.getRole()) || 
                 AdminConfig.isAdmin(currentEmail));

        if (hasPhoneField) {
            etPhone = new EditText(getContext());
            etPhone.setHint("Contact Phone Number");
            etPhone.setInputType(InputType.TYPE_CLASS_PHONE);
            etPhone.setText(currentUserProfile.getDefaultPhone() != null ? currentUserProfile.getDefaultPhone() : "");
            layout.addView(etPhone);
        }

        final EditText finalPhone = etPhone;

        new MaterialAlertDialogBuilder(getContext())
                .setTitle("Edit Profile")
                .setMessage("Update your profile details:")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    String newPhone = finalPhone != null ? finalPhone.getText().toString().trim() : "";

                    if (!newName.isEmpty()) {
                        updateProfileDetails(newName, newPhone, hasPhoneField);
                    } else {
                        Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateProfileDetails(String newName, String newPhone, boolean hasPhoneField) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || userRef == null) return;

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build();

        currentUser.updateProfile(profileUpdates).addOnCompleteListener(task -> {
            if (task.isSuccessful() && getContext() != null) {
                userRef.child("name").setValue(newName);
                if (hasPhoneField) {
                    userRef.child("defaultPhone").setValue(newPhone);
                }
                Toast.makeText(getContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
            } else {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showUpdatePasswordDialog() {
        if (getContext() == null) return;

        EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("New Password (min 6 chars)");
        
        FrameLayout container = new FrameLayout(getContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 50;
        params.rightMargin = 50;
        input.setLayoutParams(params);
        container.addView(input);

        new MaterialAlertDialogBuilder(getContext())
                .setTitle("Update Password")
                .setMessage("Please enter your new password:")
                .setView(container)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newPassword = input.getText().toString().trim();
                    if (newPassword.length() >= 6) {
                        updatePassword(newPassword);
                    } else {
                        Toast.makeText(getContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updatePassword(String newPassword) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || getContext() == null) return;

        currentUser.updatePassword(newPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Password updated successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Failed to update password.";
                        Toast.makeText(getContext(), error + "\nTip: Try logging out and logging back in first.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void logout() {
        if (getContext() == null) return;

        mAuth.signOut();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(requireContext(), gso);
        googleSignInClient.signOut();
        
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
