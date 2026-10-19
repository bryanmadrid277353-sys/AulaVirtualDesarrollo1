package com.aula.virtual.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.aula.virtual.MainActivity;
import com.aula.virtual.R;
import com.aula.virtual.data.entity.Notification;
import com.aula.virtual.data.local.AppDatabase;
import com.aula.virtual.ui.NotificationPrefs;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class VirtualFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    public static final String CHANNEL_ID = "aula_virtual_push_channel";
    public static final String CHANNEL_NAME = "Avisos Aula Virtual";

    public static int getNotificationIconRes(String iconType) {
        if (iconType == null) return R.drawable.ic_megaphone;
        switch (iconType.toUpperCase()) {
            case "ACADEMIC":
                return R.drawable.ic_person;
            case "ALERT":
                return R.drawable.ic_notification; // Campana
            case "SYSTEM":
                return R.drawable.ic_settings;
            case "SUBJECT":
                return R.drawable.ic_open_book;
            case "GENERAL":
            default:
                return R.drawable.ic_megaphone; // Megáfono
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = "Aula Virtual";
        String message = "Nuevo aviso institucional";
        String iconType = "GENERAL";
        String targetType = "ALL";
        String targetValue = "";
        String senderName = "Sistema";

        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null) {
                title = remoteMessage.getNotification().getTitle();
            }
            if (remoteMessage.getNotification().getBody() != null) {
                message = remoteMessage.getNotification().getBody();
            }
        }
        
        if (remoteMessage.getData().size() > 0) {
            if (remoteMessage.getData().containsKey("title")) {
                title = remoteMessage.getData().get("title");
            }
            if (remoteMessage.getData().containsKey("message")) {
                message = remoteMessage.getData().get("message");
            }
            if (remoteMessage.getData().containsKey("iconType")) {
                iconType = remoteMessage.getData().get("iconType");
            }
            if (remoteMessage.getData().containsKey("targetType")) {
                targetType = remoteMessage.getData().get("targetType");
            }
            if (remoteMessage.getData().containsKey("targetValue")) {
                targetValue = remoteMessage.getData().get("targetValue");
            }
            if (remoteMessage.getData().containsKey("senderName")) {
                senderName = remoteMessage.getData().get("senderName");
            }
        }

        // 1. Guardar la notificación recibida remotamente en la base de datos local (Room)
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        Notification notif = new Notification(title, message, targetType, targetValue, senderName, timeStamp, iconType);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase.getInstance(getApplicationContext()).notificationDao().insertNotification(notif);
            } catch (Exception e) {
                Log.e(TAG, "Error al guardar notificación remota en Room", e);
            }
        });

        // 2. Respetar preferencias de notificaciones push del usuario
        if (!NotificationPrefs.isPushEnabled(getApplicationContext())) {
            return;
        }

        // 3. Mostrar notificación del sistema
        showNotification(this, title, message, iconType, (int) System.currentTimeMillis());
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Nuevo FCM Token de dispositivo: " + token);
        getApplicationContext().getSharedPreferences("fcm_prefs", MODE_PRIVATE)
                .edit()
                .putString("fcm_token", token)
                .apply();
    }

    public static void createNotificationChannel(Context context) {
        getNotificationChannelId(context);
    }

    public static String getNotificationChannelId(Context context) {
        boolean isSound = NotificationPrefs.isSoundEnabled(context);
        boolean isVibration = NotificationPrefs.isVibrationEnabled(context);

        if (NotificationPrefs.isQuietHoursEnabled(context) && isCurrentTimeInQuietHours(context)) {
            if (!NotificationPrefs.isPriorityAlertsEnabled(context)) {
                isSound = false;
                isVibration = false;
            }
        }

        String activeChannelId = CHANNEL_ID + "_s" + (isSound ? "1" : "0") + "_v" + (isVibration ? "1" : "0");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                int importance = (isSound || isVibration) ? NotificationManager.IMPORTANCE_HIGH : NotificationManager.IMPORTANCE_LOW;
                
                NotificationChannel channel = new NotificationChannel(
                        activeChannelId,
                        CHANNEL_NAME,
                        importance
                );
                channel.setDescription("Canal de notificaciones push para Aula Virtual");

                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build();

                if (isSound) {
                    Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    channel.setSound(defaultSoundUri, audioAttributes);
                } else {
                    channel.setSound(null, null);
                }

                channel.enableVibration(isVibration);
                if (isVibration) {
                    channel.setVibrationPattern(new long[]{0, 250, 250, 250});
                } else {
                    channel.setVibrationPattern(new long[]{0});
                }

                notificationManager.createNotificationChannel(channel);
            }
        }

        return activeChannelId;
    }

    public static boolean isCurrentTimeInQuietHours(Context context) {
        try {
            String start = NotificationPrefs.getQuietHoursStart(context);
            String end = NotificationPrefs.getQuietHoursEnd(context);
            if (start == null || end == null) return false;

            String[] sParts = start.split(":");
            String[] eParts = end.split(":");
            if (sParts.length < 2 || eParts.length < 2) return false;

            int startMin = Integer.parseInt(sParts[0].trim()) * 60 + Integer.parseInt(sParts[1].trim());
            int endMin = Integer.parseInt(eParts[0].trim()) * 60 + Integer.parseInt(eParts[1].trim());

            Calendar cal = Calendar.getInstance();
            int curMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);

            if (startMin < endMin) {
                return curMin >= startMin && curMin < endMin;
            } else {
                return curMin >= startMin || curMin < endMin;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static void showNotification(Context context, String title, String message, String iconType, int notificationId) {
        if (!NotificationPrefs.isPushEnabled(context)) return;

        String channelId = getNotificationChannelId(context);
        boolean isSound = NotificationPrefs.isSoundEnabled(context);
        boolean isVibration = NotificationPrefs.isVibrationEnabled(context);

        if (NotificationPrefs.isQuietHoursEnabled(context) && isCurrentTimeInQuietHours(context)) {
            if (!NotificationPrefs.isPriorityAlertsEnabled(context)) {
                isSound = false;
                isVibration = false;
            }
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        int iconRes = getNotificationIconRes(iconType);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(iconRes)
                .setContentTitle(title != null ? title : "Notificación")
                .setContentText(message != null ? message : "")
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (isSound && isVibration) {
            builder.setDefaults(NotificationCompat.DEFAULT_ALL);
            builder.setPriority(NotificationCompat.PRIORITY_HIGH);
            builder.setVibrate(new long[]{0, 250, 250, 250});
        } else if (isSound) {
            builder.setDefaults(NotificationCompat.DEFAULT_SOUND);
            builder.setPriority(NotificationCompat.PRIORITY_HIGH);
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            builder.setVibrate(new long[]{0});
        } else if (isVibration) {
            builder.setDefaults(NotificationCompat.DEFAULT_VIBRATE);
            builder.setPriority(NotificationCompat.PRIORITY_HIGH);
            builder.setSound(null);
            builder.setVibrate(new long[]{0, 250, 250, 250});
        } else {
            builder.setDefaults(0);
            builder.setPriority(NotificationCompat.PRIORITY_LOW);
            builder.setSound(null);
            builder.setVibrate(new long[]{0});
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(notificationId, builder.build());
        }
    }
}
