package com.example.edulock;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StudyModeFragment extends Fragment {

    private static final String TAG = "StudyModeFragment";
    private static final String DB_URL = "https://edulock-45a4f-default-rtdb.asia-southeast1.firebasedatabase.app/";
    
    private Switch switchFocus;
    private NumberPicker numberPicker;
    private TextView tvStudyModeInfo;
    private RecyclerView rvDefaultApps, rvManuallyBlocked, rvAllApps;
    private Button btnSaveApps;
    private SharedPreferences prefs;

    private AppAdapter defaultAdapter, manuallyBlockedAdapter, allAppsAdapter;
    private final List<AppModel> defaultApps = new ArrayList<>();
    private final List<AppModel> manuallyBlockedApps = new ArrayList<>();
    private final List<AppModel> allAppsList = new ArrayList<>();

    private DatabaseReference databaseRef;
    private String userId;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    private static final Set<String> DEFAULT_BLOCKED_PACKAGES = new HashSet<>(Arrays.asList(
            "com.instagram.android",
            "com.snapchat.android",
            "com.google.android.youtube"
    ));

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_study_mode, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        switchFocus = view.findViewById(R.id.switchFocus);
        numberPicker = view.findViewById(R.id.numberPicker);
        tvStudyModeInfo = view.findViewById(R.id.tvStudyModeInfo);
        btnSaveApps = view.findViewById(R.id.btnSaveApps);
        rvDefaultApps = view.findViewById(R.id.rvDefaultApps);
        rvManuallyBlocked = view.findViewById(R.id.rvManuallyBlocked);
        rvAllApps = view.findViewById(R.id.rvAllApps);

        Context context = getContext();
        if (context == null) return;
        prefs = context.getSharedPreferences("EduLockPrefs", Context.MODE_PRIVATE);
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            databaseRef = FirebaseDatabase.getInstance(DB_URL).getReference("users").child(userId).child("blockedApps");
        }

        setupTimer(); 
        setupRecyclerViews();
        fetchBlockedAppsFromFirebase(); 
        setupListeners();
        startTimerMonitor();
    }

    private void startTimerMonitor() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded()) return;
                
                boolean isActive = prefs.getBoolean("isStudyModeActive", false);
                long endTime = prefs.getLong("studyModeEndTime", 0);
                
                if (isActive) {
                    if (System.currentTimeMillis() >= endTime) {
                        prefs.edit().putBoolean("isStudyModeActive", false).apply();
                        unlockUI();
                        switchFocus.setChecked(false);
                        tvStudyModeInfo.setText("Focus session finished! You are free.");
                    } else {
                        lockUI();
                        switchFocus.setChecked(true);
                        long diff = endTime - System.currentTimeMillis();
                        int minsLeft = (int) (diff / (1000 * 60));
                        tvStudyModeInfo.setText("Study Mode ACTIVE! " + minsLeft + " mins remaining.");
                    }
                } else {
                    unlockUI();
                }
                timerHandler.postDelayed(this, 5000); 
            }
        };
        timerHandler.post(timerRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (timerHandler != null) timerHandler.removeCallbacks(timerRunnable);
    }

    private void setupListeners() {
        btnSaveApps.setOnClickListener(v -> {
            if (prefs.getBoolean("isStudyModeActive", false)) {
                Toast.makeText(getContext(), "Cannot edit apps during an active session!", Toast.LENGTH_SHORT).show();
                return;
            }

            Set<String> selectedPkgs = new HashSet<>();
            for (AppModel app : allAppsList) {
                if (app.isChecked()) {
                    selectedPkgs.add(app.getPackageName());
                }
            }

            selectedPkgs.remove(requireContext().getPackageName());

            prefs.edit().putStringSet("userBlockedApps", selectedPkgs).apply();
            if (databaseRef != null) databaseRef.setValue(new ArrayList<>(selectedPkgs));

            fetchBlockedAppsFromFirebase();
            Toast.makeText(getContext(), "Apps saved and locked.", Toast.LENGTH_SHORT).show();
        });

        switchFocus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return; 

            if (isChecked) {
                if (!PermissionUtils.isAccessibilityServiceEnabled(requireContext(), AppBlockAccessibilityService.class)) {
                    Toast.makeText(getContext(), "Enable Accessibility first", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    switchFocus.setChecked(false);
                    return;
                }
                
                int mins = numberPicker.getValue();
                long endTime = System.currentTimeMillis() + (mins * 60 * 1000L);
                
                prefs.edit()
                        .putBoolean("isStudyModeActive", true)
                        .putInt("focusDuration", mins)
                        .putLong("studyModeEndTime", endTime)
                        .apply();
                
                lockUI();
                Toast.makeText(getContext(), "Focus Mode Locked for " + mins + " minutes.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void lockUI() {
        switchFocus.setEnabled(false);
        numberPicker.setEnabled(false);
        btnSaveApps.setEnabled(false);
    }

    private void unlockUI() {
        switchFocus.setEnabled(true);
        numberPicker.setEnabled(true);
        btnSaveApps.setEnabled(true);
    }

    private void setupTimer() {
        numberPicker.setMinValue(30);
        numberPicker.setMaxValue(480);
        
        // Ensure the value is reset to 30 every time the fragment is loaded
        // and isStudyModeActive is false.
        if (!prefs.getBoolean("isStudyModeActive", false)) {
            numberPicker.setValue(30);
        } else {
            numberPicker.setValue(prefs.getInt("focusDuration", 30));
        }
    }

    private void setupRecyclerViews() {
        rvDefaultApps.setLayoutManager(new LinearLayoutManager(getContext()));
        defaultAdapter = new AppAdapter(defaultApps, false, null);
        rvDefaultApps.setAdapter(defaultAdapter);

        rvManuallyBlocked.setLayoutManager(new LinearLayoutManager(getContext()));
        manuallyBlockedAdapter = new AppAdapter(manuallyBlockedApps, false, null);
        rvManuallyBlocked.setAdapter(manuallyBlockedAdapter);

        rvAllApps.setLayoutManager(new LinearLayoutManager(getContext()));
        allAppsAdapter = new AppAdapter(allAppsList, true, (app, isChecked) -> app.setChecked(isChecked));
        rvAllApps.setAdapter(allAppsAdapter);
    }

    private void fetchBlockedAppsFromFirebase() {
        if (databaseRef == null) {
            new LoadAppsTask(this, new HashSet<>()).execute();
            return;
        }
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Set<String> pkgs = new HashSet<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String p = ds.getValue(String.class);
                    if (p != null) pkgs.add(p);
                }
                if (isAdded()) {
                    prefs.edit().putStringSet("userBlockedApps", pkgs).apply();
                    new LoadAppsTask(StudyModeFragment.this, pkgs).execute();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private static class LoadAppsTask extends AsyncTask<Void, Void, Void> {
        private final WeakReference<StudyModeFragment> fragRef;
        private final Set<String> blockedPkgs;
        private final List<AppModel> lDefault = new ArrayList<>();
        private final List<AppModel> lManual = new ArrayList<>();
        private final List<AppModel> lAll = new ArrayList<>();

        LoadAppsTask(StudyModeFragment f, Set<String> pkgs) { fragRef = new WeakReference<>(f); this.blockedPkgs = pkgs; }

        @Override protected Void doInBackground(Void... voids) {
            StudyModeFragment f = fragRef.get();
            if (f == null || !f.isAdded() || f.getContext() == null) return null;
            PackageManager pm = f.getContext().getPackageManager();
            List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            for (ApplicationInfo info : apps) {
                try {
                    if (info.packageName.equals(f.getContext().getPackageName()) || pm.getLaunchIntentForPackage(info.packageName) == null) continue;
                    AppModel app = new AppModel(info.loadLabel(pm).toString(), info.packageName, info.loadIcon(pm));
                    if (DEFAULT_BLOCKED_PACKAGES.contains(app.getPackageName())) lDefault.add(app);
                    else {
                        if (blockedPkgs.contains(app.getPackageName())) { app.setChecked(true); lManual.add(app); }
                        lAll.add(app);
                    }
                } catch (Exception ignored) {}
            }
            return null;
        }

        @Override protected void onPostExecute(Void aVoid) {
            StudyModeFragment f = fragRef.get();
            if (f == null || !f.isAdded()) return;
            f.defaultApps.clear(); f.defaultApps.addAll(lDefault);
            f.manuallyBlockedApps.clear(); f.manuallyBlockedApps.addAll(lManual);
            f.allAppsList.clear(); f.allAppsList.addAll(lAll);
            Collections.sort(f.defaultApps, (a, b) -> a.getAppName().compareToIgnoreCase(b.getAppName()));
            Collections.sort(f.manuallyBlockedApps, (a, b) -> a.getAppName().compareToIgnoreCase(b.getAppName()));
            Collections.sort(f.allAppsList, (a, b) -> a.getAppName().compareToIgnoreCase(b.getAppName()));
            if (f.defaultAdapter != null) f.defaultAdapter.notifyDataSetChanged();
            if (f.manuallyBlockedAdapter != null) f.manuallyBlockedAdapter.notifyDataSetChanged();
            if (f.allAppsAdapter != null) f.allAppsAdapter.notifyDataSetChanged();
        }
    }
}
