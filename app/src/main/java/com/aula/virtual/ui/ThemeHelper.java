package com.aula.virtual.ui;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.aula.virtual.R;

public class ThemeHelper {
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_IS_DARK_MODE = "is_dark_mode";
    private static final String KEY_ACCENT_COLOR = "accent_color";
    private static final String KEY_NAVBAR_POSITION = "navbar_position";
    private static final String KEY_DATA_MODE = "data_mode";
    private static final String KEY_BIOMETRIC_AUTOFILL = "biometric_autofill";

    public static boolean isBiometricAutofillEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_BIOMETRIC_AUTOFILL, true);
    }

    public static void setBiometricAutofillEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_BIOMETRIC_AUTOFILL, enabled).apply();
    }

    public static final String NAVBAR_POSITION_TOP = "TOP";
    public static final String NAVBAR_POSITION_BOTTOM = "BOTTOM";

    public static final String MODE_LOCAL = "LOCAL";
    public static final String MODE_SERVER = "SERVER";

    public static String getDataMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_DATA_MODE, MODE_SERVER);
    }

    public static void setDataMode(Context context, String mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_DATA_MODE, mode).apply();
    }

    public static boolean isLocalMode(Context context) {
        return MODE_LOCAL.equals(getDataMode(context));
    }

    public static String getNavbarPosition(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_NAVBAR_POSITION, NAVBAR_POSITION_TOP);
    }

    public static void setNavbarPosition(Context context, String position) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_NAVBAR_POSITION, position).apply();
    }

    public static void applyTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        boolean isDarkMode = prefs.getBoolean(KEY_IS_DARK_MODE, false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        context.setTheme(getAccentTheme(context));
    }

    public static int getAccentTheme(Context context) {
        return R.style.Theme_MyApplication_Blue;
    }

    public static void setAccentColor(Context context, String color) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ACCENT_COLOR, "BLUE").apply();
    }

    public static String getAccentColorName(Context context) {
        return "BLUE";
    }

    public static void setDarkMode(Context context, boolean isDark) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_IS_DARK_MODE, isDark).apply();
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    public static boolean isDarkMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_IS_DARK_MODE, false);
    }

    public static int getSubjectColor(Context context, String colorName) {
        if (colorName == null) return ContextCompat.getColor(context, R.color.accent_blue);
        switch (colorName) {
            case "BLUE_LIGHT": return ContextCompat.getColor(context, R.color.accent_blue_light);
            case "BLUE": return ContextCompat.getColor(context, R.color.accent_blue);
            case "BLUE_DARK": return ContextCompat.getColor(context, R.color.accent_blue_dark);
            case "RED_LIGHT": return ContextCompat.getColor(context, R.color.accent_red_light);
            case "RED": return ContextCompat.getColor(context, R.color.accent_red);
            case "RED_DARK": return ContextCompat.getColor(context, R.color.accent_red_dark);
            case "GREEN_LIGHT": return ContextCompat.getColor(context, R.color.accent_green_light);
            case "GREEN": return ContextCompat.getColor(context, R.color.accent_green);
            case "GREEN_DARK": return ContextCompat.getColor(context, R.color.accent_green_dark);
            case "PURPLE_LIGHT": return ContextCompat.getColor(context, R.color.accent_purple_light);
            case "PURPLE": return ContextCompat.getColor(context, R.color.accent_purple);
            case "PURPLE_DARK": return ContextCompat.getColor(context, R.color.accent_purple_dark);
            case "CYAN_LIGHT": return ContextCompat.getColor(context, R.color.accent_cyan_light);
            case "CYAN": return ContextCompat.getColor(context, R.color.accent_cyan);
            case "CYAN_DARK": return ContextCompat.getColor(context, R.color.accent_cyan_dark);
            case "YELLOW_LIGHT": return ContextCompat.getColor(context, R.color.accent_yellow_light);
            case "YELLOW": return ContextCompat.getColor(context, R.color.accent_yellow);
            case "YELLOW_DARK": return ContextCompat.getColor(context, R.color.accent_yellow_dark);
            default: return ContextCompat.getColor(context, R.color.accent_blue);
        }
    }

    public static boolean isColorLight(int color) {
        double darkness = 1 - (0.299 * ((color >> 16) & 0xFF) + 0.587 * ((color >> 8) & 0xFF) + 0.114 * (color & 0xFF)) / 255;
        return darkness < 0.5;
    }
}
