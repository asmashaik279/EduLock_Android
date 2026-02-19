package com.example.edulock;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class UserProfileFragment extends Fragment {

    private static final String DB_URL = "https://edulock-45a4f-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private TextInputEditText etName, etMobile, etEmail, etAddress, etDob;
    private TextView tvProfileName, tvProfileEmail;
    private Button btnSaveProfile, btnLogout;

    private FirebaseAuth auth;
    private DatabaseReference databaseRef;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Views
        etName = view.findViewById(R.id.etName);
        etMobile = view.findViewById(R.id.etMobile);
        etEmail = view.findViewById(R.id.etEmail);
        etAddress = view.findViewById(R.id.etAddress);
        etDob = view.findViewById(R.id.etDob);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        btnSaveProfile = view.findViewById(R.id.btnSaveProfile);
        btnLogout = view.findViewById(R.id.btnLogout);

        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            if (isAdded()) Toast.makeText(getContext(), "Please login first", Toast.LENGTH_LONG).show();
            return;
        }

        databaseRef = FirebaseDatabase.getInstance(DB_URL).getReference("users").child(user.getUid());

        setupDatePicker();
        loadProfile();

        btnSaveProfile.setOnClickListener(v -> saveOrUpdateProfile());
        
        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            if (getActivity() != null) {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                getActivity().finish();
            }
        });

        // Show MIUI helper dialog if needed
        checkAndShowMiuiHelper();
    }

    private void setupDatePicker() {
        etDob.setOnClickListener(v -> {
            if (!isAdded()) return;
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                    (dpView, year1, monthOfYear, dayOfMonth) -> {
                        String date = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1;
                        etDob.setText(date);
                    }, year, month, day);
            datePickerDialog.show();
        });
    }

    private void loadProfile() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || !isAdded()) return;

        String email = user.getEmail();
        etEmail.setText(email);
        tvProfileEmail.setText(email);

        databaseRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists() && isAdded()) {
                String name = snapshot.child("name").getValue(String.class);
                etName.setText(name);
                etMobile.setText(snapshot.child("mobile").getValue(String.class));
                etAddress.setText(snapshot.child("address").getValue(String.class));
                etDob.setText(snapshot.child("dob").getValue(String.class));
                if (!TextUtils.isEmpty(name)) tvProfileName.setText(name);
            }
        }).addOnFailureListener(e -> Log.e("Firebase", "Load Failed", e));
    }

    private void saveOrUpdateProfile() {
        if (!isAdded() || etName.getText() == null) return;
        String name = etName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            etName.setError("Enter name");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("mobile", etMobile.getText().toString().trim());
        data.put("email", etEmail.getText().toString().trim());
        data.put("address", etAddress.getText().toString().trim());
        data.put("dob", etDob.getText().toString().trim());

        databaseRef.setValue(data)
                .addOnSuccessListener(unused -> {
                    if(isAdded()) {
                        Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                        tvProfileName.setText(name);
                    }
                })
                .addOnFailureListener(e -> {
                    if(isAdded()) Toast.makeText(getContext(), "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void checkAndShowMiuiHelper() {
        if (!isAdded() || getContext() == null) return;
        
        SharedPreferences miuiPrefs = getContext().getSharedPreferences("MiuiPrefs", Context.MODE_PRIVATE);
        boolean hasShown = miuiPrefs.getBoolean("hasShownMiuiHelper", false);

        if (!hasShown && Build.MANUFACTURER.equalsIgnoreCase("Xiaomi")) {
            new AlertDialog.Builder(getContext())
                .setTitle("Xiaomi Device Detected")
                .setMessage("To ensure EduLock works correctly, please enable \"Autostart\" and set \"Battery saver\" to \"No restrictions\" for EduLock in your phone's Settings or Security app.")
                .setPositiveButton("Got it", (dialog, which) -> {
                    // Try to open autostart settings, this might not work on all MIUI versions
                    try {
                        Intent intent = new Intent();
                        intent.setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"));
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.e("MIUI", "Could not open Autostart settings.", e);
                    }
                })
                .setOnDismissListener(dialog -> miuiPrefs.edit().putBoolean("hasShownMiuiHelper", true).apply())
                .show();
        }
    }
}
