package com.dancedeets.android;


import android.os.Handler;
import android.os.Looper;
import android.test.ActivityInstrumentationTestCase2;

import com.android.volley.ExecutorDelivery;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.dancedeets.dancedeets.R;
import com.google.android.apps.common.testing.ui.espresso.Espresso;

import java.io.File;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;

/**
 * Created by lambert on 2014/10/28.
 */
public class EventListActivityTest extends ActivityInstrumentationTestCase2<EventListActivity> {
    public EventListActivityTest() {
        super(EventListActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        createVolleyForEspresso();
        getActivity();
    }

    protected void createVolleyForEspresso() throws NoSuchFieldException {
        // Construct our own queue, with a proper VolleyIdlingResource handler
        Network network = new BasicNetwork(new VolleyDiskBasedHttpStack());
        File cacheDir = new File(getInstrumentation().getContext().getCacheDir(), "volleyTest");
        VolleyIdlingResource idlingResource = new VolleyIdlingResource("Load Event List");

        Handler mHandler = new Handler(Looper.getMainLooper());
        // Wrap our normal ExecutorDelivery(Handler(MainLooper)) with one that notifies our idlingResource.
        ExecutorDelivery mDispatch = idlingResource.getExecutorDeliveryWrapper(mHandler);
        RequestQueue queue = new RequestQueue(new DiskBasedCache(cacheDir), network, 4, mDispatch);
        queue.start();

        VolleySingleton.createInstance(queue);
        Espresso.registerIdlingResources(idlingResource);
    }

    @SuppressWarnings("unchecked")
    public void testOpenOverflowInActionMode() throws NoSuchFieldException {
        onView(withId(R.id.event_list_fragment));
        onView(withText("Mike's Popping Class @ Brooklyn Ballet")).perform(click());
    }

}
