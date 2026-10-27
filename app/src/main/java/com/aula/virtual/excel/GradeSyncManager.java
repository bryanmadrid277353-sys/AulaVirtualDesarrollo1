package com.aula.virtual.excel;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.aula.virtual.data.entity.GradeImportLog;
import com.aula.virtual.data.entity.Subject;
import com.aula.virtual.data.local.AppDatabase;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GradeSyncManager {
    private static final String PREFS_NAME = "grade_excel_sync_prefs";
    private static final String KEY_URI_PREFIX = "linked_excel_uri_subject_";
    private static final String PERIODIC_WORK_TAG = "grade_excel_sync_worker";

    private static final Map<Integer, Runnable> pendingDebounceMap = new HashMap<>();
    private static final Handler debounceHandler = new Handler(Looper.getMainLooper());

    public interface SyncCallback {
        void onSyncCompleted(ImportResult result);
        void onSyncSkipped(String reason);
        void onError(String error);
    }

    public static void savePersistableUri(Context context, int subjectId, Uri uri) {
        if (uri == null) return;
        try {
            int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
            context.getContentResolver().takePersistableUriPermission(uri, takeFlags);
        } catch (Exception ignored) {}

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_URI_PREFIX + subjectId, uri.toString()).apply();
    }

    public static Uri getLinkedUri(Context context, int subjectId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String uriStr = prefs.getString(KEY_URI_PREFIX + subjectId, null);
        if (uriStr == null || uriStr.isEmpty()) return null;
        try {
            return Uri.parse(uriStr);
        } catch (Exception e) {
            return null;
        }
    }

    public static void clearLinkedUri(Context context, int subjectId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_URI_PREFIX + subjectId).apply();
    }

    public static void syncWithDebounce(Context context, Subject subject, Uri fileUri, long delayMillis, SyncCallback callback) {
        if (subject == null || fileUri == null) {
            if (callback != null) callback.onError("Materia o archivo inválido.");
            return;
        }

        int subjectId = subject.id;
        Runnable existing = pendingDebounceMap.get(subjectId);
        if (existing != null) {
            debounceHandler.removeCallbacks(existing);
        }

        Runnable newRunnable = () -> syncNowInternal(context, subject, fileUri, callback);
        pendingDebounceMap.put(subjectId, newRunnable);
        debounceHandler.postDelayed(newRunnable, delayMillis > 0 ? delayMillis : 3000);
    }

    public static void syncNowInternal(Context context, Subject subject, Uri fileUri, SyncCallback callback) {
        if (context == null || subject == null || fileUri == null) {
            if (callback != null) callback.onError("Parámetros inválidos para sincronización.");
            return;
        }

        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                savePersistableUri(context, subject.id, fileUri);

                InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
                if (inputStream == null) {
                    if (callback != null) {
                        debounceHandler.post(() -> callback.onError("No se pudo abrir el archivo seleccionado."));
                    }
                    return;
                }

                byte[] bytes;
                try {
                    bytes = readAllBytes(inputStream);
                } finally {
                    inputStream.close();
                }

                String currentHash = GradeExcelImporter.calculateSHA256(bytes);

                AppDatabase db = AppDatabase.getInstance(context);
                GradeImportLog lastLog = db.gradeImportLogDao().getLastLogForSubject(subject.id);

                if (lastLog != null && currentHash.equals(lastLog.fileHash) && lastLog.rowsError == 0) {
                    if (callback != null) {
                        debounceHandler.post(() -> callback.onSyncSkipped("El archivo no presenta cambios con respecto a la última importación exitosa."));
                    }
                    return;
                }

                InputStream importStream = new java.io.ByteArrayInputStream(bytes);
                String fileName = getFileNameFromUri(context, fileUri);

                ImportResult result = GradeExcelImporter.importGradesFromStream(context, importStream, fileName, subject);
                if (callback != null) {
                    debounceHandler.post(() -> callback.onSyncCompleted(result));
                }

            } catch (Exception e) {
                if (callback != null) {
                    debounceHandler.post(() -> callback.onError("Error durante la sincronización: " + e.getMessage()));
                }
            }
        });
    }

    public static void schedulePeriodicSyncWorker(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                GradeSyncWorker.class,
                15, TimeUnit.MINUTES
        )
        .setConstraints(constraints)
        .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );
    }

    private static String getFileNameFromUri(Context context, Uri uri) {
        String name = "plantilla_notas.xlsx";
        try (android.database.Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    name = cursor.getString(nameIndex);
                }
            }
        } catch (Exception ignored) {}
        return name;
    }

    private static byte[] readAllBytes(InputStream inputStream) throws Exception {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[8192];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }
}
