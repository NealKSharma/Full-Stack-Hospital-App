package com.project.saintcyshospital;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.core.view.ViewCompat;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class NotificationsActivity extends BaseActivity {

    private static final String URL_NOTIF_LIST = "http://coms-3090-041.class.las.iastate.edu:8080/api/notifications";
    private final ArrayList<String> messages = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private final BroadcastReceiver wsReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (com.project.saintcyshospital.ws.WsHub.ACTION_WS_MESSAGE.equals(intent.getAction())) {
                String text = intent.getStringExtra(com.project.saintcyshospital.ws.WsHub.EXTRA_TEXT);
                if (text != null) {
                    messages.add(text);
                    adapter.notifyDataSetChanged();
                }
            }
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);
        setupBottomNav(0);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (view, insets) -> {
            int left = view.getPaddingLeft();
            int top = view.getPaddingTop();
            int right = view.getPaddingRight();

            view.setPadding(left, top, right, 0);
            return insets;
        });

        ListView list = findViewById(R.id.msg_list);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, messages);
        list.setAdapter(adapter);

        fetchPastNotifications();
    }

    @Override protected void onStart() {
        super.onStart();
        android.content.IntentFilter f =
                new android.content.IntentFilter(com.project.saintcyshospital.ws.WsHub.ACTION_WS_MESSAGE);
        androidx.core.content.ContextCompat.registerReceiver(
                this,
                wsReceiver,
                f,
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        );
    }

    @Override protected void onStop() {
        unregisterReceiver(wsReceiver);
        super.onStop();
    }

    private void fetchPastNotifications() {
        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET,
                URL_NOTIF_LIST,
                null,
                this::handlePastNotifsResponse,
                (VolleyError error) -> handleVolleyAuthError(error, this::fetchPastNotifications)
        ) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> h = new java.util.HashMap<>();
                String access = AuthManager.getAccessToken(getApplicationContext());
                if (access != null && !access.isEmpty()) {
                    h.put("Authorization", "Bearer " + access);
                }
                h.put("Accept", "application/json");
                return h;
            }
        };

        VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(req);
    }

    private void handlePastNotifsResponse(JSONArray resp) {
        messages.clear();
        for (int i = 0; i < resp.length(); i++) {
            JSONObject o = resp.optJSONObject(i);
            if (o == null) continue;

            String title = o.optString("title", "");
            String desc  = o.optString("content", "");

            String line;
            if (!title.isEmpty() && !desc.isEmpty()) {
                line = title + " - " + desc;
            } else if (!title.isEmpty()) {
                line = title;
            } else if (!desc.isEmpty()) {
                line = desc;
            } else {
                line = "(no content)";
            }

            messages.add(line);
        }
        adapter.notifyDataSetChanged();
    }
}
