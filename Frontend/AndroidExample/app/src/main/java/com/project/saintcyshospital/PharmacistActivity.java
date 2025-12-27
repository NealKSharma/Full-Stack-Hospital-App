package com.project.saintcyshospital;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.core.view.ViewCompat;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PharmacistActivity extends BaseActivity {

    private static final String BASE_API = "http://coms-3090-041.class.las.iastate.edu:8080";
    private static final String URL_ALL_ORDERS = BASE_API + "/api/pharmacy/orders/all";
    private static final String URL_STATUS_BASE = BASE_API + "/api/pharmacy/orders/";

    private final ArrayList<OrderRow> orders = new ArrayList<>();
    private final ArrayList<String> rows = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected boolean requiresAuth() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pharmacist);
        setupBottomNav(0);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (view, insets) -> {
                int left = view.getPaddingLeft();
                int top = view.getPaddingTop();
                int right = view.getPaddingRight();
                view.setPadding(left, top, right, 0);
                return insets;
            });
        }

        if (!AuthManager.isPharmacist(getApplicationContext())) {
            Toast.makeText(getApplicationContext(),
                    "Pharmacist access only.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ListView list = findViewById(R.id.order_list);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows);
        list.setAdapter(adapter);

        list.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= orders.size()) return;
            OrderRow o = orders.get(position);
            showOrderActions(o);
        });

        fetchOrders();
    }

    private void fetchOrders() {
        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET,
                URL_ALL_ORDERS,
                null,
                this::handleOrdersResponse,
                (VolleyError err) -> handleVolleyAuthError(err, this::fetchOrders)
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeaders();
            }
        };

        VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(req);
    }

    private void handleOrdersResponse(JSONArray resp) {
        orders.clear();
        rows.clear();

        for (int i = 0; i < resp.length(); i++) {
            JSONObject o = resp.optJSONObject(i);
            if (o == null) continue;

            long id = o.optLong("id", -1);
            if (id <= 0) continue;

            String patient = o.optString("patientName", "Patient");
            String status = o.optString("status", "PENDING");
            String drug = o.optString("productName", "");
            int quantity = o.optInt("quantity", 0);
            String date = o.optString("createdAt", "");

            OrderRow or = new OrderRow();
            or.id = id;
            or.patientName = patient;
            or.status = status;
            or.drug = drug;
            or.quantity = quantity;
            or.date = date;
            orders.add(or);

            String row = "#" + id + " • " + patient + " • " + date + "\n"
                    + "Status: " + status + "\n"
                    + "Medication: " + drug + " (" + quantity + ")";
            rows.add(row);
        }

        adapter.notifyDataSetChanged();
    }

    private void showOrderActions(OrderRow o) {
        String msg = "Order #" + o.id + "\n"
                + "Patient: " + o.patientName + "\n"
                + "Status: " + o.status + "\n";

        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle("Manage Order")
                .setMessage(msg)
                .setNegativeButton("Close", null);

        String s = o.status.toUpperCase();

        if ("PENDING".equals(s)) {
            b.setPositiveButton("Accept (Fill)", (d, w) -> updateStatus(o, "READY_FOR_PICKUP"));
            b.setNeutralButton("Decline", (d, w) -> updateStatus(o, "REJECTED"));
        } else if ("READY_FOR_PICKUP".equals(s)) {
            b.setPositiveButton("Complete", (d, w) -> updateStatus(o, "FULFILLED"));
            b.setNeutralButton("Decline", (d, w) -> updateStatus(o, "REJECTED"));
        }

        b.show();
    }

    private void updateStatus(OrderRow o, String newStatus) {
        String url = URL_STATUS_BASE + o.id + "/status";

        JSONObject body = new JSONObject();
        try {
            body.put("status", newStatus);
        } catch (Exception ignored) {}

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.PUT,
                url,
                body,
                resp -> {
                    String ns = resp.optString("newStatus", newStatus);
                    o.status = ns;
                    Toast.makeText(getApplicationContext(),
                            "Order #" + o.id + " updated to " + ns,
                            Toast.LENGTH_SHORT).show();
                    fetchOrders();
                },
                (VolleyError err) -> handleVolleyAuthError(err, () -> updateStatus(o, newStatus))
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return buildAuthHeaders();
            }
        };

        VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(req);
    }

    private Map<String, String> buildAuthHeaders() {
        Map<String, String> h = new HashMap<>();
        String access = AuthManager.getAccessToken(getApplicationContext());
        if (access != null && !access.isEmpty()) {
            h.put("Authorization", "Bearer " + access);
        }
        h.put("Accept", "application/json");
        h.put("Content-Type", "application/json");
        return h;
    }

    private static class OrderRow {
        long id;
        String patientName;
        String status;
        String drug;
        int quantity;
        String date;
    }
}
