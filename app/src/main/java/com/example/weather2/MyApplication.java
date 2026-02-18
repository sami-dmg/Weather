package com.example.weather2;

import android.app.Application;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Load the saved theme
        int themeMode = ThemeHelper.loadTheme(this);
        // Apply the theme
        ThemeHelper.applyTheme(themeMode);
    }
}