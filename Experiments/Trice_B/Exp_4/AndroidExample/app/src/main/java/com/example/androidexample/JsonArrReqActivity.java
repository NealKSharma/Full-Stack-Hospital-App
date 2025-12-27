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
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONArray;

import java.util.HashMap;
import java.util.Map;

public class JsonArrReqActivity extends AppCompatActivity {

    // UI components
    private Button btnJsonArrReq;
    private TextView msgResponse;

    // API URL for fetching JSON data
    private static final String URL_JSON_ARRAY = "https://jsonplaceholder.typicode.com/users";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_json_arr_req);

        // Initializing UI components
        btnJsonArrReq = findViewById(R.id.btnJsonArr);
        msgResponse = findViewById(R.id.msgResponse);

        // Setting click listener on the button to make JSON array request
        btnJsonArrReq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeJsonArrayReq();
            }
        });
    }

    /**
     * Makes a JSON array request using Volley library
     */
    private void makeJsonArrayReq() {
        // Creating a new JSON array request
        JsonArrayRequest jsonArrReq = new JsonArrayRequest(
                Request.Method.GET, // HTTP method (GET request)
                URL_JSON_ARRAY, // API URL
                null, // Request body (null for GET request)
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        // Log the response for debugging purposes
                        Log.d("Volley Response", response.toString());

                        // Display response in the TextView
                        msgResponse.setText(response.toString());
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
                }) {
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
        VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonArrReq);
    }
}
