package com.dancedeets.android.robotests;

import com.dancedeets.android.EventListActivity;
import com.dancedeets.android.EventListFragment;
import com.dancedeets.dancedeets.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.util.ActivityController;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricGradleTestRunner.class)
public class RobolectricTest {

    @Test
    public void testSomething() throws Exception {
        ActivityController<EventListActivity> activityController = Robolectric.buildActivity(EventListActivity.class);
        EventListActivity activity = activityController.create().get();
        assertNotNull(activity);
        EventListFragment fragment = (EventListFragment)activity.getFragmentManager().findFragmentById(R.id.event_list_fragment);
        assertNotNull(fragment);

        assertNull(activity.findViewById(R.id.event_info_fragment));

    }
}
