package com.dancedeets.dancedeets;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.IntentCompat;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.MenuItem;

import com.dancedeets.dancedeets.models.Event;
import com.dancedeets.dancedeets.models.FullEvent;
import com.parse.ParseAnalytics;

import java.util.HashMap;
import java.util.Map;


public class EventInfoActivity extends Activity {

    private static final String LOG_TAG = "EventInfoActivity";

    public static final String ARG_EVENT = "EVENT";
    public static final String ARG_EVENT_ID_LIST = "EVENT_ID_LIST";
    public static final String ARG_EVENT_INDEX = "EVENT_INDEX";

    protected ViewPager mViewPager;
    protected EventInfoPagerAdapter mEventInfoPagerAdapter;
    protected String[] mEventIdList;
    protected int mEventIndex;
    protected EventInfoFragment mOldFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        VolleySingleton.createInstance(getApplicationContext());
        super.onCreate(savedInstanceState);

        setContentView(R.layout.event_info_pager);

        Bundle b = getIntent().getExtras();
        mEventIdList = b.getStringArray(ARG_EVENT_ID_LIST);
        mEventIndex = b.getInt(ARG_EVENT_INDEX);
        mEventInfoPagerAdapter = new EventInfoPagerAdapter(getFragmentManager(), mEventIdList);
        Event event = (Event)b.getSerializable(ARG_EVENT);

        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mViewPager = (ViewPager)findViewById(R.id.event_pager);
        mViewPager.setAdapter(mEventInfoPagerAdapter);

        mViewPager.setCurrentItem(mEventIndex);

        setTitleOnPageChange(event);

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
/*TODO: FIXME
            Fragment f = new EventInfoFragment();
            if (getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_VIEW)) {
                Log.i(LOG_TAG, "Viewing URL: " + getIntent().getData());
                Uri url = getIntent().getData();
                List<String> pathSegments = url.getPathSegments();
                if (pathSegments.size() == 2 && pathSegments.get(0).equals("events")) {
                    String eventId = pathSegments.get(1);
                    IdEvent idEvent = new IdEvent(eventId);
                    f.setArguments(idEvent.getBundle());
                }
            } else {
                f.setArguments(event.getBundle());
            }
            getFragmentManager().beginTransaction().add(android.R.id.content, f).commit();
*/
            Map<String,String> dimensions = new HashMap<String, String>();
            dimensions.put("Fragment", "Event Info");
            ParseAnalytics.trackEvent("Fragment", dimensions);
        }
    }

    /**
     * Ensures that the title is set for the relevant Fragment correctly.
     * It should properly handle the race conditions of switching to an uninitialized Fragment,
     * as well as preventing the case of an old uninitialized Fragment initializing,
     * and overwriting the current title with an incorrect one.
     */
    public void setTitleOnPageChange(Event currentEvent) {
        // Since onPageSelected is not called until the first swipe, initialize the title here.
        if (currentEvent != null) {
            setTitle(currentEvent.getTitle());
        }
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                Log.i(LOG_TAG, "onPageSelected: " + position);
                // If we have an mOldFragment, we have a pending listener on it. Since it is no
                // longer the current fragment, we should destroy any listeners we attached to it.
                // Otherwise the callback an old fragment could overwrite the current correct title.
                if (mOldFragment != null) {
                    mOldFragment.setOnEventReceivedListener(null);
                    mOldFragment = null;
                }
                //TODO: crash bug: onPageSelected gets called before anything has been populated.
                EventInfoFragment fragment = mEventInfoPagerAdapter.getExistingItem(position);
                // This may return null, if the event API fetch hasn't returned yet
                if (fragment.getEvent() != null) {
                    setTitle(fragment.getEvent().getTitle());
                } else {
                    // So set our title later, when it does load
                    fragment.setOnEventReceivedListener(new EventInfoFragment.OnEventReceivedListener() {
                        @Override
                        public void onEventReceived(FullEvent event) {
                            setTitle(event.getTitle());
                        }
                    });
                    mOldFragment = fragment;
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                // Basic intent for Parent Activity
                String parentName = NavUtils.getParentActivityName(this);
                final ComponentName target = new ComponentName(this, parentName);
                Intent upIntent = new Intent().setComponent(target);

                // If this activity was started by an Intent, recreate parent Intents
                if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                    // But instead of normal basic intent, let's make a Main Intent
                    upIntent = IntentCompat.makeMainActivity(target);
                    // This activity is NOT part of this app's task, so create a new task
                    // when navigating up, with a synthesized back stack.
                    TaskStackBuilder.create(this)
                            // Add all of this activity's parents to the back stack
                            .addNextIntentWithParentStack(upIntent)
                                    // Navigate up to the closest parent
                            .startActivities();
                } else {
                    // This activity is part of this app's task, so simply
                    // navigate up to the logical parent activity.
                    NavUtils.navigateUpTo(this, upIntent);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
