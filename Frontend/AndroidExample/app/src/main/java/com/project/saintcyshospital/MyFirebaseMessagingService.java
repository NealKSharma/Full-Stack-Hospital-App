package com.project.saintcyshospital;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "notif_fcm";
    private static final String GROUP_KEY_NOTIFS = "scys.notifs.group";
    private static final int SUMMARY_ID = 100000;
    private static int nextId = 1;
    public static Integer lastCallNotificationId = null;

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        android.util.Log.d("FCM_DEBUG", "onNewToken: " + token);

        if (AuthManager.isLoggedIn(getApplicationContext())) {
            AuthManager.registerFcmToken(getApplicationContext(), token);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        android.util.Log.d(
                "FCM_DEBUG",
                "onMessageReceived: from=" + remoteMessage.getFrom()
                        + " data=" + remoteMessage.getData()
                        + " notif=" + (remoteMessage.getNotification() != null
                        ? remoteMessage.getNotification().getBody()
                        : "null")
        );

        String title = "Hospital update";
        String body  = "You have a new notification";

        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null) {
                title = remoteMessage.getNotification().getTitle();
            }
            if (remoteMessage.getNotification().getBody() != null) {
                body = remoteMessage.getNotification().getBody();
            }
        }

        String type = null;
        String conversationId = null;

        if (remoteMessage.getData() != null && !remoteMessage.getData().isEmpty()) {
            if (remoteMessage.getData().containsKey("title")) {
                title = remoteMessage.getData().get("title");
            }
            if (remoteMessage.getData().containsKey("content")) {
                body = remoteMessage.getData().get("content");
            }
            if (remoteMessage.getData().containsKey("type")) {
                type = remoteMessage.getData().get("type");
            }
            if (remoteMessage.getData().containsKey("conversationId")) {
                conversationId = remoteMessage.getData().get("conversationId");
            }
        }

        Context ctx = this;
        Intent targetIntent;

        boolean isCall = false;
        if ("CALL".equalsIgnoreCase(type) && conversationId != null && !conversationId.isEmpty()) {
            targetIntent = new Intent(ctx, VoiceCallActivity.class);
            targetIntent.putExtra("conversationId", conversationId);
            targetIntent.putExtra("isCaller", false);
            isCall = true;
        } else {

            targetIntent = new Intent(ctx, HomeActivity.class);
        }

        showNotification(ctx, title, body, targetIntent, isCall);
    }


    private void showNotification(Context ctx, String title, String body, Intent intent, boolean isCall) {
        NotificationManager nm = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "All Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            ch.enableLights(true);
            ch.enableVibration(true);
            ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            nm.createNotificationChannel(ch);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                ctx,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT |
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                                ? PendingIntent.FLAG_IMMUTABLE
                                : 0)
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_SOUND)
                .setVibrate(new long[]{0, 200, 100, 200})
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(isCall ? NotificationCompat.CATEGORY_CALL
                        : NotificationCompat.CATEGORY_MESSAGE);
        int id = nextId++;

        if (isCall) {
            lastCallNotificationId = id;
            builder.setOngoing(true);
        }

        nm.notify(id, builder.build());
    }

}
