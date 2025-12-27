package com.project.saintcyshospital;

import android.content.Context;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.VolleyError;
import com.project.saintcyshospital.ws.WsHub;

import org.json.JSONException;
import org.json.JSONObject;

public class TokenRefresher {
    private static final String URL_REFRESH = "http://coms-3090-041.class.las.iastate.edu:8080/api/refresh";
    private static final String TAG_REFRESH  = "refresh_req";

    public interface Callback {
        void onRefreshed();
        void onFailed();
    }

    public static void refresh(Context appCtx, Callback cb) {
        String refresh = AuthManager.getRefreshToken(appCtx);
        if (refresh == null) { cb.onFailed(); return; }

        JSONObject body = new JSONObject();
        try { body.put("refreshToken", refresh); }
        catch (JSONException e) { cb.onFailed(); return; }

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST, URL_REFRESH, body,
                resp -> {
                    String access  = resp.optString("accessToken", null);
                    String newRef  = resp.optString("refreshToken", refresh);
                    if (access == null || access.isEmpty()) {
                        cb.onFailed();
                        return;
                    }

                    AuthManager.saveTokens(appCtx, access, newRef);

                    if (AuthManager.isLoggedIn(appCtx)) {
                        WsHub.get().restart();
                    }
                    cb.onRefreshed();
                },
                (VolleyError err) -> cb.onFailed()
        );

        req.setTag(TAG_REFRESH);
        VolleySingleton.getInstance(appCtx).addToRequestQueue(req);

    }

}
