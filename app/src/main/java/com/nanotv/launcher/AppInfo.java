package com.nanotv.launcher;

import android.content.Intent;
import android.graphics.drawable.Drawable;

public class AppInfo {
    private final String packageName;
    private final String label;
    private final Drawable icon;
    private final Intent launchIntent;

    public AppInfo(String packageName, String label, Drawable icon, Intent launchIntent) {
        this.packageName = packageName;
        this.label = label;
        this.icon = icon;
        this.launchIntent = launchIntent;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getLabel() {
        return label;
    }

    public Drawable getIcon() {
        return icon;
    }

    public Intent getLaunchIntent() {
        return launchIntent;
    }
}
