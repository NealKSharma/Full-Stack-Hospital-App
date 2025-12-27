package com.project.saintcyshospital.ws;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.project.saintcyshospital.AuthManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WebSocketClient {
    private int attempts = 0;
    private String lastUrl;
    private Context lastCtx;
    private Listener lastListener;
    private boolean enabled = true;
    private static int SOCKET_COUNTER = 0;
    private int socketId = -1;

    public interface Listener {
        void onOpen();
        void onMessage(String text);
        void onClosed(int code, String reason);
        void onFailure(Throwable t);
    }

    private final OkHttpClient client = new OkHttpClient.Builder()
            .pingInterval(java.time.Duration.ofSeconds(30))
            .build();

    private WebSocket socket;
    private Listener listener;

    public void connect(Context ctx, String url, Listener l) {
        this.lastCtx = ctx.getApplicationContext();
        this.lastUrl = url;
        this.lastListener = l;
        attempts = 0;

        this.listener = l;

        Log.d("WS_DEBUG", "connect() called. enabled=" + enabled);

        if (socket != null) {
            Log.d("WS_DEBUG", "connect(): canceling old socket id=" + socketId);
            socket.cancel();
        }

        socketId = ++SOCKET_COUNTER;
        Log.d("WS_DEBUG", "connect(): creating NEW socket id=" + socketId);

        String access = AuthManager.getAccessToken(ctx.getApplicationContext());
        Request.Builder rb = new Request.Builder().url(url);
        if (access != null && !access.isEmpty()) {
            rb.addHeader("Authorization", "Bearer " + access);
        }
        Request req = rb.build();

        socket = client.newWebSocket(req, new WebSocketListener() {
            private final Handler main = new Handler(Looper.getMainLooper());

            @Override public void onOpen(WebSocket ws, Response resp) {
                Log.d("WS_DEBUG", "onOpen(): socket id=" + socketId);
                attempts = 0;
                main.post(() -> { if (listener != null) listener.onOpen(); });
            }

            @Override public void onMessage(WebSocket ws, String text) {
                main.post(() -> { if (listener != null) listener.onMessage(text); });
            }

            @Override public void onClosed(WebSocket ws, int code, String reason) {
                Log.d("WS_DEBUG", "onClosed(): socket id=" + socketId + " code=" + code + " reason=" + reason);
                main.post(() -> { if (listener != null) listener.onClosed(code, reason); });
                if (!enabled) return;
                if (code == 1000) return;
                if (code == 1008 || code == 1003) return;
                scheduleReconnect();
            }

            @Override public void onFailure(WebSocket ws, Throwable t, Response r) {
                Log.d("WS_DEBUG", "onFailure(): socket id=" + socketId +
                        " http=" + (r != null ? r.code() : "none") +
                        " err=" + t);
                main.post(() -> { if (listener != null) listener.onFailure(t); });
                if (!enabled) return;
                if (r != null) {
                    int http = r.code();
                    if (http >= 400 && http < 500) return;
                }
                scheduleReconnect();
            }
        });
    }


    private void scheduleReconnect() {
        long delayMs = Math.min(30_000L, (long)Math.pow(2, Math.min(6, attempts++)) * 1000L);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (enabled && lastCtx != null && lastUrl != null && lastListener != null) {
                if (socket != null) socket.cancel();
                connect(lastCtx, lastUrl, lastListener);
            }
        }, delayMs);
    }

    public void disconnect() {
        if (socket != null) {
            Log.d("WS_DEBUG", "disconnect(): closing socket id=" + socketId);
            socket.close(1000, "Goodbye!");
            socket = null;
        } else {
            Log.d("WS_DEBUG", "disconnect(): NO socket");
        }
    }

    public boolean send(String text) {
        return socket != null && socket.send(text);
    }
    public void setEnabled(boolean on) { this.enabled = on; }
}
