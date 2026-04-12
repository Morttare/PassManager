package com.example.passmanager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.appcompat.app.AppCompatActivity;

/*
Class for handling the login screen and -activity
 */
public class LoginActivity extends AppCompatActivity {

    EditText etPassword;
    Button btnLogin;
    PasswordHandler handler = new PasswordHandler();
    ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        // If user is already logged in, open main screen
        if(prefs.getBoolean("isLoggedIn", true)){
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

        // If not, we show the login screen
        // and find the fields, and connect the button
        setContentView(R.layout.activity_login);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(view -> loginUser());
    }

    private void loginUser() {

        // Get the password from the field, and compare it to the saved password
        String password = etPassword.getText().toString();

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String savedPassword = prefs.getString("password", "");

        executor.execute(() -> {
            boolean success = handler.encoder.matches(password, savedPassword);

            runOnUiThread(() ->{

                if(success){

                    // If password is correct, log in and open main screen
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("isLoggedIn", true);
                    editor.apply();

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                }else{
                    // Otherwise notify the user
                    Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}