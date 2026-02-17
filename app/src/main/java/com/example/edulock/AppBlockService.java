package com.example.edulock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AppBlockService extends Service {

    private static final String CHANNEL_ID = "EduLockBlockService";
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable checkerRunnable;
    private Set<String> blockedApps;
    private static final long CHECK_INTERVAL = 1000; // Check every second

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, getNotification());

        // Default blocked apps
        blockedApps = new HashSet<>(Arrays.asList(
                "com.instagram.android",
                "com.snapchat.android",
                "com.google.android.youtube"
        ));

        // Load user-added blocked apps from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("EduLockPrefs", MODE_PRIVATE);
        Set<String> userApps = prefs.getStringSet("userBlockedApps", new HashSet<>());
        blockedApps.addAll(userApps);

        checkerRunnable = new Runnable() {
            @Override
            public void run() {
                checkForegroundApp();
                handler.postDelayed(this, CHECK_INTERVAL);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handler.removeCallbacks(checkerRunnable);
        handler.post(checkerRunnable);
        return START_STICKY;
    }

    private void checkForegroundApp() {
        String foregroundApp = getForegroundPackageName();
        if (foregroundApp != null && blockedApps.contains(foregroundApp)) {
            Log.d("AppBlockService", "Blocking app: " + foregroundApp);
            Intent intent = new Intent(this, BlockedAppActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }

    private String getForegroundPackageName() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();
        UsageEvents usageEvents = usageStatsManager.queryEvents(time - 10000, time);
        UsageEvents.Event event = new UsageEvents.Event();
        String lastPackage = null;
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event);
            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastPackage = event.getPackageName();
            }
        }
        return lastPackage;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "EduLock Focus Mode",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification getNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Focus Mode is Active")
                .setContentText("EduLock is monitoring and blocking distractor apps.")
                .setSmallIcon(R.mipmap.ic_launcher) // Use your app icon
                .build();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(checkerRunnable);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
