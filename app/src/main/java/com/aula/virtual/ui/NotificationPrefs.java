package com.aula.virtual.ui;

import android.content.Context;
import android.content.SharedPreferences;

public class NotificationPrefs {
    private static final String PREFS_NAME = "notification_prefs";

    private static final String KEY_PUSH_ENABLED = "push_enabled";
    private static final String KEY_SOUND_ENABLED = "sound_enabled";
    private static final String KEY_VIBRATION_ENABLED = "vibration_enabled";

    private static final String KEY_CHANNEL_GENERAL = "channel_general";
    private static final String KEY_CHANNEL_SUBJECTS = "channel_subjects";
    private static final String KEY_CHANNEL_FACILITIES = "channel_facilities";
    private static final String KEY_CHANNEL_INSTITUTIONAL = "channel_institutional";

    private static final String KEY_PRIORITY_ALERTS = "priority_alerts";
    private static final String KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled";
    private static final String KEY_QUIET_HOURS_START = "quiet_hours_start";
    private static final String KEY_QUIET_HOURS_END = "quiet_hours_end";
    private static final String KEY_FREQUENCY = "frequency";

    public static final String FREQUENCY_INSTANT = "INSTANT";
    public static final String FREQUENCY_DAILY = "DAILY";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // 1. Interruptor General (Push Notifications)
    public static boolean isPushEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_PUSH_ENABLED, true);
    }

    public static void setPushEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_PUSH_ENABLED, enabled).apply();
    }

    // 2. Alertas de Sonido y Vibración
    public static boolean isSoundEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_SOUND_ENABLED, true);
    }

    public static void setSoundEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply();
    }

    public static boolean isVibrationEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_VIBRATION_ENABLED, true);
    }

    public static void setVibrationEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply();
    }

    // 3. Filtros por Canal
    public static boolean isChannelGeneralEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_CHANNEL_GENERAL, true);
    }

    public static void setChannelGeneralEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_CHANNEL_GENERAL, enabled).apply();
    }

    public static boolean isChannelSubjectsEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_CHANNEL_SUBJECTS, true);
    }

    public static void setChannelSubjectsEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_CHANNEL_SUBJECTS, enabled).apply();
    }

    public static boolean isChannelFacilitiesEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_CHANNEL_FACILITIES, true);
    }

    public static void setChannelFacilitiesEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_CHANNEL_FACILITIES, enabled).apply();
    }

    public static boolean isChannelInstitutionalEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_CHANNEL_INSTITUTIONAL, true);
    }

    public static void setChannelInstitutionalEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_CHANNEL_INSTITUTIONAL, enabled).apply();
    }

    // 4. Excepción para Avisos Urgentes ("Priorizar Alertas y Sistema")
    public static boolean isPriorityAlertsEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_PRIORITY_ALERTS, true);
    }

    public static void setPriorityAlertsEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_PRIORITY_ALERTS, enabled).apply();
    }

    // 5. Modo No Molestar / Horario Silencioso
    public static boolean isQuietHoursEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_QUIET_HOURS_ENABLED, false);
    }

    public static void setQuietHoursEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_QUIET_HOURS_ENABLED, enabled).apply();
    }

    public static String getQuietHoursStart(Context context) {
        return getPrefs(context).getString(KEY_QUIET_HOURS_START, "21:00");
    }

    public static void setQuietHoursStart(Context context, String time) {
        getPrefs(context).edit().putString(KEY_QUIET_HOURS_START, time).apply();
    }

    public static String getQuietHoursEnd(Context context) {
        return getPrefs(context).getString(KEY_QUIET_HOURS_END, "06:00");
    }

    public static void setQuietHoursEnd(Context context, String time) {
        getPrefs(context).edit().putString(KEY_QUIET_HOURS_END, time).apply();
    }

    // 6. Resumen de Actividad / Frecuencia de Entrega
    public static String getFrequency(Context context) {
        return getPrefs(context).getString(KEY_FREQUENCY, FREQUENCY_INSTANT);
    }

    public static void setFrequency(Context context, String frequency) {
        getPrefs(context).edit().putString(KEY_FREQUENCY, frequency).apply();
    }
}
