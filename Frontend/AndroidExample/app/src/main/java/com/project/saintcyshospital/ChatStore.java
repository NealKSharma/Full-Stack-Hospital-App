package com.project.saintcyshospital;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatStore {

    private static final String PREFS = "chat_preview_prefs";
    private static final String KEY_PREFIX_MSG = "last_msg_";

    private static ChatStore INSTANCE = new ChatStore();
    public static ChatStore get() { return INSTANCE; }

    private Context appCtx;

    public void init(Context ctx) {
        if (appCtx == null) {
            appCtx = ctx.getApplicationContext();
        }
    }

    public void saveLastMessage(String conversationId, String text) {
        if (appCtx == null) return;
        SharedPreferences sp = appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit()
                .putString(KEY_PREFIX_MSG + conversationId, text)
                .apply();
    }

    public List<ConversationPreview> getAllConversations() {
        List<ConversationPreview> out = new ArrayList<>();
        if (appCtx == null) return out;

        SharedPreferences sp = appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Map<String, ?> all = sp.getAll();
        for (Map.Entry<String, ?> e : all.entrySet()) {
            String k = e.getKey();
            if (!k.startsWith(KEY_PREFIX_MSG)) continue;
            String convoId = k.substring(KEY_PREFIX_MSG.length());
            String lastMsg = (e.getValue() instanceof String) ? (String) e.getValue() : "";
            out.add(new ConversationPreview(convoId, lastMsg));
        }
        return out;
    }

    public static class ConversationPreview {
        public final String conversationId;
        public final String lastMessage;
        public ConversationPreview(String id, String msg) {
            this.conversationId = id;
            this.lastMessage = msg;
        }
        @Override public String toString() {
            return conversationId + ": " + lastMessage;
        }
    }
}
