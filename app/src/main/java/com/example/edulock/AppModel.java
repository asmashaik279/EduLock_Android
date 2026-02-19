package com.example.edulock;

import android.graphics.drawable.Drawable;

/**
 * Model class representing an installed app
 */
public class AppModel {

    private String appName;
    private String packageName;
    private Drawable icon;

    // 🔥 Added this to support checkbox state
    private boolean checked;

    public AppModel(String appName,
                    String packageName,
                    Drawable icon,
                    boolean checked) {
        this.appName = appName;
        this.packageName = packageName;
        this.icon = icon;
        this.checked = checked;
    }

    public AppModel(String appName, String packageName, Drawable icon) {
        this.appName = appName;
        this.packageName = packageName;
        this.icon = icon;
        this.checked = false;
    }

    // Getters
    public String getAppName() {
        return appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public boolean isChecked() {
        return checked;
    }

    // Setter for checkbox state
    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
