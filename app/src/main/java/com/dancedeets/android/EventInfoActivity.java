package com.dancedeets.android;

import android.content.ComponentName;
import android.content.Context;
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
import com.dancedeets.android.uistate.BundledState;
import com.dancedeets.dancedeets.R;

import java.util.List;


public class EventInfoActivity extends FacebookActivity implements EventInfoFragment.OnEventReceivedListener {

    private static final String LOG_TAG = "EventInfoActivity";

    public static final String ARG_EVENT = "EVENT";
    public static final String ARG_EVENT_ID_LIST = "EVENT_ID_LIST";
    public static final String ARG_EVENT_INDEX = "EVENT_INDEX";

    protected ViewPager mViewPager;
    protected EventInfoPagerAdapter mEventInfoPagerAdapter;

    public static Intent buildIntentFor(Context context, String[] eventIdList, int positionSelected, Event event) {
        Bundle bundle = new Bundle();
        bundle.putStringArray(ARG_EVENT_ID_LIST, eventIdList);
        bundle.putInt(ARG_EVENT_INDEX, positionSelected);
        bundle.putSerializable(ARG_EVENT, event);
        Intent intent = new Intent(context, EventInfoActivity.class);
        intent.putExtras(bundle);
        return intent;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        VolleySingleton.createInstance(getApplicationContext());
        super.onCreate(savedInstanceState);

        setContentView(R.layout.event_info_pager);

        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mViewPager = (ViewPager)findViewById(R.id.event_pager);

        if (savedInstanceState == null) {
            mBundled = buildBundledState();
            handleIntent(getIntent());
        } else {
            // Saved state should replace whatever intent we originally opened with,
            // and only be overridden by any onNewIntent down the line.
            String tag = getUniqueTag();
            mBundled = (MyBundledState) savedInstanceState.getSerializable(tag);
        }

        initializeViewPagerWithBundledState();

        /**
         * Ensures that the title is set for the relevant Fragment correctly when we change pages.
         * In the case of switching to a loaded fragment, set the title directly, below.
         * In the case of switching to an unloaded fragment, we do nothing. The fragment
         * will call us when the event is loaded, so that we can set the event title.
         */
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                getBundledState().mEventIndex = position;
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

    /**
     * This initializes the ViewPager with a new EventInfoPagerAdapter,
     * based on the BundledState() data. Can be called after loading intents and bundled state,
     * and can be re-called after calling onNewIntent later in the activity lifecycle flow.
     */
    public void initializeViewPagerWithBundledState() {
        mEventInfoPagerAdapter = new EventInfoPagerAdapter(getFragmentManager(), this, getBundledState().mEventIdList);
        Log.i(LOG_TAG, "setAdapter(new EventInfoPagerAdapter(...))");
        mViewPager.setAdapter(mEventInfoPagerAdapter);
        // The ViewPager retains its own CurrentItem state, so this is not strictly necessary here.
        // However, we must call setCurrentItem on all the intent-derived initializations,
        // and the flow just makes it easier to store/restore our own EventIndex for everyone here.
        mViewPager.setCurrentItem(getBundledState().mEventIndex);
    }

    static class MyBundledState extends BundledState {
        public String[] mEventIdList = {};
        public int mEventIndex;
    }

    MyBundledState mBundled;

    public String getUniqueTag() {
        return LOG_TAG;
    }

    private MyBundledState buildBundledState() {
        return new MyBundledState();
    }
    protected MyBundledState getBundledState() {
        return mBundled;
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(getUniqueTag(), mBundled);
    }

    public boolean handleIntent(Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_VIEW)) {
            Uri url = intent.getData();
            Log.i(LOG_TAG, "Viewing URL: " + url);
            List<String> pathSegments = url.getPathSegments();
            if (pathSegments.size() == 2 && pathSegments.get(0).equals("events")) {
                String eventId = pathSegments.get(1);
                getBundledState().mEventIdList = new String[]{eventId};
                getBundledState().mEventIndex = 0;
            }
            return true;
        } else if (intent.getExtras() != null) {
            Bundle b = intent.getExtras();
            getBundledState().mEventIdList = b.getStringArray(ARG_EVENT_ID_LIST);
            getBundledState().mEventIndex = b.getInt(ARG_EVENT_INDEX);

            Event event = (Event)b.getSerializable(ARG_EVENT);
            // Since onPageSelected is not called until the first swipe, initialize the title here.
            setTitle(event.getTitle());
            return true;
        }
        return false;
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
        if (getBundledState().mEventIdList[mViewPager.getCurrentItem()].equals(event.getId())) {
            setTitle(event.getTitle());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Even if our old intent had better information,
        // it should all be captured in BundledState by now.
        setIntent(intent);
        Log.i(LOG_TAG, "onNewIntent");
        if (handleIntent(intent)) {
            initializeViewPagerWithBundledState();
        }
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
