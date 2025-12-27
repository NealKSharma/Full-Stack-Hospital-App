package com.project.saintcyshospital;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.Nullable;
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

public class DoctorPrescriptionsActivity extends BaseActivity {

    private ArrayAdapter<Prescription> adapter;
    private final ArrayList<Prescription> data = new ArrayList<>();

    private static final String BASE_API = "http://coms-3090-041.class.las.iastate.edu:8080";
    private static final String URL_PRESC_LIST = BASE_API + "/api/doctor/prescriptions";
    private static final String URL_PRESC_WRITE = BASE_API + "/api/doctor/prescriptions";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_prescriptions);
        setupBottomNav(0);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (view, insets) -> {
            int left = view.getPaddingLeft();
            int top = view.getPaddingTop();
            int right = view.getPaddingRight();

            view.setPadding(left, top, right, 0);
            return insets;
        });

        ListView list = findViewById(R.id.prescriptionList);
        EditText search = findViewById(R.id.searchPrescriptions);
        Button newBtn = findViewById(R.id.btnNewPrescription);

        adapter = new ArrayAdapter<Prescription>(this, android.R.layout.simple_list_item_2, android.R.id.text1, data) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, ViewGroup parent) {
                android.view.View v = super.getView(position, convertView, parent);
                Prescription p = getItem(position);

                String patientLabel = (p.patientUsername != null && !p.patientUsername.isEmpty())
                        ? p.patientUsername
                        : "Unknown patient";

                ((TextView) v.findViewById(android.R.id.text1)).setText(
                        p.drug + " • " + p.dosage + "  (" + patientLabel + ")"
                );

                String created = (p.createdAt != null && !p.createdAt.isEmpty()) ? p.createdAt : "—";
                String refillInfo = "Refills: " + p.refillAmount;

                ((TextView) v.findViewById(android.R.id.text2)).setText(
                        created + "  •  " + p.directions + "  •  " + refillInfo
                );

                return v;
            }
        };

        list.setAdapter(adapter);

        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { adapter.getFilter().filter(s); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        newBtn.setOnClickListener(v -> promptNewPrescription());

        fetchPrescriptions();
    }

    private Map<String, String> buildHeaders(boolean hasJsonBody) {
        Map<String, String> h = new HashMap<>();

        String access = AuthManager.getAccessToken(getApplicationContext());
        if (access != null && !access.isEmpty()) {
            h.put("Authorization", "Bearer " + access);
        }

        h.put("Accept", "application/json");

        if (hasJsonBody) {
            h.put("Content-Type", "application/json");
        }

        return h;
    }

    private void fetchPrescriptions() {
        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET, URL_PRESC_LIST, null,
                (JSONArray resp) -> {
                    data.clear();
                    for (int i = 0; i < resp.length(); i++) {
                        JSONObject o = resp.optJSONObject(i);
                        if (o == null) continue;
                        Prescription p = new Prescription();
                        p.id = o.optLong("id", 0);

                        JSONObject patientObj = o.optJSONObject("patient");
                        if (patientObj != null) {
                            p.patientUsername = patientObj.optString("name", null);
                        } else {
                            p.patientUsername = o.optString("patientUsername", null);
                        }

                        p.drug = o.optString("medication", "");
                        p.dosage = o.optString("dosage", "");
                        p.directions = o.optString("notes", "");
                        p.createdAt = o.optString("createdAt", null);
                        p.refillAmount = o.optInt("refill", 0);

                        data.add(p);
                    }
                    adapter.notifyDataSetChanged();
                },
                (VolleyError error) -> handleVolleyAuthError(error, this::fetchPrescriptions)
        ) {
            @Override public Map<String, String> getHeaders() {
                return buildHeaders(false);
            }
        };

        VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(req);
    }

    private void promptNewPrescription() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        // Username only (no patient ID)
        final EditText etUsername = new EditText(this);
        etUsername.setHint("Patient Username (required)");
        etUsername.setInputType(InputType.TYPE_CLASS_TEXT);

        final EditText etDrug = new EditText(this);
        etDrug.setHint("Drug");
        etDrug.setInputType(InputType.TYPE_CLASS_TEXT);

        final EditText etDosage = new EditText(this);
        etDosage.setHint("Dosage (e.g., 10 mg)");
        etDosage.setInputType(InputType.TYPE_CLASS_TEXT);

        final EditText etDirections = new EditText(this);
        etDirections.setHint("Directions (SIG)");
        etDirections.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        final EditText etRefill = new EditText(this);
        etRefill.setHint("Refill amount (leave blank for 0)");
        etRefill.setInputType(InputType.TYPE_CLASS_NUMBER);

        root.addView(etUsername);
        root.addView(etDrug);
        root.addView(etDosage);
        root.addView(etDirections);
        root.addView(etRefill);

        new AlertDialog.Builder(this)
                .setTitle("Write Prescription")
                .setView(root)
                .setPositiveButton("Submit", (d, w) -> {
                    String drug = etDrug.getText().toString().trim();
                    String dosage = etDosage.getText().toString().trim();
                    String directions = etDirections.getText().toString().trim();
                    String username = etUsername.getText().toString().trim();
                    String refillStr = etRefill.getText().toString().trim();

                    if (drug.isEmpty() || dosage.isEmpty() || directions.isEmpty()) {
                        Toast.makeText(getApplicationContext(), "Fill drug, dosage, and directions", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (username.isEmpty()) {
                        Toast.makeText(getApplicationContext(), "Provide patient username", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int refillAmount = 0;
                    if (!refillStr.isEmpty()) {
                        try {
                            refillAmount = Integer.parseInt(refillStr);
                        } catch (NumberFormatException e) {
                            Toast.makeText(getApplicationContext(), "Refill amount must be a valid number", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (refillAmount <= 0) {
                            Toast.makeText(getApplicationContext(), "Refill amount must be positive or left blank", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    JSONObject body = new JSONObject();
                    try {
                        body.put("patientUsername", username);
                        body.put("medication", drug);
                        body.put("dosage", dosage);
                        body.put("notes", directions);
                        body.put("refill", refillAmount);
                    } catch (Exception ignored) {}

                    submitPrescription(body);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void submitPrescription(JSONObject body) {
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST, URL_PRESC_WRITE, body,
                resp -> {
                    Toast.makeText(getApplicationContext(), "Prescription submitted", Toast.LENGTH_SHORT).show();
                    fetchPrescriptions();
                },
                (VolleyError err) -> handleVolleyAuthError(err, () -> submitPrescription(body))
        ) {
            @Override public Map<String, String> getHeaders() {
                return buildHeaders(true);
            }
        };

        VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(req);
    }

    static class Prescription {
        long id;
        String patientUsername;
        String drug;
        String dosage;
        String directions;
        int refillAmount;
        String createdAt;

        @Override
        public String toString() {
            return drug + " " + dosage + " (Refills: " + refillAmount + ")";
        }
    }
}
