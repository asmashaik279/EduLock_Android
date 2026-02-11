package com.example.edulock;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class StudyModeFragment extends Fragment {

    private NumberPicker timePicker;
    private Button startStudyButton;

    public StudyModeFragment() {
        // Required empty public constructor
    }

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        View view = inflater.inflate(
                R.layout.fragment_study_mode,
                container,
                false
        );

        // Bind views
        timePicker = view.findViewById(R.id.timePicker);
        startStudyButton = view.findViewById(R.id.btnStartStudy);

        // Extra safety (no crash)
        if (timePicker == null || startStudyButton == null) {
            return view;
        }

        // NumberPicker setup
        timePicker.setMinValue(30);
        timePicker.setMaxValue(180);
        timePicker.setValue(30);

        // Button click
        startStudyButton.setOnClickListener(v -> {
            int minutes = timePicker.getValue();
            Toast.makeText(
                    requireContext(),
                    "Study Mode started for " + minutes + " minutes",
                    Toast.LENGTH_SHORT
            ).show();
        });

        return view;
    }
}
