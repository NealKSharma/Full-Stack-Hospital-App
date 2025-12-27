package com.project.saintcyshospital;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.android.volley.NetworkResponse;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.project.saintcyshospital.net.MultipartFormRequest;
import com.project.saintcyshospital.ws.ChatWsHub;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ChatDetailActivity extends BaseActivity {

    private String conversationId;
    private final ArrayList<String> lines = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private final Map<Integer, String> positionToFilename = new HashMap<>();


    private static final String URL_CHAT_HISTORY_BASE =
            "http://coms-3090-041.class.las.iastate.edu:8080/api/chat/history/";
    private boolean isGroupChat;
    private String chatTitle;

    private android.widget.ImageButton btnAttach;
    private androidx.activity.result.ActivityResultLauncher<String> pickFile;
    private final java.util.Map<Integer, Long> positionToMsgId = new java.util.HashMap<>();

    private final android.content.BroadcastReceiver chatReceiver =
            new android.content.BroadcastReceiver() {
                @Override
                public void onReceive(android.content.Context c,
                                      android.content.Intent i) {

                    String cid = i.getStringExtra(ChatWsHub.EXTRA_CONVO_ID);
                    if (cid == null || !cid.equals(conversationId)) return;

                    String senderRaw = i.getStringExtra(ChatWsHub.EXTRA_SENDER);
                    String content   = i.getStringExtra(ChatWsHub.EXTRA_CONTENT);
                    if (content == null || content.isEmpty()) return;

                    String meFirst = getMyFirstName();

                    String senderFirst = (senderRaw != null && !senderRaw.isEmpty())
                            ? senderRaw
                            : "peer";

                    int spaceIdx = senderFirst.indexOf(' ');
                    if (spaceIdx > 0) {
                        senderFirst = senderFirst.substring(0, spaceIdx);
                    }

                    String displaySender =
                            (!meFirst.isEmpty() && senderFirst.equalsIgnoreCase(meFirst))
                                    ? "me"
                                    : senderFirst;

                    String line = displaySender + ": " + content;

                    lines.add(line);
                    adapter.notifyDataSetChanged();
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_detail);
        setupBottomNav(0);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (view, insets) -> {
            int left = view.getPaddingLeft();
            int top = view.getPaddingTop();
            int right = view.getPaddingRight();

            view.setPadding(left, top, right, 0);
            return insets;
        });

        conversationId = getIntent().getStringExtra("conversationId");
        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = "unknown";
        }

        boolean isGroupChat = getIntent().getBooleanExtra("isGroupChat", false);

        String chatTitle = getIntent().getStringExtra("chatTitle");
        if (chatTitle == null || chatTitle.isEmpty()) {
            chatTitle = isGroupChat ? "Group Chat" : "Chat";
        }

        this.isGroupChat = isGroupChat;
        this.chatTitle  = chatTitle;

        isGroupChat = getIntent().getBooleanExtra("isGroupChat", false);

        ImageButton backBtn = findViewById(R.id.btn_back);
        ImageButton callBtn = findViewById(R.id.btn_call);
        ImageView avatar  = findViewById(R.id.chat_avatar);
        TextView titleTv = findViewById(R.id.chat_title);

        titleTv.setText(chatTitle);

        backBtn.setOnClickListener(v -> finish());

        boolean finalIsGroupChat1 = isGroupChat;
        callBtn.setOnClickListener(v -> {
            if (finalIsGroupChat1) {
                Toast.makeText(this, "Group calls not supported yet", Toast.LENGTH_SHORT).show();
                return;
            }

            android.content.Intent intent = new android.content.Intent(this, VoiceCallActivity.class);
            intent.putExtra("conversationId", conversationId);
            intent.putExtra("isCaller", true);
            startActivity(intent);
        });


        if (isGroupChat) {
            avatar.setImageResource(R.drawable.ic_people);
        }
        else if (conversationId.startsWith("assistant&")) {
            avatar.setImageResource(R.drawable.ic_robot);

        }else {
            avatar.setImageResource(R.drawable.ic_person);
        }

        ListView chatList = findViewById(R.id.chat_list);
        EditText msgInput = findViewById(R.id.msg_input);
        ImageButton btnSend = findViewById(R.id.btn_send);

        btnAttach = findViewById(R.id.btn_attach);

        boolean finalIsGroupChat = isGroupChat;
        adapter = new ArrayAdapter<String>(this, 0, lines) {
            @Override
            public android.view.View getView(int position, android.view.View convertView,
                                             android.view.ViewGroup parent) {

                String line = getItem(position);
                if (line == null) line = "";

                int colonIdx = line.indexOf(':');
                String sender = "other";
                String text = line;

                if (colonIdx >= 0 && colonIdx + 2 <= line.length()) {
                    sender = line.substring(0, colonIdx).trim();
                    text   = line.substring(colonIdx + 2);
                }

                boolean isMe = sender.equalsIgnoreCase("me");
                int layoutId = isMe ? R.layout.item_chat_me : R.layout.item_chat_other;

                if (convertView == null || convertView.getTag() == null
                        || (int) convertView.getTag() != layoutId) {
                    convertView = getLayoutInflater().inflate(layoutId, parent, false);
                    convertView.setTag(layoutId);
                }

                android.widget.TextView msgText   = convertView.findViewById(R.id.msg_text);
                android.widget.TextView senderLbl = convertView.findViewById(R.id.msg_sender);

                String fname = positionToFilename.get(position);
                if (fname != null && !fname.isEmpty()) {
                    String combined;

                    if (text == null) text = "";
                    if (text.isEmpty()) {
                        combined = fname;
                    } else {
                        combined = text + "  •  " + fname;
                    }

                    SpannableString ss = new SpannableString(combined);
                    int start = combined.length() - fname.length();
                    int end   = combined.length();

                    ss.setSpan(
                            new ForegroundColorSpan(0xFF134ab0),
                            start,
                            end,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                    msgText.setText(ss);
                } else {
                    msgText.setText(text);
                }


                if (senderLbl != null) {
                    if (finalIsGroupChat && !isMe && isEndOfRun(position, sender)) {
                        senderLbl.setText(sender);
                        senderLbl.setVisibility(android.view.View.VISIBLE);
                    } else {
                        senderLbl.setVisibility(android.view.View.GONE);
                    }
                }

                return convertView;
            }

            private boolean isEndOfRun(int position, String sender) {
                int count = getCount();
                if (position >= count - 1) {
                    return true;
                }
                String nextLine = getItem(position + 1);
                if (nextLine == null) return true;

                int colonIdx = nextLine.indexOf(':');
                String nextSender = (colonIdx >= 0)
                        ? nextLine.substring(0, colonIdx).trim()
                        : "other";

                return !sender.equalsIgnoreCase(nextSender);
            }
        };

        chatList.setAdapter(adapter);

        pickFile = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) uploadAttachment(uri);
                });

        if (btnAttach != null) {
            btnAttach.setOnClickListener(v -> pickFile.launch("*/*"));
        }

        chatList.setOnItemClickListener((parent, view, position, id) -> {
            Long msgId = positionToMsgId.get(position);
            String fname = positionToFilename.get(position);

            if (msgId != null && msgId > 0
                    && fname != null && !fname.isEmpty()) {
                downloadAttachment(msgId, fname);
            }
        });

        loadHistory();

        btnSend.setOnClickListener(v -> {
            String text = msgInput.getText().toString().trim();
            if (text.isEmpty()) return;

            try {
                JSONObject out = new JSONObject()
                        .put("type", "CHAT_SEND")
                        .put("roomId", conversationId)
                        .put("content", text);

                boolean ok = ChatWsHub.get().send(out.toString());
                if (ok) {
                    String mine = "me: " + text;
                    lines.add(mine);
                    adapter.notifyDataSetChanged();
                    msgInput.setText("");
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Socket not connected",
                            Toast.LENGTH_SHORT).show();
                }
            } catch (Exception ignored) {}
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        ChatWsHub.get().start(getApplicationContext(), conversationId);

        android.content.IntentFilter f =
                new android.content.IntentFilter(ChatWsHub.ACTION_CHAT_MESSAGE);

        androidx.localbroadcastmanager.content.LocalBroadcastManager
                .getInstance(this)
                .registerReceiver(chatReceiver, f);
    }


    @Override
    protected void onStop() {
        androidx.localbroadcastmanager.content.LocalBroadcastManager
                .getInstance(this)
                .unregisterReceiver(chatReceiver);

        if (!ChatWsHub.get().isCallActive()) {
            ChatWsHub.get().stop();
        }
        super.onStop();

    }

    private void loadHistory() {
        String url = URL_CHAT_HISTORY_BASE
                + android.net.Uri.encode(conversationId);

        com.android.volley.toolbox.JsonArrayRequest req =
                new com.android.volley.toolbox.JsonArrayRequest(
                        com.android.volley.Request.Method.GET,
                        url,
                        null,
                        resp -> {
                            lines.clear();
                            positionToMsgId.clear();
                            positionToFilename.clear();

                            String meFirst = getMyFirstName();

                            for (int i = 0; i < resp.length(); i++) {
                                org.json.JSONObject o =
                                        resp.optJSONObject(i);
                                if (o == null) continue;

                                String senderRaw =
                                        o.optString("sender", "peer");
                                String content =
                                        o.optString("content", "");

                                String senderFirst = senderRaw;
                                int spaceIdx = senderFirst.indexOf(' ');
                                if (spaceIdx > 0) {
                                    senderFirst =
                                            senderFirst.substring(0, spaceIdx);
                                }

                                String displaySender;
                                if (!meFirst.isEmpty() &&
                                        senderFirst.equalsIgnoreCase(meFirst)) {
                                    displaySender = "me";
                                } else {
                                    displaySender = senderFirst;
                                }

                                Long msgId = o.optLong("id", -1);
                                boolean hasAttachment = o.optBoolean("hasAttachment", false);
                                String filename = o.optString("attachmentFilename", "");

                                String display = displaySender + ": " + content;

                                if (hasAttachment && (filename == null || filename.isEmpty())) {
                                    filename = "attachment";
                                }

                                int pos = lines.size();
                                positionToMsgId.put(pos, (msgId > 0) ? msgId : null);

                                if (hasAttachment) {
                                    positionToFilename.put(pos, filename);
                                }

                                lines.add(display);

                            }


                            adapter.notifyDataSetChanged();
                        },
                        err -> Toast.makeText(
                                getApplicationContext(),
                                "Failed to load history",
                                Toast.LENGTH_SHORT
                        ).show()
                ) {
                    @Override
                    public java.util.Map<String, String> getHeaders() {
                        java.util.Map<String, String> h =
                                new java.util.HashMap<>();
                        String access =
                                AuthManager.getAccessToken(
                                        getApplicationContext());
                        if (access != null && !access.isEmpty()) {
                            h.put("Authorization",
                                    "Bearer " + access);
                        }
                        h.put("Accept", "application/json");
                        return h;
                    }
                };

        VolleySingleton
                .getInstance(getApplicationContext())
                .addToRequestQueue(req);
    }

    private String getMyFirstName() {
        SharedPreferences prefs =
                getSharedPreferences("auth_prefs", MODE_PRIVATE);
        return prefs.getString("last_user", "");
    }

    private void uploadAttachment(android.net.Uri uri) {
        String text = "";

        String fileName = "attachment";
        String mime = getContentResolver().getType(uri);
        if (mime == null) mime = "application/octet-stream";

        try (java.io.InputStream in = getContentResolver().openInputStream(uri);
             java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream()) {

            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) bos.write(buf, 0, n);
            byte[] data = bos.toByteArray();

            if (data.length > 25 * 1024 * 1024) {
                toast("File exceeds 25MB limit");
                return;
            }

            try (android.database.Cursor c = getContentResolver().query(uri, null, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    int idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) fileName = c.getString(idx);
                }
            }

            String url = "http://coms-3090-041.class.las.iastate.edu:8080/api/chat/attachment/upload";

            java.util.Map<String, String> headers = new java.util.HashMap<>();
            String access = AuthManager.getAccessToken(getApplicationContext());
            if (access != null && !access.isEmpty()) headers.put("Authorization", "Bearer " + access);
            headers.put("Accept", "application/json");

            java.util.Map<String, String> fields = new java.util.HashMap<>();
            fields.put("conversationId", conversationId);
            fields.put("content", text);

            java.util.Map<String, com.project.saintcyshospital.net.MultipartFormRequest.FilePart> files = new java.util.HashMap<>();
            files.put("file", new com.project.saintcyshospital.net.MultipartFormRequest.FilePart(
                    fileName, mime, data
            ));

            String finalFileName = fileName;
            com.project.saintcyshospital.net.MultipartFormRequest req =
                    new com.project.saintcyshospital.net.MultipartFormRequest(
                            com.android.volley.Request.Method.POST,
                            url,
                            headers,
                            fields,
                            files,
                            resp -> {
                                toast("Attachment sent");
                                loadHistory();
                            },
                            err -> {
                                NetworkResponse nr = err.networkResponse;
                                if (nr != null && nr.data != null) {
                                    String body = new String(nr.data);
                                    toast("Upload failed: " + nr.statusCode + "\n" + body);
                                } else {
                                    toast("Upload failed: " + err.toString());
                                }
                            }
                    );


            VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(req);

        } catch (Exception e) {
            toast("Could not read file");
        }
    }

    private void downloadAttachment(long messageId, String preferredName) {
        String url = "http://coms-3090-041.class.las.iastate.edu:8080/api/chat/attachment/download/" + messageId;

        java.util.Map<String, String> headers = new java.util.HashMap<>();
        String access = AuthManager.getAccessToken(getApplicationContext());
        if (access != null && !access.isEmpty()) headers.put("Authorization", "Bearer " + access);
        headers.put("Accept", "*/*");

        com.project.saintcyshospital.net.ByteRequest req =
                new com.project.saintcyshospital.net.ByteRequest(
                        com.android.volley.Request.Method.GET,
                        url,
                        headers,
                        payload -> {
                            if (payload == null || payload.data == null || payload.data.length == 0) {
                                toast("Empty file");
                                return;
                            }

                            String mime = payload.headers.getOrDefault("Content-Type", "application/octet-stream");

                            String name;
                            if (preferredName != null && !preferredName.isEmpty()) {
                                name = preferredName;
                            } else if (mime.contains("png"))       name = "attachment.png";
                            else if (mime.contains("jpeg"))        name = "attachment.jpg";
                            else if (mime.contains("jpg"))         name = "attachment.jpg";
                            else if (mime.contains("gif"))         name = "attachment.gif";
                            else if (mime.contains("pdf"))         name = "attachment.pdf";
                            else                                   name = "attachment.bin";


                            try {
                                File downloadsDir = Environment.getExternalStoragePublicDirectory(
                                        Environment.DIRECTORY_DOWNLOADS
                                );

                                File out = new File(downloadsDir, name);

                                if (out.exists()) {
                                    int dot = name.lastIndexOf('.');
                                    String base = (dot == -1) ? name : name.substring(0, dot);
                                    String ext  = (dot == -1) ? ""   : name.substring(dot);

                                    int n = 1;
                                    File candidate;
                                    do {
                                        candidate = new File(downloadsDir, base + " (" + n + ")" + ext);
                                        n++;
                                    } while (candidate.exists());

                                    out = candidate;
                                }

                                try (FileOutputStream fos = new FileOutputStream(out)) {
                                    fos.write(payload.data);
                                }

                                toast("File successfully saved to downloads.");


                            } catch (Exception e) {
                                toast("Failed to save file");
                            }
                        },
                        err -> toast("Download failed")
                );

        VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(req);
    }

}
