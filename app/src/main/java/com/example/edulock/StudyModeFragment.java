package com.example.edulock;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StudyModeFragment extends Fragment {

    private RecyclerView rvUserApps;
    private Button btnStartStudy;
    private SharedPreferences prefs;
    private Set<String> blockedPackages;
    private List<AppModel> installedApps = new ArrayList<>();
    private AppAdapter adapter;

    public StudyModeFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_study_mode, container, false);

        rvUserApps = view.findViewById(R.id.rvUserApps);
        btnStartStudy = view.findViewById(R.id.btnStartStudy);
        prefs = requireContext().getSharedPreferences("EduLockPrefs", Context.MODE_PRIVATE);

        // Load saved blocked apps
        blockedPackages = new HashSet<>(prefs.getStringSet("userBlockedApps", new HashSet<>(Arrays.asList(
                "com.instagram.android",
                "com.snapchat.android",
                "com.google.android.youtube"
        ))));

        setupRecyclerView();
        loadInstalledApps();

        boolean isActive = prefs.getBoolean("isStudyModeActive", false);
        updateButtonUI(isActive);

        btnStartStudy.setOnClickListener(v -> {
            if (!hasUsageStatsPermission()) {
                requestUsageStatsPermission();
                return;
            }

            if (!hasOverlayPermission()) {
                requestOverlayPermission();
                return;
            }

            toggleStudyMode();
        });

        return view;
    }

    private void setupRecyclerView() {
        rvUserApps.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AppAdapter(installedApps, blockedPackages, (app, isChecked) -> {
            if (isChecked) {
                blockedPackages.add(app.getPackageName());
            } else {
                blockedPackages.remove(app.getPackageName());
            }
            prefs.edit().putStringSet("userBlockedApps", blockedPackages).apply();
            
            // If service is running, it needs to know the list changed
            if (prefs.getBoolean("isStudyModeActive", false)) {
                restartService();
            }
        });
        rvUserApps.setAdapter(adapter);
    }

    private void loadInstalledApps() {
        PackageManager pm = requireContext().getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        installedApps.clear();
        for (ApplicationInfo appInfo : packages) {
            // Filter out system apps mostly, keep user apps
            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0 || 
                appInfo.packageName.equals("com.google.android.youtube")) {
                
                String name = pm.getApplicationLabel(appInfo).toString();
                installedApps.add(new AppModel(name, appInfo.packageName, pm.getApplicationIcon(appInfo), false));
            }
        }
        // Sort alphabetically
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            installedApps.sort((a, b) -> a.getAppName().compareToIgnoreCase(b.getAppName()));
        }
        adapter.notifyDataSetChanged();
    }

    private void toggleStudyMode() {
        boolean isActive = prefs.getBoolean("isStudyModeActive", false);
        boolean newState = !isActive;

        prefs.edit().putBoolean("isStudyModeActive", newState).apply();

        if (newState) {
            startService();
            Toast.makeText(getContext(), "Focus Mode Activated!", Toast.LENGTH_SHORT).show();
        } else {
            stopService();
            Toast.makeText(getContext(), "Focus Mode Deactivated!", Toast.LENGTH_SHORT).show();
        }

        updateButtonUI(newState);
    }

    private void startService() {
        Intent serviceIntent = new Intent(requireContext(), AppBlockService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(serviceIntent);
        } else {
            requireContext().startService(serviceIntent);
        }
    }

    private void stopService() {
        Intent serviceIntent = new Intent(requireContext(), AppBlockService.class);
        requireContext().stopService(serviceIntent);
    }

    private void restartService() {
        stopService();
        startService();
    }

    private void updateButtonUI(boolean isActive) {
        if (isActive) {
            btnStartStudy.setText("STOP FOCUS MODE");
            btnStartStudy.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#EF4444")));
        } else {
            btnStartStudy.setText("START FOCUS MODE");
            btnStartStudy.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#38BDF8")));
        }
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) requireContext().getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), requireContext().getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void requestUsageStatsPermission() {
        Toast.makeText(getContext(), "Grant Usage Access to block apps", Toast.LENGTH_LONG).show();
        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
    }

    private boolean hasOverlayPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(requireContext());
    }

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Toast.makeText(getContext(), "Grant Overlay Permission to show Blocked screen", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:" + requireContext().getPackageName()));
            startActivity(intent);
        }
    }
}
