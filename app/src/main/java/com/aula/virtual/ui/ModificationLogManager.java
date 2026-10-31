package com.aula.virtual.ui;

import android.content.Context;
import com.aula.virtual.data.RetrofitClient;
import com.aula.virtual.data.entity.ModificationLog;
import com.aula.virtual.data.local.AppDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ModificationLogManager {

    public interface LogCallback {
        void onResult(String logText);
    }

    public static void saveLog(Context context, String entityKey, String userName) {
        if (context == null || entityKey == null || entityKey.trim().isEmpty()) return;

        boolean isLocal = ThemeHelper.isLocalMode(context);
        String timeStr = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        String authorStr = (userName != null && !userName.trim().isEmpty()) ? userName.trim() : "Usuario del sistema";
        String logText = "Última modificación: " + timeStr + " por " + authorStr;

        String keyToUse = isLocal ? ("local_" + entityKey.trim()) : entityKey.trim();
        ModificationLog log = new ModificationLog(keyToUse, logText, timeStr, authorStr);

        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase.getInstance(context).modificationLogDao().insertLog(log);
            } catch (Exception ignored) {}

            if (!isLocal) {
                try {
                    RetrofitClient.getApiService(context).saveModificationLog(log).execute();
                } catch (Exception ignored) {}
            }
        });
    }

    public static void loadLogTextAsync(Context context, String entityKey, LogCallback callback) {
        if (context == null || entityKey == null || entityKey.trim().isEmpty()) {
            if (callback != null) callback.onResult("Última modificación: Sin registros recientes");
            return;
        }

        boolean isLocal = ThemeHelper.isLocalMode(context);
        String keyToUse = isLocal ? ("local_" + entityKey.trim()) : entityKey.trim();

        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            // First check local Room cache for fast UI response
            String initialText = null;
            try {
                ModificationLog localLog = AppDatabase.getInstance(context).modificationLogDao().getLog(keyToUse);
                if (localLog != null && localLog.logText != null && !localLog.logText.isEmpty()) {
                    initialText = localLog.logText;
                }
            } catch (Exception ignored) {}

            if (initialText != null && callback != null) {
                final String fastLog = initialText;
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onResult(fastLog));
            }

            // In server mode, fetch latest official log from Retrofit API
            if (!isLocal) {
                try {
                    retrofit2.Response<ModificationLog> resp =
                            RetrofitClient.getApiService(context).getModificationLog(keyToUse).execute();
                    if (resp.isSuccessful() && resp.body() != null && resp.body().logText != null) {
                        ModificationLog serverLog = resp.body();
                        String serverLogText = serverLog.logText;
                        try {
                            AppDatabase.getInstance(context).modificationLogDao().insertLog(serverLog);
                        } catch (Exception ignored) {}

                        if (callback != null) {
                            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onResult(serverLogText));
                        }
                        return;
                    }
                } catch (Exception ignored) {}
            }

            if (initialText == null && callback != null) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                        callback.onResult("Última modificación: Sin registros recientes"));
            }
        });
    }
}