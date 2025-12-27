package com.project.saintcyshospital;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.List;
import java.util.Map;

public class HistoriesActivity extends BaseActivity {

    private ArrayAdapter<Patient> adapter;
    private final List<Patient> data = new ArrayList<>();

    private static final String BASE_API = "http://coms-3090-041.class.las.iastate.edu:8080";
    private static final String URL_PATIENTS = BASE_API + "/patients";
    private static final String URL_ADDITIONAL = BASE_API + "/patients/";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_histories);
        setupBottomNav(0);

        // Doctors only
        if (!AuthManager.isDoctor(getApplicationContext())) {
            Toast.makeText(
                    getApplicationContext(),
                    "Patient histories are only available to doctors.",
                    Toast.LENGTH_LONG
            ).show();
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }

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

        ListView list = findViewById(R.id.patientList);
        EditText search = findViewById(R.id.searchPatients);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, data);
        list.setAdapter(adapter);

        // Filter by name/MRN
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                adapter.getFilter().filter(s);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        list.setOnItemClickListener((parent, view, position, id) -> {
            Patient p = adapter.getItem(position);
            if (p != null) loadPatientDetails(p);
        });

        fetchPatients();
    }

    private static String clean(String s) {
        if (s == null) return "—";
        String t = s.trim();
        if (t.isEmpty() || t.equalsIgnoreCase("null")) return "—";
        return t;
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

    private void fetchPatients() {
        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET,
                URL_PATIENTS,
                null,
                (JSONArray resp) -> {
                    data.clear();
                    for (int i = 0; i < resp.length(); i++) {
                        JSONObject o = resp.optJSONObject(i);
                        if (o == null) continue;

                        long id = o.optLong("id");

                        // Either "name" or "username"
                        String name = o.optString("name", "");
                        if (name.isEmpty()) {
                            name = o.optString("username", "");
                        }

                        String mrn = o.optString("mrn", "");

                        String dob = o.optString("dob", "");
                        if (dob.isEmpty()) {
                            dob = o.optString("dateOfBirth", "");
                        }

                        String gender = o.optString("gender", "");
                        if (gender.isEmpty()) {
                            gender = o.optString("sex", "");
                        }

                        data.add(new Patient(id, name, mrn, dob, gender));
                    }
                    adapter.notifyDataSetChanged();
                },
                (VolleyError error) -> handleVolleyAuthError(error, this::fetchPatients)
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return buildHeaders();
            }
        };

        VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(req);
    }

    private void loadPatientDetails(Patient p) {
        String url = URL_ADDITIONAL + p.id + "/additional";

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                details -> showPatientDialog(p, details),
                err -> {
                    int code = (err.networkResponse != null)
                            ? err.networkResponse.statusCode : -1;

                    if (code == 401) {
                        handleVolleyAuthError(err, () -> loadPatientDetails(p));
                    } else {
                        showPatientDialog(p, null);
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return buildHeaders();
            }
        };

        VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(req);
    }

    private void showPatientDialog(Patient p, JSONObject details) {
        int pad = (int) (16 * getResources().getDisplayMetrics().density);

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(pad, pad, pad, pad);
        scroll.addView(layout);

        TextView headerBasic = new TextView(this);
        headerBasic.setText("Basic Information");
        headerBasic.setTextSize(16f);
        headerBasic.setTypeface(headerBasic.getTypeface(), android.graphics.Typeface.BOLD);
        layout.addView(headerBasic);

        String basicText =
                "Name: "   + clean(p.name)   + "\n" +
                        "MRN: "    + clean(p.mrn)    + "\n" +
                        "DOB: "    + clean(p.dob)    + "\n" +
                        "Gender: " + clean(p.gender);

        TextView bodyBasic = new TextView(this);
        bodyBasic.setText(basicText);
        bodyBasic.setPadding(0, (int) (8 * getResources().getDisplayMetrics().density), 0,
                (int) (12 * getResources().getDisplayMetrics().density));
        layout.addView(bodyBasic);

        final String symptomsFinal;
        final String medsFinal;
        final JSONArray appts;
        final JSONArray prescriptions;

        if (details != null) {
            symptomsFinal   = details.optString("symptoms", "");
            medsFinal       = details.optString("medications", "");
            appts           = details.optJSONArray("appointments");
            prescriptions   = details.optJSONArray("prescriptions");
        } else {
            symptomsFinal   = "";
            medsFinal       = "";
            appts           = null;
            prescriptions   = null;
        }

        TextView headerClinical = new TextView(this);
        headerClinical.setText("Clinical Notes");
        headerClinical.setTextSize(16f);
        headerClinical.setTypeface(headerClinical.getTypeface(), android.graphics.Typeface.BOLD);
        layout.addView(headerClinical);

        StringBuilder clinical = new StringBuilder();
        clinical.append("Symptoms: ");
        clinical.append(symptomsFinal.isEmpty() ? "None recorded" : symptomsFinal);
        clinical.append("\nMedications: ");
        clinical.append(medsFinal.isEmpty() ? "None recorded" : medsFinal);

        TextView bodyClinical = new TextView(this);
        bodyClinical.setText(clinical.toString());
        bodyClinical.setPadding(0, (int) (8 * getResources().getDisplayMetrics().density), 0,
                (int) (12 * getResources().getDisplayMetrics().density));
        layout.addView(bodyClinical);

        TextView headerAppt = new TextView(this);
        headerAppt.setText("Upcoming Appointments");
        headerAppt.setTextSize(16f);
        headerAppt.setTypeface(headerAppt.getTypeface(), android.graphics.Typeface.BOLD);
        layout.addView(headerAppt);

        TextView bodyAppt = new TextView(this);
        StringBuilder apptText = new StringBuilder();
        if (appts != null && appts.length() > 0) {
            for (int i = 0; i < appts.length(); i++) {
                JSONObject a = appts.optJSONObject(i);
                if (a == null) continue;

                String date   = clean(a.optString("date", null));
                String time   = clean(a.optString("time", null));
                String doctor = clean(a.optString("doctor", null));
                String status = clean(a.optString("status", null));

                if (apptText.length() > 0) apptText.append("\n");
                apptText.append("• ").append(date);
                if (!time.equals("—"))   apptText.append(" ").append(time);
                if (!doctor.equals("—")) apptText.append(" • Dr. ").append(doctor);
                if (!status.equals("—")) apptText.append(" • ").append(status);
            }
        } else {
            apptText.append("No upcoming appointments.");
        }
        bodyAppt.setText(apptText.toString());
        bodyAppt.setPadding(0, (int) (4 * getResources().getDisplayMetrics().density), 0,
                (int) (12 * getResources().getDisplayMetrics().density));
        layout.addView(bodyAppt);

        TextView headerRx = new TextView(this);
        headerRx.setText("Prescriptions");
        headerRx.setTextSize(16f);
        headerRx.setTypeface(headerRx.getTypeface(), android.graphics.Typeface.BOLD);
        layout.addView(headerRx);

        TextView bodyRx = new TextView(this);
        StringBuilder rxText = new StringBuilder();
        if (prescriptions != null && prescriptions.length() > 0) {
            for (int i = 0; i < prescriptions.length(); i++) {
                JSONObject m = prescriptions.optJSONObject(i);
                if (m == null) continue;

                String name = clean(m.optString("name", null));
                String dose = clean(m.optString("dose", null));
                String freq = clean(m.optString("freq", null));

                if (rxText.length() > 0) rxText.append("\n");
                rxText.append("• ").append(name);
                if (!dose.equals("—")) rxText.append(" • ").append(dose);
                if (!freq.equals("—")) rxText.append(" • ").append(freq);
            }
        } else {
            rxText.append("No active prescriptions.");
        }
        bodyRx.setText(rxText.toString());
        bodyRx.setPadding(0, (int) (4 * getResources().getDisplayMetrics().density), 0, 0);
        layout.addView(bodyRx);

        // Dialog buttons
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(clean(p.name))
                .setView(scroll)
                .setPositiveButton("Close", null)
                .setNeutralButton("Edit",
                        (d, which) -> showEditDialog(p, symptomsFinal, medsFinal))
                .create();

        dialog.show();
    }

    // Edit dialog that starts with existing symptoms/meds
    private void showEditDialog(Patient p, String existingSymptoms, String existingMeds) {
        int pad = (int) (16 * getResources().getDisplayMetrics().density);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(pad, pad, pad, pad);

        EditText etSymptoms = new EditText(this);
        etSymptoms.setHint("Symptoms");
        if (!"None recorded".equals(existingSymptoms) && !existingSymptoms.isEmpty()) {
            etSymptoms.setText(existingSymptoms);
        }

        EditText etMeds = new EditText(this);
        etMeds.setHint("Current medications");
        if (!"None recorded".equals(existingMeds) && !existingMeds.isEmpty()) {
            etMeds.setText(existingMeds);
        }

        layout.addView(etSymptoms);
        layout.addView(etMeds);

        new AlertDialog.Builder(this)
                .setTitle("Edit " + clean(p.name))
                .setView(layout)
                .setPositiveButton("Save", (d, which) -> {
                    String symptoms = etSymptoms.getText().toString().trim();
                    String meds = etMeds.getText().toString().trim();
                    updateAdditional(p, symptoms, meds);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateAdditional(Patient p, String symptoms, String medications) {
        String url = URL_ADDITIONAL + p.id + "/additional";

        JSONObject body = new JSONObject();
        try {
            body.put("symptoms", symptoms);
            body.put("medications", medications);
        } catch (Exception ignored) {}

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.PUT,
                url,
                body,
                resp -> {
                    Toast.makeText(getApplicationContext(), "Updated", Toast.LENGTH_SHORT).show();
                    // Reload details
                    loadPatientDetails(p);
                },
                err -> handleVolleyAuthError(err, () -> updateAdditional(p, symptoms, medications))
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return buildHeaders();
            }
        };

        VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(req);
    }
}