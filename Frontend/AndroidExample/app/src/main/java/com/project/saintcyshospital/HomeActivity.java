package com.project.saintcyshospital;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.view.ViewCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Home screen activity for the Saint Cys Hospital app.
 * <p>
 * Displays a welcome message and login/sign-up options when the user is not
 * authenticated, or a personalized greeting and bottom navigation when the
 * user is logged in. Acts as the main entry point after launching the app.
 * @author quinn beckman
 */
public class HomeActivity extends BaseActivity {

    /**
     * Text view used to display the main message on the home screen.
     */
    private TextView messageText;

    /**
     * Text view used to show the current user's name or a prominent title.
     */
    private TextView usernameText;

    /**
     * Button that navigates the user to the login screen.
     */
    private Button loginButton;

    /**
     * Button that navigates the user to the sign-up/registration screen.
     */
    private Button signupButton;

    /**
     * Initializes the home screen, sets up view bindings, and configures the UI
     * based on whether the user is currently logged in.
     *
     * @param savedInstanceState previously saved instance state, or {@code null}
     *                           if the activity is starting fresh.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CartStore.get().init(getApplicationContext());
        setContentView(R.layout.activity_main);

        // Bind views
        messageText = findViewById(R.id.main_msg_text);
        usernameText = findViewById(R.id.main_username_txt);
        loginButton = findViewById(R.id.main_login_btn);
        signupButton = findViewById(R.id.main_signup_btn);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        View moreMenu = findViewById(R.id.moreMenu);
        boolean loggedIn = AuthManager.isLoggedIn(getApplicationContext());

        if (loggedIn) {
            // Show welcome state
            String username = AuthManager.getUsername(getApplicationContext());
            if (username == null) username = "";

            messageText.setText("Welcome!");
            usernameText.setText("Welcome " + username);
            usernameText.setVisibility(View.VISIBLE);

            loginButton.setVisibility(View.GONE);
            signupButton.setVisibility(View.GONE);

            if (bottomNav != null) {
                bottomNav.setVisibility(View.VISIBLE);

                // Keep inset fix
                ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (view, insets) -> {
                    int left = view.getPaddingLeft();
                    int top = view.getPaddingTop();
                    int right = view.getPaddingRight();
                    view.setPadding(left, top, right, 0);
                    return insets;
                });

                // Enable nav only when logged in
                setupBottomNav(R.id.nav_home);
            }
            if (moreMenu != null) {
                moreMenu.setVisibility(View.VISIBLE);
            }
        } else {
            usernameText.setTextSize(50);
            usernameText.setVisibility(View.VISIBLE);

            loginButton.setVisibility(View.VISIBLE);
            signupButton.setVisibility(View.VISIBLE);

            if (bottomNav != null) {
                bottomNav.setVisibility(View.GONE);
            }
            if (moreMenu != null) {
                moreMenu.setVisibility(View.GONE);
            }
        }

        // Button actions (safe in both states; visibility controls access)
        loginButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        signupButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Indicates whether this activity requires the user to be authenticated
     * before it can be accessed.
     *
     * @return {@code false} because the home screen is accessible
     *         without authentication.
     */
    @Override
    protected boolean requiresAuth() {
        return false;
    }
}