package com.project.saintcyshospital;

import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
public class TriceSystemTest {

    /**
     * Test 1:
     * When the user is NOT logged in, the home screen should:
     * - show Login and Signup buttons
     * - hide the bottom navigation and more menu.
     */
    @Test
    public void home_notLoggedIn_showsAuthButtonsAndHidesNav() {
        ActivityScenario<HomeActivity> scenario =
                ActivityScenario.launch(HomeActivity.class);

        onView(withId(R.id.main_login_btn))
                .check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
        onView(withId(R.id.main_signup_btn))
                .check(matches(withEffectiveVisibility(Visibility.VISIBLE)));

        onView(withId(R.id.bottomNav))
                .check(matches(withEffectiveVisibility(Visibility.GONE)));
        onView(withId(R.id.moreMenu))
                .check(matches(withEffectiveVisibility(Visibility.GONE)));

    }

    /**
     * Test 2:
     * From the home screen, tapping "Get Started" should open the login screen.
     * We verify by checking that the username field on LoginActivity is visible.
     */
    @Test
    public void home_clickGetStarted_opensLoginScreen() {
        ActivityScenario<HomeActivity> scenario =
                ActivityScenario.launch(HomeActivity.class);

        onView(withId(R.id.main_login_btn)).perform(click());

        onView(withId(R.id.login_username_edt))
                .check(matches(isDisplayed()));
    }

    /**
     * Test 3:
     * On LoginActivity, if username and password are empty,
     * clicking Login should NOT navigate away.
     * We verify by checking that we are still on LoginActivity
     * and the login UI is still visible.
     */
    @Test
    public void login_emptyFields_keepsUserOnLoginScreen() {
        ActivityScenario<LoginActivity> scenario =
                ActivityScenario.launch(LoginActivity.class);

        onView(withId(R.id.login_login_btn)).perform(click());

        onView(withId(R.id.login_username_edt))
                .check(matches(isDisplayed()));
        onView(withId(R.id.login_password_edt))
                .check(matches(isDisplayed()));
    }

    /**
     * Test 4:
     * On SignupActivity, if password and confirm password don't match,
     * clicking Signup should NOT navigate away.
     * We verify by checking that we are still on SignupActivity
     * and the signup UI is still visible.
     */
    @Test
    public void signup_mismatchedPasswords_keepsUserOnSignupScreen() {
        ActivityScenario<SignupActivity> scenario =
                ActivityScenario.launch(SignupActivity.class);

        onView(withId(R.id.signup_username_edt))
                .perform(typeText("testuser"), closeSoftKeyboard());
        onView(withId(R.id.signup_email_edt))
                .perform(typeText("test@example.com"), closeSoftKeyboard());
        onView(withId(R.id.signup_password_edt))
                .perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.signup_password_confirm_edt))
                .perform(typeText("different123"), closeSoftKeyboard());

        onView(withId(R.id.signup_signup_btn)).perform(click());

        onView(withId(R.id.signup_username_edt))
                .check(matches(isDisplayed()));
        onView(withId(R.id.signup_signup_btn))
                .check(matches(isDisplayed()));
    }

    /**
     * Test 5:
     * From the signup screen, tapping the "ALREADY HAVE AN ACCOUNT? LOG IN NOW!"
     * link should navigate back to LoginActivity.
     */
    @Test
    public void signup_clickLoginLink_opensLoginScreen() {
        ActivityScenario<SignupActivity> scenario =
                ActivityScenario.launch(SignupActivity.class);

        onView(withId(R.id.signup_login_link)).perform(click());

        onView(withId(R.id.login_username_edt))
                .check(matches(isDisplayed()));
    }
}
