package com.aula.virtual.data.local;

import android.content.Context;
import android.os.Environment;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LocalDatabaseBackupManager {

    public interface BackupCallback {
        void onSuccess(String message, String filePath);
        void onError(String error);
    }

    public static void exportBackupToJsonStreamSync(Context context, OutputStream outputStream) throws Exception {
        if (context == null || outputStream == null) {
            throw new IllegalArgumentException("Parámetros no válidos para la exportación.");
        }

        AppDatabase db = AppDatabase.getInstance(context);

        LocalBackupDto dto = new LocalBackupDto();
        dto.exportedAt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(new Date());

        // Populate DTO from Room DB DAOs
        dto.users = db.userDao().getAllUsers();
        dto.students = db.studentDao().getAllStudents();
        dto.subjects = db.subjectDao().getAllSubjects();
        dto.facilities = db.facilityDao().getAllFacilities();
        dto.enrollments = db.enrollmentDao().getAllEnrollments();
        dto.facilitySchedules = db.facilityScheduleDao().getAllSchedules();
        dto.blogEntries = db.blogDao().getAllEntries();
        dto.blogComments = db.blogDao().getAllComments();
        dto.notifications = db.notificationDao().getAllNotifications();
        dto.userSubjectColors = db.userSubjectColorDao().getAllColors();
        dto.directMessages = db.directMessageDao().getAllMessages();
        dto.gradeImportLogs = db.gradeImportLogDao().getAllLogs();
        dto.conductRecords = db.conductRecordDao().getAllRecords();
        dto.academicPeriods = db.academicCalendarDao().getAllPeriods();
        dto.nonSchoolDays = db.academicCalendarDao().getAllNonSchoolDays();
        dto.attendanceRecords = db.attendanceRecordDao().getAllRecords();
        dto.modificationLogs = db.modificationLogDao().getAllLogsList();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(dto);

        OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        writer.write(json);
        writer.flush();
    }

    public static void exportBackupToJsonStream(Context context, OutputStream outputStream, BackupCallback callback) {
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                exportBackupToJsonStreamSync(context, outputStream);
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            callback.onSuccess("Copia de seguridad local exportada exitosamente.", null));
                }
            } catch (Exception e) {
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            callback.onError("Error al exportar copia de seguridad: " + e.getMessage()));
                }
            }
        });
    }

    public static void generateAndAutoSaveBackupJson(Context context, BackupCallback callback) {
        if (context == null) {
            if (callback != null) callback.onError("Contexto no válido.");
            return;
        }

        String timeTag = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "Respaldo_Local_" + timeTag + ".json";

        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                File dir = new File(Environment.getExternalStorageDirectory(), "Aula Virtual/Respaldos Datos Locales");
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                String displayPath = "";
                File targetFile = null;

                if (dir.exists()) {
                    targetFile = new File(dir, fileName);
                    displayPath = "Almacenamiento interno/Aula Virtual/Respaldos Datos Locales/" + fileName;
                } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    try {
                        android.content.ContentValues values = new android.content.ContentValues();
                        values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                        values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/json");
                        values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Download/Aula Virtual/Respaldos Datos Locales");

                        android.net.Uri uri = context.getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                        if (uri != null) {
                            try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
                                exportBackupToJsonStreamSync(context, os);
                            }
                            final String mediaPath = "Almacenamiento interno/Download/Aula Virtual/Respaldos Datos Locales/" + fileName;
                            if (callback != null) {
                                new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                                        callback.onSuccess("Copia de seguridad local exportada exitosamente.", mediaPath));
                            }
                            return;
                        }
                    } catch (Exception ignored) {}
                }

                if (targetFile == null) {
                    File fallbackDir = new File(context.getExternalFilesDir(null), "Aula Virtual/Respaldos Datos Locales");
                    if (!fallbackDir.exists()) fallbackDir.mkdirs();
                    targetFile = new File(fallbackDir, fileName);
                    displayPath = "Archivos de la aplicación/Aula Virtual/Respaldos Datos Locales/" + fileName;
                }

                try (OutputStream os = new FileOutputStream(targetFile)) {
                    exportBackupToJsonStreamSync(context, os);
                }

                final String finalPath = displayPath;
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            callback.onSuccess("Copia de seguridad local exportada exitosamente.", finalPath));
                }

            } catch (Exception e) {
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            callback.onError("Error al exportar copia de seguridad: " + e.getMessage()));
                }
            }
        });
    }

    public static void importBackupFromJsonStream(Context context, InputStream inputStream, BackupCallback callback) {
        if (context == null || inputStream == null) {
            if (callback != null) callback.onError("Parámetros no válidos para la importación.");
            return;
        }

        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                }

                String json = sb.toString();
                Gson gson = new Gson();
                LocalBackupDto dto = gson.fromJson(json, LocalBackupDto.class);

                if (dto == null || !"Aula Virtual Local Backup".equalsIgnoreCase(dto.app)) {
                    if (callback != null) {
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                                callback.onError("El archivo seleccionado no es una copia de seguridad válida de Aula Virtual."));
                    }
                    return;
                }

                AppDatabase db = AppDatabase.getInstance(context);

                // Run atomic restore transaction on local Room DB
                db.runInTransaction(() -> {
                    // Clear existing tables
                    db.clearAllTables();

                    // Restore data
                    if (dto.users != null && !dto.users.isEmpty()) {
                        for (com.aula.virtual.data.entity.User u : dto.users) {
                            if (u != null && u.role != null) {
                                u.role = u.role.trim().toUpperCase();
                            }
                        }
                        db.userDao().insertAll(dto.users);
                    }
                    if (dto.students != null && !dto.students.isEmpty()) db.studentDao().insertAll(dto.students);
                    if (dto.subjects != null && !dto.subjects.isEmpty()) db.subjectDao().insertAll(dto.subjects);
                    if (dto.facilities != null && !dto.facilities.isEmpty()) db.facilityDao().insertAll(dto.facilities);
                    if (dto.enrollments != null && !dto.enrollments.isEmpty()) db.enrollmentDao().insertAll(dto.enrollments);
                    if (dto.facilitySchedules != null && !dto.facilitySchedules.isEmpty()) db.facilityScheduleDao().insertAll(dto.facilitySchedules);
                    if (dto.blogEntries != null && !dto.blogEntries.isEmpty()) db.blogDao().insertEntries(dto.blogEntries);
                    if (dto.blogComments != null && !dto.blogComments.isEmpty()) db.blogDao().insertComments(dto.blogComments);
                    if (dto.notifications != null && !dto.notifications.isEmpty()) db.notificationDao().insertAll(dto.notifications);
                    if (dto.userSubjectColors != null && !dto.userSubjectColors.isEmpty()) db.userSubjectColorDao().insertAll(dto.userSubjectColors);
                    if (dto.directMessages != null && !dto.directMessages.isEmpty()) db.directMessageDao().insertAll(dto.directMessages);
                    if (dto.gradeImportLogs != null && !dto.gradeImportLogs.isEmpty()) db.gradeImportLogDao().insertAll(dto.gradeImportLogs);
                    if (dto.conductRecords != null && !dto.conductRecords.isEmpty()) db.conductRecordDao().insertAll(dto.conductRecords);
                    if (dto.academicPeriods != null && !dto.academicPeriods.isEmpty()) db.academicCalendarDao().insertPeriods(dto.academicPeriods);
                    if (dto.nonSchoolDays != null && !dto.nonSchoolDays.isEmpty()) db.academicCalendarDao().insertAllNonSchoolDays(dto.nonSchoolDays);
                    if (dto.attendanceRecords != null && !dto.attendanceRecords.isEmpty()) db.attendanceRecordDao().insertAll(dto.attendanceRecords);
                    if (dto.modificationLogs != null && !dto.modificationLogs.isEmpty()) db.modificationLogDao().insertAll(dto.modificationLogs);
                });

                String summary = "Copia de seguridad restaurada exitosamente.\n\nRegistros restaurados:\n" +
                        "• Usuarios: " + (dto.users != null ? dto.users.size() : 0) + "\n" +
                        "• Estudiantes: " + (dto.students != null ? dto.students.size() : 0) + "\n" +
                        "• Materias: " + (dto.subjects != null ? dto.subjects.size() : 0) + "\n" +
                        "• Asistencias: " + (dto.attendanceRecords != null ? dto.attendanceRecords.size() : 0) + "\n" +
                        "• Conducta: " + (dto.conductRecords != null ? dto.conductRecords.size() : 0);

                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            callback.onSuccess(summary, null));
                }

            } catch (Exception e) {
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            callback.onError("Error al restaurar la copia de seguridad: " + e.getMessage()));
                }
            }
        });
    }
}