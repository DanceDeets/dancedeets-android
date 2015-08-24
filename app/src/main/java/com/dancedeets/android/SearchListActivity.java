package com.dancedeets.android;

import android.app.SearchManager;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.dancedeets.android.models.FullEvent;
import com.dancedeets.android.uistate.BundledState;
import com.dancedeets.android.uistate.RetainedState;
import com.dancedeets.android.uistate.StateHolder;
import com.dancedeets.android.uistate.StateUtil;
import com.facebook.login.LoginManager;

import java.util.ArrayList;


public class SearchListActivity extends FacebookActivity implements StateHolder<SearchListActivity.MyBundledState, RetainedState>, EventListFragment.Callbacks, SearchDialogFragment.OnSearchListener, FetchLocation.AddressListener, SearchPagerAdapter.SearchOptionsManager {

    private static final String LOG_TAG = "SearchListActivity";

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    private ViewPager mViewPager;

    private FetchLocation mFetchLocation;

    // These are exposed as member variables for the sake of testing.
    SearchDialogFragment mSearchDialog;


    private MyBundledState mBundled;

    @Override
    public MyBundledState buildBundledState() {
        return new MyBundledState();
    }

    @Override
    public RetainedState buildRetainedState() {
        return null;
    }

    @Override
    public String getUniqueTag() {
        return LOG_TAG;
    }

    static protected class MyBundledState extends BundledState {
        SearchOptions mSearchOptions = new SearchOptions();
    }

    @Override
    public SearchOptions getSearchOptions() {
        return mBundled.mSearchOptions;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        VolleySingleton.createInstance(getApplicationContext());
        super.onCreate(savedInstanceState);
        mBundled = StateUtil.createBundled(this, savedInstanceState);

        // Set (DEBUG) title
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            //The .debug specified in gradle
            if (pInfo.packageName.equals("com.dancedeets.android.debug")) {
                setTitle(getTitle() + " (DEBUG)");
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        setContentView(R.layout.activity_event_list);


        // Locate the viewpager in activity_main.xml
        mViewPager = (ViewPager) findViewById(R.id.pager);

        if (findViewById(R.id.event_info_fragment) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

        // Set the ViewPagerAdapter into ViewPager
        mViewPager.setAdapter(new SearchPagerAdapter(getFragmentManager(), this, getResources(), mTwoPane));
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            public void onPageSelected(int position) {
                SearchPagerAdapter adapter = ((SearchPagerAdapter)mViewPager.getAdapter());
                Crashlytics.log(Log.INFO, LOG_TAG, "New page selection index " + position + ": " + adapter.getSearchTarget(position));
                adapter.getSearchTarget(position).loadSearchTab();
            }
        });

        if (savedInstanceState == null) {
            handleIntent(getIntent());
        }
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        StateUtil.saveBundled(this, mBundled, outState);
    }

    public void onStart() {
        super.onStart();
        if (mBundled.mSearchOptions.isEmpty()) {
            mFetchLocation = new FetchLocation();
            mFetchLocation.onStart(this, this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mFetchLocation != null) {
            mFetchLocation.onStop();
        }
    }

    public void startSearchFor(SearchOptions newSearchOptions) {
        Log.i(LOG_TAG, "startSearchFor " + newSearchOptions);
        // Can't perform fully-empty searches, so let's prompt the user
        if (newSearchOptions == null || (newSearchOptions.location.isEmpty() && newSearchOptions.keywords.isEmpty())) {
            showSearchDialog(getString(R.string.couldnt_detect_location));
            return;
        }
        mBundled.mSearchOptions = newSearchOptions;
        SearchPagerAdapter adapter = ((SearchPagerAdapter)mViewPager.getAdapter());
        for (int i = 0; i < adapter.getCount(); i++) {
            // We only want to grab targets that are non-empty, not create missing ones
            SearchTarget searchTarget = adapter.getSearchTarget(i);
            if (searchTarget == null) {
                continue;
            }
            Log.i(LOG_TAG, "SearchTarget is " + searchTarget);
            searchTarget.prepareForSearchOptions(mBundled.mSearchOptions);
            if (i == mViewPager.getCurrentItem()) {
                Log.i(LOG_TAG, "Initiating startSearch() on index " + i + ": " + searchTarget);
                searchTarget.loadSearchTab();
            }
        }
        AnalyticsUtil.track("Search Events",
                "Location", mBundled.mSearchOptions.location,
                "Keywords", mBundled.mSearchOptions.keywords);
    }


        @Override
    public void onAddressFound(Location location, Address address) {
        String optionalSubLocality = (address != null) ? " (with SubLocality " + address.getSubLocality() + ")" : "";
        Crashlytics.log(Log.INFO, LOG_TAG, "Address found: " + address + optionalSubLocality);
        if (address != null) {
            String addressString = FetchLocation.formatAddress(address);
            startSearchFor(new SearchOptions(addressString));
        } else {
            if (location == null) {
                // Both are null. No GPS, but perhaps there still is network connectivity...
                // Perhaps we can return a list of selected cities/locations?
                startSearchFor(null);
            } else {
                // We have GPS, but our reverse geocode from the GMaps API failed.
                // Could be without network connectivity, or just a transient failure.
                // Is their cached data we can use? Or just use the lat/long directly?
                Toast.makeText(this, "Google Geocoder Request failed.", Toast.LENGTH_LONG).show();
                Crashlytics.log(Log.ERROR, LOG_TAG, "No address returned from FetchCityTask, fetching with empty location.");
                startSearchFor(null);
            }
        }
    }



    @Override
    public void onSearch(String location, String keywords) {
        // Search dialog submitted!
        Crashlytics.log(Log.INFO, LOG_TAG, "Search: " + location + ", " + keywords);
        startSearchFor(new SearchOptions(location, keywords));
    }

    public void showSearchDialog(String message) {
        Crashlytics.log(Log.INFO, LOG_TAG, "Opening search dialog: " + message);
        Crashlytics.log(Log.INFO, LOG_TAG, "mSearchDialog is " + mSearchDialog);
        mSearchDialog = new SearchDialogFragment();
        Bundle b = new Bundle();
        b.putSerializable(SearchDialogFragment.ARG_SEARCH_OPTIONS, mBundled.mSearchOptions);
        b.putString(SearchDialogFragment.ARG_MESSAGE, message);
        mSearchDialog.setArguments(b);
        mSearchDialog.show(getFragmentManager(), "search");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.general_menu, menu);
        inflater.inflate(R.menu.events_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_feedback:
                SendFeedback.sendFeedback(this);
                return true;
            case R.id.action_search:
                showSearchDialog("");
                //SEARCH
                return true;
            case R.id.action_refresh:
                //SEARCH
                startSearchFor(mBundled.mSearchOptions);
                return true;
            case R.id.action_add_event:
                HelpSystem.openAddEvent(this);
                return true;
            case R.id.action_help:
                HelpSystem.openHelp(this);
                return true;
            case R.id.action_logout:
                LoginManager.getInstance().logOut();
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
        Crashlytics.log(Log.INFO, LOG_TAG, "handleIntent: " + intent);
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            startSearchFor(new SearchOptions("", intent.getStringExtra(SearchManager.QUERY)));
        }
    }

    /**
     * Callback method from {@link EventListFragment.Callbacks} indicating that
     * the item with the given ID was selected.
     */
    @Override
    public void onEventSelected(ArrayList<FullEvent> allEvents, int positionSelected) {
        FullEvent event = allEvents.get(positionSelected);
        Crashlytics.log(Log.INFO, LOG_TAG, "Sending Event: " + event);
        Crashlytics.log("onEventSelected: Index " + positionSelected + ": Event " + event.getId());
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
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) v.getLayoutParams();
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
