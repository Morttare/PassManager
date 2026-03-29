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

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {


    // https://docs.spring.io/spring-security/site/docs/4.2.4.RELEASE/apidocs/org/springframework/security/crypto/bcrypt/BCryptPasswordEncoder.html
    // Perhaps a map of (user, [credential]), requiring credential object etc.
    ListView itemList;
    Button btnAddItem;

    ArrayList<String> items;
    ArrayAdapter<String> adapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find the password list and add buttons
        itemList = findViewById(R.id.itemList);
        btnAddItem = findViewById(R.id.btnAddItem);

        items = new ArrayList<>();
        items.add("Test item");

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                items
        );

        itemList.setAdapter(adapter);
        itemList.setOnItemLongClickListener((parent, view, position, id) -> {
            showDeleteDialog(position);
            return true;
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

    private void showDeleteDialog(int position) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Delete Item");
        builder.setMessage("Are you sure you want to delete this item?");

        builder.setPositiveButton("Delete", (dialog, which) -> {

            items.remove(position);
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

            String name = etSiteName.getText().toString();
            String info = etUserInfo.getText().toString();
            String password = etPassword.getText().toString();

            // Missing the hashing, or perhaps in logout?
            Credentials creds = new Credentials();
            creds.setWebsite(name);
            creds.setUsername(info);
            creds.setPassword(password);


            // This is also where you would call the hashing function to hash the password
            /* if and when you want to make this into an object?
            Item item = new Item(name, info);
                items.add(item);
             */
            items.add(name + " - " + info + " - " + password);
            adapter.notifyDataSetChanged();

        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
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