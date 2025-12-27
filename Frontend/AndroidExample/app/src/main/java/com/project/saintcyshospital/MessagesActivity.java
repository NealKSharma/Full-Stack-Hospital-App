package com.project.saintcyshospital;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MessagesActivity extends BaseActivity {

    private final ArrayList<Conversation> conversations = new ArrayList<>();
    private final ArrayList<Conversation> filteredConversations = new ArrayList<>();
    private ArrayAdapter<Conversation> adapter;


    private static final String BASE_API = "http://coms-3090-041.class.las.iastate.edu:8080";
    private static final String URL_CONVERSATIONS = BASE_API + "/api/chat/conversations";
    private static final String URL_CONVERSATION_CREATE = BASE_API + "/api/chat/start";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);
        setupBottomNav(R.id.nav_messages);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (view, insets) -> {
            int left = view.getPaddingLeft();
            int top = view.getPaddingTop();
            int right = view.getPaddingRight();

            view.setPadding(left, top, right, 0);
            return insets;
        });

        ListView convoList = findViewById(R.id.chat_list);
        ImageButton btnNewMessage = findViewById(R.id.btn_new_message);
        EditText search = findViewById(R.id.messages_search);

        btnNewMessage.setOnClickListener(v -> promptNewConversation());

        adapter = new ArrayAdapter<Conversation>(this, R.layout.item_conversation, filteredConversations) {
            @Override
            public android.view.View getView(int position, android.view.View convertView,
                                             android.view.ViewGroup parent) {

                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.item_conversation, parent, false);
                }

                Conversation c = getItem(position);
                if (c == null) return convertView;

                TextView timeTv = convertView.findViewById(R.id.convo_time);
                TextView nameTv    = convertView.findViewById(R.id.convo_name);
                TextView previewTv = convertView.findViewById(R.id.convo_preview);
                ImageView avatar   = convertView.findViewById(R.id.convo_avatar);
                FrameLayout avatarbg   = convertView.findViewById(R.id.convo_bg);

                nameTv.setText(c.title);
                if (c.lastMessage == null || c.lastMessage.isEmpty()) {
                    previewTv.setText("");
                } else {
                    previewTv.setText(c.lastMessage);
                }

                if (c.isGroupChat) {
                    avatar.setImageResource(R.drawable.ic_people);
                    avatarbg.getBackground().setTint(Color.parseColor("#FC0303"));
                }
                else if (c.roomId.startsWith("assistant&")) {
                    avatar.setImageResource(R.drawable.ic_robot);
                    //avatar.setImageTintList(ColorStateList.valueOf(Color.parseColor("#000000")));
                    avatarbg.getBackground().setTint(Color.parseColor("#800080"));
                }
                else {
                    avatar.setImageResource(R.drawable.ic_person);
                    avatarbg.getBackground().setTint(Color.parseColor("#FC0303"));
                }

                if (c.timestamp == null) {
                    timeTv.setText("");
                } else {
                    timeTv.setText(formatTimestamp(c.timestamp));
                }

                return convertView;
            }
        };
        convoList.setAdapter(adapter);

        convoList.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= filteredConversations.size()) return;
            Conversation c = filteredConversations.get(position);
            openConversation(c.roomId);
        });

        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                String q = s.toString().trim().toLowerCase();
                filteredConversations.clear();
                if (q.isEmpty()) {
                    filteredConversations.addAll(conversations);
                } else {
                    for (Conversation c : conversations) {
                        String haystack = (c.title + " " + c.lastMessage).toLowerCase();
                        if (haystack.contains(q)) {
                            filteredConversations.add(c);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchConversations();
    }

    private void openConversation(String conversationId) {
        Intent detail = new Intent(this, ChatDetailActivity.class);
        detail.putExtra("conversationId", conversationId);
        detail.putExtra("isGroupChat", conversationId.contains("group"));

        SharedPreferences prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE);
        String ignoredName = prefs.getString("last_user", "");
        String[] parts = conversationId.split("-");
        String chatTitle = java.util.Arrays.stream(parts)
                .filter(p -> !p.equalsIgnoreCase("GROUP"))
                .filter(p -> !p.equalsIgnoreCase(ignoredName))
                .collect(java.util.stream.Collectors.joining(", "));
        if(conversationId.startsWith("assistant&"))
        {
            chatTitle = "A.S.H.W.I.N";
        }
        detail.putExtra("chatTitle", chatTitle);
        startActivity(detail);
    }

    private Map<String, String> buildHeaders() {
        Map<String, String> h = new HashMap<>();
        String access = AuthManager.getAccessToken(getApplicationContext());
        if (access != null && !access.isEmpty()) {
            h.put("Authorization", "Bearer " + access);
        }
        h.put("Accept", "application/json");
        h.put("Content-Type", "application/json");
        return h;
    }

    private void fetchConversations() {
        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET,
                URL_CONVERSATIONS,
                null,
                (JSONArray resp) -> {
                    conversations.clear();
                    filteredConversations.clear();

                    for (int i = 0; i < resp.length(); i++) {
                        JSONObject o = resp.optJSONObject(i);

                        if (o == null) continue;

                        Conversation c = new Conversation();
                        c.roomId = o.optString("conversationId", "");
                        String prettyRoom = c.roomId.startsWith("group-")
                                ? c.roomId.substring(6).replace("-", ", ")
                                : c.roomId;
                        if (c.roomId.startsWith("assistant&"))
                        {
                            prettyRoom = "A.S.H.W.I.N";
                        }
                        c.otherUser   = o.optString("otherUser", "");
                        c.lastMessage = o.optString("lastMessage", "");
                        if (o.optBoolean("lastMessageHasAttachment", false))
                        {
                            c.lastMessage = o.optString("lastMessageAttachmentFilename", "");
                        }

                        String tsRaw = o.optString("lastMessageTime", null);
                        if (tsRaw != null && !tsRaw.isEmpty()) {
                            try {
                                c.timestamp = java.time.LocalDateTime.parse(tsRaw);
                            } catch (Exception e) {
                                c.timestamp = null;
                            }
                        } else {
                            c.timestamp = null;
                        }

                        if (!c.roomId.isEmpty()) {
                            c.title = (c.otherUser.isEmpty() || c.roomId.startsWith("assistant&")) ? prettyRoom : c.otherUser;
                            conversations.add(c);
                        }
                        c.isGroupChat = c.roomId.toLowerCase().contains("group");
                    }

                    Collections.sort(conversations, (a, b) -> {
                        boolean aIsAssistant = a.roomId.startsWith("assistant&");
                        boolean bIsAssistant = b.roomId.startsWith("assistant&");

                        if (aIsAssistant && !bIsAssistant) return -1;
                        if (!aIsAssistant && bIsAssistant) return 1;

                        if (a.timestamp == null && b.timestamp == null) return 0;
                        if (a.timestamp == null) return 1;
                        if (b.timestamp == null) return -1;
                        return b.timestamp.compareTo(a.timestamp);
                    });

                    filteredConversations.addAll(conversations);
                    adapter.notifyDataSetChanged();
                },
                (VolleyError error) -> handleVolleyAuthError(error, this::fetchConversations)
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return buildHeaders();
            }
        };

        VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(req);
    }


    private void promptNewConversation() {
        final EditText input = new EditText(this);
        input.setHint("Recipient username");
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setPadding(pad, pad, pad, pad);
        wrapper.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("New Message")
                .setView(wrapper)
                .setPositiveButton("Start", (d, w) -> {
                    String target = input.getText().toString().trim();
                    if (target.isEmpty()) {
                        Toast.makeText(getApplicationContext(),
                                "Please enter someone to message",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    createConversation(target);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createConversation(String target) {
        JSONObject body = new JSONObject();
        try {
            body.put("recipient", target);
        } catch (Exception ignored) {}

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                URL_CONVERSATION_CREATE,
                body,
                resp -> {
                    String roomId = resp.optString("conversationId", "");
                    if (roomId.isEmpty()) {
                        Toast.makeText(getApplicationContext(),
                                "No roomId returned", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    openConversation(roomId);
                },
                (VolleyError err) -> handleVolleyAuthError(err, () -> createConversation(target))
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return buildHeaders();
            }
        };

        VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(req);
    }

    private String formatTimestamp(java.time.LocalDateTime ts) {
        if (ts == null) return "";

        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate date  = ts.toLocalDate();

        long days = java.time.temporal.ChronoUnit.DAYS.between(date, today);

        if (days == 0) {
            return ts.toLocalTime()
                    .withSecond(0)
                    .withNano(0)
                    .format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"));
        }

        if (days == 1) {
            return "Yesterday";
        }

        if (days < 7) {
            return date.getDayOfWeek()
                    .name()
                    .substring(0, 3);
        }

        return date.format(java.time.format.DateTimeFormatter.ofPattern("MMM d"));
    }


    private static class Conversation {
        String roomId;
        String otherUser;
        String lastMessage;
        String title;
        boolean isGroupChat;
        java.time.LocalDateTime timestamp;
    }
}
