package com.project.saintcyshospital;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.core.view.ViewCompat;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.project.saintcyshospital.ws.WsHub;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends BaseActivity {

    private static final String PREFS = "auth_prefs";
    private static final String KEY_LAST_USER = "last_user";
    private static final String KEY_LAST_EMAIL = "last_email";

    private static final String URL_ME_PUT   = "http://coms-3090-041.class.las.iastate.edu:8080/api/user/update";
    private static final String URL_ME_DELETE= "http://coms-3090-041.class.las.iastate.edu:8080/api/user/delete";

    private EditText etUser, etEmail, etPass;
    private Button btnEdit, btnSave, btnDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setupBottomNav(0);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (view, insets) -> {
            int left = view.getPaddingLeft();
            int top = view.getPaddingTop();
            int right = view.getPaddingRight();

            view.setPadding(left, top, right, 0);
            return insets;
        });

        etUser = findViewById(R.id.pi_username);
        etEmail= findViewById(R.id.pi_email);
        etPass = findViewById(R.id.pi_password);
        btnEdit= findViewById(R.id.pi_edit_btn);
        btnSave= findViewById(R.id.pi_save_btn);
        btnDelete = findViewById(R.id.pi_delete_btn);

        String username = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_LAST_USER, "");
        String email    = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_LAST_EMAIL, "");
        etUser.setText(username);
        etEmail.setText(email);
        etPass.setText("");

        btnEdit.setOnClickListener(v -> setEditing(true));
        btnSave.setOnClickListener(v -> doSave());
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void setEditing(boolean editing) {
        etUser.setEnabled(editing);
        etEmail.setEnabled(editing);
        etPass.setEnabled(editing);
        btnSave.setEnabled(editing);
    }

    private void doSave() {
        btnSave.setEnabled(false);

        String newUser = etUser.getText().toString().trim();
        String newEmail= etEmail.getText().toString().trim();
        String newPass = etPass.getText().toString();

        JSONObject body = new JSONObject();
        try {
            body.put("username", newUser);
            body.put("email", newEmail);
            if (!newPass.isEmpty()) body.put("password", newPass);
        } catch (JSONException ignored) {}

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.PUT,
                URL_ME_PUT,
                body,
                resp -> {
                    getSharedPreferences(PREFS, MODE_PRIVATE)
                            .edit()
                            .putString(KEY_LAST_USER, newUser)
                            .putString(KEY_LAST_EMAIL, newEmail)
                            .apply();

                    Toast.makeText(getApplicationContext(), "Saved", Toast.LENGTH_SHORT).show();
                    setEditing(false);
                    btnSave.setEnabled(true);
                    etPass.setText("");
                },
                (VolleyError error) -> handleVolleyAuthError(error, () -> {
                    btnSave.setEnabled(true);
                    doSave();
                })
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                String access = AuthManager.getAccessToken(getApplicationContext());
                if (access != null) h.put("Authorization", "Bearer " + access);
                h.put("Content-Type", "application/json");
                return h;
            }
        };

        VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(req);
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("This will permanently remove your account. Continue?")
                .setPositiveButton("Delete", (d, w) -> doDelete())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void doDelete() {
        StringRequest req = new StringRequest(
                Request.Method.DELETE,
                URL_ME_DELETE,
                resp -> {
                    Toast.makeText(getApplicationContext(), "Account deleted", Toast.LENGTH_LONG).show();
                    WsHub.get().stop();
                    AuthManager.clear(getApplicationContext());
                    Intent i = new Intent(SettingsActivity.this, HomeActivity.class);
                    startActivity(i);
                },
                (VolleyError error) -> handleVolleyAuthError(error, () -> doDelete())
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                String access = AuthManager.getAccessToken(getApplicationContext());
                if (access != null) h.put("Authorization", "Bearer " + access);
                return h;
            }
        };

        VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(req);
    }
}
