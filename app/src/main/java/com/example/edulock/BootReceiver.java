package com.example.edulock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

            SharedPreferences prefs =
                    context.getSharedPreferences("STUDY_MODE", Context.MODE_PRIVATE);

            boolean isStudyModeOn = prefs.getBoolean("isStudyModeOn", false);

            // No need to start service
            // AccessibilityService will handle automatically
        }
    }
}
