package com.project.saintcyshospital;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.VolleyError;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

public class PharmacyActivity extends BaseActivity {

    private ListView medsList;
    private LinearLayout openCartBtn;
    private TextView cartBadge;

    private final ArrayList<Medicine> meds = new ArrayList<>();
    private ArrayAdapter<Medicine> adapter;
    private int openPosition = -1;


    private static final String URL_MED_LIST = "http://coms-3090-041.class.las.iastate.edu:8080/api/products";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pharmacy);
        setupBottomNav(R.id.nav_pharmacy);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (view, insets) -> {
            int left = view.getPaddingLeft();
            int top = view.getPaddingTop();
            int right = view.getPaddingRight();

            view.setPadding(left, top, right, 0);
            return insets;
        });

        medsList = findViewById(R.id.meds_list);
        android.view.animation.LayoutAnimationController controller = android.view.animation.AnimationUtils.loadLayoutAnimation(this, R.anim.layout_fall_down);
        medsList.setLayoutAnimation(controller);

        openCartBtn = findViewById(R.id.open_cart_btn);
        cartBadge = findViewById(R.id.cart_badge);

        updateCartBadge();

        adapter = new ArrayAdapter<Medicine>(this, R.layout.item_med, meds) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View v = convertView;
                if (v == null) {
                    v = getLayoutInflater().inflate(R.layout.item_med, parent, false);
                }

                Medicine m = getItem(position);

                com.google.android.material.card.MaterialCardView card =
                        v.findViewById(R.id.med_card);
                android.view.View addBg = v.findViewById(R.id.add_to_cart_bg);
                android.view.View addIcon = v.findViewById(R.id.add_to_cart_icon);

                android.widget.TextView brand   = v.findViewById(R.id.med_brand_name);
                android.widget.TextView generic = v.findViewById(R.id.med_generic_name);
                android.widget.TextView dosage  = v.findViewById(R.id.med_dosage);
                android.widget.TextView price   = v.findViewById(R.id.med_price);

                brand.setText(m.name);
                generic.setText(m.genericName);
                dosage.setText(m.dosage);
                price.setText(String.format("$%.2f", m.priceCents / 100.0));

                float openOffset = dpToPx(91);
                if (position == openPosition) {
                    card.setTranslationX(-openOffset);
                } else {
                    card.setTranslationX(0f);
                }

                card.setOnClickListener(view -> {
                    float target;
                    if (openPosition == position) {
                        openPosition = -1;
                        target = 0f;
                    } else {
                        int oldPos = openPosition;
                        openPosition = position;
                        target = -openOffset;

                        if (oldPos != -1) {
                            int first = medsList.getFirstVisiblePosition();
                            int last  = medsList.getLastVisiblePosition();
                            if (oldPos >= first && oldPos <= last) {
                                android.view.View oldView = medsList.getChildAt(oldPos - first);
                                if (oldView != null) {
                                    com.google.android.material.card.MaterialCardView oldCard =
                                            oldView.findViewById(R.id.med_card);
                                    if (oldCard != null) {
                                        oldCard.animate().translationX(0f).setDuration(140).start();
                                    }
                                }
                            }
                        }
                    }
                    card.animate().translationX(target).setDuration(140).start();
                });

                addBg.setOnClickListener(view -> {
                    CartStore.get().add(m);
                    updateCartBadge();
                    Toast.makeText(getContext(), "Added: " + m.name, Toast.LENGTH_SHORT).show();

                    openPosition = -1;
                    card.animate().translationX(0f).setDuration(140).start();
                });
                addIcon.setOnClickListener(vv -> addBg.performClick());

                return v;
            }
        };

        medsList.setAdapter(adapter);

        openCartBtn.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));

        fetchMedicines();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCartBadge();
    }
    private void fetchMedicines() {
        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET,
                URL_MED_LIST,
                null,
                resp -> {
                    meds.clear();
                    for (int i = 0; i < resp.length(); i++) {
                        org.json.JSONObject o = resp.optJSONObject(i);
                        if (o == null) continue;

                        String id    = o.optString("id", "");
                        String name  = o.optString("name", "");
                        String gen   = o.optString("genericName", "");
                        String dose  = o.optString("dosage", "");
                        int price    = o.optInt("priceInCents", 0);
                        String img   = o.optString("imageUrl", null);

                        meds.add(new Medicine(id, name, gen, dose, price, img));
                    }
                    adapter.notifyDataSetChanged();
                    medsList.scheduleLayoutAnimation();
                },
                (VolleyError error) -> handleVolleyAuthError(error, () -> fetchMedicines())
        ) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> h = new java.util.HashMap<>();
                String access = AuthManager.getAccessToken(getApplicationContext());
                if (access != null && !access.isEmpty()) {
                    h.put("Authorization", "Bearer " + access);
                }
                h.put("Accept", "application/json");
                return h;
            }
        };

        VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(req);
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private void updateCartBadge() {
        int count = CartStore.get().totalItems();

        if (count <= 0) {
            cartBadge.setText("0");           // or "" if you want it empty
        } else {
            cartBadge.setText(String.valueOf(count));
        }
    }


}
