package com.example.edulock;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.Calendar;
import java.util.Locale;

public class PlannerFragment extends Fragment {

    private static final String TAG = "PlannerFragment";
    private static final String DB_URL = "https://edulock-45a4f-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private EditText etSubject, etNotes, etStartTime, etEndTime;
    private Spinner spinnerDay;
    private Button btnSave;
    private TableLayout tableLayout;
    private ProgressBar progressBar;

    private FirebaseAuth auth;
    private DatabaseReference databaseReference;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_planner, container, false);

        etSubject = view.findViewById(R.id.etSubject);
        etNotes = view.findViewById(R.id.etNotes);
        etStartTime = view.findViewById(R.id.etStartTime);
        etEndTime = view.findViewById(R.id.etEndTime);
        spinnerDay = view.findViewById(R.id.spinnerDay);
        btnSave = view.findViewById(R.id.btnSave);
        tableLayout = view.findViewById(R.id.tableLayout);
        progressBar = view.findViewById(R.id.progressBar);

        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, days);
        spinnerDay.setAdapter(dayAdapter);

        etStartTime.setOnClickListener(v -> showTimePicker(etStartTime));
        etEndTime.setOnClickListener(v -> showTimePicker(etEndTime));

        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        
        if (user == null) {
            Toast.makeText(getContext(), "User not logged in!", Toast.LENGTH_SHORT).show();
            return view;
        }

        databaseReference = FirebaseDatabase.getInstance(DB_URL).getReference("Timetable").child(user.getUid());
        btnSave.setOnClickListener(v -> saveData());
        loadData();

        return view;
    }

    private void showTimePicker(final EditText editText) {
        Calendar mcurrentTime = Calendar.getInstance();
        int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
        int minute = mcurrentTime.get(Calendar.MINUTE);
        new TimePickerDialog(getContext(), (timePicker, selectedHour, selectedMinute) -> {
            String am_pm = (selectedHour < 12) ? "AM" : "PM";
            int hourDisplay = (selectedHour > 12) ? selectedHour - 12 : (selectedHour == 0 ? 12 : selectedHour);
            editText.setText(String.format(Locale.getDefault(), "%02d:%02d %s", hourDisplay, selectedMinute, am_pm));
        }, hour, minute, false).show();
    }

    private void saveData() {
        String subject = etSubject.getText().toString().trim();
        String day = spinnerDay.getSelectedItem().toString();
        String startTime = etStartTime.getText().toString().trim();
        String endTime = etEndTime.getText().toString().trim();

        if (TextUtils.isEmpty(subject) || TextUtils.isEmpty(startTime) || TextUtils.isEmpty(endTime)) {
            Toast.makeText(getContext(), "Fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        String id = databaseReference.push().getKey();
        TimetableModel model = new TimetableModel(id, subject, etNotes.getText().toString(), day, startTime, endTime);

        if (id != null) {
            databaseReference.child(id).setValue(model)
                    .addOnSuccessListener(unused -> {
                        progressBar.setVisibility(View.GONE);
                        btnSave.setEnabled(true);
                        Toast.makeText(getContext(), "Saved Successfully", Toast.LENGTH_SHORT).show();
                        clearFields();
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        btnSave.setEnabled(true);
                        Toast.makeText(getContext(), "Save Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }

    private void loadData() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                tableLayout.removeAllViews();
                addHeaderRow();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    TimetableModel model = ds.getValue(TimetableModel.class);
                    if (model != null) addRow(model);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Load failed: " + error.getMessage());
            }
        });
    }

    private void addHeaderRow() {
        TableRow header = new TableRow(getContext());
        header.setBackgroundColor(Color.parseColor("#1E293B"));
        header.addView(createCell("Day", true));
        header.addView(createCell("Subject", true));
        header.addView(createCell("Time", true));
        header.addView(createCell("Del", true));
        tableLayout.addView(header);
    }

    private void addRow(TimetableModel model) {
        TableRow row = new TableRow(getContext());
        row.setPadding(0, 10, 0, 10);
        row.addView(createCell(model.getDay(), false));
        row.addView(createCell(model.getSubject(), false));
        row.addView(createCell(model.getStartTime() + "\n" + model.getEndTime(), false));

        ImageView ivDelete = new ImageView(getContext());
        ivDelete.setImageResource(android.R.drawable.ic_delete);
        ivDelete.setColorFilter(Color.RED);
        ivDelete.setPadding(10, 10, 10, 10);
        
        TableRow.LayoutParams params = new TableRow.LayoutParams(60, 60);
        params.gravity = Gravity.CENTER;
        ivDelete.setLayoutParams(params);

        ivDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete this?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    databaseReference.child(model.getId()).removeValue();
                })
                .setNegativeButton("No", null)
                .show();
        });
        
        row.addView(ivDelete);
        tableLayout.addView(row);

        // Add a separator line
        View separator = new View(getContext());
        separator.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 1));
        separator.setBackgroundColor(Color.parseColor("#334155"));
        tableLayout.addView(separator);
    }

    private TextView createCell(String text, boolean isHeader) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setPadding(10, 20, 10, 20);
        tv.setGravity(Gravity.CENTER);
        if (isHeader) {
            tv.setTypeface(null, Typeface.BOLD);
            tv.setTextSize(14);
        } else {
            tv.setTextSize(12);
        }
        return tv;
    }

    private void clearFields() {
        etSubject.setText(""); etNotes.setText(""); etStartTime.setText(""); etEndTime.setText("");
    }
}
