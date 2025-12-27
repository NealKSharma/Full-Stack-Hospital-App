package com.project.saintcyshospital;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;
import org.json.JSONObject;

public class AdminActivity extends BaseActivity {
    private static final String URL_USER_CREATE = "http://coms-3090-041.class.las.iastate.edu:8080/api/admin/user";
    private static final String URL_USER_READ_BY_USERNAME_BASE = "http://coms-3090-041.class.las.iastate.edu:8080/api/admin/user/";
    private static final String URL_USER_UPDATE_BASE = "http://coms-3090-041.class.las.iastate.edu:8080/api/admin/user/";
    private static final String URL_USER_DELETE_BASE = "http://coms-3090-041.class.las.iastate.edu:8080/api/admin/user/";

    private static final String URL_MED_CREATE = "http://coms-3090-041.class.las.iastate.edu:8080/api/admin/products";
    private static final String URL_MED_READ_BASE = "http://coms-3090-041.class.las.iastate.edu:8080/api/admin/products/";
    private static final String URL_MED_UPDATE_BASE = "http://coms-3090-041.class.las.iastate.edu:8080/api/admin/products/";
    private static final String URL_MED_DELETE_BASE = "http://coms-3090-041.class.las.iastate.edu:8080/api/notification/products/";

    private static final String URL_BROADCAST = "http://coms-3090-041.class.las.iastate.edu:8080/api/notifications/broadcast";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);
        setupBottomNav(0);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (view, insets) -> {
            int left = view.getPaddingLeft();
            int top = view.getPaddingTop();
            int right = view.getPaddingRight();

            view.setPadding(left, top, right, 0);
            return insets;
        });

        Button uCreate = findViewById(R.id.btnUserCreate);
        Button uRead   = findViewById(R.id.btnUserRead);
        Button uUpdate = findViewById(R.id.btnUserUpdate);
        Button uDelete = findViewById(R.id.btnUserDelete);

        Button mCreate = findViewById(R.id.btnMedCreate);
        Button mRead   = findViewById(R.id.btnMedRead);
        Button mUpdate = findViewById(R.id.btnMedUpdate);
        Button mDelete = findViewById(R.id.btnMedDelete);

        Button bcast = findViewById(R.id.btnBroadcast);


        uCreate.setOnClickListener(v -> promptUserCreate());
        uRead.setOnClickListener(v -> promptUserRead());
        uUpdate.setOnClickListener(v -> promptUserUpdate());
        uDelete.setOnClickListener(v -> promptUserDelete());

        mCreate.setOnClickListener(v -> promptMedCreate());
        mRead.setOnClickListener(v -> promptMedRead());
        mUpdate.setOnClickListener(v -> promptMedUpdate());
        mDelete.setOnClickListener(v -> promptMedDelete());

        bcast.setOnClickListener(v -> promptBroadcast());
    }

    private LinearLayout formLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad / 2);
        root.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return root;
    }

    private EditText addField(LinearLayout parent, String hint, int inputType) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setInputType(inputType);
        parent.addView(e);
        return e;
    }

    private Spinner addRoleSpinner(LinearLayout parent) {
        Spinner s = new Spinner(this);
        String[] roles = new String[] { "PATIENT", "DOCTOR", "ADMIN", "PHARMACIST"};
        ArrayAdapter<String> aa = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(aa);
        parent.addView(s);
        return s;
    }

    private void applyDevRetryPolicy(com.android.volley.Request<?> req) {
        req.setRetryPolicy(new DefaultRetryPolicy(5000, 1, 1f));
    }

    private void promptUserCreate() {
        LinearLayout f = formLayout();
        EditText email    = addField(f, "Email", InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        EditText username = addField(f, "Username", InputType.TYPE_CLASS_TEXT);
        Spinner  role     = addRoleSpinner(f);
        EditText pass     = addField(f, "Password", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle("Create User")
                .setView(f)
                .setPositiveButton("Create", (d, w) -> {
                    JSONObject body = new JSONObject();
                    try {
                        body.put("email", email.getText().toString().trim());
                        body.put("username", username.getText().toString().trim());
                        body.put("role", role.getSelectedItem().toString());
                        body.put("password", pass.getText().toString());
                    } catch (JSONException ignored) {}

                    JsonObjectRequest req = new JsonObjectRequest(
                            Request.Method.POST,
                            URL_USER_CREATE,
                            body,
                            resp -> showJson("User Created", resp.toString()),
                            (VolleyError err) -> toast("Create user failed")
                    );
                    applyDevRetryPolicy(req);
                    VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(req);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void promptUserRead() {
        LinearLayout f = formLayout();
        EditText username = addField(f, "Username", android.text.InputType.TYPE_CLASS_TEXT);

        new AlertDialog.Builder(this)
                .setTitle("Read User by Username")
                .setView(f)
                .setPositiveButton("Fetch", (d, w) -> {
                    String u = username.getText().toString().trim();
                    if (u.isEmpty()) { toast("Enter a username"); return; }

                    // Encode path segment
                    String url = URL_USER_READ_BY_USERNAME_BASE + android.net.Uri.encode(u);

                    com.android.volley.toolbox.JsonObjectRequest req =
                            new com.android.volley.toolbox.JsonObjectRequest(
                                    com.android.volley.Request.Method.GET,
                                    url,
                                    null,
                                    resp -> showJson("User", resp.toString()),
                                    (com.android.volley.VolleyError err) -> toast("Read user failed")
                            );
                    applyDevRetryPolicy(req);
                    VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(req);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void promptUserUpdate() {
        LinearLayout f = formLayout();
        EditText id       = addField(f, "User ID", InputType.TYPE_CLASS_NUMBER);
        EditText email    = addField(f, "New Email (optional)", InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        EditText username = addField(f, "New Username (optional)", InputType.TYPE_CLASS_TEXT);
        Spinner  role     = addRoleSpinner(f);

        new AlertDialog.Builder(this)
                .setTitle("Update User")
                .setView(f)
                .setPositiveButton("Update", (d, w) -> {
                    JSONObject body = new JSONObject();
                    try {
                        if (!email.getText().toString().trim().isEmpty())
                            body.put("email", email.getText().toString().trim());
                        if (!username.getText().toString().trim().isEmpty())
                            body.put("username", username.getText().toString().trim());
                        body.put("role", role.getSelectedItem().toString());
                    } catch (JSONException ignored) {}

                    String url = URL_USER_UPDATE_BASE + id.getText().toString().trim();

                    JsonObjectRequest req = new JsonObjectRequest(
                            Request.Method.PUT,
                            url,
                            body,
                            resp -> showJson("User updated", resp.toString()),
                            (VolleyError err) -> toast("Update user failed")
                    );
                    applyDevRetryPolicy(req);
                    VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(req);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void promptUserDelete() {
        LinearLayout f = formLayout();
        EditText id = addField(f, "User ID", InputType.TYPE_CLASS_NUMBER);

        new AlertDialog.Builder(this)
                .setTitle("Delete User")
                .setView(f)
                .setPositiveButton("Delete", (d, w) -> {
                    String url = URL_USER_DELETE_BASE + id.getText().toString().trim();

                    StringRequest req = new StringRequest(
                            Request.Method.DELETE,
                            url,
                            resp -> toast("User deleted"),
                            (VolleyError err) -> toast("Delete user failed")
                    );
                    applyDevRetryPolicy(req);
                    VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(req);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void promptMedCreate() {
        LinearLayout f = formLayout();
        EditText name    = addField(f, "Drug Name", InputType.TYPE_CLASS_TEXT);
        EditText generic = addField(f, "Generic Name", InputType.TYPE_CLASS_TEXT);
        EditText dosage  = addField(f, "Dosage (e.g., 10 mg)", InputType.TYPE_CLASS_TEXT);
        EditText price   = addField(f, "Price (cents, e.g., 1299)", InputType.TYPE_CLASS_NUMBER);
        EditText image   = addField(f, "Image URL (optional)", InputType.TYPE_TEXT_VARIATION_URI);

        TextView protectedLabel = new TextView(this);
        protectedLabel.setText("Protected Status:");
        protectedLabel.setTextSize(16);
        f.addView(protectedLabel);

        android.widget.Spinner protectedSpinner = new android.widget.Spinner(this);
        String[] protectedOptions = new String[] { "No", "Yes" };
        android.widget.ArrayAdapter<String> protectedAdapter =
                new android.widget.ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item,
                        protectedOptions);
        protectedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        protectedSpinner.setAdapter(protectedAdapter);
        protectedSpinner.setPrompt("Protected status");
        f.addView(protectedSpinner);

        new AlertDialog.Builder(this)
                .setTitle("Create Medicine")
                .setView(f)
                .setPositiveButton("Create", (d, w) -> {
                    JSONObject body = new JSONObject();
                    try {
                        body.put("name", name.getText().toString().trim());
                        body.put("genericName", generic.getText().toString().trim());
                        body.put("dosage", dosage.getText().toString().trim());
                        body.put("priceInCents", parseIntSafe(price.getText().toString().trim()));
                        if (!image.getText().toString().trim().isEmpty()) {
                            body.put("imageUrl", image.getText().toString().trim());
                        }

                        boolean isProtected =
                                "Yes".equalsIgnoreCase(String.valueOf(protectedSpinner.getSelectedItem()));
                        body.put("isProtected", isProtected);

                    } catch (JSONException ignored) {}

                    JsonObjectRequest req = new JsonObjectRequest(
                            Request.Method.POST,
                            URL_MED_CREATE,
                            body,
                            resp -> showJson("Medicine created", resp.toString()),
                            (VolleyError err) -> showBigMessage("Error", err.toString())
                    );
                    applyDevRetryPolicy(req);
                    VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(req);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void promptMedRead() {
        LinearLayout f = formLayout();
        EditText name = addField(f, "Medicine Name (Brand)", InputType.TYPE_CLASS_TEXT);

        new AlertDialog.Builder(this)
                .setTitle("Read Medicine")
                .setView(f)
                .setPositiveButton("Fetch", (d, w) -> {
                    String url = URL_MED_READ_BASE + name.getText().toString().trim();

                    JsonObjectRequest req = new JsonObjectRequest(
                            Request.Method.GET,
                            url,
                            null,
                            resp -> showJson("Medicine", resp.toString()),
                            (VolleyError err) -> toast("Read medicine failed")
                    );
                    applyDevRetryPolicy(req);
                    VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(req);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void promptMedUpdate() {
        LinearLayout f = formLayout();
        EditText id      = addField(f, "Medicine ID", InputType.TYPE_CLASS_TEXT);
        EditText name    = addField(f, "New Name (optional)", InputType.TYPE_CLASS_TEXT);
        EditText generic = addField(f, "New Generic (optional)", InputType.TYPE_CLASS_TEXT);
        EditText dosage  = addField(f, "New Dosage (optional)", InputType.TYPE_CLASS_TEXT);
        EditText price   = addField(f, "New Price cents (optional)", InputType.TYPE_CLASS_NUMBER);
        EditText image   = addField(f, "New Image URL (optional)", InputType.TYPE_TEXT_VARIATION_URI);

        TextView protectedLabel = new TextView(this);
        protectedLabel.setText("Protected Status (optional):");
        protectedLabel.setTextSize(16);
        f.addView(protectedLabel);

        Spinner protectedSpinner = new Spinner(this);
        String[] protectedOptions = new String[]{"No Change", "No", "Yes"};
        ArrayAdapter<String> protectedAdapter =
                new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item,
                        protectedOptions);
        protectedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        protectedSpinner.setAdapter(protectedAdapter);
        f.addView(protectedSpinner);

        new AlertDialog.Builder(this)
                .setTitle("Update Medicine")
                .setView(f)
                .setPositiveButton("Update", (d, w) -> {
                    JSONObject body = new JSONObject();
                    try {
                        if (!name.getText().toString().trim().isEmpty())
                            body.put("name", name.getText().toString().trim());
                        if (!generic.getText().toString().trim().isEmpty())
                            body.put("genericName", generic.getText().toString().trim());
                        if (!dosage.getText().toString().trim().isEmpty())
                            body.put("dosage", dosage.getText().toString().trim());
                        if (!price.getText().toString().trim().isEmpty())
                            body.put("priceInCents", parseIntSafe(price.getText().toString().trim()));
                        if (!image.getText().toString().trim().isEmpty())
                            body.put("imageUrl", image.getText().toString().trim());

                        String selected = String.valueOf(protectedSpinner.getSelectedItem());
                        if ("Yes".equalsIgnoreCase(selected)) {
                            body.put("isProtected", true);
                        } else if ("No".equalsIgnoreCase(selected)) {
                            body.put("isProtected", false);
                        }

                    } catch (JSONException ignored) {}

                    String url = URL_MED_UPDATE_BASE + id.getText().toString().trim();

                    JsonObjectRequest req = new JsonObjectRequest(
                            Request.Method.PUT,
                            url,
                            body,
                            resp -> showJson("Medicine updated", resp.toString()),
                            (VolleyError err) -> toast("Update medicine failed")
                    );
                    applyDevRetryPolicy(req);
                    VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(req);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void promptMedDelete() {
        LinearLayout f = formLayout();
        EditText id = addField(f, "Medicine ID", InputType.TYPE_CLASS_TEXT);

        new AlertDialog.Builder(this)
                .setTitle("Delete Medicine")
                .setView(f)
                .setPositiveButton("Delete", (d, w) -> {
                    String url = URL_MED_DELETE_BASE + id.getText().toString().trim();

                    StringRequest req = new StringRequest(
                            Request.Method.DELETE,
                            url,
                            resp -> toast("Medicine deleted"),
                            (VolleyError err) -> toast("Delete medicine failed")
                    );
                    applyDevRetryPolicy(req);
                    VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(req);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void promptBroadcast() {
        LinearLayout f = formLayout();
        EditText title   = addField(f, "Title", InputType.TYPE_CLASS_TEXT);
        EditText content = addField(f, "Content", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        new AlertDialog.Builder(this)
                .setTitle("Broadcast Notification")
                .setView(f)
                .setPositiveButton("Send", (d, w) -> {
                    JSONObject body = new JSONObject();
                    try {
                        body.put("title", title.getText().toString().trim());
                        body.put("content", content.getText().toString().trim());
                    } catch (JSONException ignored) {}

                    JsonObjectRequest req = new JsonObjectRequest(
                            Request.Method.POST,
                            URL_BROADCAST,
                            body,
                            resp -> toast(resp.toString()),
                            (VolleyError err) -> toast(err.toString())
                    ) {
                        @Override
                        public java.util.Map<String, String> getHeaders() {
                            java.util.Map<String, String> h = new java.util.HashMap<>();
                            String access = AuthManager.getAccessToken(getApplicationContext());
                            if (access != null && !access.isEmpty()) {
                                h.put("Authorization", "Bearer " + access);
                            }
                            h.put("Accept", "application/json");
                            h.put("Content-Type", "application/json");
                            return h;
                        }
                    };
                    applyDevRetryPolicy(req);
                    VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(req);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    private void showBigMessage(String title, String message) {
        // Pretty safe width and padding
        int pad = (int) (16 * getResources().getDisplayMetrics().density);

        android.widget.ScrollView scroller = new android.widget.ScrollView(this);
        android.widget.TextView tv = new android.widget.TextView(this);
        tv.setTextIsSelectable(true); // allow copy
        tv.setPadding(pad, pad, pad, pad);
        tv.setText(message);
        tv.setTextSize(14);
        tv.setTypeface(android.graphics.Typeface.MONOSPACE); // nice for JSON
        scroller.addView(tv);

        new android.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setView(scroller)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showJson(String title, String rawJson) {
        String pretty = rawJson;
        try {
            if (rawJson.trim().startsWith("{")) {
                pretty = new org.json.JSONObject(rawJson).toString(2);
            } else if (rawJson.trim().startsWith("[")) {
                pretty = new org.json.JSONArray(rawJson).toString(2);
            }
        } catch (Exception ignored) {}
        showBigMessage(title, pretty);
    }

}

