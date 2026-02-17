package com.example.edulock;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Random;

public class ProgressFragment extends Fragment {

    private TextView tvStreak, tvQuote;
    private SharedPreferences prefs;

    private String[] quotes = {
            "\"The secret of getting ahead is getting started.\"",
            "\"It always seems impossible until it's done.\"",
            "\"Don't watch the clock; do what it does. Keep going.\"",
            "\"Success is not final, failure is not fatal: it is the courage to continue that counts.\"",
            "\"Hardships often prepare ordinary people for an extraordinary destiny.\"",
            "\"Believe you can and you're halfway there.\"",
            "\"Your limitation—it's only your imagination.\"",
            "\"Push yourself, because no one else is going to do it for you.\"",
            "\"Great things never come from comfort zones.\"",
            "\"Dream it. Wish it. Do it.\""
    };

    public ProgressFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_progress, container, false);

        tvStreak = view.findViewById(R.id.tvStreak);
        tvQuote = view.findViewById(R.id.tvQuote);
        prefs = requireContext().getSharedPreferences("EduLockPrefs", Context.MODE_PRIVATE);

        displayStreak();
        displayRandomQuote();

        return view;
    }

    private void displayStreak() {
        int streak = prefs.getInt("study_streak", 5); // Default to 5 for demo if not set
        tvStreak.setText("🔥 " + streak + " Days");
    }

    private void displayRandomQuote() {
        Random random = new Random();
        int index = random.nextInt(quotes.length);
        tvQuote.setText(quotes[index]);
    }
}
