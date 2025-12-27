package com.project.saintcyshospital;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.ViewCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;

public class CheckoutActivity extends BaseActivity {

    private TextInputEditText nameInput, numberInput, expiryInput, cvcInput, zipInput;
    private TextView totalTxt;
    private Button placeOrderBtn;
    private static final String URL_CHECKOUT =
            "http://coms-3090-041.class.las.iastate.edu:8080/api/pharmacy/orders/place";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);
        setupBottomNav(0);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (view, insets) -> {
            int left = view.getPaddingLeft();
            int top = view.getPaddingTop();
            int right = view.getPaddingRight();

            view.setPadding(left, top, right, 0);
            return insets;
        });

        CartStore.get().init(getApplicationContext());

        nameInput   = findViewById(R.id.input_card_name);
        numberInput = findViewById(R.id.input_card_number);
        expiryInput = findViewById(R.id.input_card_expiry);
        cvcInput    = findViewById(R.id.input_card_cvc);
        zipInput    = findViewById(R.id.input_card_zip);
        totalTxt    = findViewById(R.id.checkout_total_txt);
        placeOrderBtn = findViewById(R.id.place_order_btn);

        totalTxt.setText(String.format("Total: $%.2f", CartStore.get().totalCents() / 100.0));
        placeOrderBtn.setEnabled(CartStore.get().totalCents() > 0);

        placeOrderBtn.setOnClickListener(v -> {
            if (!basicValidate()) {
                Toast.makeText(this, "Please complete all fields.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (CartStore.get().totalCents() <= 0) {
                Toast.makeText(this, "Your cart is empty.", Toast.LENGTH_SHORT).show();
                return;
            }
            submitOrder();
        });

    }

    private boolean basicValidate() {
        return notEmpty(nameInput) && notEmpty(numberInput)
                && notEmpty(expiryInput) && notEmpty(cvcInput) && notEmpty(zipInput);
    }

    private boolean notEmpty(TextInputEditText e) {
        return e.getText() != null && e.getText().toString().trim().length() > 0;
    }

    private void submitOrder() {
        long totalCents = CartStore.get().totalCents();

        org.json.JSONObject body = new org.json.JSONObject();
        try {
            body.put("cardName", safeText(nameInput));
            body.put("cardNumber", safeText(numberInput));
            body.put("cardExpiry", safeText(expiryInput));
            body.put("cardCvc", safeText(cvcInput));
            body.put("zip", safeText(zipInput));
            body.put("totalCents", totalCents);

            org.json.JSONArray itemsArr = new org.json.JSONArray();

            for (CartStore.CartItem ci : CartStore.get().items()) {
                org.json.JSONObject ji = new org.json.JSONObject();
                ji.put("medicineId", ci.medicine.id);
                ji.put("name", ci.medicine.name);
                ji.put("genericName", ci.medicine.genericName);
                ji.put("dosage", ci.medicine.dosage);
                ji.put("priceCents", ci.medicine.priceCents);
                ji.put("quantity", ci.qty);
                itemsArr.put(ji);
            }

            body.put("items", itemsArr);
        } catch (org.json.JSONException ignored) {}

        com.android.volley.toolbox.JsonObjectRequest req =
                new com.android.volley.toolbox.JsonObjectRequest(
                        com.android.volley.Request.Method.POST,
                        URL_CHECKOUT,
                        body,
                        resp -> {
                            Toast.makeText(getApplicationContext(),
                                    "Order placed successfully.",
                                    Toast.LENGTH_LONG).show();
                            CartStore.get().clear();
                            Intent i = new Intent(this, HomeActivity.class);
                            startActivity(i);
                            finish();
                        },
                        err -> handleVolleyAuthError(err, this::submitOrder)
                ) {
                    @Override
                    public java.util.Map<String, String> getHeaders() {
                        java.util.Map<String, String> h = new java.util.HashMap<>();
                        String access = AuthManager.getAccessToken(getApplicationContext());
                        if (access != null && !access.isEmpty()) {
                            h.put("Authorization", "Bearer " + access);
                        }
                        h.put("Content-Type", "application/json");
                        h.put("Accept", "application/json");
                        return h;
                    }
                };

        VolleySingleton
                .getInstance(getApplicationContext())
                .addToRequestQueue(req);
    }

    private String safeText(TextInputEditText e) {
        return (e.getText() != null) ? e.getText().toString().trim() : "";
    }

}
