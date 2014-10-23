package com.dancedeets.android.robotests;

import android.app.Activity;

import com.dancedeets.android.EventListActivity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
public class RobolectricTest {

    @Test
    public void testSomething() throws Exception {
        Activity activity = Robolectric.buildActivity(EventListActivity.class).create().get();
        assertTrue(activity != null);
    }
}
