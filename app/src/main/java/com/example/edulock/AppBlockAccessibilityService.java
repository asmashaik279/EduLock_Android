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
 * Specifically optimized for long-term stability and automatic recovery.
 */
public class AppBlockAccessibilityService extends AccessibilityService {

    private static final String TAG = "EduLockBlocker";
    private static final String CHANNEL_ID = "EduLock_Focus_Permanent_Channel";
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
        Log.d(TAG, "Service Connected and Active");
        createNotificationChannel();
        refreshServiceState();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // START_STICKY ensures the system restarts the service if it's killed
        refreshServiceState();
        return START_STICKY;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        // Ensure state is correct on every window change
        refreshServiceState();

        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }

        CharSequence pkgName = event.getPackageName();
        if (pkgName == null) return;

        String currentApp = pkgName.toString();
        String myPackage = getPackageName();

        // 1. NEVER block EduLock
        if (currentApp.equalsIgnoreCase(myPackage)) {
            return;
        }

        // 2. Ignore system components to prevent device lock-outs
        if (currentApp.equals("android") || 
            currentApp.contains("launcher") || 
            currentApp.contains("systemui") ||
            currentApp.contains("settings") ||
            currentApp.contains("miui")) {
            return;
        }

        SharedPreferences prefs = getSharedPreferences("EduLockPrefs", MODE_PRIVATE);
        boolean isActive = prefs.getBoolean("isStudyModeActive", false);

        if (!isActive) return;

        long endTime = prefs.getLong("studyModeEndTime", 0);
        if (System.currentTimeMillis() > endTime) {
            prefs.edit().putBoolean("isStudyModeActive", false).apply();
            refreshServiceState();
            return;
        }

        Set<String> manuallyBlocked = prefs.getStringSet("userBlockedApps", new HashSet<>());

        // 3. Blocking logic
        if (DEFAULT_BLOCKED_PACKAGES.contains(currentApp) || manuallyBlocked.contains(currentApp)) {
            Log.d(TAG, "Blocking restricted app: " + currentApp);
            
            performGlobalAction(GLOBAL_ACTION_HOME);
            
            Intent intent = new Intent(this, BlockedAppActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
    }

    private void refreshServiceState() {
        SharedPreferences prefs = getSharedPreferences("EduLockPrefs", MODE_PRIVATE);
        boolean isActive = prefs.getBoolean("isStudyModeActive", false);
        
        // Double check expiration
        long endTime = prefs.getLong("studyModeEndTime", 0);
        if (isActive && System.currentTimeMillis() > endTime) {
            prefs.edit().putBoolean("isStudyModeActive", false).apply();
            isActive = false;
        }

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
                .setContentTitle("EduLock Blocker is Active")
                .setContentText("Study Mode is currently protecting your focus.")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "EduLock Core Service",
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
        isForeground = false;
        return super.onUnbind(intent);
    }
}
