package com.dancedeets.android;


import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Looper;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.View;

import com.android.volley.ExecutorDelivery;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.NoCache;
import com.dancedeets.dancedeets.R;
import com.google.android.apps.common.testing.ui.espresso.Espresso;

import org.hamcrest.Matcher;

import java.util.Random;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.clearText;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.closeSoftKeyboard;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.typeText;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.hasSibling;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDescendantOfA;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withClassName;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.text.StringContains.containsString;

/**
 * Created by lambert on 2014/10/28.
 */
public class EventListActivityTest extends ActivityInstrumentationTestCase2<EventListActivity> {

    private final static String LOG_TAG = "EventListActivityTest";

    private VolleyDiskBasedHttpStack mHttpStack;
    private VolleyIdlingResource mIdlingResource;

    private final static String mEventTitle = "Mike's Popping Class @ Brooklyn Ballet";

    public EventListActivityTest() throws NoSuchFieldException {
        super(EventListActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        getActivity();

        createVolleyForEspresso();
    }

    protected void createVolleyForEspresso() throws NoSuchFieldException {
        // Construct our own queue, with a proper VolleyIdlingResource handler
        mHttpStack = new VolleyDiskBasedHttpStack();
        Network network = new BasicNetwork(mHttpStack);
        String randomString = Integer.toString(new Random().nextInt());
        mIdlingResource = new VolleyIdlingResource("VolleyRequestQueue_" + randomString);

        Handler mHandler = new Handler(Looper.getMainLooper());
        // Wrap our normal ExecutorDelivery(Handler(MainLooper)) with one that notifies our idlingResource.
        ExecutorDelivery mDispatch = mIdlingResource.getExecutorDeliveryWrapper(mHandler);
        RequestQueue queue = new RequestQueue(new NoCache(), network, 4, mDispatch);

        VolleySingleton.createInstance(queue);
        Espresso.registerIdlingResources(mIdlingResource);
        queue.start();
    }

    @Override
    public void tearDown() throws Exception {
        // We need to "kill" the IdlingResource since we don't need it anymore.
        // But Espresso does not currently have an unregisterIdlingResources() to call.
        // So instead we make it impotent, so it doesn't affect anything going forward.
        mIdlingResource.makeImpotent();
        super.tearDown();
    }

    protected void waitForVolley(boolean runVolley) {
        mHttpStack.setBlockResponses(!runVolley);
        mIdlingResource.setWaitForVolley(runVolley);
    }

    @SuppressWarnings("unchecked")
    public void testEventNavigation() {
        onView(withId(R.id.event_list_fragment));
        waitForVolley(true);
        // Click on an event
        onView(withText(mEventTitle)).perform(click());
        // Verify the description loaded
        onView(allOf(hasSibling(withText(mEventTitle)),withId(R.id.description))).check(matches(withText(containsString("Come learn something new"))));
    }

    @SuppressWarnings("unchecked")
    public static Matcher<View> withinActivePager(Matcher<View> matcher) {
        return allOf(matcher, isDescendantOfA(allOf(withId(R.id.event_info_fragment), isDisplayed())));
    }

    @SuppressWarnings("unchecked")
    public void testScreenRotations() {

        Log.i(LOG_TAG, "Setting up preconditions");
        waitForVolley(false);

        Log.i(LOG_TAG, "getActivity");
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Log.i(LOG_TAG, "Wait for View");
        onView(withId(R.id.event_list_fragment));
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

        //TODO: move all this to an EventInfoActivityTest!

        onView(withinActivePager(withId(R.id.progress_container))).check(matches(isDisplayed()));

        waitForVolley(true);

        onView(withinActivePager(withId(R.id.progress_container))).check(matches(not(isDisplayed())));

        onView(withinActivePager(withId(R.id.description))).check(matches(withText(containsString("Come learn something new"))));

        //This use of getActivity does not work, as it points at the wrong (old!) activity!
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        onView(withinActivePager(withId(R.id.description))).check(matches(withText(containsString("Come learn something new"))));

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        onView(withinActivePager(withId(R.id.description))).check(matches(withText(containsString("Come learn something new"))));

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

}
