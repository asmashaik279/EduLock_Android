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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class UserProfileFragment extends Fragment {

    private static final String DB_URL = "https://edulock-45a4f-default-rtdb.asia-southeast1.firebasedatabase.app/";

    EditText etName, etMobile, etEmail, etAddress, etDob;
    TextView txtEmailTop;
    Button btnSave;

    FirebaseAuth auth;
    DatabaseReference databaseRef;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_user_profile, container, false);

        etName = view.findViewById(R.id.etName);
        etMobile = view.findViewById(R.id.etMobile);
        etEmail = view.findViewById(R.id.etEmail);
        etAddress = view.findViewById(R.id.etAddress);
        etDob = view.findViewById(R.id.etDob);
        txtEmailTop = view.findViewById(R.id.txtEmailTop);
        btnSave = view.findViewById(R.id.btnSave);

        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(getContext(), "Please login first", Toast.LENGTH_LONG).show();
            return view;
        }

        databaseRef = FirebaseDatabase.getInstance(DB_URL).getReference("users").child(user.getUid());

        loadProfile();

        btnSave.setOnClickListener(v -> saveOrUpdateProfile());

        return view;
    }

    private void loadProfile() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        etEmail.setText(user.getEmail());
        txtEmailTop.setText(user.getEmail());

        databaseRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                etName.setText(snapshot.child("name").getValue(String.class));
                etMobile.setText(snapshot.child("mobile").getValue(String.class));
                etAddress.setText(snapshot.child("address").getValue(String.class));
                etDob.setText(snapshot.child("dob").getValue(String.class));
            }
        }).addOnFailureListener(e -> Log.e("Firebase", "Load Failed", e));
    }

    private void saveOrUpdateProfile() {
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
                .addOnSuccessListener(unused -> Toast.makeText(getContext(), "Profile saved successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
