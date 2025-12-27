package com.project.saintcyshospital;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

public class MoreMenuLayout extends FrameLayout {

    public interface Listener {
        void onNotifications();
        void onHistories();
        void onAdmin();
        void onSettings();
        void onDoctorPrescriptions();
        void onLogout();
        void onPharmacist();
    }

    private View scrim;
    private LinearLayout panel;
    private boolean isOpen = false;
    private Listener listener;

    public MoreMenuLayout(Context context) {
        super(context);
        init();
    }

    public MoreMenuLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MoreMenuLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.view_more_menu, this);
        scrim = findViewById(R.id.more_scrim);
        panel = findViewById(R.id.more_panel);

        setVisibility(GONE);

        post(() -> {
            panel.setTranslationX(panel.getWidth());
        });

        scrim.setOnClickListener(v -> close());


        findViewById(R.id.more_notifications).setOnClickListener(v -> {
            if (listener != null) listener.onNotifications();
            close();
        });

        findViewById(R.id.more_pharmacist).setOnClickListener(v -> {
            if (listener != null) listener.onPharmacist();
            close();
        });

        findViewById(R.id.more_histories).setOnClickListener(v -> {
            if (listener != null) listener.onHistories();
            close();
        });

        findViewById(R.id.more_admin).setOnClickListener(v -> {
            if (listener != null) listener.onAdmin();
            close();
        });

        findViewById(R.id.more_settings).setOnClickListener(v -> {
            if (listener != null) listener.onSettings();
            close();
        });

        findViewById(R.id.more_doctor_prescriptions).setOnClickListener(v -> {
            if (listener != null) listener.onDoctorPrescriptions();
            close();
        });

        findViewById(R.id.more_logout).setOnClickListener(v -> {
            if (listener != null) listener.onLogout();
            close();
        });
    }

    public void setListener(Listener l) {
        this.listener = l;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void open() {
        if (isOpen) return;
        isOpen = true;

        setVisibility(VISIBLE);
        scrim.setVisibility(VISIBLE);

        scrim.animate()
                .alpha(1f)
                .setDuration(200)
                .start();

        panel.animate()
                .translationX(0f)
                .setDuration(200)
                .start();
    }

    public void close() {
        if (!isOpen) return;
        isOpen = false;

        scrim.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> scrim.setVisibility(GONE))
                .start();

        panel.animate()
                .translationX(panel.getWidth())
                .setDuration(200)
                .withEndAction(() -> {
                    if (!isOpen) {
                        setVisibility(GONE);
                    }
                })
                .start();
    }

    public void configureForRole(String role) {
        role = role == null ? "" : role.toUpperCase();

        View notif     = findViewById(R.id.more_notifications);
        View histories = findViewById(R.id.more_histories);
        View admin     = findViewById(R.id.more_admin);
        View docRx     = findViewById(R.id.more_doctor_prescriptions);
        View pharmacist = findViewById(R.id.more_pharmacist);

        if (notif != null) notif.setVisibility(View.GONE);
        if (histories != null) histories.setVisibility(View.GONE);
        if (admin != null) admin.setVisibility(View.GONE);
        if (docRx != null) docRx.setVisibility(View.GONE);
        if (pharmacist != null) pharmacist.setVisibility(View.GONE);

        if ("ADMIN".equals(role)) {
            if (notif != null) notif.setVisibility(View.VISIBLE);
            if (admin != null) admin.setVisibility(View.VISIBLE);
        } else if ("DOCTOR".equals(role)) {
            if (notif != null) notif.setVisibility(View.VISIBLE);
            if (histories != null) histories.setVisibility(View.VISIBLE);
            if (docRx != null) docRx.setVisibility(View.VISIBLE);
        } else if ("PATIENT".equals(role)) {
            if (notif != null) notif.setVisibility(View.VISIBLE);
        }
        else if ("PHARMACIST".equals(role)) {
            if (pharmacist != null) pharmacist.setVisibility(View.VISIBLE);
        }
    }

}
