package com.example.edulock;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HomeDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_dashboard);

        updateStreak();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        // Load default fragment when dashboard opens
        loadFragment(new StudyModeFragment());

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.nav_study) {
                selectedFragment = new StudyModeFragment();

            } else if (item.getItemId() == R.id.nav_progress) {
                selectedFragment = new ProgressFragment();

            } else if (item.getItemId() == R.id.nav_resources) {
                selectedFragment = new ResourcesFragment();

            } else if (item.getItemId() == R.id.nav_planner) {
                selectedFragment = new PlannerFragment();

            } else if (item.getItemId() == R.id.nav_profile) {
                selectedFragment = new UserProfileFragment();
            }

            return loadFragment(selectedFragment);
        });
    }

    private void updateStreak() {
        SharedPreferences prefs = getSharedPreferences("EduLockPrefs", Context.MODE_PRIVATE);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String today = sdf.format(new Date());
        String lastDate = prefs.getString("last_streak_date", "");

        if (today.equals(lastDate)) {
            // Already updated today
            return;
        }

        int currentStreak = prefs.getInt("study_streak", 0);

        if (lastDate.isEmpty()) {
            // First time using the app or streak initialized
            currentStreak = 1;
        } else {
            // Check if last login was yesterday
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -1);
            String yesterday = sdf.format(cal.getTime());

            if (lastDate.equals(yesterday)) {
                // Consecutive day
                currentStreak++;
            } else {
                // Streak broken
                currentStreak = 1;
            }
        }

        prefs.edit()
                .putInt("study_streak", currentStreak)
                .putString("last_streak_date", today)
                .apply();
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commit();
            return true;
        }
        return false;
    }
}
