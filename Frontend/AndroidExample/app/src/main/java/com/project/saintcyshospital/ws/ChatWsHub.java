package com.project.saintcyshospital.ws;

import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.project.saintcyshospital.AuthManager;

import org.json.JSONObject;

/**
 * Room-scoped chat websocket hub.
 *
 * <p>Connects to <code>/chat</code>, sends a <code>CHAT_JOIN</code> for the active room,
 * relays incoming messages as local broadcasts (action {@link #ACTION_CHAT_MESSAGE})
 * that include {@link #EXTRA_CONVO_ID}, {@link #EXTRA_SENDER}, and {@link #EXTRA_CONTENT}.</p>
 *
 * <p>Use {@link #start(Context, String)} in {@code ChatDetailActivity.onStart()} and
 * {@link #stop()} in {@code onStop()}.</p>
* @author Trice Buchanan
 */
public class ChatWsHub {
    public interface CallSignalListener {
        /**
         * Called whenever a CALL_* signaling message is received
         * on the chat websocket.
         *
         * @param type the signaling type (CALL_OFFER, CALL_ANSWER, CALL_ICE, CALL_END)
         * @param json the raw JSON payload
         */
        void onCallSignal(String type, String json);
    }

    private CallSignalListener callSignalListener;

    public void setCallSignalListener(CallSignalListener listener) {
        this.callSignalListener = listener;
    }

    public void clearCallSignalListener() {
        this.callSignalListener = null;
    }

    /**
     * Local broadcast action for incoming chat messages delivered by the
     * chat websocket. Receivers can extract details using the {@code EXTRA_*}
     * keys defined in this class.
     */
    public static final String ACTION_CHAT_MESSAGE = "com.project.saintcyshospital.CHAT_MESSAGE";
    /**
     * Intent extra key containing the conversation / room identifier
     * associated with a chat message.
     */
    public static final String EXTRA_CONVO_ID = "conversationId";
    /**
     * Intent extra key containing the logical sender of the message
     * (e.g., "self", "peer", or a user identifier).
     */
    public static final String EXTRA_SENDER = "sender";
    /**
     * Intent extra key containing the message body/content of the chat
     * notification.
     */
    public static final String EXTRA_CONTENT = "content";
    /**
     * Singleton instance of the room-scoped chat websocket hub.
     */
    private static final ChatWsHub INSTANCE = new ChatWsHub();

    private boolean callActive = false;
    private boolean started = false;

    /**
     * Returns the shared {@link ChatWsHub} instance.
     *
     * @return the process-wide chat websocket hub
     */
    public static ChatWsHub get() { return INSTANCE; }
    /**
     * Underlying websocket client used to connect to the chat endpoint and
     * send/receive frames.
     */
    private final WebSocketClient client = new WebSocketClient();
    /**
     * Application context captured from the first {@link #start(Context, String)}
     * call. Used for broadcasting messages without leaking activity contexts.
     */
    private Context appCtx;
    /**
     * Currently joined conversation / room id. Used both when sending the
     * initial JOIN frame and when broadcasting incoming messages.
     */
    private String activeConversationId;

    /**
     * Starts (or restarts) the chat websocket for the given conversation id and immediately
     * sends a <code>CHAT_JOIN</code> frame.
     *
     * @param ctx context used to obtain the application context
     * @param conversationId the backend room id (e.g., "alice-bob")
     */
    public synchronized void start(Context ctx, String conversationId) {
        appCtx = ctx.getApplicationContext();

        if (started
                && activeConversationId != null
                && activeConversationId.equals(conversationId)) {
            return;
        }

        activeConversationId = conversationId;

        client.setEnabled(true);
        connect();
        started = true;
    }


    /**
     * Stops the chat websocket and clears the active room.
     * Safe to call multiple times.
     */
    public synchronized void stop() {
        client.setEnabled(false);
        client.disconnect();
        activeConversationId = null;
        started = false;
    }

    /**
     * Internal connect routine: builds the client listener, handles onOpen (JOIN),
     * parses incoming frames, and broadcasts them to the UI layer.
     */
    private void connect() {
        if (activeConversationId == null) return;

        final String WS_CHAT_URL = "ws://coms-3090-041.class.las.iastate.edu:8080/chat";

        client.connect(appCtx, WS_CHAT_URL, new WebSocketClient.Listener() {
            /**
             * Called when the chat websocket connection is opened. Immediately
             * sends a {@code CHAT_JOIN} frame for the active room.
             */
            @Override public void onOpen() {
                try {
                    JSONObject join = new JSONObject()
                            .put("type", "CHAT_JOIN")
                            .put("roomId", activeConversationId);
                    client.send(join.toString());
                } catch (Exception ignored) {}
            }

            /**
             * Called when a text frame is received from the chat server.
             * Attempts to parse JSON, filters for {@code CHAT_MESSAGE} type,
             * and then broadcasts the message details using
             * @link #ACTION_CHAT_MESSAGE.
             *
             * @param text raw message payload received from the websocket
             */
            @Override
            public void onMessage(String text) {
                String sender = "peer";
                String content = text;
                String type = "";

                try {
                    JSONObject j = new JSONObject(text);
                    sender = j.optString("sender", sender);
                    content = j.optString("content", content);
                    type = j.optString("type", "");
                } catch (Exception ignored) {}

                android.util.Log.d("CALL_SIGNAL_RX",
                        "onMessage: type=" + type
                                + " sender=" + sender
                                + " room=" + activeConversationId
                                + " raw=" + text);

                if (type != null && type.startsWith("CALL_")) {
                    if (callSignalListener != null) {
                        callSignalListener.onCallSignal(type, text);
                    }
                    return;
                }

                if (!"CHAT_MESSAGE".equals(type)) {
                    return;
                }

                Intent i = new Intent(ACTION_CHAT_MESSAGE);
                i.putExtra(EXTRA_CONVO_ID, activeConversationId);
                i.putExtra(EXTRA_SENDER, sender);
                i.putExtra(EXTRA_CONTENT, content);
                LocalBroadcastManager.getInstance(appCtx).sendBroadcast(i);
            }

            /**
             * Called when the chat websocket is closed. Currently a no-op but
             * available for future logging or reconnection logic.
             *
             * @param code   close status code
             * @param reason close reason string, if provided
             */
            @Override public void onClosed(int code, String reason) { }
            /**
             * Called when the chat websocket encounters an error. Currently a
             * no-op placeholder for potential error reporting or retry logic.
             *
             * @param t the underlying failure cause
             */
            @Override public void onFailure(Throwable t) { }
        });
    }

    public void setCallActive(boolean active) {
        this.callActive = active;
    }

    public boolean isCallActive() {
        return callActive;
    }

    /**
     * Sends a preformatted JSON string over the active chat websocket.
     *
     * @param textJson a JSON payload (e.g., {"type":"CHAT_SEND","roomId":"...","content":"..."})
     * @return {@code true} if the client accepted the send, {@code false} otherwise
     */
    public boolean send(String textJson) {
        return client.send(textJson);
    }
}
