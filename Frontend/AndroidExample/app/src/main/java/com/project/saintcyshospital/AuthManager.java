package com.project.saintcyshospital;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class AuthManager {
    private static final String PREFS = "auth_prefs";
    private static final String KEY_ACCESS  = "access_token";
    private static final String KEY_REFRESH = "refresh_token";

    public static void saveTokens(Context appCtx, String access, String refresh) {
        SharedPreferences sp = appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit()
                .putString(KEY_ACCESS, access)
                .putString(KEY_REFRESH, refresh)
                .apply();
    }

    public static String getAccessToken(Context appCtx) {
        return appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ACCESS, null);
    }

    public static String getRefreshToken(Context appCtx) {
        return appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_REFRESH, null);
    }

    public static void clear(Context appCtx) {
        appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }
    public static String getUsername(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        return prefs.getString("last_user", "");
    }

    public static String getRole(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        return prefs.getString("last_role", "");
    }

    public static boolean isLoggedIn(Context appCtx) {
        return getAccessToken(appCtx) != null;
    }

    public static boolean isAdmin(Context ctx) {
        return "ADMIN".equalsIgnoreCase(getRole(ctx));
    }

    public static boolean isDoctor(Context ctx) {
        return "DOCTOR".equalsIgnoreCase(getRole(ctx));
    }

    public static boolean isPatient(Context ctx) {
        return "PATIENT".equalsIgnoreCase(getRole(ctx));
    }

    public static boolean isPharmacist(Context ctx) {
        return "PHARMACIST".equalsIgnoreCase(getRole(ctx));
    }

    public static void registerFcmToken(Context ctx, String token) {
        String url = "http://coms-3090-041.class.las.iastate.edu:8080/api/push/register";

        JSONObject body = new JSONObject();
        try {
            body.put("token", token);
        } catch (Exception ignored) {}

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                url,
                body,
                response -> Log.d("FCM", "FCM token registered"),
                error -> Log.e("FCM", "Failed to register FCM token: " + error)
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + getAccessToken(ctx));
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        VolleySingleton.getInstance(ctx).addToRequestQueue(req);
    }


}
