package com.example.passmanager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
/*
The main class of the program, which keeps track of the currently saved credential items,
logging the user out whenever the focus is lost on the application, and allowing for the deletion/insertion
of credentials.
 */
public class MainActivity extends AppCompatActivity {


    // https://docs.spring.io/spring-security/site/docs/4.2.4.RELEASE/apidocs/org/springframework/security/crypto/bcrypt/BCryptPasswordEncoder.html
    // https://www.baeldung.com/java-aes-encryption-decryption
    ListView itemList;
    Button btnAddItem;
    String masterPassword;
    ArrayList<Credentials> items;
    ArrayList<String> displayList;
    ArrayAdapter<String> adapter;
    PasswordHandler handler = new PasswordHandler();
    ExecutorService executor = Executors.newSingleThreadExecutor();
    SecretKey key;
    String algorithm = "AES/GCM/NoPadding";
    Context context;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find the password list and add buttons
        itemList = findViewById(R.id.itemList);
        btnAddItem = findViewById(R.id.btnAddItem);
        context = this;

        // Get the master password from preferences, to be used with encryption later on
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        masterPassword = prefs.getString("password", "");

        // Load items from storage/initialize if not found
        loadItems();

        // Display the loaded items
        displayList = new ArrayList<>();
        for (Credentials c : items) {
            displayList.add(c.getWebsite() + " - " + c.getUsername());
        }

        // Set the items on an adapter, which allows for runtime updating of the UI
        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                displayList
        );
        itemList.setAdapter(adapter);

        // Main functionality: hold to delete credentials, click to show full credentials
        // and use add button to store more credentials.
        itemList.setOnItemLongClickListener((parent, view, position, id) -> {
            showDeleteDialog(position);
            return true;
        });
        itemList.setOnItemClickListener((parent, view, position, id) -> {
            showPasswordDialog(position);
        });
        btnAddItem.setOnClickListener(view -> addItem());
    }

    private void addItem() {
        showAddItemDialog();
        adapter.notifyDataSetChanged();
    }

    void logOut(){

        // Log out the user and open login screen
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isLoggedIn", false);
        editor.apply();

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void showPasswordDialog(String pass, Credentials creds) {

        // Show the specified credentials
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Credentials for " + creds.getWebsite());
        builder.setMessage("Username: " + creds.getUsername() + "\nPassword: " +pass);

        builder.setNegativeButton("Close", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void showPasswordDialog(int position){

        // Decrypt the password for the credentials in the position of the click
        Credentials creds = items.get(position);
        executor.execute(() ->{

            try {
                PasswordHandler handler = new PasswordHandler();

                byte[] salt = Base64.getDecoder().decode(creds.getSalt());
                byte[] ivBytes = Base64.getDecoder().decode(creds.getIv());

                SecretKey key = handler.getKeyFromPassword(masterPassword, salt);
                GCMParameterSpec iv = new GCMParameterSpec(128, ivBytes);

                String decrypted = handler.decrypt(
                        algorithm,
                        creds.getPassword(),
                        key,
                        iv
                );

                runOnUiThread(() ->{
                    // And show them on a dialog
                    showPasswordDialog(decrypted, creds);
                });

            } catch (Exception e) {
                Toast.makeText(context, "An error occurred", Toast.LENGTH_SHORT).show();
            }

        });
    }

    private void showDeleteDialog(int position) {

        // Show confirmation dialog for deleting the item
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Credentials");
        builder.setMessage("Are you sure you want to delete credentials for " + items.get(position).getWebsite() + "?");
        builder.setPositiveButton("Delete", (dialog, which) -> {

            // Remove the deleted item from all stored lists and save the new list
            items.remove(position);
            displayList.remove(position);
            saveItems();
            adapter.notifyDataSetChanged();

        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void showAddItemDialog() {

        // Show a dialog for adding items
        // containing fields for website, username and password
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();

        View dialogView = inflater.inflate(R.layout.dialog_add_item, null);
        builder.setView(dialogView);

        EditText etSiteName = dialogView.findViewById(R.id.etSiteName);
        EditText etUserInfo = dialogView.findViewById(R.id.etUserInfo);
        EditText etPassword = dialogView.findViewById(R.id.etPassword);

        builder.setTitle("Add Credentials");

        builder.setPositiveButton("Add", (dialog, which) -> {

            // Get the given values and random salt
            String name = etSiteName.getText().toString();
            String info = etUserInfo.getText().toString();
            String password = etPassword.getText().toString();
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            executor.execute( () ->{


                try {

                    // Create secret key from master password
                    // and encrypt the password with given parameters
                    key = handler.getKeyFromPassword(masterPassword, salt);
                    GCMParameterSpec iv = handler.generateIv();
                    String cipher = handler.encrypt(algorithm, password, key, iv);

                    // Store credentials into object
                    Credentials creds = new Credentials(name, info, cipher);
                    creds.setIv(Base64.getEncoder().encodeToString(iv.getIV()));
                    creds.setSalt(Base64.getEncoder().encodeToString(salt));

                    runOnUiThread(() ->{
                        // Add the new credential object to the list
                        // and save+load to update the storage
                        items.add(creds);
                        displayList.add(creds.getWebsite() + " - " + creds.getUsername());
                        saveItems();
                        loadItems();
                        adapter.notifyDataSetChanged();
                    });
                } catch (Exception e) {
                    // In case of an error, display a notification
                    Toast.makeText(this, "An error occurred", Toast.LENGTH_SHORT).show();
                }
            });

        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void loadItems() {

        // Load the items from preference JSON into itemlist
        SharedPreferences prefs = getSharedPreferences("ItemPrefs", MODE_PRIVATE);

        Gson gson = new Gson();
        String json = prefs.getString("item_list", null);

        // If there was no stored JSON, create an empty list
        if (json != null) {
            Type type = new TypeToken<ArrayList<Credentials>>() {}.getType();
            items = gson.fromJson(json, type);
        } else {
            items = new ArrayList<>();
        }
    }

    private void saveItems() {

        // Save the credentials as stringified JSON
        SharedPreferences prefs = getSharedPreferences("ItemPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Gson gson = new Gson();
        String json = gson.toJson(items);

        editor.putString("item_list", json);
        editor.apply();
    }

    // Log out when losing focus or closing application
    @Override
    protected void onStop(){
        super.onStop();
        logOut();
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        logOut();
    }


}