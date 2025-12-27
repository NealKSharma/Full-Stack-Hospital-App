package com.project.saintcyshospital;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ReportsActivity extends BaseActivity {

    private View tabTreatment, tabMeds, tabLabs;
    private View panelTreatment, panelMeds, panelLabs;
    private ListView listTreatment, listMeds, listLabs;
    private ProgressBar progress;
    private TextView errorText;

    // Point this at your patient "self" endpoint:
    // If your backend uses different paths, update this one line.
    private static final String REPORTS_URL = "http://coms-3090-041.class.las.iastate.edu:8080/api/reports/me";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);
        setTitle("Reports");

        // Tabs
        tabTreatment = findViewById(R.id.tab_treatment);
        tabMeds = findViewById(R.id.tab_meds);
        tabLabs = findViewById(R.id.tab_labs);

        // Panels
        panelTreatment = findViewById(R.id.panel_treatment);
        panelMeds = findViewById(R.id.panel_medicines);
        panelLabs = findViewById(R.id.panel_labs);

        // Lists + status
        listTreatment = findViewById(R.id.list_treatment);
        listMeds = findViewById(R.id.list_meds);
        listLabs = findViewById(R.id.list_labs);
        progress = findViewById(R.id.progress);
        errorText = findViewById(R.id.errorText);

        // Tab switching
        View.OnClickListener show = v -> {
            panelTreatment.setVisibility(v == tabTreatment ? View.VISIBLE : View.GONE);
            panelMeds.setVisibility(v == tabMeds ? View.VISIBLE : View.GONE);
            panelLabs.setVisibility(v == tabLabs ? View.VISIBLE : View.GONE);
        };
        tabTreatment.setOnClickListener(show);
        tabMeds.setOnClickListener(show);
        tabLabs.setOnClickListener(show);

        tabTreatment.performClick();

        // Gate: must be logged in
        if (!AuthManager.isLoggedIn(this)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        fetchReports();
    }

    private void fetchReports() {
        setLoading(true, null);

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                REPORTS_URL,
                null,
                response -> {
                    try {
                        bindResponse(response);
                        setLoading(false, null);
                    } catch (JSONException e) {
                        setLoading(false, "Parse error: " + e.getMessage());
                    }
                },
                error -> {
                    // 401 -> force login
                    if (error != null && error.networkResponse != null
                            && error.networkResponse.statusCode == 401) {
                        AuthManager.clear(ReportsActivity.this);
                        startActivity(new Intent(ReportsActivity.this, LoginActivity.class));
                        finish();
                        return;
                    }
                    setLoading(false, "Failed to load reports");
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> h = new HashMap<>();
                String token = AuthManager.getAccessToken(ReportsActivity.this);
                if (token != null && !token.isEmpty()) {
                    h.put("Authorization", "Bearer " + token);
                }
                h.put("Accept", "application/json");
                return h;
            }
        };

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    private void bindResponse(JSONObject json) throws JSONException {
        // Expected JSON (adjust if yours differs):
        // {
        //   "treatmentPlan": ["Step 1 ...", "Step 2 ..."],
        //   "medicines": [ { "name":"Amoxicillin", "dose":"500mg", "freq":"2x/day" }, ... ],
        //   "labs": [ { "title":"CBC", "value":"Normal", "date":"2025-05-01" }, ... ]
        // }

        // Treatment
        ArrayList<String> tItems = new ArrayList<>();
        JSONArray tArr = json.optJSONArray("treatmentPlan");
        if (tArr != null) {
            for (int i = 0; i < tArr.length(); i++) tItems.add(tArr.optString(i));
        }
        listTreatment.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, tItems));

        // Medicines
        ArrayList<String> mItems = new ArrayList<>();
        JSONArray mArr = json.optJSONArray("medicines");
        if (mArr != null) {
            for (int i = 0; i < mArr.length(); i++) {
                JSONObject m = mArr.optJSONObject(i);
                if (m != null) {
                    String name = m.optString("name");
                    String dose = m.optString("dose");
                    String freq = m.optString("freq");
                    mItems.add(name
                            + (dose.isEmpty() ? "" : " • " + dose)
                            + (freq.isEmpty() ? "" : " • " + freq));
                }
            }
        }
        listMeds.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mItems));

        // Labs
        ArrayList<String> lItems = new ArrayList<>();
        JSONArray lArr = json.optJSONArray("labs");
        if (lArr != null) {
            for (int i = 0; i < lArr.length(); i++) {
                JSONObject l = lArr.optJSONObject(i);
                if (l != null) {
                    String title = l.optString("title");
                    String val = l.optString("value");
                    String date = l.optString("date");
                    lItems.add(title
                            + (val.isEmpty() ? "" : " • " + val)
                            + (date.isEmpty() ? "" : " • " + date));
                }
            }
        }
        listLabs.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, lItems));
    }

    private void setLoading(boolean loading, String error) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);

        boolean showError = error != null && !error.isEmpty();
        errorText.setVisibility(showError ? View.VISIBLE : View.GONE);

        if (showError) {
            errorText.setText(error);
            // Tap the error message to retry loading reports
            errorText.setOnClickListener(v -> fetchReports());
        } else {
            errorText.setText("");
            errorText.setOnClickListener(null);
        }
    }
}
