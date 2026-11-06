package com.example.bashabhai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bashabhai.databinding.ActivityRegisterBinding;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        binding.btnRegister.setOnClickListener(v -> {
            String name = binding.etName.getText().toString().trim();
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // Can also save the user's name to the Realtime database here later
                            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(RegisterActivity.this, "Registration failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        binding.tvLoginLabel.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }
}
