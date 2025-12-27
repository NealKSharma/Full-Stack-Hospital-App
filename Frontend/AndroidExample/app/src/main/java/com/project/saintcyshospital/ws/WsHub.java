package com.project.saintcyshospital.ws;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;


/**
 * App-wide notification websocket hub.
 *
 * <p>Maintains a single process-wide connection to <code>/notif</code>, posts system
 * notifications for server-pushed events, and broadcasts raw text payloads via
 * {@link #ACTION_WS_MESSAGE} intents.</p>
 *
 * <p>Use {@link #start(Context)} in each visible activity (we ref-count) and
 * {@link #stop()} in {@code onStop()}.</p>
 * @author Trice Buchanan
 */
public class WsHub {

    /** Broadcast action for delivered websocket messages (payload in {@link #EXTRA_TEXT}). */
    public static final String ACTION_WS_MESSAGE = "com.project.saintcyshospital.WS_MESSAGE";
    /** Intent extra key containing the text body of the notification. */
    public static final String EXTRA_TEXT = "text";
    /** Optional title for in-app notifications. */
    public static final String EXTRA_TITLE = "title";

    /**
     * Singleton instance of the websocket hub used throughout the app.
     */
    private static final WsHub INSTANCE = new WsHub();

    /**
     * Returns the shared {@link WsHub} instance.
     *
     * @return the process-wide websocket hub
     */
    public static WsHub get() { return INSTANCE; }

    /**
     * Underlying websocket client responsible for connecting to the
     * notifications endpoint and dispatching events to the listener.
     */
    private final WebSocketClient client = new WebSocketClient();

    /**
     * Application context captured on first {@link #start(Context)} call and
     * used for toasts, notifications, and broadcasts to avoid leaking
     * activity references.
     */
    private Context appCtx;

    /**
     * System {@link NotificationManager} used to post notifications for
     * incoming websocket messages.
     */
    private NotificationManager nm;

    /**
     * Count of active callers that have invoked {@link #start(Context)} and
     * not yet matched it with {@link #stop()}. When this reaches zero the
     * websocket connection is disabled and closed.
     */
    private int refCount = 0;

    /**
     * Websocket endpoint used for receiving live notification messages.
     */
    private static final String WS_URL = "ws://coms-3090-041.class.las.iastate.edu:8080/notif";

    /**
     * Notification channel id used for websocket-driven notifications.
     * Created on first {@link #start(Context)} call.
     */
    private static final String CHANNEL_ID_DEBUG = "notif_ws_debug";

    /**
     * Group key used to group individual notification messages into a single
     * summary notification in the status shade.
     */
    private static final String GROUP_KEY_NOTIFS = "scys.notifs.group";

    /**
     * Monotonically increasing id used to assign unique ids to each posted
     * notification so they do not overwrite each other.
     */
    private static int nextNotifyId = 1;

    /**
     * Fixed notification id for the summary notification that represents the
     * grouped set of websocket notifications.
     */
    private static final int SUMMARY_ID = 999000;



    /**
     * Increments the visible-screen ref count and ensures the websocket is connected.
     * Also creates the notification channel (id {@value #CHANNEL_ID_DEBUG}) once.
     *
     * @param ctx any context; the hub holds an application context internally
     */
    public synchronized void start(Context ctx) {
        if (appCtx == null) {
            appCtx = ctx.getApplicationContext();
            nm = (NotificationManager) appCtx.getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID_DEBUG,
                    "Live Notifications DEBUG",
                    NotificationManager.IMPORTANCE_HIGH
            );
            ch.enableVibration(true);
            ch.enableLights(true);
            nm.createNotificationChannel(ch);
        }

        Log.d("WS_DEBUG", "WsHub.start(): refCount BEFORE=" + refCount);
        refCount++;
        Log.d("WS_DEBUG", "WsHub.start(): refCount AFTER=" + refCount);

        if (refCount == 1) {
            Log.d("WS_DEBUG", "WsHub.start(): FIRST START → connect()");
            client.setEnabled(true);
            connect();
        } else {
            Log.d("WS_DEBUG", "WsHub.start(): already started → NO connect()");
        }
    }


    /**
     * Decrements the visible-screen ref count. When it reaches zero, the hub
     * disables and disconnects the websocket to avoid unnecessary reconnect loops.
     */
    public synchronized void stop() {
        if (refCount > 0) refCount--;
        if (refCount == 0) {
            client.setEnabled(false);
            client.disconnect();
        }
    }

    /**
     * Internal connect routine that builds the client listener and handles
     * onOpen/onMessage/onClosed/onFailure. Posts grouped notifications and emits
     * a broadcast ({@link #ACTION_WS_MESSAGE}) for screens that want the raw text.
     */
    private void connect() {
        client.connect(appCtx, WS_URL, new WebSocketClient.Listener() {
            /**
             * Called when the websocket connection is successfully opened.
             * Currently shows a simple toast for debugging/visibility.
             */
            @Override public void onOpen() {
                //Toast.makeText(appCtx.getApplicationContext(), "WS Connected", Toast.LENGTH_SHORT).show();
            }

            /**
             * Called whenever a text message is received from the server.
             * Attempts to parse a JSON payload for title/content, posts one
             * or more grouped system notifications, and broadcasts the body
             * via @link #ACTION_WS_MESSAGE.
             *
             * @param text raw message payload received from the websocket
             */
            @Override public void onMessage(String text) {
                String title = "Hospital update";
                String body  = text;
                try {
                    org.json.JSONObject o = new org.json.JSONObject(text);
                    title = o.optString("title", title);
                    body  = o.optString("content", body);
                } catch (Exception ignored) {}

                android.util.Log.d("WS_NOTIF", "WS message: " + title + " / " + body);

                android.content.Intent i = new android.content.Intent(ACTION_WS_MESSAGE);
                i.setPackage(appCtx.getPackageName());
                i.putExtra(EXTRA_TITLE, title);
                i.putExtra(EXTRA_TEXT, body);
                appCtx.sendBroadcast(i);
            }


            /**
             * Called when the websocket connection is closed by either side.
             * Used primarily for lightweight debug feedback via a toast.
             *
             * @param code   close status code
             * @param reason human-readable description of the close reason
             */
            @Override public void onClosed(int code, String reason) {
                //Toast.makeText(appCtx.getApplicationContext(), "WS Closed", Toast.LENGTH_SHORT).show();
            }

            /**
             * Called when the websocket connection encounters an error.
             * Currently logs the failure via a toast for visibility.
             *
             * @param t the underlying error encountered by the client
             */
            @Override public void onFailure(Throwable t) {
                //Toast.makeText(appCtx.getApplicationContext(), "WS Failure", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public synchronized void restart() {
        if (appCtx == null) return;

        Log.d("WS_DEBUG", "WsHub.restart(): refCount=" + refCount);

        if (refCount <= 0) {
            Log.d("WS_DEBUG", "WsHub.restart(): no active consumers → NO reconnect");
            return;
        }

        client.setEnabled(true);

        Log.d("WS_DEBUG", "WsHub.restart(): reconnecting websocket");
        client.disconnect();
        connect();
    }

}
