package com.dancedeets.android;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.dancedeets.android.models.FullEvent;
import com.facebook.Session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class EventListActivity extends FacebookActivity implements EventListFragment.Callbacks {

    private static final String LOG_TAG = "EventListActivity";

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    public void onStart() {
        super.onStart();
        Map<String, String> dimensions = new HashMap<String, String>();
        dimensions.put("Fragment", "Event List");
        //TODO: PARSE
        // ParseAnalytics.trackEvent("Fragment", dimensions);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        VolleySingleton.createInstance(getApplicationContext());
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_event_list);


        if (findViewById(R.id.event_info_fragment) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            ((EventListFragment) getFragmentManager().findFragmentById(
                    R.id.event_list_fragment)).setActivateOnItemClick(true);
        }

        if (savedInstanceState == null) {
            handleIntent(getIntent());
            //TODO: PARSE
            // ParseAnalytics.trackAppOpened(getIntent());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.general_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_feedback:
                SendFeedback.sendFeedback(this, null);
                return true;
            case R.id.action_logout:
                Session.getActiveSession().closeAndClearTokenInformation();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // getIntent return the intent that started this activity,
        // so call setIntent in case we ever want to call getIntent.
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        Log.i(LOG_TAG, "handleIntent: " + intent);
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            EventListFragment fragment = (EventListFragment) getFragmentManager().findFragmentById(
                    R.id.event_list_fragment);
            fragment.startSearchFor("", intent.getStringExtra(SearchManager.QUERY));
        }
    }

    /**
     * Callback method from {@link EventListFragment.Callbacks} indicating that
     * the item with the given ID was selected.
     */
    @Override
    public void onEventSelected(ArrayList<FullEvent> allEvents, int positionSelected) {
        FullEvent event = allEvents.get(positionSelected);
        Log.i(LOG_TAG, "Sending Event: " + event);
        if (mTwoPane) {
            Bundle bundle = event.getBundle();
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            EventInfoFragment fragment = new EventInfoFragment();
            fragment.setArguments(bundle);
            getFragmentManager().beginTransaction()
                    .replace(R.id.event_info_fragment, fragment)
                    .commit();

            // Make the view visible, if it was still collapsed from initialization.
            View v = findViewById(R.id.event_info_fragment);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)v.getLayoutParams();
            if (params.weight != 1.2) {
                params.weight = 1.2f;
                v.setLayoutParams(params);
            }

        } else {
            Intent intent = EventInfoActivity.buildIntentFor(this, allEvents, positionSelected);

            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            startActivity(intent);
        }
    }

}
