package com.project.saintcyshospital;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

/**
 * Quinn's system / end-to-end tests for the Saint Cys Hospital app.
 * Focuses on Login and Signup flows and their navigation / validation.
 */
@RunWith(AndroidJUnit4.class)
public class QuinnSystemTest {

    /**
     * Test 1 (non-trivial):
     * From the login screen, tapping the "Don't have an account? Sign up"
     * link should open SignupActivity (verify username field is visible).
     */
    @Test
    public void login_clickSignupLink_opensSignupScreen() {
        ActivityScenario<LoginActivity> scenario =
                ActivityScenario.launch(LoginActivity.class);

        // Click the sign-up link on the login screen
        onView(withId(R.id.login_signup_link)).perform(click());

        // Now the signup username field should be displayed
        onView(withId(R.id.signup_username_edt))
                .check(matches(isDisplayed()));
    }

    /**
     * Test 2 (non-trivial):
     * On the login screen, if ONLY username is entered (no password),
     * clicking Login should keep us on the Login screen (no navigation).
     */
    @Test
    public void login_onlyUsername_staysOnLoginScreen() {
        ActivityScenario<LoginActivity> scenario =
                ActivityScenario.launch(LoginActivity.class);

        // Type username but leave password empty
        onView(withId(R.id.login_username_edt))
                .perform(typeText("someuser"), closeSoftKeyboard());

        // Click Login
        onView(withId(R.id.login_login_btn)).perform(click());

        // We should still be on LoginActivity (username field visible)
        onView(withId(R.id.login_username_edt))
                .check(matches(isDisplayed()));
    }

    /**
     * Test 3 (non-trivial):
     * On the login screen, if ONLY password is entered (no username),
     * clicking Login should also keep us on the Login screen.
     */
    @Test
    public void login_onlyPassword_staysOnLoginScreen() {
        ActivityScenario<LoginActivity> scenario =
                ActivityScenario.launch(LoginActivity.class);

        // Type password but leave username empty
        onView(withId(R.id.login_password_edt))
                .perform(typeText("password123"), closeSoftKeyboard());

        // Click Login
        onView(withId(R.id.login_login_btn)).perform(click());

        // We should still be on LoginActivity (password field visible)
        onView(withId(R.id.login_password_edt))
                .check(matches(isDisplayed()));
    }

    /**
     * Test 4 (non-trivial):
     * From the signup screen, tapping the "Already have an account? Log in"
     * link should open LoginActivity.
     */
    @Test
    public void signup_clickLoginLink_opensLoginScreen() {
        ActivityScenario<SignupActivity> scenario =
                ActivityScenario.launch(SignupActivity.class);

        // Click the login link at the bottom of the signup screen
        onView(withId(R.id.signup_login_link)).perform(click());

        // Login screen's username field should now be visible
        onView(withId(R.id.login_username_edt))
                .check(matches(isDisplayed()));
    }
}
