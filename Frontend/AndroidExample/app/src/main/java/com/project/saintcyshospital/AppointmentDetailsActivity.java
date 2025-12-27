package com.project.saintcyshospital;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.view.ViewCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Locale;

public class AppointmentDetailsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_appointment_details);
        setupBottomNav(0);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (view, insets) -> {
            int left = view.getPaddingLeft();
            int top = view.getPaddingTop();
            int right = view.getPaddingRight();
            view.setPadding(left, top, right, 0);
            return insets;
        });

        // Back button
        Button btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        TextView tvName   = findViewById(R.id.tvName);
        TextView tvEmail  = findViewById(R.id.tvEmail);
        TextView tvPhone  = findViewById(R.id.tvPhone);
        TextView tvDate   = findViewById(R.id.tvDate);
        TextView tvTime   = findViewById(R.id.tvTime);
        TextView tvType   = findViewById(R.id.tvType);
        TextView tvStatus = findViewById(R.id.tvStatus);

        boolean isDoctor = AuthManager.isDoctor(getApplicationContext());

        // Extras from AppointmentsActivity.Adapter
        String doctorUsername  = getIntent().getStringExtra("doctorUsername");
        String patientUsername = getIntent().getStringExtra("patientUsername");
        String patientName     = getIntent().getStringExtra("patientName");

        String email = getIntent().getStringExtra("email");
        if (email == null) email = getIntent().getStringExtra("patientEmail");

        String phone = getIntent().getStringExtra("phone");
        if (phone == null) phone = getIntent().getStringExtra("patientPhone");

        String type       = getIntent().getStringExtra("type");
        String statusRaw  = getIntent().getStringExtra("status");
        long   startMs    = getIntent().getLongExtra("startEpochMillis", 0L);

        if (tvName != null) {
            if (isDoctor) {
                String label = (patientName != null && !patientName.trim().isEmpty())
                        ? patientName.trim()
                        : (patientUsername != null ? patientUsername.trim() : "");
                if (label.isEmpty()) label = "Patient";
                tvName.setText(label);
            } else {
                String doc = doctorUsername == null ? "" : doctorUsername;
                if (!doc.isEmpty() && !doc.toLowerCase(Locale.US).startsWith("dr")) {
                    doc = "Dr. " + doc;
                }
                tvName.setText(doc.isEmpty() ? "—" : doc);
            }
        }

        if (tvEmail != null) tvEmail.setText(emptyDash(email));
        if (tvPhone != null) tvPhone.setText(emptyDash(phone));
        if (tvType  != null) tvType.setText(emptyDash(type));

        if (startMs > 0) {
            String dateText = DateFormat.format("MMM dd, yyyy", startMs).toString();
            String timeText = DateFormat.format("h:mm a", startMs).toString();
            if (tvDate != null) tvDate.setText(dateText);
            if (tvTime != null) tvTime.setText(timeText);
        }

        String statusUpper = statusRaw == null
                ? "PENDING"
                : statusRaw.trim().toUpperCase(Locale.US);

        String displayStatus = statusUpper;
        if ("APPROVED".equals(statusUpper)) {
            displayStatus = "CONFIRMED";
        }

        if (tvStatus != null) {
            tvStatus.setText(nice(displayStatus));
            Drawable bg = tvStatus.getBackground();
            if (bg != null) {
                bg = bg.mutate();
                bg.setTint(colorForStatus(displayStatus));
                tvStatus.setBackground(bg);
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

    private static int colorForStatus(String s) {
        if (s == null) return Color.parseColor("#757575"); // gray
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
            case "APPROVED":
            default:
                return Color.parseColor("#757575"); // gray
        }
    }
}
