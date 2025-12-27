package com.project.saintcyshospital;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import android.graphics.Color;
import android.graphics.drawable.Drawable;

public class AppointmentsActivity extends BaseActivity {

    private static final String BASE = "http://coms-3090-041.class.las.iastate.edu:8080";
    private static final String URL_LIST_MY     = BASE + "/api/appointments/my";
    private static final String URL_CREATE      = BASE + "/api/appointments";
    private static final String URL_APPROVE_FMT = BASE + "/api/appointments/%s/approve";
    private static final String URL_CONFIRM_FMT = BASE + "/api/appointments/%s/confirm";
    private static final String URL_CANCEL_FMT  = BASE + "/api/appointments/%s/cancel";
    private static final String URL_COMPLETE_FMT= BASE + "/api/appointments/%s/complete";
    private SquareCalendarView calendar;

    private Button btnNew;

    static class Appointment {
        String id, doctorUsername, patientUsername;
        String patientName, email, phone, type, status;
        long startMs;
        static Appointment from(JSONObject o) {
            Appointment a = new Appointment();
            a.id = String.valueOf(o.optLong("id", o.optLong("appointmentId", 0)));

            // Patient
            JSONObject p = o.optJSONObject("patient");
            if (p != null) {
                String fn = p.optString("firstName", "");
                String ln = p.optString("lastName", "");
                String fullName = (fn + " " + ln).trim();
                if (fullName.isEmpty()) {
                    fullName = p.optString("name", "");
                }

                String username = p.optString("username",
                        o.optString("patientUsername", ""));
                a.patientUsername = username;
                a.patientName = fullName;
                if (a.patientName == null || a.patientName.trim().isEmpty()) {
                    a.patientName = a.patientUsername;
                }
            } else {
                a.patientUsername = o.optString("patientUsername", "");
                a.patientName = o.optString("patientName", a.patientUsername);
                if (a.patientName == null || a.patientName.trim().isEmpty()) {
                    a.patientName = a.patientUsername;
                }
            }

            // Doctor
            JSONObject d = o.optJSONObject("doctor");
            a.doctorUsername = (d != null)
                    ? d.optString("username", o.optString("doctorUsername", ""))
                    : o.optString("doctorUsername", "");

            a.email  = o.optString("patientEmail", "");
            a.phone  = o.optString("patientPhone", "");
            a.type   = o.optString("type", o.optString("notes", ""));
            a.status = o.optString("status", "PENDING");

            long epoch = o.optLong("startEpochMillis", 0);
            if (epoch == 0) {
                String date = o.optString("date", null);
                String time = o.optString("time", null);
                if (date != null && time != null) {
                    try {
                        String[] dmy = date.split("-");
                        String[] hms = time.split(":");
                        Calendar c = Calendar.getInstance(TimeZone.getDefault());
                        c.set(
                                Integer.parseInt(dmy[0]),
                                Integer.parseInt(dmy[1]) - 1,
                                Integer.parseInt(dmy[2]),
                                Integer.parseInt(hms[0]),
                                Integer.parseInt(hms[1]),
                                0
                        );
                        c.set(Calendar.MILLISECOND, 0);
                        epoch = c.getTimeInMillis();
                    } catch (Exception ignored) {}
                }
            }
            a.startMs = epoch;
            return a;
        }
    }

    private final List<Appointment> all = new ArrayList<>();
    private final List<Appointment> filtered = new ArrayList<>();

    private RecyclerView rv;
    private EditText etSearch;
    private Adapter adapter;
    private TextView tvEmpty;
    private Long dayStartMs = null, dayEndMs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointments);
        setupBottomNav(R.id.nav_messages);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (view, insets) -> {
            int left = view.getPaddingLeft();
            int top = view.getPaddingTop();
            int right = view.getPaddingRight();

            view.setPadding(left, top, right, 0);
            return insets;
        });

        tvEmpty = findViewById(R.id.tvEmpty);

        rv = findViewById(R.id.rvAppointments);
        rv.setLayoutManager(new LinearLayoutManager(this));

        boolean isDoctor = isDoctor();

        adapter = new Adapter(
                filtered,
                isDoctor,
                this::confirmAndUpdateStatus
        );
        adapter.setHasStableIds(true);
        rv.setAdapter(adapter);
        rv.setHasFixedSize(true);

        etSearch = findViewById(R.id.etSearch);
        calendar = findViewById(R.id.calendarView);

        calendar.setOnDateSelectedListener((y, m, d) -> applyFilters(y, m, d));

        btnNew = findViewById(R.id.btnNewAppointment);
        if (btnNew != null) {
            btnNew.setVisibility(isDoctor() ? View.GONE : View.VISIBLE);
            btnNew.setOnClickListener(v -> showCreateDialog(dayStartMs));
        }



        calendar.setOnDateSelectedListener((y, m, d) -> applyFilters(y, m, d));

        java.util.Calendar today = java.util.Calendar.getInstance(java.util.TimeZone.getDefault());
        applyFilters(
                today.get(java.util.Calendar.YEAR),
                today.get(java.util.Calendar.MONTH),         // 0-based month, same as before
                today.get(java.util.Calendar.DAY_OF_MONTH)
        );

        fetchAppointments();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchAppointments();
    }

    private boolean isDoctor() {
        return AuthManager.isDoctor(getApplicationContext());
    }

    private void fetchAppointments() {
        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET, URL_LIST_MY, null,
                this::onData,
                this::showVolleyError
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String,String> h = new HashMap<>();
                String access = AuthManager.getAccessToken(getApplicationContext());
                if (access != null) h.put("Authorization", "Bearer " + access);
                return h;
            }
        };
        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    private void onData(JSONArray arr) {
        all.clear();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null) all.add(Appointment.from(o));
        }
        Collections.sort(all, Comparator.comparingLong(a -> a.startMs));

        // NEW: mark days with appointments
        java.util.HashSet<Integer> marked = new java.util.HashSet<>();
        java.util.Calendar tmp = java.util.Calendar.getInstance(TimeZone.getDefault());
        for (Appointment a : all) {
            if (a.startMs == 0) continue;
            tmp.setTimeInMillis(a.startMs);
            int y = tmp.get(java.util.Calendar.YEAR);
            int m = tmp.get(java.util.Calendar.MONTH);         // 0-based
            int d = tmp.get(java.util.Calendar.DAY_OF_MONTH);
            int key = y * 10000 + (m + 1) * 100 + d;
            marked.add(key);
        }
        if (calendar != null) {
            calendar.setMarkedDays(marked);
        }

        applyFilters();
    }


    private void applyFilters() {
        final String q = etSearch.getText() == null
                ? ""
                : etSearch.getText().toString().trim().toLowerCase(Locale.US);
        final boolean iAmDoctor = isDoctor();

        List<Appointment> fresh = new ArrayList<>();
        for (Appointment a : all) {
            boolean dayOK = (dayStartMs == null)
                    || (a.startMs >= dayStartMs && a.startMs < dayEndMs);

            boolean textOK;
            if (q.isEmpty()) {
                textOK = true;
            } else {
                String nameForSearch;
                if (iAmDoctor) {
                    nameForSearch = n(a.patientName);
                } else {
                    String docLabel = (a.doctorUsername == null) ? "" : a.doctorUsername;
                    if (!docLabel.isEmpty()
                            && !docLabel.toLowerCase(Locale.US).startsWith("dr")) {
                        docLabel = "dr. " + docLabel;
                    }
                    nameForSearch = docLabel.toLowerCase(Locale.US);
                }
                textOK = nameForSearch.contains(q);
            }

            if (dayOK && textOK) fresh.add(a);
        }

        Collections.sort(fresh, Comparator.comparingLong(x -> x.startMs));

        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffCallback(filtered, fresh));
        filtered.clear();
        filtered.addAll(fresh);
        diff.dispatchUpdatesTo(adapter);

        boolean empty = filtered.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rv.setVisibility(empty ? View.GONE : View.VISIBLE);

        if (btnNew != null) {
            btnNew.setVisibility(isDoctor() ? View.GONE : View.VISIBLE);
        }
    }

    private void applyFilters(int y, int m, int d) {
        Calendar c = Calendar.getInstance(TimeZone.getDefault());
        c.set(y, m, d, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        dayStartMs = c.getTimeInMillis();
        dayEndMs   = dayStartMs + 24L * 60L * 60L * 1000L;
        applyFilters();
    }

    private void showCreateDialog(Long prefillDayMs) {
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        android.view.ContextThemeWrapper themed =
                new android.view.ContextThemeWrapper(this,
                        R.style.ThemeOverlay_SaintCys_Pickers);

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(pad, pad, pad, pad);

        EditText etDoctor = new EditText(this); etDoctor.setHint("Doctor username");
        EditText etPatient = new EditText(this); etPatient.setHint("Patient username");
        EditText etEmail  = new EditText(this);  etEmail.setHint("Patient email (optional)");
        EditText etPhone  = new EditText(this);  etPhone.setHint("Patient phone (optional)");
        EditText etNotes  = new EditText(this);  etNotes.setHint("Type / notes");

        final boolean isDoctorUser = isDoctor();

        if (isDoctorUser) {
            String me = AuthManager.getUsername(getApplicationContext());
            if (me == null) me = "";
            etDoctor.setText(me);
        } else {
            String me = AuthManager.getUsername(getApplicationContext());
            if (me == null) me = "";
            etPatient.setText(me);
            etPatient.setEnabled(false);
            etPatient.setAlpha(0.6f);
        }

        TextView tvDateHdr = new TextView(this); tvDateHdr.setText("Pick date");
        DatePicker dp = new DatePicker(themed);

        Calendar initial = Calendar.getInstance(TimeZone.getDefault());
        if (prefillDayMs != null) {
            initial.setTimeInMillis(prefillDayMs);
        } else {
            initial.setTimeInMillis(calendar.getSelectedDateInMillis());
        }
        dp.updateDate(
                initial.get(Calendar.YEAR),
                initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH)
        );

        TextView tvTimeHdr = new TextView(this); tvTimeHdr.setText("Set time");
        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        timeRow.setPadding(0, pad, 0, 0);

        EditText etHour = new EditText(this); etHour.setHint("hh");
        etHour.setInputType(android.text.InputType.TYPE_CLASS_NUMBER); etHour.setEms(2);
        TextView colon = new TextView(this); colon.setText(" : ");
        EditText etMin  = new EditText(this);  etMin.setHint("mm");
        etMin.setInputType(android.text.InputType.TYPE_CLASS_NUMBER); etMin.setEms(2);
        android.widget.Spinner spAmPm = new android.widget.Spinner(this);
        spAmPm.setAdapter(new android.widget.ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"AM","PM"}));

        timeRow.addView(etHour); timeRow.addView(colon); timeRow.addView(etMin); timeRow.addView(spAmPm);

        form.addView(etDoctor);
        form.addView(etPatient);
        form.addView(etEmail);
        form.addView(etPhone);
        form.addView(etNotes);
        form.addView(tvTimeHdr);
        form.addView(timeRow);
        form.addView(tvDateHdr);
        form.addView(dp);

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(form);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(scroll)
                .setPositiveButton("Create", null)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .create();

        LinearLayout titleBar = new LinearLayout(this);
        titleBar.setOrientation(LinearLayout.HORIZONTAL);
        titleBar.setPadding(pad, pad, pad, pad);
        TextView tvTitle = new TextView(this);
        tvTitle.setText("Create Appointment");
        tvTitle.setTextSize(18);
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1));
        android.widget.ImageButton btnClose = new android.widget.ImageButton(this);
        btnClose.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        btnClose.setBackground(null);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        titleBar.addView(tvTitle); titleBar.addView(btnClose);
        dialog.setCustomTitle(titleBar);

        dialog.setCanceledOnTouchOutside(true);
        dialog.show();

        Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Runnable validate = () -> {
            String h = etHour.getText().toString().trim();
            String m = etMin.getText().toString().trim();
            String doc = etDoctor.getText().toString().trim();
            String pat = etPatient.getText().toString().trim();

            boolean okUsers;
            if (isDoctorUser) {
                okUsers = !doc.isEmpty() && !pat.isEmpty();
            } else {
                okUsers = !doc.isEmpty();
            }

            boolean okTime = h.matches("\\d{1,2}") && m.matches("\\d{1,2}");
            if (okTime) {
                int ih = Integer.parseInt(h), im = Integer.parseInt(m);
                okTime = ih >= 1 && ih <= 12 && im >= 0 && im <= 59;
            }
            positive.setEnabled(okUsers && okTime);
        };
        TextWatcher tw = simpleWatcher(validate);
        etDoctor.addTextChangedListener(tw);
        etPatient.addTextChangedListener(tw);
        etHour.addTextChangedListener(tw);
        etMin.addTextChangedListener(tw);
        validate.run();

        positive.setOnClickListener(v -> {
            int y = dp.getYear(), m = dp.getMonth() + 1, d = dp.getDayOfMonth();
            int h12 = Integer.parseInt(etHour.getText().toString().trim());
            int mm = Integer.parseInt(etMin.getText().toString().trim());
            boolean pm = spAmPm.getSelectedItemPosition() == 1;
            int hh24 = (h12 % 12) + (pm ? 12 : 0);

            JSONObject body = new JSONObject();
            try {
                body.put("doctorUsername", etDoctor.getText().toString().trim());

                String patientUser;
                if (isDoctorUser) {
                    patientUser = etPatient.getText().toString().trim();
                } else {
                    patientUser = AuthManager.getUsername(getApplicationContext());
                    if (patientUser == null) patientUser = "";
                }
                body.put("patientUsername", patientUser);

                body.put("date", String.format(Locale.US, "%04d-%02d-%02d", y, m, d));
                body.put("time", String.format(Locale.US, "%02d:%02d:00", hh24, mm));
                body.put("notes", etNotes.getText().toString().trim());
                body.put("patientEmail", etEmail.getText().toString().trim());
                body.put("patientPhone", etPhone.getText().toString().trim());
            } catch (Exception ignored) {}

            JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.POST, URL_CREATE, body,
                    res -> {
                        Toast.makeText(this, "Appointment created", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        fetchAppointments();
                    },
                    this::showVolleyError
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String,String> h = new HashMap<>();
                    String access = AuthManager.getAccessToken(getApplicationContext());
                    if (access != null) h.put("Authorization", "Bearer " + access);
                    h.put("Content-Type", "application/json");
                    return h;
                }
            };
            VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(req);
        });
    }


    private TextWatcher simpleWatcher(Runnable r) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { r.run(); }
            @Override public void afterTextChanged(Editable s) {}
        };
    }

    private void confirmAndUpdateStatus(long id, String action) {
        String message;
        String positiveText;
        String negativeText = "No";

        switch (action) {
            case "confirm":
                message = "Confirm this appointment?";
                positiveText = "Confirm";
                break;
            case "complete":
                message = "Mark this appointment as completed?";
                positiveText = "Complete";
                break;
            case "cancel":
            default:
                message = "Cancel this appointment?";
                positiveText = "Yes, cancel";
                break;
        }

        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(positiveText, (d, w) -> updateStatus(id, action))
                .setNegativeButton(negativeText, null)
                .show();
    }

    private void updateStatus(long id, String action) {
        String url;
        switch (action) {
            case "confirm":
                url = String.format(Locale.US, URL_CONFIRM_FMT, id);
                break;
            case "complete":
                url = String.format(Locale.US, URL_COMPLETE_FMT, id);
                break;
            case "cancel":
            default:
                url = String.format(Locale.US, URL_CANCEL_FMT, id);
                break;
        }

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.PUT, url, null,
                res -> {
                    Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show();
                    fetchAppointments();
                },
                this::showVolleyError
        ) {
            @Override public Map<String, String> getHeaders() {
                Map<String,String> h = new HashMap<>();
                String access = AuthManager.getAccessToken(getApplicationContext());
                if (access != null) h.put("Authorization", "Bearer " + access);
                return h;
            }

            @Override protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                if (response != null && (response.data == null || response.data.length == 0)) {
                    try {
                        return Response.success(new JSONObject(),
                                HttpHeaderParser.parseCacheHeaders(response));
                    } catch (Exception e) {
                        return Response.error(new com.android.volley.ParseError(e));
                    }
                }
                return super.parseNetworkResponse(response);
            }
        };
        VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(req);
    }

    private void showVolleyError(com.android.volley.VolleyError err) {
        int code = (err.networkResponse != null) ? err.networkResponse.statusCode : -1;
        String body = "";
        try {
            if (err.networkResponse != null && err.networkResponse.data != null) {
                body = new String(err.networkResponse.data, StandardCharsets.UTF_8);
            } else if (err.getMessage() != null) {
                body = err.getMessage();
            }
        } catch (Exception ignored) {}
        android.util.Log.e("APPTS", "HTTP " + code + " " + body, err);
        Toast.makeText(this, "Error " + code + (body.isEmpty() ? "" : ": " + body),
                Toast.LENGTH_LONG).show();
    }

    static class DiffCallback extends DiffUtil.Callback {
        private final List<Appointment> oldL, newL;
        DiffCallback(List<Appointment> oldL, List<Appointment> newL) {
            this.oldL = oldL; this.newL = newL;
        }
        @Override public int getOldListSize() { return oldL.size(); }
        @Override public int getNewListSize() { return newL.size(); }
        @Override public boolean areItemsTheSame(int oldPos, int newPos) {
            return n(oldL.get(oldPos).id).equals(n(newL.get(newPos).id));
        }
        @Override public boolean areContentsTheSame(int oldPos, int newPos) {
            Appointment a = oldL.get(oldPos), b = newL.get(newPos);
            return a.startMs == b.startMs
                    && n(a.patientName).equals(n(b.patientName))
                    && n(a.email).equals(n(b.email))
                    && n(a.phone).equals(n(b.phone))
                    && n(a.type).equals(n(b.type))
                    && n(a.status).equals(n(b.status));
        }
    }

    private static String n(String s) {
        return s == null ? "" : s.toLowerCase(Locale.US);
    }

    static class Adapter extends RecyclerView.Adapter<Adapter.VH> {
        interface Action { void onAction(long id, String action); }

        private final List<Appointment> data;
        private final boolean isDoctor;
        private final Action actionCall;

        Adapter(List<Appointment> data,
                boolean isDoctor,
                Action action) {
            this.data = data;
            this.isDoctor = isDoctor;
            this.actionCall = action;
        }

        @Override public long getItemId(int position) {
            try {
                return Long.parseLong(data.get(position).id);
            } catch (Exception e) {
                return RecyclerView.NO_ID;
            }
        }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            View v = LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_appointments, p, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            Appointment a = data.get(pos);

            String statusRaw = (a.status == null ? "" : a.status.trim());
            String statusUpper = statusRaw.toUpperCase(Locale.US);

            // Map APPROVED -> CONFIRMED for display
            String displayStatus = statusUpper;
            if ("APPROVED".equals(statusUpper)) {
                displayStatus = "CONFIRMED";
            }

            // Title: doctor sees patient name, patient sees doctor name
            if (isDoctor) {
                h.name.setText(pickPatientLabel(a));
            } else {
                String docLabel = emptyDash(a.doctorUsername);
                if (!docLabel.isEmpty()
                        && !docLabel.toLowerCase(Locale.US).startsWith("dr")) {
                    docLabel = "Dr. " + docLabel;
                }
                h.name.setText(docLabel.isEmpty() ? "—" : docLabel);
            }

            h.email.setText(emptyDash(a.email));
            h.phone.setText(emptyDash(a.phone));
            h.type.setText(emptyDash(a.type));
            h.status.setText(nice(displayStatus));

            int color = colorForStatus(displayStatus);
            Drawable bg = h.status.getBackground();
            if (bg != null) {
                bg = bg.mutate();
                bg.setTint(color);
                h.status.setBackground(bg);
            }

            String date = DateFormat.format("MMM dd, yyyy", a.startMs).toString();
            String time = DateFormat.format("h:mm a", a.startMs).toString();
            h.date.setText(date);
            h.time.setText(time);

            View.OnClickListener openDetails = v -> {
                Intent i = new Intent(v.getContext(), AppointmentDetailsActivity.class);
                i.putExtra("id", a.id);
                i.putExtra("doctorUsername", a.doctorUsername);
                i.putExtra("patientUsername", a.patientUsername);

                i.putExtra("patientName", a.patientName);
                i.putExtra("patientEmail", a.email);
                i.putExtra("patientPhone", a.phone);
                i.putExtra("name", a.patientName);
                i.putExtra("email", a.email);
                i.putExtra("phone", a.phone);
                i.putExtra("type", a.type);
                i.putExtra("status", statusUpper);
                i.putExtra("startEpochMillis", a.startMs);
                v.getContext().startActivity(i);
            };

            if (!isDoctor) {
                h.btn.setVisibility(View.VISIBLE);
                h.btn.setText("Details");
                h.btn.setOnClickListener(openDetails);
            } else {
                // Doctor actions:
                if ("PENDING".equals(statusUpper) || "APPROVED".equals(statusUpper)) {
                    h.btn.setVisibility(View.VISIBLE);
                    h.btn.setText("Actions");
                    h.btn.setOnClickListener(v -> {
                        PopupMenu pm = new PopupMenu(v.getContext(), v);
                        pm.getMenu().add("Confirm");
                        pm.getMenu().add("Cancel");
                        pm.setOnMenuItemClickListener(mi -> {
                            String title = mi.getTitle().toString();
                            String action = title.equalsIgnoreCase("confirm")
                                    ? "confirm"
                                    : "cancel";
                            try {
                                long id = Long.parseLong(a.id);
                                actionCall.onAction(id, action);
                            } catch (NumberFormatException ignored) {}
                            return true;
                        });
                        pm.show();
                    });
                } else if ("CONFIRMED".equals(statusUpper)) {
                    h.btn.setVisibility(View.VISIBLE);
                    h.btn.setText("Actions");
                    h.btn.setOnClickListener(v -> {
                        PopupMenu pm = new PopupMenu(v.getContext(), v);
                        pm.getMenu().add("Complete");
                        pm.getMenu().add("Cancel");
                        pm.setOnMenuItemClickListener(mi -> {
                            String title = mi.getTitle().toString();
                            String action = title.equalsIgnoreCase("complete")
                                    ? "complete"
                                    : "cancel";
                            try {
                                long id = Long.parseLong(a.id);
                                actionCall.onAction(id, action);
                            } catch (NumberFormatException ignored) {}
                            return true;
                        });
                        pm.show();
                    });
                } else {
                    h.btn.setVisibility(View.VISIBLE);
                    h.btn.setText("Details");
                    h.btn.setOnClickListener(openDetails);
                }
            }

            h.itemView.setOnClickListener(openDetails);
            h.itemView.setOnLongClickListener(null);
        }

        static String pickPatientLabel(Appointment a) {
            if (a.patientName != null && !a.patientName.trim().isEmpty()) {
                return a.patientName.trim();
            }
            if (a.patientUsername != null && !a.patientUsername.trim().isEmpty()) {
                return a.patientUsername.trim();
            }
            if (a.email != null && !a.email.trim().isEmpty()) {
                return a.email.trim();
            }
            return "Patient";
        }

        private static int colorForStatus(String s) {
            if (s == null) return Color.parseColor("#757575");
            String k = s.trim().toUpperCase(Locale.US);
            switch (k) {
                case "CONFIRMED":
                    return Color.parseColor("#1565C0"); // blue
                case "COMPLETED":
                    return Color.parseColor("#2E7D32"); // green
                case "CANCELLED":
                case "CANCELED":
                    return Color.parseColor("#C62828"); // red
                case "PENDING":
                default:
                    return Color.parseColor("#757575"); // gray
            }
        }

        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView name, email, phone, date, time, type, status;
            Button btn;
            VH(@NonNull View v) {
                super(v);
                name = v.findViewById(R.id.tvName);
                email = v.findViewById(R.id.tvEmail);
                phone = v.findViewById(R.id.tvPhone);
                date = v.findViewById(R.id.tvDate);
                time = v.findViewById(R.id.tvTime);
                type = v.findViewById(R.id.tvType);
                status = v.findViewById(R.id.tvStatus);
                btn = v.findViewById(R.id.btnDetails);
            }
        }
    }

    private static String nice(String s) {
        if (s == null || s.isEmpty()) return "-";
        String k = s.toLowerCase(Locale.US);
        return k.substring(0, 1).toUpperCase(Locale.US) + k.substring(1);
    }

    private static String emptyDash(String s) {
        return (s == null || s.trim().isEmpty()) ? "—" : s;
    }
}
