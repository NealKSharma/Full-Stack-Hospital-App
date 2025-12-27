package com.project.saintcyshospital;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.project.saintcyshospital.ws.WsHub;

import android.util.Log;
import com.google.firebase.messaging.FirebaseMessaging;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.view.View;
import android.widget.TextView;

/**
 * Base activity for all screens that use the bottom navigation, auth guard, and
 * shared behaviors (token refresh handling, global notification websocket, etc.).
 *
 * <p>Derive your activity from this class and call {@link #setupBottomNav(int)} in
 * {@code onCreate()} after {@code setContentView(...)}.</p>
 *  * @author Trice Buchanan
 */
public class BaseActivity extends AppCompatActivity {

    /**
     * Process-wide flag indicating whether this process has already prompted
     * the user for the Android 13+ notification permission. Prevents repeated
     * permission dialogs as activities are started and stopped.
     */
    private static boolean sAskedNotifPerm = false;

    /**
     * Process-wide flag to ensure we only register the FCM token once per
     * app run. The backend will de-duplicate tokens anyway, but this keeps
     * network chatter down.
     */
    private static boolean sRegisteredFcmToken = false;

    /**
     * Tracks whether a failed request has already been retried after a token
     * refresh attempt. Used by {@link #handleVolleyAuthError(com.android.volley.VolleyError, Runnable)}
     * to ensure only a single retry per failing request.
     */
    protected boolean retriedAfterRefresh = false;

    /**
     * Broadcast receiver for websocket in-app notifications.
     */
    private BroadcastReceiver wsReceiver;

    /**
     * Tracks whether the websocket broadcast receiver is currently registered.
     */
    private boolean wsReceiverRegistered = false;
    /**
     * Whether this activity requires an authenticated user. If {@code true},
     * {@link #onStart()} will redirect to {@link HomeActivity} when the user is not logged in.
     * Override and return {@code false} for public screens (e.g., Login, Signup).
     */
    protected boolean requiresAuth() {
        return true;
    }

    /**
     * Reference to the slide-over "More" menu layout shown from the bottom
     * navigation bar. May be {@code null} on screens that do not include it.
     */
    private MoreMenuLayout moreMenu;
    /**
     * Back-press callback that first closes the "More" menu if it is open,
     * and otherwise defers to the default back navigation. Registered in
     * {@link #setupBottomNav(int)} when the menu is present.
     */
    private OnBackPressedCallback moreMenuBackCallback;

    /**
     * Starts the global notification websocket if the user is logged in,
     * checks (and optionally requests) notification permission (Android 13+),
     * and enforces the {@link #requiresAuth()} gate.
     */
    @Override
    protected void onStart() {
        super.onStart();

        if (requiresAuth() && !AuthManager.isLoggedIn(getApplicationContext())) {
            Intent i = new Intent(this, HomeActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
            return;
        }

        if (android.os.Build.VERSION.SDK_INT >= 33 && !sAskedNotifPerm) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        4242
                );
            }
            sAskedNotifPerm = true;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    "notif_fcm_console_default",
                    "Console Test Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );

            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(ch);
        }

        if (AuthManager.isLoggedIn(getApplicationContext())) {
            WsHub.get().start(getApplicationContext());

            if (!sRegisteredFcmToken) {
                FirebaseMessaging.getInstance().getToken()
                        .addOnCompleteListener(task -> {
                            if (!task.isSuccessful()) {
                                Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                                return;
                            }

                            String token = task.getResult();
                            Log.d("FCM", "Initial FCM token: " + token);
                            AuthManager.registerFcmToken(getApplicationContext(), token);
                            sRegisteredFcmToken = true;
                        });
            }

            if (wsReceiver == null) {
                wsReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (!WsHub.ACTION_WS_MESSAGE.equals(intent.getAction())) return;

                        String title = intent.getStringExtra(WsHub.EXTRA_TITLE);
                        String body  = intent.getStringExtra(WsHub.EXTRA_TEXT);
                        if (title == null) title = "Hospital update";
                        if (body == null)  body  = "";

                        showInAppNotification(title, body);
                    }
                };
            }

            if (!wsReceiverRegistered) {
                IntentFilter filter = new IntentFilter(WsHub.ACTION_WS_MESSAGE);
                registerReceiver(wsReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
                wsReceiverRegistered = true;
            }

        }
    }

    /**
     * Decrements the websocket reference count and disconnects it when
     * no screens are visible (app background), minimizing reconnection churn.
     */
    @Override
    protected void onStop() {
        super.onStop();

        if (wsReceiverRegistered && wsReceiver != null) {
            try {
                unregisterReceiver(wsReceiver);
            } catch (IllegalArgumentException ignored) {
            }
            wsReceiverRegistered = false;
        }

        WsHub.get().stop();
    }


    /**
     * Suppresses the legacy toolbar menu for screens that use the bottom navigation
     * and the slide-over “More” panel. Returning {@code false} prevents the menu
     * from being shown.
     *
     * @param menu ignored
     * @return {@code false} to indicate no options menu is provided
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    /**
     * Handler because this base class delegates navigation to the bottom
     * nav and the “More” panel. Subclasses that actually inflate an options menu
     * can override this method and call {@code super} as needed.
     *
     * @param item the selected menu item
     * @return {@code super.onOptionsItemSelected(item)} by default
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    /**
     * Standard toolbar helper for activities that still use an app bar.
     * Safe to call even when the layout omits a toolbar.
     *
     * @param toolbarId the resource id of the Toolbar in the layout
     */
    protected void wireToolbar(int toolbarId) {
        androidx.appcompat.widget.Toolbar tb = findViewById(toolbarId);
        setSupportActionBar(tb);
    }

    /**
     * Wires the bottom navigation and the custom "More" slide-over menu.
     * Handles role-based visibility and navigation without recreating animations.
     *
     * @param selectedItemId the id of the nav item that should appear selected in this screen
     */
    protected void setupBottomNav(int selectedItemId) {
        BottomNavigationView nav = findViewById(R.id.bottomNav);
        moreMenu = findViewById(R.id.moreMenu);

        if (moreMenu != null) {
            String role = AuthManager.getRole(getApplicationContext());
            moreMenu.configureForRole(role);
            moreMenu.setListener(new MoreMenuLayout.Listener() {
                /**
                 * Invoked when the user selects the notifications entry in the
                 * "More" menu. Opens {@link NotificationsActivity}.
                 */
                @Override public void onNotifications() {
                    startActivity(new Intent(BaseActivity.this, NotificationsActivity.class));
                }

                /**
                 * Invoked when the user selects the histories entry. For users
                 * with a doctor role, opens {@link HistoriesActivity}; ignored
                 * for other roles.
                 */
                @Override public void onHistories() {
                    if (AuthManager.isDoctor(getApplicationContext())) {
                        startActivity(new Intent(BaseActivity.this, HistoriesActivity.class));
                    }
                }

                /**
                 * Invoked when the admin entry is selected in the "More" menu.
                 * Opens {@link AdminActivity} for users with an admin role.
                 */
                @Override public void onAdmin() {
                    if (AuthManager.isAdmin(getApplicationContext())) {
                        startActivity(new Intent(BaseActivity.this, AdminActivity.class));
                    }
                }

                /**
                 * Invoked when the pharmacist entry is selected. For users
                 * with a pharmacist role, opens {@link PharmacistActivity}.
                 */
                @Override public void onPharmacist() {
                    if (AuthManager.isPharmacist(getApplicationContext())) {
                        startActivity(new Intent(BaseActivity.this, PharmacistActivity.class));
                    }
                }

                /**
                 * Invoked when the doctor prescriptions entry is selected.
                 * For doctor users, opens {@link DoctorPrescriptionsActivity}.
                 */
                @Override public void onDoctorPrescriptions() {
                    if (AuthManager.isDoctor(getApplicationContext())) {
                        startActivity(new Intent(BaseActivity.this, DoctorPrescriptionsActivity.class));
                    }
                }

                /**
                 * Invoked when the settings entry is selected in the "More"
                 * menu. Opens {@link SettingsActivity}.
                 */
                @Override public void onSettings() {
                    startActivity(new Intent(BaseActivity.this, SettingsActivity.class));
                }

                /**
                 * Invoked when the logout entry is selected. Closes the menu,
                 * stops global websocket connections, clears auth state, shows
                 * a confirmation toast, and returns the user to {@link HomeActivity}.
                 */
                @Override public void onLogout() {
                    moreMenu.close();

                    WsHub.get().stop();
                    AuthManager.clear(getApplicationContext());
                    Toast.makeText(getApplicationContext(),
                            "You have been logged out.", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(BaseActivity.this, HomeActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                }
            });

            if (moreMenuBackCallback == null) {
                moreMenuBackCallback = new OnBackPressedCallback(true) {
                    /**
                     * Handles system back presses while this activity is visible.
                     * If the "More" menu is open, it is closed; otherwise the
                     * callback disables itself and delegates to the default
                     * back-press dispatcher.
                     */
                    @Override
                    public void handleOnBackPressed() {
                        if (moreMenu != null && moreMenu.isOpen()) {
                            moreMenu.close();
                        } else {
                            setEnabled(false);
                            getOnBackPressedDispatcher().onBackPressed();
                        }
                    }
                };
                getOnBackPressedDispatcher().addCallback(this, moreMenuBackCallback);
            }
        }

        if (nav == null) return;

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_more) {
                if (moreMenu != null) {
                    if (!moreMenu.isOpen()) moreMenu.open();
                    else moreMenu.close();
                }
                return false;
            }

            if (moreMenu != null && moreMenu.isOpen()) {
                moreMenu.close();
            }

            if (id == selectedItemId) return true;

            Intent intent = null;
            if (id == R.id.nav_home) {
                intent = new Intent(this, HomeActivity.class);
            } else if (id == R.id.nav_pharmacy) {
                intent = new Intent(this, PharmacyActivity.class);
            } else if (id == R.id.nav_messages) {
                intent = new Intent(this, MessagesActivity.class);
            } else if (id == R.id.nav_appointments) {
                if (AuthManager.isAdmin(getApplicationContext()) || AuthManager.isPharmacist(getApplicationContext()))
                {
                    toast("No access");
                }
                else {
                    intent = new Intent(this, AppointmentsActivity.class);
                }
            }

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });

        if (selectedItemId != 0) {
            nav.setSelectedItemId(selectedItemId);
        }
    }

    /**
     * Centralized Volley 401 handling. Attempts a single token refresh; if refresh fails,
     * clears auth state and navigates to {@link LoginActivity}.
     *
     * @param error the Volley error received from a request
     * @param retry a {@link Runnable} to execute if refresh succeeds
     */
    protected void handleVolleyAuthError(com.android.volley.VolleyError error, Runnable retry) {
        int code = (error.networkResponse != null) ? error.networkResponse.statusCode : -1;

        if (code == 401) {
            if (retriedAfterRefresh) {
                retriedAfterRefresh = false;
                WsHub.get().stop();
                AuthManager.clear(getApplicationContext());
                Toast.makeText(getApplicationContext(),
                        "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(BaseActivity.this, LoginActivity.class));
                finish();
                return;
            }
            retriedAfterRefresh = true;
            TokenRefresher.refresh(getApplicationContext(), new TokenRefresher.Callback() {
                /**
                 * Called when the access token has been successfully refreshed.
                 * If the activity is still running, the original request is
                 * retried via the supplied {@code retry} runnable.
                 */
                @Override public void onRefreshed() {
                    if (!isFinishing()) retry.run();
                }
                /**
                 * Called when token refresh fails. Clears any saved auth state,
                 * stops websocket connections, notifies the user, and navigates
                 * to {@link LoginActivity} to force a new sign-in.
                 */
                @Override public void onFailed() {
                    retriedAfterRefresh = false;
                    WsHub.get().stop();
                    AuthManager.clear(getApplicationContext());
                    Toast.makeText(getApplicationContext(),
                            "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(BaseActivity.this, LoginActivity.class));
                    finish();
                }
            });
            return;
        }

        retriedAfterRefresh = false;
        Toast.makeText(getApplicationContext(),
                "Request failed (" + code + ")", Toast.LENGTH_SHORT).show();
    }

    /**
     * Shows an in-app slide-down notification banner, if the current layout
     * includes the banner views (R.id.inAppNotifBar, inAppNotifTitle, inAppNotifBody).
     */
    protected void showInAppNotification(String title, String body) {
        final View bar = findViewById(R.id.inAppNotifBar);
        if (bar == null) {
            return;
        }

        TextView titleView = bar.findViewById(R.id.inAppNotifTitle);
        TextView bodyView  = bar.findViewById(R.id.inAppNotifBody);

        if (titleView != null) titleView.setText(title);
        if (bodyView != null)  bodyView.setText(body);

        bar.clearAnimation();
        bar.setVisibility(View.VISIBLE);

        bar.post(() -> {
            float fromY = -bar.getHeight();
            bar.setTranslationY(fromY);

            bar.animate()
                    .translationY(0f)
                    .setDuration(220)
                    .withEndAction(() -> {
                        bar.postDelayed(() -> {
                            bar.animate()
                                    .translationY(fromY)
                                    .setDuration(200)
                                    .withEndAction(() -> bar.setVisibility(View.GONE))
                                    .start();
                        }, 3000);
                    })
                    .start();
        });

        bar.setOnClickListener(v -> {
            bar.clearAnimation();
            bar.animate()
                    .translationY(-bar.getHeight())
                    .setDuration(150)
                    .withEndAction(() -> bar.setVisibility(View.GONE))
                    .start();
        });
    }


    /**
     * Convenience toast helper used across subclasses.
     *
     * @param s the message to display as a long toast
     */
    void toast(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }
}
