package com.dancedeets.android;


import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Looper;
import android.test.ActivityInstrumentationTestCase2;

import com.android.volley.ExecutorDelivery;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.NoCache;
import com.dancedeets.dancedeets.R;
import com.google.android.apps.common.testing.ui.espresso.Espresso;

import java.io.File;

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
    public EventListActivityTest() {
        super(EventListActivity.class);
    }

    private VolleyDiskBasedHttpStack mHttpStack;
    private VolleyIdlingResource mIdlingResource;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        createVolleyForEspresso();
        getActivity();
    }

    protected void createVolleyForEspresso() throws NoSuchFieldException {
        // Construct our own queue, with a proper VolleyIdlingResource handler
        mHttpStack = new VolleyDiskBasedHttpStack();
        Network network = new BasicNetwork(mHttpStack);
        File cacheDir = new File(getInstrumentation().getContext().getCacheDir(), "volleyTest");
        mIdlingResource = new VolleyIdlingResource("Load Event List");

        Handler mHandler = new Handler(Looper.getMainLooper());
        // Wrap our normal ExecutorDelivery(Handler(MainLooper)) with one that notifies our idlingResource.
        ExecutorDelivery mDispatch = mIdlingResource.getExecutorDeliveryWrapper(mHandler);
        RequestQueue queue = new RequestQueue(new NoCache(), network, 4, mDispatch);
        queue.start();

        VolleySingleton.createInstance(queue);
        Espresso.registerIdlingResources(mIdlingResource);
    }

    protected void setBlockVolleyResponses(boolean blockVolleyResponses) {
        mHttpStack.setBlockResponses(blockVolleyResponses);
    }

    protected void setWaitForVolley(boolean waitForVolley) {
        mIdlingResource.setWaitForVolley(waitForVolley);
    }

    @SuppressWarnings("unchecked")
    public void testEventNavigation() throws NoSuchFieldException {
        onView(withId(R.id.event_list_fragment));
        String eventTitle = "Mike's Popping Class @ Brooklyn Ballet";
        // Click on an event
        onView(withText(eventTitle)).perform(click());
        // Verify the description loaded
        onView(allOf(hasSibling(withText(eventTitle)),withId(R.id.description))).check(matches(withText(containsString("Come learn something new"))));
    }

    @SuppressWarnings("unchecked")
    public void testRotationDuringLoad() throws NoSuchFieldException {
        setBlockVolleyResponses(true);
        setWaitForVolley(false);
        onView(withId(R.id.event_list_fragment));
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        // If this crashes, then we are not handling the orientation change correctly
        setBlockVolleyResponses(false);
        setWaitForVolley(true);
        // Click on an event
        onView(withText("Mike's Popping Class @ Brooklyn Ballet")).perform(click());
        // Verify the description loaded
        onView(withId(R.id.description)).check(matches(withText(containsString("Come learn something new"))));
    }


}
