package com.thesis.coinbox.ui.main;

import static com.thesis.coinbox.utilities.Constants.SAVINGS_COLLECTION;
import static com.thesis.coinbox.utilities.Constants.USERS_COLLECTION;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import java.text.NumberFormat;
import java.util.Locale;
import com.google.firebase.firestore.SetOptions;
import com.thesis.coinbox.R;
import com.thesis.coinbox.data.model.LoggedInUser;
import com.thesis.coinbox.data.model.Savings;
import com.thesis.coinbox.databinding.ActivityMainBinding;
import com.thesis.coinbox.ui.login.LoginActivity;



public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

        firebaseAuth.addAuthStateListener(firebaseAuth1 -> {
            if(firebaseAuth1.getCurrentUser() == null){
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
            }
        });


        if (firebaseAuth.getCurrentUser() == null) {
            // User is not logged in, redirect to LoginActivity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Optional: finish the MainActivity so that the user cannot navigate back to it
        }

        // Disable the back button (up button) in the Toolbar
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            // Handle logout action
            FirebaseAuth.getInstance().signOut();
            // Redirect to LoginActivity or perform any other actions
            return true;
        }else if (item.getItemId() == android.R.id.home){

            //remove the backbutton when the screen is on homescreen (mainactivity)

            onBackPressed(); // This will perform the same action as the back button press
            return true;

        }

        return super.onOptionsItemSelected(item);


    }
}