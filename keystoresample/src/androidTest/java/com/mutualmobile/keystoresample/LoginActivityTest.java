package com.mutualmobile.keystoresample;

import android.support.design.widget.TextInputLayout;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

@LargeTest @RunWith(AndroidJUnit4.class) public class LoginActivityTest {

  @Rule public ActivityTestRule<LoginActivity> mActivityTestRule =
      new ActivityTestRule<>(LoginActivity.class);

  @Test public void loginActivityTest() {
    ViewInteraction appCompatAutoCompleteTextView = onView(allOf(withId(R.id.email),
        childAtPosition(
            childAtPosition(withClassName(is("android.support.design.widget.TextInputLayout")), 0),
            0)));
    appCompatAutoCompleteTextView.perform(scrollTo(), click());

    ViewInteraction appCompatAutoCompleteTextView2 = onView(
        allOf(withId(R.id.email), withText("anmol@mm.com"), childAtPosition(
            childAtPosition(withClassName(is("android.support.design.widget.TextInputLayout")), 0),
            0)));
    appCompatAutoCompleteTextView2.perform(scrollTo(), click());

    ViewInteraction appCompatAutoCompleteTextView3 = onView(
        allOf(withId(R.id.email), withText("anmol@mm.com"), childAtPosition(
            childAtPosition(withClassName(is("android.support.design.widget.TextInputLayout")), 0),
            0)));
    appCompatAutoCompleteTextView3.perform(scrollTo(), replaceText("anmol@mm.com"));

    ViewInteraction appCompatAutoCompleteTextView4 = onView(
        allOf(withId(R.id.email), withText("anmol@mm.com"), childAtPosition(
            childAtPosition(withClassName(is("android.support.design.widget.TextInputLayout")), 0),
            0), isDisplayed()));
    appCompatAutoCompleteTextView4.perform(closeSoftKeyboard());

    ViewInteraction appCompatEditText = onView(allOf(withId(R.id.password), withText("qwerty"),
        childAtPosition(
            childAtPosition(withClassName(is("android.support.design.widget.TextInputLayout")), 0),
            0)));
    appCompatEditText.perform(scrollTo(), replaceText("qwerty"));

    ViewInteraction appCompatEditText2 = onView(allOf(withId(R.id.password), withText("qwerty"),
        childAtPosition(
            childAtPosition(withClassName(is("android.support.design.widget.TextInputLayout")), 0),
            0), isDisplayed()));
    appCompatEditText2.perform(closeSoftKeyboard());

    ViewInteraction appCompatButton = onView(
        allOf(withId(R.id.email_sign_in_button), withText("Sign in or register"), childAtPosition(
            allOf(withId(R.id.email_login_form), childAtPosition(withId(R.id.login_form), 0)), 2)));
    appCompatButton.perform(scrollTo(), click());

    ViewInteraction editText = onView(allOf(withId(R.id.email),
        childAtPosition(childAtPosition(IsInstanceOf.<View>instanceOf(TextInputLayout.class), 0),
            0), isDisplayed()));
    editText.check(matches(withText("")));

    ViewInteraction editText2 = onView(allOf(withId(R.id.password),
        childAtPosition(childAtPosition(IsInstanceOf.<View>instanceOf(TextInputLayout.class), 0),
            0), isDisplayed()));
    editText2.check(matches(withText("")));

    ViewInteraction editText3 = onView(allOf(withId(R.id.password),
        childAtPosition(childAtPosition(IsInstanceOf.<View>instanceOf(TextInputLayout.class), 0),
            0), isDisplayed()));
    editText3.check(matches(withText("")));
  }

  private static Matcher<View> childAtPosition(final Matcher<View> parentMatcher,
      final int position) {

    return new TypeSafeMatcher<View>() {
      @Override public void describeTo(Description description) {
        description.appendText("Child at position " + position + " in parent ");
        parentMatcher.describeTo(description);
      }

      @Override public boolean matchesSafely(View view) {
        ViewParent parent = view.getParent();
        return parent instanceof ViewGroup && parentMatcher.matches(parent) && view.equals(
            ((ViewGroup) parent).getChildAt(position));
      }
    };
  }
}
