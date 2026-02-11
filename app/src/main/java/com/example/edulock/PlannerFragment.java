package com.example.edulock;

import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Map;

public class PlannerFragment extends Fragment {

    EditText etSubject, etNotes, etStartTime, etEndTime;
    Spinner spinnerStartAmPm, spinnerEndAmPm;
    Button btnSave;
    TableLayout tableLayout;

    DatabaseReference plannerRef;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_planner, container, false);

        etSubject = view.findViewById(R.id.etSubject);
        etNotes = view.findViewById(R.id.etNotes);
        etStartTime = view.findViewById(R.id.etStartTime);
        etEndTime = view.findViewById(R.id.etEndTime);
        spinnerStartAmPm = view.findViewById(R.id.spinnerStartAmPm);
        spinnerEndAmPm = view.findViewById(R.id.spinnerEndAmPm);
        btnSave = view.findViewById(R.id.btnSave);
        tableLayout = view.findViewById(R.id.tableLayout);

        spinnerStartAmPm.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"AM", "PM"}));

        spinnerEndAmPm.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"AM", "PM"}));

        plannerRef = FirebaseDatabase.getInstance()
                .getReference("planner")
                .child("test_user_001");

        btnSave.setOnClickListener(v -> saveTimetable());
        loadTimetable();

        return view;
    }

    private void saveTimetable() {

        String subject = etSubject.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();
        String start = etStartTime.getText().toString().trim()
                + " " + spinnerStartAmPm.getSelectedItem();
        String end = etEndTime.getText().toString().trim()
                + " " + spinnerEndAmPm.getSelectedItem();

        if (subject.isEmpty() || etStartTime.getText().toString().isEmpty()
                || etEndTime.getText().toString().isEmpty()) {
            Toast.makeText(getContext(), "Fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String id = plannerRef.push().getKey();

        Map<String, String> map = new HashMap<>();
        map.put("subject", subject);
        map.put("notes", notes);
        map.put("start", start);
        map.put("end", end);

        assert id != null;
        plannerRef.child(id).setValue(map);

        etSubject.setText("");
        etNotes.setText("");
        etStartTime.setText("");
        etEndTime.setText("");
    }

    private void loadTimetable() {

        plannerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (tableLayout.getChildCount() > 1) {
                    tableLayout.removeViews(1,
                            tableLayout.getChildCount() - 1);
                }

                for (DataSnapshot snap : snapshot.getChildren()) {

                    String key = snap.getKey();
                    String subject = snap.child("subject").getValue(String.class);
                    String start = snap.child("start").getValue(String.class);
                    String end = snap.child("end").getValue(String.class);

                    TableRow row = new TableRow(getContext());

                    row.addView(cell(subject));
                    row.addView(cell(start));
                    row.addView(cell(end));

                    Button del = new Button(getContext());
                    del.setText("X");
                    del.setOnClickListener(v ->
                    {
                        assert key != null;
                        plannerRef.child(key).removeValue();
                    });
                    row.addView(del);

                    tableLayout.addView(row);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private TextView cell(String text) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextColor(0xFFFFFFFF);
        tv.setPadding(8, 8, 8, 8);
        return tv;
    }
}
