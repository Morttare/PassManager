package com.example.passmanager;

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

public class MainActivity extends AppCompatActivity {


    // https://docs.spring.io/spring-security/site/docs/4.2.4.RELEASE/apidocs/org/springframework/security/crypto/bcrypt/BCryptPasswordEncoder.html
    // Perhaps a map of (user, [credential]), requiring credential object etc.
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



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find the password list and add buttons
        itemList = findViewById(R.id.itemList);
        btnAddItem = findViewById(R.id.btnAddItem);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        masterPassword = prefs.getString("password", "");

        // Load items from storage/initialize if not found
        loadItems();

        // does not add on first startup?? also cannot be seen with single press
        // subsequent openings show this correctly as the first one and then indexing works
        // on first startup shows the -1 index info when pressed
        // needs to have some items before refreshing adds the test item??
        // apparently they get added on startup but only visible once another item has been added
        items.add(new Credentials("testsite", "testname", "testpass"));


        // this fixes the visibility, not necessarily the indexing
        saveItems();
        loadItems();

        // Display the loaded items
        displayList = new ArrayList<>();
        for (Credentials c : items) {
            displayList.add(c.getWebsite() + " - " + c.getUsername());
        }

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                displayList
        );

        itemList.setAdapter(adapter);
        itemList.setOnItemLongClickListener((parent, view, position, id) -> {
            showDeleteDialog(position);
            return true;
        });

        // this doesn't work for test items??
        itemList.setOnItemClickListener((parent, view, position, id) -> {
            showPasswordDialog(position);
        });

        btnAddItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addItem();
            }
        });
    }

    private void addItem() {
        showAddItemDialog();
        adapter.notifyDataSetChanged();
    }

    void logOut(){
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isLoggedIn", false);
        editor.apply();

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void showPasswordDialog(String pass, Credentials creds) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Credentials for " + creds.getWebsite());
        builder.setMessage("Username: " + creds.getUsername() + "\nPassword: " +pass);

        builder.setNegativeButton("Close", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void showPasswordDialog(int position){
        Credentials creds = items.get(position);

        executor.execute(() ->{

            try {
                PasswordHandler handler = new PasswordHandler();

                byte[] salt = Base64.getDecoder().decode(creds.getSalt());
                byte[] ivBytes = Base64.getDecoder().decode(creds.getIv());

                SecretKey key = handler.getKeyFromPassword(masterPassword, salt);
                GCMParameterSpec iv = new GCMParameterSpec(128, ivBytes);

                String decrypted = handler.decrypt(
                        "AES/GCM/NoPadding",
                        creds.getPassword(),
                        key,
                        iv
                );

                runOnUiThread(() ->{
                    showPasswordDialog(decrypted, creds);
                });

            } catch (Exception e) {
                e.printStackTrace();
            }

        });
    }

    private void showDeleteDialog(int position) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Delete Item");
        builder.setMessage("Are you sure you want to delete this item?");

        builder.setPositiveButton("Delete", (dialog, which) -> {

            items.remove(position);
            displayList.remove(position);
            saveItems();
            adapter.notifyDataSetChanged();

        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void showAddItemDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();

        View dialogView = inflater.inflate(R.layout.dialog_add_item, null);
        builder.setView(dialogView);

        EditText etSiteName = dialogView.findViewById(R.id.etSiteName);
        EditText etUserInfo = dialogView.findViewById(R.id.etUserInfo);
        EditText etPassword = dialogView.findViewById(R.id.etPassword);

        builder.setTitle("Add Credentials");

        builder.setPositiveButton("Add", (dialog, which) -> {
            // Will need multithreading somewhere

            String name = etSiteName.getText().toString();
            String info = etUserInfo.getText().toString();
            String password = etPassword.getText().toString();

            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);

            try {
                key = handler.getKeyFromPassword(masterPassword, salt);

                GCMParameterSpec iv = handler.generateIv();
                String cipher = handler.encrypt(algorithm, password, key, iv);

                // Store credentials into object
                Credentials creds = new Credentials(name, info, cipher);
                creds.setIv(Base64.getEncoder().encodeToString(iv.getIV()));
                creds.setSalt(Base64.getEncoder().encodeToString(salt));

                items.add(creds);
                displayList.add(creds.getWebsite() + " - " + creds.getUsername());
                saveItems();
                loadItems();
                adapter.notifyDataSetChanged();

            } catch (Exception e) {
                // Possibly different toasts for different exceptions?
                Toast.makeText(this, "An error occurred", Toast.LENGTH_SHORT).show();
            }


        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    // Load the items from JSON into itemlist
    private void loadItems() {
        SharedPreferences prefs = getSharedPreferences("ItemPrefs", MODE_PRIVATE);

        Gson gson = new Gson();
        String json = prefs.getString("item_list", null);

        if (json != null) {
            Type type = new TypeToken<ArrayList<Credentials>>() {}.getType();
            items = gson.fromJson(json, type);
        } else {
            items = new ArrayList<>();
        }
    }

    // Save the password items as stringified JSON
    private void saveItems() {
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