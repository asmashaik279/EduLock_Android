package com.example.edulock;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import androidx.core.app.NotificationCompat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Enhanced Accessibility Service for app blocking.
 * Specifically optimized to prevent self-blocking when accessed from Recents.
 */
public class AppBlockAccessibilityService extends AccessibilityService {

    private static final String TAG = "EduLockBlocker";
    private static final String CHANNEL_ID = "EduLock_Focus_Status_Channel";
    private static final int NOTIFICATION_ID = 101;

    private static final Set<String> DEFAULT_BLOCKED_PACKAGES = new HashSet<>(Arrays.asList(
            "com.instagram.android",
            "com.snapchat.android",
            "com.google.android.youtube",
            "com.google.android.apps.youtube.music"
    ));

    private boolean isForeground = false;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        createNotificationChannel();
        checkAndToggleForeground();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }

        CharSequence pkgName = event.getPackageName();
        if (pkgName == null) return;

        String currentApp = pkgName.toString();
        String myPackage = getPackageName();

        // 1. ABSOLUTE EXCLUSION: Never process events from our own app
        if (currentApp.equals(myPackage)) {
            return;
        }

        // 2. SYSTEM EXCLUSION: Ignore launcher, system UI, and common MIUI components
        if (currentApp.equals("android") || 
            currentApp.contains("launcher") || 
            currentApp.contains("systemui") ||
            currentApp.contains("settings") ||
            currentApp.contains("miui.home") ||
            currentApp.contains("miui.recents")) {
            return;
        }

        SharedPreferences prefs = getSharedPreferences("EduLockPrefs", MODE_PRIVATE);
        boolean isActive = prefs.getBoolean("isStudyModeActive", false);

        if (!isActive) {
            if (isForeground) checkAndToggleForeground();
            return;
        }

        long endTime = prefs.getLong("studyModeEndTime", 0);
        if (System.currentTimeMillis() > endTime) {
            prefs.edit().putBoolean("isStudyModeActive", false).apply();
            checkAndToggleForeground();
            return;
        }

        // Ensure persistent notification is shown
        checkAndToggleForeground();

        Set<String> manuallyBlocked = prefs.getStringSet("userBlockedApps", new HashSet<>());

        // 3. BLOCKING LOGIC: Check both default and manual lists
        if (DEFAULT_BLOCKED_PACKAGES.contains(currentApp) || manuallyBlocked.contains(currentApp)) {
            Log.d(TAG, "Restricting app: " + currentApp);
            
            // Step A: Minimize the blocked app
            performGlobalAction(GLOBAL_ACTION_HOME);
            
            // Step B: Redirect to blocked info screen
            Intent intent = new Intent(this, BlockedAppActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
    }

    private void checkAndToggleForeground() {
        SharedPreferences prefs = getSharedPreferences("EduLockPrefs", MODE_PRIVATE);
        boolean isActive = prefs.getBoolean("isStudyModeActive", false);

        if (isActive && !isForeground) {
            startForeground(NOTIFICATION_ID, createNotification());
            isForeground = true;
        } else if (!isActive && isForeground) {
            stopForeground(true);
            isForeground = false;
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Focus Mode Active")
                .setContentText("Distracting apps are currently locked.")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Focus Mode Status",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onInterrupt() {}

    @Override
    public boolean onUnbind(Intent intent) {
        stopForeground(true);
        isForeground = false;
        return super.onUnbind(intent);
    }
}
