package com.dancedeets.android;

import android.location.Location;
import android.os.Bundle;

import com.dancedeets.android.robotests.MockGoogleApiClient;
import com.dancedeets.android.robotests.MockLocationProviderApi;
import com.dancedeets.android.robotests.RobolectricGradleTestRunner;
import com.dancedeets.dancedeets.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowGeocoder;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.util.ActivityController;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricGradleTestRunner.class)
public class RobolectricTest {

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
    }

    @Test
    public void testSomething() throws Exception {
        ActivityController<SearchListActivity> activityController = Robolectric.buildActivity(SearchListActivity.class);
        SearchListActivity activity = activityController.create().get();
        assertNotNull(activity);

        // Assert that we get the single-pane view
        EventListFragment listFragment = (EventListFragment)activity.getFragmentManager().findFragmentById(R.id.event_list_fragment);
        assertNotNull(listFragment);
        FetchLocation fetchLocation = listFragment.mRetained.mFetchLocation;
        listFragment.mGoogleApiClient = new MockGoogleApiClient(fetchLocation);
        Location location = new Location("flp");
        location.setLatitude(37.377166);
        location.setLongitude(-122.086966);
        location.setAccuracy(3.0f);
        fetchLocation.mLocationProviderApi = new MockLocationProviderApi(location);

        assertNull(activity.findViewById(R.id.event_info_fragment));
        assertEquals(0, listFragment.mBundled.mEventList.size());


        ShadowGeocoder geocoder = Robolectric.shadowOf(fetchLocation.mGeocoder);
        geocoder.setSimulatedResponse("", "New York", "NY", "", "US");

        Bundle restoreBundle = new Bundle();
        activityController.start().restoreInstanceState(restoreBundle).postCreate(restoreBundle).resume().visible();

        // Run the background task (which should be the data load)
        Robolectric.runUiThreadTasksIncludingDelayedTasks();
        assertEquals(0, listFragment.mBundled.mEventList.size());

    }
}
