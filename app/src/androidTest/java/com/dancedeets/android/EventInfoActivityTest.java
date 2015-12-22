package com.dancedeets.android;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.core.deps.guava.base.Charsets;
import android.support.test.espresso.core.deps.guava.io.Resources;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.util.Log;

import com.dancedeets.android.eventinfo.EventInfoActivity;
import com.dancedeets.android.models.FullEvent;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.registerIdlingResources;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.doubleClick;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.test.TouchUtils.dragQuarterScreenDown;
import static android.test.TouchUtils.dragQuarterScreenUp;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Created by lambert on 2014/11/06.
 */
public class EventInfoActivityTest extends CommonActivityTest<EventInfoActivity> {

    private ViewPagerIdlingResource mViewPagerIdlingResource;

    public EventInfoActivityTest() throws NoSuchFieldException {
        super(EventInfoActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mViewPagerIdlingResource = new ViewPagerIdlingResource("Waiting for Pager Adapter", EventInfoActivity.class, R.id.event_pager);
        registerIdlingResources(mViewPagerIdlingResource);
        ActivityLifecycleMonitorRegistry.getInstance().addLifecycleCallback(mViewPagerIdlingResource);
    }

    @Override
    public void tearDown() throws Exception {
        ActivityLifecycleMonitorRegistry.getInstance().removeLifecycleCallback(mViewPagerIdlingResource);
        super.tearDown();
    }

    public Intent getStartIntent() throws Exception {
        String json = Resources.toString(getClass().getResource("local_volley/feed"), Charsets.UTF_8);
        JSONObject jsonObject = (JSONObject) new JSONArray(json).get(0);
        FullEvent event = FullEvent.parse(jsonObject);

        ArrayList<FullEvent> eventList = new ArrayList<>();
        eventList.add(event);
        eventList.add(event);
        eventList.add(event);
        Intent intent = EventInfoActivity.buildIntentFor(getInstrumentation().getTargetContext(), eventList, 0);
        return intent;
    }

    public void testRotation() throws Exception {
        waitForVolley(false);

        setActivityIntent(getStartIntent());
        getActivity();

        onView(withinActivePager(withId(R.id.progress_container))).check(matches(isDisplayed()));

        waitForVolley(true);

        onView(withinActivePager(withId(R.id.progress_container))).check(matches(not(isDisplayed())));

        onView(MyMatchers.withResourceName("android:id/action_bar_title")).check(matches(withText("Event 1")));
        onView(withinActivePager(withId(R.id.description))).check(matches(withText(is("Event 1 description."))));

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        onView(MyMatchers.withResourceName("android:id/action_bar_title")).check(matches(withText("Event 1")));
        onView(withinActivePager(withId(R.id.description))).check(matches(withText(is("Event 1 description."))));

        Log.i("TEST", "swipe left");

        onView(withinActivePager(withId(R.id.event_info_fragment))).perform(ViewActions.swipeLeft());

        onView(MyMatchers.withResourceName("android:id/action_bar_title")).check(matches(withText("Event 2")));

        Log.i("TEST", "swipe left");
        onView(withinActivePager(withId(R.id.event_info_fragment))).perform(ViewActions.swipeLeft());

        onView(MyMatchers.withResourceName("android:id/action_bar_title")).check(matches(withText("Event 3")));

        onView(withinActivePager(withId(R.id.event_info_fragment))).perform(click());

        getCurrentActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        onView(MyMatchers.withResourceName("android:id/action_bar_title")).check(matches(withText("Flyer for Event 3")));

        onView(withClassName(containsString("ImageViewTouch"))).perform(doubleClick());
        dragQuarterScreenDown(this, getCurrentActivity());

        getCurrentActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        onView(MyMatchers.withResourceName("android:id/action_bar_title")).check(matches(withText("Flyer for Event 3")));

        onView(withClassName(containsString("ImageViewTouch"))).perform(doubleClick());
        dragQuarterScreenUp(this, getCurrentActivity());

        /** If we remove this, we sometimes get errors like:
         * Injection of up event failed (corresponding down event: MotionEvent { .. })
         * Injection of up event as part of the click failed. Send cancel event.
         * Error performing 'inject cancel event (corresponding down event: MotionEvent {  .. }
         * No idea why (the title is already set, why wouldn't home be clickable?), though the sleep here seems to help
         */

        Log.i("TEST", "click on home");
        onView(withId(android.R.id.home)).perform(click());

        onView(MyMatchers.withResourceName("android:id/action_bar_title")).check(matches(withText("Event 3")));

        Log.i("TEST", "swipe right");

        onView(withinActivePager(withId(R.id.event_info_fragment))).perform(ViewActions.swipeRight());

        onView(MyMatchers.withResourceName("android:id/action_bar_title")).check(matches(withText("Event 2")));

        Log.i("TEST", "swipe right");
        onView(withinActivePager(withId(R.id.event_info_fragment))).perform(ViewActions.swipeRight());


        onView(MyMatchers.withResourceName("android:id/action_bar_title")).check(matches(withText("Event 1")));
    }
}
