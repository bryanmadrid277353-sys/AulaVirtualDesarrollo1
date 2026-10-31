package com.aula.virtual.ui;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.aula.virtual.R;
import com.aula.virtual.data.local.AppDatabase;
import java.util.concurrent.Executors;

public class ThemeHelper {
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_IS_DARK_MODE = "is_dark_mode";
    private static final String KEY_ACCENT_COLOR = "accent_color";
    private static final String KEY_NAVBAR_POSITION = "navbar_position";
    private static final String KEY_DATA_MODE = "data_mode";
    private static final String KEY_BIOMETRIC_AUTOFILL = "biometric_autofill";
    private static final String KEY_HAPTIC_FEEDBACK = "haptic_feedback";

    public static boolean isHapticFeedbackEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true);
    }

    public static void setHapticFeedbackEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, enabled).apply();
    }

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

    public static void wipeAllResidualData(Context context) {
        if (context == null) return;
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                if (db != null) {
                    androidx.sqlite.db.SupportSQLiteDatabase sqliteDb = db.getOpenHelper().getWritableDatabase();
                    sqliteDb.execSQL("PRAGMA foreign_keys = OFF;");
                    sqliteDb.execSQL("DELETE FROM attendance_records;");
                    sqliteDb.execSQL("DELETE FROM conduct_records;");
                    sqliteDb.execSQL("DELETE FROM enrollments;");
                    sqliteDb.execSQL("DELETE FROM blog_comments;");
                    sqliteDb.execSQL("DELETE FROM blog_entries;");
                    sqliteDb.execSQL("DELETE FROM facility_schedules;");
                    sqliteDb.execSQL("DELETE FROM grade_import_log;");
                    sqliteDb.execSQL("DELETE FROM user_subject_colors;");
                    sqliteDb.execSQL("DELETE FROM notifications;");
                    sqliteDb.execSQL("DELETE FROM direct_messages;");
                    sqliteDb.execSQL("DELETE FROM non_school_days;");
                    sqliteDb.execSQL("DELETE FROM academic_periods;");
                    sqliteDb.execSQL("DELETE FROM facilities;");
                    sqliteDb.execSQL("DELETE FROM subjects;");
                    sqliteDb.execSQL("DELETE FROM students;");
                    sqliteDb.execSQL("DELETE FROM users;");
                    sqliteDb.execSQL("DELETE FROM conduct_levels;");
                    sqliteDb.execSQL("DELETE FROM attendance_states;");
                    sqliteDb.execSQL("DELETE FROM sqlite_sequence;");
                    sqliteDb.execSQL("PRAGMA foreign_keys = ON;");

                    db.clearAllTables();

                    // Re-seed reference tables
                    sqliteDb.execSQL("INSERT OR IGNORE INTO conduct_levels (value, label) VALUES (1, 'Malo'), (2, 'Regular'), (3, 'Bueno'), (4, 'Muy bueno'), (5, 'Excelente');");
                    sqliteDb.execSQL("INSERT OR IGNORE INTO attendance_states (value, label) VALUES (0, 'Sin evaluar'), (1, 'Asistió'), (2, 'No asistió'), (3, 'Justificado');");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void resetLocalSubjectsAndEnrollments(Context context) {
        if (context == null) return;
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                if (db != null) {
                    db.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM subjects WHERE id > 4 OR code NOT IN ('ASIGN10001', 'ASIGN10002', 'ASIGN10003', 'ASIGN10004');");
                    db.getOpenHelper().getWritableDatabase().execSQL("INSERT OR IGNORE INTO subjects (id, code, name, description, professorId, section, color) VALUES " +
                            "(1, 'ASIGN10001', 'Matemáticas 5º Grado', 'Aritmética, geometría básica, fracciones y resolución de problemas aplicados.', 2, '01', 'BLUE'), " +
                            "(2, 'ASIGN10002', 'Lenguaje y Literatura', 'Gramática, ortografía, redacción estructurada y comprensión lectora.', 3, '01', 'GREEN'), " +
                            "(3, 'ASIGN10003', 'Ciencias Naturales', 'Biología fundamental, ecosistemas, método científico y cuidado medioambiental.', 4, '01', 'YELLOW_DARK'), " +
                            "(4, 'ASIGN10004', 'Sociales e Historia', 'Geografía nacional, historia universal contemporánea y formación cívica.', 2, '01', 'PURPLE');");
                    db.getOpenHelper().getWritableDatabase().execSQL("INSERT OR IGNORE INTO enrollments (id, studentId, subjectId, grade1, grade2, grade3, grade4, grade5, color) VALUES " +
                            "(1, 1, 1, 8.5, 9.0, 8.0, NULL, NULL, 'BLUE'), " +
                            "(2, 1, 2, 9.0, 8.5, 9.5, NULL, NULL, 'GREEN'), " +
                            "(3, 1, 3, 8.0, 8.5, 9.0, NULL, NULL, 'YELLOW_DARK'), " +
                            "(4, 2, 1, 9.5, 10.0, 9.0, NULL, NULL, 'BLUE'), " +
                            "(5, 3, 1, 7.5, 8.0, 8.5, NULL, NULL, 'BLUE'), " +
                            "(6, 4, 4, 9.0, 9.2, 9.5, NULL, NULL, 'PURPLE');");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void clearAcademicCalendarLocalData(Context context) {
        if (context == null) return;
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                if (db != null && db.academicCalendarDao() != null) {
                    db.academicCalendarDao().deleteAllPeriods();
                    db.academicCalendarDao().deleteAllNonSchoolDays();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
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

    public static boolean isSystemDarkMode(Context context) {
        int currentNightMode = context.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    public static void applyTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        if (!prefs.contains(KEY_IS_DARK_MODE)) {
            boolean systemDark = isSystemDarkMode(context);
            AppCompatDelegate.setDefaultNightMode(systemDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            boolean isDarkMode = prefs.getBoolean(KEY_IS_DARK_MODE, false);
            if (isDarkMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
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
        if (!prefs.contains(KEY_IS_DARK_MODE)) {
            return isSystemDarkMode(context);
        }
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
