package com.dancedeets.android;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.dancedeets.android.models.Event;
import com.dancedeets.dancedeets.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class EventListActivity extends Activity implements EventListFragment.Callbacks {

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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // getIntent return the intent that started this activity,
        // so call setIntent in case we ever want to call getIntent.
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        Log.i(LOG_TAG, "handleIntent: " + intent);
        if (Intent.ACTION_MAIN.equals(intent.getAction())) {
            EventListFragment fragment = (EventListFragment) getFragmentManager().findFragmentById(
                    R.id.event_list_fragment);
            fragment.initializeGoogleApiClient();
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            EventListFragment fragment = (EventListFragment) getFragmentManager().findFragmentById(
                    R.id.event_list_fragment);
            //TODO: make a better API for this
            fragment.mSearchOptions.location = intent.getStringExtra(SearchManager.QUERY);
            fragment.fetchJsonData();

        }
    }

    /**
     * Callback method from {@link EventListFragment.Callbacks} indicating that
     * the item with the given ID was selected.
     */
    @Override
    public void onEventSelected(List<Event> allEvents, int positionSelected) {
        Event event = allEvents.get(positionSelected);
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
            Bundle bundle = new Bundle();
            String[] eventIdList = new String[allEvents.size()];
            int i = 0;
            for (Event otherEvent : allEvents) {
                eventIdList[i++] = otherEvent.getId();
            }
            bundle.putStringArray(EventInfoActivity.ARG_EVENT_ID_LIST, eventIdList);
            bundle.putInt(EventInfoActivity.ARG_EVENT_INDEX, positionSelected);
            bundle.putSerializable(EventInfoActivity.ARG_EVENT, event);

            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, EventInfoActivity.class);
            detailIntent.putExtras(bundle);
            startActivity(detailIntent);
        }
    }

}
