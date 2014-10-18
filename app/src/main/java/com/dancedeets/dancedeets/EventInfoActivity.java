package com.dancedeets.dancedeets;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
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
import java.util.List;
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

        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mViewPager = (ViewPager)findViewById(R.id.event_pager);
        mViewPager.setCurrentItem(mEventIndex);

        setTitleOnPageChange();


        if (savedInstanceState == null) {
            handleIntent(getIntent());

            Map<String,String> dimensions = new HashMap<String, String>();
            dimensions.put("Fragment", "Event Info");
            ParseAnalytics.trackEvent("Fragment", dimensions);
        }
    }

    public void handleIntent(Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_VIEW)) {
            Uri url = intent.getData();
            Log.i(LOG_TAG, "Viewing URL: " + url);
            List<String> pathSegments = url.getPathSegments();
            if (pathSegments.size() == 2 && pathSegments.get(0).equals("events")) {
                String eventId = pathSegments.get(1);
                mEventIdList = new String[]{eventId};
                mEventIndex = 0;
            }
        } else {
            Bundle b = intent.getExtras();
            mEventIdList = b.getStringArray(ARG_EVENT_ID_LIST);
            mEventIndex = b.getInt(ARG_EVENT_INDEX);

            Event event = (Event)b.getSerializable(ARG_EVENT);
            // Since onPageSelected is not called until the first swipe, initialize the title here.
            if (event != null) {
                setTitle(event.getTitle());
            }
        }
        mEventInfoPagerAdapter = new EventInfoPagerAdapter(getFragmentManager(), mEventIdList);

        mViewPager.setAdapter(mEventInfoPagerAdapter);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // getIntent return the intent that started this activity,
        // so call setIntent in case we ever want to call getIntent.
        setIntent(intent);
        handleIntent(intent);
    }

    /**
     * Ensures that the title is set for the relevant Fragment correctly.
     * It should properly handle the race conditions of switching to an uninitialized Fragment,
     * as well as preventing the case of an old uninitialized Fragment initializing,
     * and overwriting the current title with an incorrect one.
     */
    public void setTitleOnPageChange() {
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
