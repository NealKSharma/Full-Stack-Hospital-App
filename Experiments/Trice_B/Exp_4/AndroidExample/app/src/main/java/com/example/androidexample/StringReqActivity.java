package com.example.androidexample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

public class StringReqActivity extends AppCompatActivity {

    // UI components
    private Button btnStringReq;
    private TextView msgResponse;

    // API URL for fetching string response
    private static final String URL_STRING_REQ = "https://jsonplaceholder.typicode.com/users/1";
    // Alternative URLs for testing purposes
    // public static final String URL_STRING_REQ = "https://2aa87adf-ff7c-45c8-89bc-f3fbfaa16d15.mock.pstmn.io/users/1";
    // public static final String URL_STRING_REQ = "http://10.0.2.2:8080/users/1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_string_req);

        // Initializing UI components
        btnStringReq = findViewById(R.id.btnStringReq);
        msgResponse = findViewById(R.id.msgResponse);

        // Setting click listener on the button to trigger the string request
        btnStringReq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeStringReq();
            }
        });
    }

    /**
     * Makes a string request using Volley library
     **/
    private void makeStringReq() {
        // Creating a new String request
        StringRequest stringRequest = new StringRequest(
                Request.Method.GET, // HTTP method (GET request)
                URL_STRING_REQ, // API URL
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Log the response for debugging purposes
                        Log.d("Volley Response", response);

                        // Display response in the TextView
                        msgResponse.setText(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Log the error details
                        Log.e("Volley Error", error.toString());

                        // Show an error message in the UI
                        msgResponse.setText("Failed to load data. Please try again.");
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                // Headers for the request (if needed)
                Map<String, String> headers = new HashMap<>();
                // Example headers (uncomment if needed)
                // headers.put("Authorization", "Bearer YOUR_ACCESS_TOKEN");
                // headers.put("Content-Type", "application/json");
                return headers;
            }

            @Override
            protected Map<String, String> getParams() {
                // Parameters for the request (if needed)
                Map<String, String> params = new HashMap<>();
                // Example parameters (uncomment if needed)
                // params.put("param1", "value1");
                // params.put("param2", "value2");
                return params;
            }
        };

        // Adding request to the Volley request queue
        VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(stringRequest);
    }
}
