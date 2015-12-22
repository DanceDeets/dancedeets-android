package com.dancedeets.android;


import android.content.pm.ActivityInfo;
import android.util.Log;

import com.dancedeets.android.eventlist.SearchListActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringContains.containsString;

/**
 * Created by lambert on 2014/10/28.
 */
public class SearchListActivityTest extends CommonActivityTest<SearchListActivity> {

    private final static String LOG_TAG = "EventListActivityTest";

    private final static String mEventTitle = "Event 1";

    public SearchListActivityTest() throws NoSuchFieldException {
        super(SearchListActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        getActivity();
    }

    @SuppressWarnings("unchecked")
    public void testEventNavigation() {
        onView(withId(R.id.event_list_description));
        waitForVolley(true);
        // Click on an event
        onView(withText(mEventTitle)).perform(click());
        // Verify the description loaded
        onView(allOf(hasSibling(withText(mEventTitle)),withId(R.id.description))).check(matches(withText(containsString("Come learn something new"))));
    }

    @SuppressWarnings("unchecked")
    public void testScreenRotations() {

        Log.i(LOG_TAG, "Setting up preconditions");
        waitForVolley(false);

        Log.i(LOG_TAG, "getActivity");
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Log.i(LOG_TAG, "Wait for View");
        onView(withId(R.id.event_list_description));
        onView(withClassName(endsWith("ProgressBar"))).check(matches(isDisplayed()));

        Log.i(LOG_TAG, "Rotating");
        // Guarantee deconstruction and construction of views...is there a better way to do this?
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        // If this crashes, then we are not handling the orientation change correctly
        Log.i(LOG_TAG, "Letting responses finish, and making volley wait.");
        waitForVolley(true);
        Log.i(LOG_TAG, "Verify data loaded.");

        onView(withClassName(endsWith("ProgressBar"))).check(matches(not(isDisplayed())));

        Log.i(LOG_TAG, "Clicking search button");
        onView(withId(R.id.action_search)).perform(click());

        onView(withId(R.id.search_location)).perform(clearText(), typeText("location"), closeSoftKeyboard());
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        onView(withId(R.id.search_keywords)).perform(clearText(), typeText("keywords"), closeSoftKeyboard());
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        onView(withId(R.id.search_location)).check(matches(withText("location")));
        onView(withId(R.id.search_keywords)).check(matches(withText("keywords")));
        onView(withId(android.R.id.button1)).perform(click());

        // Wait for events to load
        onView(withText(mEventTitle)).check(matches(withText(mEventTitle)));

        // Now turn off Volley responses, before we click the event
        waitForVolley(false);

        // Now click it
        onView(withText(mEventTitle)).perform(click());


        // Verify loading the info page is waiting on volley
        onView(withinActivePager(withId(R.id.progress_container))).check(matches(isDisplayed()));

        waitForVolley(true);

        // And that it now completes successfully
        onView(withinActivePager(withId(R.id.progress_container))).check(matches(not(isDisplayed())));

        onView(withinActivePager(withId(R.id.description))).check(matches(withText(is("Event 1 description."))));
    }

}
