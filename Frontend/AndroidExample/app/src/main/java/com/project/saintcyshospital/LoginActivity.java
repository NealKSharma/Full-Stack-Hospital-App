package com.project.saintcyshospital;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends BaseActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView signupLink;

    private static final String PREFS = "auth_prefs";
    private static final String KEY_LAST_USER  = "last_user";
    private static final String KEY_LAST_ROLE  = "last_role";
    private static final String KEY_LAST_EMAIL = "last_email";

    private static final String URL_LOGIN = "http://coms-3090-041.class.las.iastate.edu:8080/api/login";
    private static final String TAG_LOGIN = "login_req";

    private static final String KEY_ACCESS_TOKEN  = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameEditText = findViewById(R.id.login_username_edt);
        passwordEditText = findViewById(R.id.login_password_edt);
        loginButton      = findViewById(R.id.login_login_btn);
        signupLink       = findViewById(R.id.login_signup_link);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String savedUser = prefs.getString(KEY_LAST_USER, "");
        if (!savedUser.isEmpty()) {
            usernameEditText.setText(savedUser);
            passwordEditText.requestFocus();
        }

        String prefill = getIntent().getStringExtra("USERNAME");
        if (prefill != null && !prefill.isEmpty()) {
            usernameEditText.setText(prefill);
            passwordEditText.requestFocus();
        }

        loginButton.setOnClickListener(v -> doLogin());
        signupLink.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignupActivity.class)));
    }

    private void doLogin() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(getApplicationContext(),
                    "Enter username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        loginButton.setEnabled(false);

        JSONObject body = new JSONObject();
        try {
            body.put("username", username);
            body.put("password", password);

        } catch (JSONException e) {
            loginButton.setEnabled(true);
            Toast.makeText(getApplicationContext(),
                    "Failed to build request", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                URL_LOGIN,
                body,
                response -> {
                    loginButton.setEnabled(true);

                    String access  = response.optString("accessToken", null);
                    String refresh = response.optString("refreshToken", null);
                    String respRole  = response.optString("role", null);
                    String respEmail = response.optString("email", null);

                    if (access == null || access.isEmpty()) {
                        Toast.makeText(getApplicationContext(),
                                "Bad response from server (no access token)",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    AuthManager.saveTokens(getApplicationContext(), access, refresh);

                    getSharedPreferences(PREFS, MODE_PRIVATE)
                            .edit()
                            .putString(KEY_ACCESS_TOKEN, access)
                            .putString(KEY_REFRESH_TOKEN, refresh)
                            .putString(KEY_LAST_USER, username)
                            .putString(KEY_LAST_ROLE, respRole)
                            .putString(KEY_LAST_EMAIL, respEmail)
                            .apply();

                    Intent i = new Intent(LoginActivity.this, HomeActivity.class);
                    i.putExtra("USERNAME", username);
                    startActivity(i);
                    finish();
                },
                (VolleyError error) -> {
                    loginButton.setEnabled(true);

                    String msg = "Login failed";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String bodyStr = new String(error.networkResponse.data, "UTF-8");
                            JSONObject obj = new JSONObject(bodyStr);
                            String serverMsg = obj.optString("message", null);
                            if (serverMsg != null && !serverMsg.isEmpty()) {
                                msg = serverMsg;
                            }
                        } catch (Exception ignored) {}
                    }
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                }
        );

        req.setTag(TAG_LOGIN);
        VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(req);
    }

    @Override
    protected void onStop() {
        super.onStop();
        VolleySingleton.getInstance(getApplicationContext())
                .getRequestQueue()
                .cancelAll(TAG_LOGIN);
    }

    @Override
    protected boolean requiresAuth() {
        return false;
    }
}
