package com.dancedeets.android;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.util.Log;

import com.dancedeets.android.models.Event;
import com.dancedeets.dancedeets.R;
import com.google.android.apps.common.testing.testrunner.ActivityLifecycleMonitorRegistry;
import com.google.android.apps.common.testing.ui.espresso.action.ViewActions;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import org.json.JSONArray;
import org.json.JSONObject;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.registerIdlingResources;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.text.StringContains.containsString;

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

    public Intent getStartIntent() throws Exception {
        String json = Resources.toString(getClass().getResource("local_volley/feed"), Charsets.UTF_8);
        JSONObject jsonObject = (JSONObject) new JSONArray(json).get(0);
        Event event = Event.parse(jsonObject);

        String[] eventIdList = new String[]{"event1", "event2", "event3"};
        Intent intent = EventInfoActivity.buildIntentFor(getInstrumentation().getTargetContext(), eventIdList, 0, event);
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
        onView(withinActivePager(withId(R.id.description))).check(matches(withText(containsString("Come learn something new"))));

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        onView(MyMatchers.withResourceName("android:id/action_bar_title")).check(matches(withText("Event 1")));
        onView(withinActivePager(withId(R.id.description))).check(matches(withText(containsString("Come learn something new"))));

        Log.i("TEST", "swipe left");

        onView(withinActivePager(withId(R.id.event_info_fragment))).perform(ViewActions.swipeLeft());

        onView(MyMatchers.withResourceName("android:id/action_bar_title")).check(matches(withText("Event 2")));

        Log.i("TEST", "swipe left");
        onView(withinActivePager(withId(R.id.event_info_fragment))).perform(ViewActions.swipeLeft());

        onView(MyMatchers.withResourceName("android:id/action_bar_title")).check(matches(withText("Event 3")));

        Log.i("TEST", "swipe right");

        onView(withinActivePager(withId(R.id.event_info_fragment))).perform(ViewActions.swipeRight());

        onView(MyMatchers.withResourceName("android:id/action_bar_title")).check(matches(withText("Event 2")));

        Log.i("TEST", "swipe right");
        onView(withinActivePager(withId(R.id.event_info_fragment))).perform(ViewActions.swipeRight());


        onView(MyMatchers.withResourceName("android:id/action_bar_title")).check(matches(withText("Event 1")));
    }
}
