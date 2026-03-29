package com.example.passmanager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.appcompat.app.AppCompatActivity;

public class SignUpActivity extends AppCompatActivity {

    EditText etUsername, etPassword;
    Button btnSignUp;
    PasswordHandler handler = new PasswordHandler();

    ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        // If user already exists → go to login
        if (prefs.contains("username")) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }

        setContentView(R.layout.activity_sign_up);

        etUsername = findViewById(R.id.etNewUsername);
        etPassword = findViewById(R.id.etNewPassword);
        btnSignUp = findViewById(R.id.btnSignUp);

        btnSignUp.setOnClickListener(view -> signUpUser());
    }

    private void signUpUser() {

        String username = etUsername.getText().toString();
        String password = etPassword.getText().toString();

        executor.execute(() -> {

            String encodedPassword = handler.encoder.encode(password);

            runOnUiThread(() ->{
                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();

                editor.putString("username", username);
                editor.putString("password", encodedPassword);
                editor.apply();

                // Go to login screen after signup
                // Possibly change this to main activity since login twice is tedious
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            });

        });

    }
}
