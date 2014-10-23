package com.dancedeets.android;

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

import com.dancedeets.android.models.Event;
import com.dancedeets.android.models.FullEvent;
import com.dancedeets.dancedeets.R;

import java.util.List;


public class EventInfoActivity extends Activity implements EventInfoFragment.OnEventReceivedListener {

    private static final String LOG_TAG = "EventInfoActivity";

    public static final String ARG_EVENT = "EVENT";
    public static final String ARG_EVENT_ID_LIST = "EVENT_ID_LIST";
    public static final String ARG_EVENT_INDEX = "EVENT_INDEX";

    protected ViewPager mViewPager;
    protected EventInfoPagerAdapter mEventInfoPagerAdapter;
    protected String[] mEventIdList;
    protected int mEventIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        VolleySingleton.createInstance(getApplicationContext());
        super.onCreate(savedInstanceState);

        setContentView(R.layout.event_info_pager);

        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mViewPager = (ViewPager)findViewById(R.id.event_pager);

        // TODO: Do we want this out here? Seems okay because ViewPager retains state,
        // though really, we shouldn't reinitialize stuff if ViewPager will overwrite it.
        handleIntent(getIntent());

        /**
         * Ensures that the title is set for the relevant Fragment correctly.
         * In the case of switching to a loaded fragment, set the title directly, below.
         * In the case of switching to an unloaded fragment, we do nothing. The fragment
         * will call us when the event is loaded, so that we can set the event title.
         */
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                EventInfoFragment fragment = mEventInfoPagerAdapter.getExistingItem(position);
                // This may return null, if the event API fetch hasn't returned yet
                if (fragment.getEvent() != null) {
                    setTitle(fragment.getEvent().getTitle());
                }
            }
        });

        if (savedInstanceState == null) {
            // TODO: PARSE
            // Map<String,String> dimensions = new HashMap<String, String>();
            // dimensions.put("Fragment", "Event Info");
            // ParseAnalytics.trackEvent("Fragment", dimensions);
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
            setTitle(event.getTitle());
        }
        mEventInfoPagerAdapter = new EventInfoPagerAdapter(getFragmentManager(), this, mEventIdList);
        mViewPager.setAdapter(mEventInfoPagerAdapter);
        mViewPager.setCurrentItem(mEventIndex);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // This is used by the VIEW URL intent, on page-rotate, to re-set the Title.
        // TODO: For some reason this is necessary for VIEW but not regular navigation.
        EventInfoFragment fragment = mEventInfoPagerAdapter.getExistingItem(mViewPager.getCurrentItem());
        // Fragment may return null, if we are loading the activity (but not on rotating).
        // Event may return null, if the event API fetch hasn't returned yet
        if (fragment != null && fragment.getEvent() != null) {
            setTitle(fragment.getEvent().getTitle());
        }

    }

    @Override
    public void onEventReceived(FullEvent event) {
        if (mEventIdList[mViewPager.getCurrentItem()].equals(event.getId())) {
            setTitle(event.getTitle());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // getIntent returns the intent that started this activity, so let's update with setIntent.
        // However, sometimes we have app-internal "up" navigation with only a Component set...
        // and that leaves the EventInfoActivity with nothing to work with, and no saved state.
        // So instead, take the incoming intent (which may be minimal), and overlay it with the data
        // from the old intent, and construct a new intent for use in setIntent and handleIntent.
        Intent newIntent = (Intent) intent.clone();
        newIntent.fillIn(getIntent(), 0);
        setIntent(newIntent);

        handleIntent(newIntent);
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
