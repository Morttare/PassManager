package com.example.passmanager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.appcompat.app.AppCompatActivity;

/*
Class for handling the creation of master password
 */
public class SignUpActivity extends AppCompatActivity {

    EditText etPassword;
    Button btnSignUp;
    PasswordHandler handler = new PasswordHandler();

    ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        // If user already exists we can open login screen
        if (prefs.contains("password")) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }

        // If not, we show the sign-up screen
        // and find the fields, and connect the button
        setContentView(R.layout.activity_sign_up);
        etPassword = findViewById(R.id.etNewPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnSignUp.setOnClickListener(view -> signUpUser());
    }

    private void signUpUser() {

        // Get the master password from the field
        // and run the encoding algorithm on it
        // and then save the password hash
        // and update login status
        String password = etPassword.getText().toString();

        executor.execute(() -> {

            String encodedPassword = handler.encoder.encode(password);

            runOnUiThread(() ->{
                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();

                editor.putString("password", encodedPassword);
                editor.putBoolean("isLoggedIn", true);
                editor.apply();

                // Go to main screen after signing up
                startActivity(new Intent(this, MainActivity.class));
                finish();
            });

        });

    }
}
