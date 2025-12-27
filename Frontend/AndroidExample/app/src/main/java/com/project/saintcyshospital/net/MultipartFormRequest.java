package com.project.saintcyshospital.net;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.HashMap;

public class MultipartFormRequest extends Request<JSONObject> {
    private final Response.Listener<JSONObject> listener;
    private final Map<String, String> headers;
    private final Map<String, String> formFields;   // text parts
    private final Map<String, FilePart> fileParts;  // files
    private final String boundary;

    public static class FilePart {
        public final String fileName;
        public final String contentType;
        public final byte[] data;
        public FilePart(String fileName, String contentType, byte[] data) {
            this.fileName = fileName; this.contentType = contentType; this.data = data;
        }
    }

    public MultipartFormRequest(
            int method, String url,
            Map<String, String> headers,
            Map<String, String> formFields,
            Map<String, FilePart> fileParts,
            Response.Listener<JSONObject> listener,
            Response.ErrorListener errorListener
    ) {
        super(method, url, errorListener);
        this.listener = listener;
        this.headers = (headers != null) ? headers : new HashMap<>();
        this.formFields = (formFields != null) ? formFields : new HashMap<>();
        this.fileParts = (fileParts != null) ? fileParts : new HashMap<>();
        this.boundary = "----AndroidFormBoundary" + System.currentTimeMillis();
    }

    @Override public String getBodyContentType() {
        return "multipart/form-data; boundary=" + boundary;
    }

    @Override public Map<String, String> getHeaders() throws AuthFailureError {
        return headers;
    }

    @Override public byte[] getBody() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            String lineEnd = "\r\n";
            String twoHyphens = "--";

            for (Map.Entry<String, String> e : formFields.entrySet()) {
                bos.write((twoHyphens + boundary + lineEnd).getBytes(StandardCharsets.UTF_8));
                bos.write(("Content-Disposition: form-data; name=\"" + e.getKey() + "\"" + lineEnd).getBytes());
                bos.write(("Content-Type: text/plain; charset=UTF-8" + lineEnd + lineEnd).getBytes());
                bos.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                bos.write(lineEnd.getBytes(StandardCharsets.UTF_8));
            }

            for (Map.Entry<String, FilePart> e : fileParts.entrySet()) {
                FilePart p = e.getValue();
                bos.write((twoHyphens + boundary + lineEnd).getBytes());
                bos.write(("Content-Disposition: form-data; name=\"" + e.getKey() + "\"; filename=\"" + p.fileName + "\"" + lineEnd).getBytes());
                bos.write(("Content-Type: " + p.contentType + lineEnd + lineEnd).getBytes());
                bos.write(p.data);
                bos.write(lineEnd.getBytes(StandardCharsets.UTF_8));
            }

            bos.write((twoHyphens + boundary + twoHyphens + lineEnd).getBytes());
        } catch (Exception ignored) {}
        return bos.toByteArray();
    }

    @Override protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        try {
            String json = new String(response.data, HttpHeaderParser.parseCharset(response.headers, "UTF-8"));
            return Response.success(new JSONObject(json), HttpHeaderParser.parseCacheHeaders(response));
        } catch (Exception e) {
            return Response.success(new JSONObject(), HttpHeaderParser.parseCacheHeaders(response));
        }
    }

    @Override protected void deliverResponse(JSONObject response) {
        listener.onResponse(response);
    }
}
