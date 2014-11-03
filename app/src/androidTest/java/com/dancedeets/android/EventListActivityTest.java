package com.dancedeets.android;


import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Looper;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.android.volley.ExecutorDelivery;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.NoCache;
import com.dancedeets.dancedeets.R;
import com.google.android.apps.common.testing.ui.espresso.Espresso;

import java.util.Random;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.hasSibling;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.text.StringContains.containsString;

/**
 * Created by lambert on 2014/10/28.
 */
public class EventListActivityTest extends ActivityInstrumentationTestCase2<EventListActivity> {

    private VolleyDiskBasedHttpStack mHttpStack;
    private VolleyIdlingResource mIdlingResource;

    public EventListActivityTest() throws NoSuchFieldException {
        super(EventListActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        createVolleyForEspresso();
    }

    protected void createVolleyForEspresso() throws NoSuchFieldException {
        Log.i("TEST", "createVolleyForEspresso");
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
        Log.i("TEST", "registerIdlingResources: " + randomString + ": " + mIdlingResource);
        Espresso.registerIdlingResources(mIdlingResource);
        queue.start();
    }

    @Override
    public void tearDown() throws Exception {
        Log.i("TEST", "tearDown");
        // We need to "kill" the IdlingResource since we don't need it anymore.
        // But Espresso does not currently have an unregisterIdlingResources() to call.
        // So instead we make it impotent, so it doesn't affect anything going forward.
        mIdlingResource.makeImpotent();
        super.tearDown();
    }

    protected void setBlockVolleyResponses(boolean blockVolleyResponses) {
        mHttpStack.setBlockResponses(blockVolleyResponses);
    }

    protected void setWaitForVolley(boolean waitForVolley) {
        mIdlingResource.setWaitForVolley(waitForVolley);
    }

    @SuppressWarnings("unchecked")
    public void testEventNavigation() {
        Log.i("TEST", "mIdlingResource is "+mIdlingResource);
        getActivity();

        onView(withId(R.id.event_list_fragment));
        String eventTitle = "Mike's Popping Class @ Brooklyn Ballet";
        // Click on an event
        onView(withText(eventTitle)).perform(click());
        // Verify the description loaded
        onView(allOf(hasSibling(withText(eventTitle)),withId(R.id.description))).check(matches(withText(containsString("Come learn something new"))));
    }

    @SuppressWarnings("unchecked")
    public void testRotationDuringLoad() {
        Log.i("TEST", "Setting up preconditions");
        setBlockVolleyResponses(true);
        setWaitForVolley(false);

        Log.i("TEST", "getActivity");
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Log.i("TEST", "Wait for View");
        onView(withId(R.id.event_list_fragment));
        Log.i("TEST", "Rotating");
        // Guarantee deconstruction and construction of views...is there a better way to do this?
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        // If this crashes, then we are not handling the orientation change correctly
        Log.i("TEST", "Letting responses finish, and making volley wait.");
        setWaitForVolley(true);
        setBlockVolleyResponses(false);
        Log.i("TEST", "Verify data loaded.");
        // We should verify that the event is displayed and shown, not try to click on it
        String eventTitle = "Mike's Popping Class @ Brooklyn Ballet";
        onView(withText(eventTitle)).perform(click());

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }


}
