package com.project.saintcyshospital;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CartStore {
    private static final String PREFS = "cart_prefs";
    private static final String KEY_CART = "cart_json";
    private Context appCtx;

    private static final CartStore INSTANCE = new CartStore();
    public static CartStore get() { return INSTANCE; }

    private final Map<String, CartItem> map = new LinkedHashMap<>();

    public void add(Medicine m) {
        CartItem ci = map.get(m.id);
        if (ci == null) map.put(m.id, new CartItem(m, 1));
        else ci.qty += 1;
        save();
    }

    public void removeOne(String medicineId) {
        CartItem ci = map.get(medicineId);
        if (ci == null) return;
        if (ci.qty > 1) ci.qty -= 1;
        else map.remove(medicineId);
        save();
    }

    public List<CartItem> items() {
        return new ArrayList<>(map.values());
    }

    public int totalCents() {
        int sum = 0;
        for (CartItem ci : map.values()) sum += ci.medicine.priceCents * ci.qty;
        return sum;
    }

    public void clear() {
        map.clear();
        save();
    }

    public static class CartItem {
        public final Medicine medicine;
        public int qty;
        public CartItem(Medicine m, int q) { this.medicine = m; this.qty = q; }
    }

    public void init(Context context) {
        if (appCtx == null) {
            appCtx = context.getApplicationContext();
            load();
        }
    }

    private void save() {
        if (appCtx == null) return;
        try {
            JSONArray arr = new JSONArray();
            for (CartItem ci : map.values()) {
                JSONObject o = new JSONObject();
                o.put("id", ci.medicine.id);
                o.put("name", ci.medicine.name);
                o.put("genericName", ci.medicine.genericName);
                o.put("dosage", ci.medicine.dosage);
                o.put("priceCents", ci.medicine.priceCents);
                o.put("imageUrl", ci.medicine.imageUrl);
                o.put("qty", ci.qty);
                arr.put(o);
            }
            SharedPreferences sp = appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            sp.edit().putString(KEY_CART, arr.toString()).apply();
        } catch (JSONException ignored) {}
    }

    private void load() {
        if (appCtx == null) return;
        SharedPreferences sp = appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = sp.getString(KEY_CART, null);
        map.clear();
        if (json == null || json.isEmpty()) return;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                String id = o.optString("id", "");
                if (id.isEmpty()) continue;
                String name = o.optString("name", "");
                String genericName = o.optString("genericName", "");
                String dosage = o.optString("dosage", "");
                int priceCents = o.optInt("priceCents", 0);
                String imageUrl = o.optString("imageUrl", null);
                int qty = o.optInt("qty", 1);

                Medicine m = new Medicine(id, name, genericName, dosage, priceCents, imageUrl);
                map.put(id, new CartItem(m, qty));
            }
        } catch (JSONException ignored) {}
    }

    public int totalItems() {
        int sum = 0;
        for (CartItem ci : map.values()) {
            sum += ci.qty;
        }
        return sum;
    }


}
