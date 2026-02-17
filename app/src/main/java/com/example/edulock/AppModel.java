package com.example.edulock;

import android.graphics.drawable.Drawable;

public class AppModel {
    private String appName;
    private String packageName;
    private Drawable icon;
    private boolean isDefault;

    public AppModel(String appName, String packageName, Drawable icon, boolean isDefault) {
        this.appName = appName;
        this.packageName = packageName;
        this.icon = icon;
        this.isDefault = isDefault;
    }

    public String getAppName() { return appName; }
    public String getPackageName() { return packageName; }
    public Drawable getIcon() { return icon; }
    public boolean isDefault() { return isDefault; }
}
