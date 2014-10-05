package com.dancedeets.dancedeets;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;


public class EventListActivity extends Activity implements EventListFragment.Callbacks {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    static final String LOG_TAG = "EventListActivity";


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
            fragment.mSearchOptions.location = null;
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
    public void onEventSelected(Event event) {
        Bundle bundle = event.getBundle();
        Log.i(LOG_TAG, "Sending Event: " + event);
        if (mTwoPane) {
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
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, EventInfoActivity.class);
            detailIntent.putExtras(bundle);
            startActivity(detailIntent);
        }
    }

}
