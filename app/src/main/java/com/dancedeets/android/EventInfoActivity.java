package com.dancedeets.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.IntentCompat;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.dancedeets.android.models.FullEvent;
import com.dancedeets.android.models.ParcelableUtil;
import com.dancedeets.android.uistate.BundledState;
import com.dancedeets.android.uistate.RetainedState;
import com.dancedeets.android.uistate.StateHolder;
import com.dancedeets.android.uistate.StateUtil;
import com.facebook.login.LoginManager;

import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;


public class EventInfoActivity extends FacebookActivity implements StateHolder<BundledState, RetainedState> {

    private static final String LOG_TAG = "EventInfoActivity";

    public static final String ARG_EVENT_LIST_FILENAME = "EVENT_ID_LIST_FILENAME";
    public static final String ARG_EVENT_INDEX = "EVENT_INDEX";

    protected ViewPager mViewPager;
    protected EventInfoPagerAdapter mEventInfoPagerAdapter;
    protected RetainedState mRetained;

    static class MyBundledState extends BundledState {
        public List<FullEvent> mEventList = new ArrayList<>();
        public int mEventIndex;
    }

    protected MyBundledState mBundled;

    public String getUniqueTag() {
        return LOG_TAG;
    }

    @Override
    public MyBundledState buildBundledState() {
        return new MyBundledState();
    }

    @Override
    public RetainedState buildRetainedState() {
        return new RetainedState();
    }

    private static File getCacheFile(Context context) {
        return new File(context.getExternalCacheDir(), "event_list.json");
    }

    private static void writeFile(File eventListFile, ArrayList<FullEvent> eventList) throws IOException {
        OutputStream fos = new FileOutputStream(eventListFile);
        long time = System.currentTimeMillis();
        fos.write(ParcelableUtil.marshallList(eventList));
        Crashlytics.log(Log.INFO, LOG_TAG, "Writing eventList to disk took " + (System.currentTimeMillis() - time) + "ms");
        fos.close();
    }

    private static ArrayList<FullEvent> readFile(File eventListFile) throws IOException, ClassNotFoundException {
        byte[] bytes = ParcelableUtil.readBytes(eventListFile);
        ArrayList<FullEvent> eventList = new ArrayList<>();
        long time = System.currentTimeMillis();
        ParcelableUtil.unmarshallList(bytes, eventList, FullEvent.CREATOR);
        Crashlytics.log(Log.INFO, LOG_TAG, "Loading eventList from disk took " + (System.currentTimeMillis() - time) + "ms");
        return eventList;
    }

    public static Intent buildIntentFor(Context context, ArrayList<FullEvent> eventList, int positionSelected) {
        Bundle bundle = new Bundle();
        File eventListFile = getCacheFile(context);
        try {
            writeFile(eventListFile, eventList);
        } catch (IOException e) {
            Crashlytics.log(Log.ERROR, LOG_TAG, "Error writing event list cache file: " + e);
            return null;
        }
        String eventListFilename = eventListFile.getAbsolutePath();
        bundle.putString(ARG_EVENT_LIST_FILENAME, eventListFilename);
        bundle.putInt(ARG_EVENT_INDEX, positionSelected);
        Intent intent = new Intent(context, EventInfoActivity.class);
        intent.putExtras(bundle);
        return intent;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        VolleySingleton.createInstance(getApplicationContext());
        super.onCreate(savedInstanceState);
        mRetained = StateUtil.createRetained(this, this);

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
            Crashlytics.log(Log.INFO, LOG_TAG, "onCreate.savedInstanceState: List size is " + mBundled.mEventList.size());
            Crashlytics.log(Log.INFO, LOG_TAG, "onCreate.savedInstanceState: Event index is " + mBundled.mEventIndex);
            // Only initialize the viewpager if we've gotten a full bundled state,
            // otherwise we're probably waiting for an event callback, so do nothing here.
            if (mBundled.mEventList.size() != 0) {
                initializeViewPagerWithBundledState();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.general_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * This initializes the ViewPager with a new EventInfoPagerAdapter,
     * based on the BundledState() data. Can be called after loading intents and bundled state,
     * and can be re-called after calling onNewIntent later in the activity lifecycle flow.
     */
    public void initializeViewPagerWithBundledState() {
        Crashlytics.log(Log.INFO, LOG_TAG, "List size is " + mBundled.mEventList.size());
        Crashlytics.log(Log.INFO, LOG_TAG, "Event index is " + mBundled.mEventIndex);

        mEventInfoPagerAdapter = new EventInfoPagerAdapter(getFragmentManager(), mBundled.mEventList);
        Crashlytics.log(Log.INFO, LOG_TAG, "setAdapter(new EventInfoPagerAdapter(...))");
        mViewPager.setAdapter(mEventInfoPagerAdapter);
        // The ViewPager retains its own CurrentItem state, so this is not strictly necessary here.
        // However, we must call setCurrentItem on all the intent-derived initializations,
        // and the flow just makes it easier to store/restore our own EventIndex for everyone here.
        mViewPager.setCurrentItem(mBundled.mEventIndex);

        Crashlytics.setInt("mEventIndex", mBundled.mEventIndex);
        Crashlytics.setInt("mBundled.mEventList.size()", mBundled.mEventList.size());

        // Since onPageSelected is not called until the first swipe, always initialize the title here.
        FullEvent event = mBundled.mEventList.get(mBundled.mEventIndex);
        setTitle(event.getTitle());

        // Ensures that the title is set for the relevant Fragment correctly when we change pages.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mBundled.mEventIndex = position;
                setTitle(mBundled.mEventList.get(position).getTitle());
                Crashlytics.log(Log.INFO, LOG_TAG, "onPageSelected: List size is " + mBundled.mEventList.size());
                Crashlytics.log(Log.INFO, LOG_TAG, "onPageSelected: Event index is " + mBundled.mEventIndex);
            }
        });
    }

    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(getUniqueTag(), mBundled);
    }

    // This is done in a static class, so there are no references to this Fragment leaked
    static class EventHandler implements DanceDeetsApi.OnEventReceivedListener {
        private final RetainedState mRetainedState;

        public EventHandler(RetainedState retainedState) {
            mRetainedState = retainedState;
        }
        @Override
        public void onEventReceived(FullEvent event) {
            EventInfoActivity eventInfoActivity = (EventInfoActivity) mRetainedState.getActivity();
            eventInfoActivity.onEventReceived(event);
        }

        @Override
        public void onError(Exception e) {
            if (e instanceof JSONException) {
                Crashlytics.log(Log.ERROR, LOG_TAG, "Error reading from event api: " + e);
            } else {
                Crashlytics.log(Log.ERROR, LOG_TAG, "Error retrieving data: " + e);
            }
            Toast.makeText(mRetainedState.getActivity().getBaseContext(), "Failed to load event info! " + e, Toast.LENGTH_LONG).show();
            //TODO(lambert): implement a better error handling display to the user
        }
    }

    public boolean handleIntent(Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_VIEW)) {
            Uri url = intent.getData();
            Crashlytics.log(Log.INFO, LOG_TAG, "Viewing URL: " + url);
            List<String> pathSegments = url.getPathSegments();
            if (pathSegments.size() == 2 && pathSegments.get(0).equals("events")) {
                String eventId = pathSegments.get(1);
                // Add Event requests
                DanceDeetsApi.getEvent(eventId, new EventHandler(mRetained));
            }
            Crashlytics.log(Log.INFO, LOG_TAG, "handleIntent: Loading " + url);
            Crashlytics.setString("Intent View URL", url.toString());
            return true;
        } else if (intent.getExtras() != null) {
            Bundle b = intent.getExtras();
            try {
                mBundled.mEventList = readFile(new File(b.getString(ARG_EVENT_LIST_FILENAME)));
            } catch (IOException | ClassNotFoundException e) {
                mBundled.mEventList = null;
                Crashlytics.log(Log.ERROR, LOG_TAG, "Error reading event list cache file: " + e);
            }
            mBundled.mEventIndex = b.getInt(ARG_EVENT_INDEX);
            Crashlytics.log(Log.INFO, LOG_TAG, "Intent.getExtras: List size is " + mBundled.mEventList.size());
            Crashlytics.log(Log.INFO, LOG_TAG, "Intent.getExtras: Event index is " + mBundled.mEventIndex);

            initializeViewPagerWithBundledState();
            return true;
        }
        return false;
    }

    protected void onEventReceived(FullEvent event) {
        mBundled.mEventList = new ArrayList<>(1);
        mBundled.mEventList.add(event);
        mBundled.mEventIndex = 0;
        Crashlytics.log(Log.INFO, LOG_TAG, "onEventReceived: List size is " + mBundled.mEventList.size());
        Crashlytics.log(Log.INFO, LOG_TAG, "onEventReceived: Event index is " + mBundled.mEventIndex);
        //eventInfoActivity.mEventInfoPagerAdapter.notifyDataSetChanged();
        initializeViewPagerWithBundledState();
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Even if our old intent had better information,
        // it should all be captured in BundledState by now.
        setIntent(intent);
        Crashlytics.log(Log.INFO, LOG_TAG, "onNewIntent");
        handleIntent(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_feedback:
                SendFeedback.sendFeedback(this);
                return true;
            case R.id.action_help:
                HelpSystem.openHelp(this);
                return true;
            case R.id.action_logout:
                LoginManager.getInstance().logOut();
                return true;
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
