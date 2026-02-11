package com.example.edulock;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class UserProfileFragment extends Fragment {

    EditText etName, etMobile, etEmail, etAddress, etDob;
    TextView txtEmailTop;
    Button btnSave;

    FirebaseAuth auth;
    DatabaseReference databaseRef;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_user_profile, container, false);

        // Views
        etName = view.findViewById(R.id.etName);
        etMobile = view.findViewById(R.id.etMobile);
        etEmail = view.findViewById(R.id.etEmail);
        etAddress = view.findViewById(R.id.etAddress);
        etDob = view.findViewById(R.id.etDob);
        txtEmailTop = view.findViewById(R.id.txtEmailTop);
        btnSave = view.findViewById(R.id.btnSave);

        // Firebase
        auth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("users");

        // Check login
        if (auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Please login first", Toast.LENGTH_LONG).show();
            Log.e("Firebase", "User not logged in");
            return view;
        }

        Log.d("Firebase", "UID: " + auth.getCurrentUser().getUid());

        // Load existing profile
        loadProfile();

        // Save button
        btnSave.setOnClickListener(v -> saveOrUpdateProfile());

        return view;
    }

    // ------------------ LOAD DATA ------------------
    private void loadProfile() {

        String uid = auth.getCurrentUser().getUid();
        String email = auth.getCurrentUser().getEmail();

        etEmail.setText(email);
        txtEmailTop.setText(email);

        databaseRef.child(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {

                        etName.setText(snapshot.child("name").getValue(String.class));
                        etMobile.setText(snapshot.child("mobile").getValue(String.class));
                        etAddress.setText(snapshot.child("address").getValue(String.class));
                        etDob.setText(snapshot.child("dob").getValue(String.class));

                        Log.d("Firebase", "Data Loaded");
                    } else {
                        Log.d("Firebase", "No existing data");
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("Firebase", "Load Failed", e));
    }

    // ------------------ SAVE DATA ------------------
    private void saveOrUpdateProfile() {

        String uid = auth.getCurrentUser().getUid();

        String name = etName.getText().toString().trim();
        String mobile = etMobile.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String dob = etDob.getText().toString().trim();

        // Basic validation
        if (TextUtils.isEmpty(name)) {
            etName.setError("Enter name");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("mobile", mobile);
        data.put("email", email);
        data.put("address", address);
        data.put("dob", dob);

        Log.d("Firebase", "Saving data for UID: " + uid);

        databaseRef.child(uid).setValue(data)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(),
                            "Profile saved successfully",
                            Toast.LENGTH_SHORT).show();

                    Log.d("Firebase", "Data Saved Successfully");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(),
                            "Save failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();

                    Log.e("Firebase", "Save Failed", e);
                });
    }
}
