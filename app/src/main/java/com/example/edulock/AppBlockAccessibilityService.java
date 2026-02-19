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
 * Robust Accessibility Service for app blocking.
 * Optimized for blocking default and manually selected apps while ensuring
 * the app itself is never blocked.
 */
public class AppBlockAccessibilityService extends AccessibilityService {

    private static final String TAG = "EduLockBlocker";
    private static final String CHANNEL_ID = "EduLock_Focus_Service_Channel";
    private static final int NOTIFICATION_ID = 101;

    // Default apps that are always blocked in Focus Mode
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
        Log.d(TAG, "Accessibility Service connected");
        createNotificationChannel();
        checkAndToggleForeground();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        // Ensure service is in foreground if Focus Mode is active
        checkAndToggleForeground();

        // Listen for app window changes
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }

        CharSequence pkgName = event.getPackageName();
        if (pkgName == null) return;

        String currentApp = pkgName.toString();
        String myPackage = getPackageName();

        // 1. CRITICAL: Never block EduLock or essential system components
        if (currentApp.equals(myPackage) || 
            currentApp.equals("android") || 
            currentApp.contains("launcher") || 
            currentApp.contains("systemui") ||
            currentApp.contains("settings") ||
            currentApp.contains("packageinstaller") ||
            currentApp.contains("permissioncontroller")) {
            return;
        }

        SharedPreferences prefs = getSharedPreferences("EduLockPrefs", MODE_PRIVATE);
        boolean isActive = prefs.getBoolean("isStudyModeActive", false);

        if (!isActive) {
            return;
        }

        long endTime = prefs.getLong("studyModeEndTime", 0);
        if (System.currentTimeMillis() > endTime) {
            prefs.edit().putBoolean("isStudyModeActive", false).apply();
            checkAndToggleForeground();
            return;
        }

        // 2. Retrieve user-selected blocked apps
        Set<String> manuallyBlocked = prefs.getStringSet("userBlockedApps", new HashSet<>());

        // 3. Block if the app is in the default list OR the manual list
        if (DEFAULT_BLOCKED_PACKAGES.contains(currentApp) || manuallyBlocked.contains(currentApp)) {
            Log.d(TAG, "Blocking restricted app: " + currentApp);
            
            // Redirect to home screen
            performGlobalAction(GLOBAL_ACTION_HOME);
            
            // Show the custom Blocked screen
            Intent intent = new Intent(this, BlockedAppActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                          Intent.FLAG_ACTIVITY_CLEAR_TASK | 
                          Intent.FLAG_ACTIVITY_NO_ANIMATION);
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
                .setContentTitle("Focus Mode is Active")
                .setContentText("Your selected apps are currently locked.")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "EduLock Focus Status",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
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
