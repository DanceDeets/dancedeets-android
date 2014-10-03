package com.dancedeets.dancedeets;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;


public class EventListActivity extends Activity implements EventListFragment.Callbacks {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    static final String LOG_TAG = "EventListActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        VolleySingleton.getInstance(getApplicationContext());
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_event_list);

        Log.i(LOG_TAG, "onCreate");

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
            // TODO: If exposing deep links into your app, handle intents here.
            handleIntent(getIntent());
        }
    }

/* DEBUGGING: The "Up" button doesn't seem to do anything in some cases, trying to investigate why...
    public boolean onNavigateUp() {
        Log.i(LOG_TAG, "onNavigateUp");
        return super.onNavigateUp();
    }

    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        CharSequence titleCondensed = item.getTitleCondensed();
        Log.i(LOG_TAG, "onMenuItemSelected, featureId is " + featureId + ", item is " + item);

        switch (featureId) {
            case Window.FEATURE_OPTIONS_PANEL:
                Log.i(LOG_TAG, "FEATURE_OPTIONS_PANEL, mParent is " + getParent());
                break;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Nullable
    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        Log.i(LOG_TAG, "onCreateView name " + name + ", isChild? " + isChild());

        return super.onCreateView(name, context, attrs);
    }
*/
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
            fragment.mLocation = null;
            fragment.initializeFromLocation();
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            EventListFragment fragment = (EventListFragment) getFragmentManager().findFragmentById(
                    R.id.event_list_fragment);
            //TODO: make a better API for this
            fragment.mLocation = intent.getStringExtra(SearchManager.QUERY);
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
        Log.i(LOG_TAG, "Sending Bundle: " + bundle);
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Log.i(LOG_TAG, "Replacing fragment for info page.");
            EventInfoFragment fragment = new EventInfoFragment();
            fragment.setArguments(bundle);
            getFragmentManager().beginTransaction()
                    .replace(R.id.event_info_fragment, fragment)
                    // Add this transaction to the back stack
                    .addToBackStack(null)
                    .commit();

        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Log.i(LOG_TAG, "Creating intent for info page.");
            Intent detailIntent = new Intent(this, EventInfoActivity.class);
            detailIntent.putExtras(bundle);
            startActivity(detailIntent);
        }
    }

}
