package com.project.saintcyshospital;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.VolleyError;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONObject;
import org.json.JSONException;

public class SignupActivity extends BaseActivity {

    private EditText usernameEditText;  // define username edittext variable
    private EditText emailEditText;         // define email edittext variable
    private EditText passwordEditText;  // define password edittext variable
    private EditText confirmEditText;   // define confirm edittext variable
    private TextView loginLink;         // define login button variable
    private Button signupButton;        // define signup button variable

    private static final String URL_SIGNUP = "http://coms-3090-041.class.las.iastate.edu:8080/api/signup";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        /* initialize UI elements */
        usernameEditText = findViewById(R.id.signup_username_edt);  // link to username edtext in the Signup activity XML
        emailEditText = findViewById(R.id.signup_email_edt);    // link to email edText in the Signup activity XML
        passwordEditText = findViewById(R.id.signup_password_edt);  // link to password edtext in the Signup activity XML
        confirmEditText = findViewById(R.id.signup_password_confirm_edt);    // link to confirm edtext in the Signup activity XML
        loginLink = findViewById(R.id.signup_login_link);    // link to login button in the Signup activity XML
        signupButton = findViewById(R.id.signup_signup_btn);  // link to signup button in the Signup activity XML

        /* click listener on login button pressed */
        loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /* when login button is pressed, use intent to switch to Login Activity */
                Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                startActivity(intent);  // go to LoginActivity
            }
        });

        /* click listener on signup button pressed */
        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /* grab strings from user inputs */
                String username = usernameEditText.getText().toString();
                String email = emailEditText.getText().toString();
                String password = passwordEditText.getText().toString();
                String confirm = confirmEditText.getText().toString();

                if (!password.equals(confirm)) {
                    Toast.makeText(getApplicationContext(), "Passwords don't match", Toast.LENGTH_LONG).show();
                    return;
                }

                if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                signupButton.setEnabled(false);

                JSONObject body = new JSONObject();
                try {
                    body.put("username", username);
                    body.put("email", email);
                    body.put("password", password);
                } catch (JSONException e) {
                    signupButton.setEnabled(true);
                    Toast.makeText(getApplicationContext(), "Failed to build request", Toast.LENGTH_SHORT).show();
                    return;
                }

                JsonObjectRequest req = new JsonObjectRequest(
                        Request.Method.POST,
                        URL_SIGNUP,
                        body,
                        response -> {
                            Intent i = new Intent(SignupActivity.this, LoginActivity.class);
                            Toast.makeText(getApplicationContext(), response.toString(), Toast.LENGTH_LONG).show();
                            i.putExtra("USERNAME", username);
                            startActivity(i);
                        },
                        (VolleyError error) -> {
                            signupButton.setEnabled(true);
                            Toast.makeText(getApplicationContext(), "Signup failed", Toast.LENGTH_SHORT).show();
                        }
                );

                VolleySingleton.getInstance(getApplicationContext())
                        .addToRequestQueue(req);
            }
        });
    }
    @Override
    protected boolean requiresAuth() {
        return false;
    }
}