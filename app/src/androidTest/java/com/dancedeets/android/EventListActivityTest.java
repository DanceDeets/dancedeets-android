package com.dancedeets.android;


import android.test.ActivityInstrumentationTestCase2;

import com.dancedeets.dancedeets.R;
import com.google.android.apps.common.testing.ui.espresso.Espresso;

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
        VolleySingleton.forceCreateInstance(getInstrumentation().getContext(), new VolleyDiskBasedHttpStack());
        getActivity();
    }


    @SuppressWarnings("unchecked")
    public void testOpenOverflowInActionMode() throws NoSuchFieldException {
        onView(withId(R.id.event_list_fragment));
        VolleyIdlingResource resource = new VolleyIdlingResource("Load Event List");
        Espresso.registerIdlingResources(resource);
        onView(withText("Mike's Popping Class @ Brooklyn Ballet")).perform(click());
        VolleyIdlingResource resource2 = new VolleyIdlingResource("Load Event Data");
        Espresso.registerIdlingResources(resource2);
    }

}
