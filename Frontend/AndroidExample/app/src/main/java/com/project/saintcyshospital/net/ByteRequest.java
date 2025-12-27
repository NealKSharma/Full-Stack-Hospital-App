package com.project.saintcyshospital.net;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import java.util.HashMap;
import java.util.Map;

public class ByteRequest extends Request<ByteRequest.Payload> {

    public static class Payload {
        public final byte[] data;
        public final Map<String, String> headers;
        public final int statusCode;

        public Payload(byte[] data, Map<String, String> headers, int statusCode) {
            this.data = data;
            this.headers = headers != null ? headers : new HashMap<>();
            this.statusCode = statusCode;
        }
    }

    private final Response.Listener<Payload> listener;
    private final Map<String, String> headers;

    public ByteRequest(int method,
                       String url,
                       Map<String, String> headers,
                       Response.Listener<Payload> listener,
                       Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.listener = listener;
        this.headers = (headers != null) ? headers : new HashMap<>();
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return headers;
    }

    @Override
    protected Response<Payload> parseNetworkResponse(NetworkResponse response) {
        Payload payload = new Payload(response.data, response.headers, response.statusCode);
        return Response.success(payload, HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected void deliverResponse(Payload response) {
        listener.onResponse(response);
    }
}
