package com.example.edulock;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
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

        // UI Initialization
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
        
        // Firebase Setup
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            databaseRef = FirebaseDatabase.getInstance(DB_URL).getReference("users").child(userId).child("blockedApps");
        }

        setupRecyclerViews();
        setupTimer();
        fetchBlockedAppsFromFirebase(); // Initial load
        
        setupListeners();

        boolean isActive = prefs.getBoolean("isStudyModeActive", false);
        switchFocus.setChecked(isActive);
        if (isActive) lockUI();
    }

    private void setupRecyclerViews() {
        if (getContext() == null) return;
        
        rvDefaultApps.setLayoutManager(new LinearLayoutManager(getContext()));
        defaultAdapter = new AppAdapter(defaultApps, false, null);
        rvDefaultApps.setAdapter(defaultAdapter);

        rvManuallyBlocked.setLayoutManager(new LinearLayoutManager(getContext()));
        manuallyBlockedAdapter = new AppAdapter(manuallyBlockedApps, false, null);
        rvManuallyBlocked.setAdapter(manuallyBlockedAdapter);

        rvAllApps.setLayoutManager(new LinearLayoutManager(getContext()));
        allAppsAdapter = new AppAdapter(allAppsList, true, (app, isChecked) -> {
            app.setChecked(isChecked);
        });
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
                Set<String> blockedPkgs = new HashSet<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String pkg = ds.getValue(String.class);
                    if (pkg != null) blockedPkgs.add(pkg);
                }
                
                if (isAdded() && prefs != null) {
                    prefs.edit().putStringSet("userBlockedApps", blockedPkgs).apply();
                    new LoadAppsTask(StudyModeFragment.this, blockedPkgs).execute();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded() && prefs != null) {
                    new LoadAppsTask(StudyModeFragment.this, prefs.getStringSet("userBlockedApps", new HashSet<>())).execute();
                }
            }
        });
    }

    private static class LoadAppsTask extends AsyncTask<Void, Void, Void> {
        private final WeakReference<StudyModeFragment> fragmentReference;
        private final Set<String> userBlockedPkgs;
        private final List<AppModel> localDefault = new ArrayList<>();
        private final List<AppModel> localManual = new ArrayList<>();
        private final List<AppModel> localAll = new ArrayList<>();

        LoadAppsTask(StudyModeFragment fragment, Set<String> userBlockedPkgs) {
            fragmentReference = new WeakReference<>(fragment);
            this.userBlockedPkgs = userBlockedPkgs;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            StudyModeFragment fragment = fragmentReference.get();
            if (fragment == null || !fragment.isAdded()) return null;

            Context context = fragment.getContext();
            if (context == null) return null;

            PackageManager pm = context.getPackageManager();
            List<ApplicationInfo> installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

            for (ApplicationInfo appInfo : installedApps) {
                try {
                    if (appInfo.packageName.equals(context.getPackageName()) || pm.getLaunchIntentForPackage(appInfo.packageName) == null) continue;

                    AppModel app = new AppModel(
                            appInfo.loadLabel(pm).toString(),
                            appInfo.packageName,
                            appInfo.loadIcon(pm)
                    );

                    if (DEFAULT_BLOCKED_PACKAGES.contains(app.getPackageName())) {
                        localDefault.add(app);
                    } else {
                        if (userBlockedPkgs.contains(app.getPackageName())) {
                            app.setChecked(true);
                            localManual.add(app);
                        }
                        localAll.add(app);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error loading app: " + appInfo.packageName, e);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            StudyModeFragment fragment = fragmentReference.get();
            if (fragment == null || !fragment.isAdded()) return;

            fragment.defaultApps.clear();
            fragment.defaultApps.addAll(localDefault);
            Collections.sort(fragment.defaultApps, (a, b) -> a.getAppName().compareToIgnoreCase(b.getAppName()));
            if (fragment.defaultAdapter != null) fragment.defaultAdapter.notifyDataSetChanged();

            fragment.manuallyBlockedApps.clear();
            fragment.manuallyBlockedApps.addAll(localManual);
            Collections.sort(fragment.manuallyBlockedApps, (a, b) -> a.getAppName().compareToIgnoreCase(b.getAppName()));
            if (fragment.manuallyBlockedAdapter != null) fragment.manuallyBlockedAdapter.notifyDataSetChanged();

            fragment.allAppsList.clear();
            fragment.allAppsList.addAll(localAll);
            Collections.sort(fragment.allAppsList, (a, b) -> a.getAppName().compareToIgnoreCase(b.getAppName()));
            if (fragment.allAppsAdapter != null) fragment.allAppsAdapter.notifyDataSetChanged();
        }
    }

    private void setupListeners() {
        btnSaveApps.setOnClickListener(v -> {
            if (prefs == null) return;
            
            if (prefs.getBoolean("isStudyModeActive", false)) {
                Toast.makeText(getContext(), "Cannot edit while Focus Mode is active!", Toast.LENGTH_SHORT).show();
                return;
            }

            Set<String> selectedPkgs = new HashSet<>();
            for (AppModel app : allAppsList) {
                if (app.isChecked()) {
                    selectedPkgs.add(app.getPackageName());
                }
            }

            // Save locally
            prefs.edit().putStringSet("userBlockedApps", selectedPkgs).apply();
            
            // Save to Firebase
            if (databaseRef != null) {
                databaseRef.setValue(new ArrayList<>(selectedPkgs))
                    .addOnSuccessListener(aVoid -> {
                        if (isAdded()) Toast.makeText(getContext(), "Selection saved to Cloud", Toast.LENGTH_SHORT).show();
                    });
            }

            fetchBlockedAppsFromFirebase(); // Re-fetch and categorize
        });

        switchFocus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Context context = getContext();
            if (context == null || prefs == null) return;

            if (isChecked) {
                if (!PermissionUtils.isAccessibilityServiceEnabled(context, AppBlockAccessibilityService.class)) {
                    Toast.makeText(context, "Enable Accessibility first", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    switchFocus.setChecked(false);
                    return;
                }
                long endTime = System.currentTimeMillis() + (numberPicker.getValue() * 60 * 1000L);
                prefs.edit().putBoolean("isStudyModeActive", true).putLong("studyModeEndTime", endTime).apply();
                lockUI();
            }
        });
    }

    private void lockUI() {
        switchFocus.setEnabled(false);
        numberPicker.setEnabled(false);
        btnSaveApps.setEnabled(false);
    }

    private void setupTimer() {
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(180);
        numberPicker.setValue(30);
    }
}
