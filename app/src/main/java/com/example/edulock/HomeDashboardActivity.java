package com.example.edulock;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_dashboard);

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
