package com.dancedeets.android.eventlist;

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
import com.dancedeets.android.AdManager;
import com.dancedeets.android.AnalyticsUtil;
import com.dancedeets.android.FacebookActivity;
import com.dancedeets.android.HelpSystem;
import com.dancedeets.android.R;
import com.dancedeets.android.SendFeedback;
import com.dancedeets.android.SettingsActivity;
import com.dancedeets.android.eventinfo.EventInfoActivity;
import com.dancedeets.android.eventinfo.EventInfoFragment;
import com.dancedeets.android.geo.FetchAddress;
import com.dancedeets.android.geo.FetchLocation;
import com.dancedeets.android.models.FullEvent;
import com.dancedeets.android.uistate.BundledState;
import com.dancedeets.android.uistate.RetainedState;
import com.dancedeets.android.uistate.StateHolder;
import com.dancedeets.android.uistate.StateUtil;
import com.dancedeets.android.util.VolleySingleton;
import com.facebook.login.LoginManager;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.doubleclick.PublisherAdView;

import java.util.ArrayList;


public class SearchListActivity extends FacebookActivity implements StateHolder<SearchListActivity.MyBundledState, RetainedState>, EventListFragment.Callbacks, SearchDialogFragment.OnSearchListener, FetchLocation.LocationListener, FetchAddress.AddressListener, SearchTabAdapter.SearchOptionsManager {

    private static final String LOG_TAG = "SearchListActivity";

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    private ViewPager mViewPager;

    private FetchLocation mFetchLocation;
    private FetchAddress mFetchAddress;

    // These are exposed as member variables for the sake of testing.
    SearchDialogFragment mSearchDialog;


    private MyBundledState mBundled;

    private PublisherAdView mPublisherAdView;

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

        setContentView(R.layout.search_activity);


        // Set up bottom banner ad
        mPublisherAdView = (PublisherAdView) findViewById(R.id.publisherAdView);
        mPublisherAdView.setVisibility(View.GONE);
        mPublisherAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                mPublisherAdView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                mPublisherAdView.setVisibility(View.GONE);
            }
        });

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
        mViewPager.setAdapter(new SearchTabAdapter(getFragmentManager(), this, getResources(), mTwoPane));
        // Since we do lazy-loading ourselves, we can keep all tabs in our ViewPager loaded
        mViewPager.setOffscreenPageLimit(mViewPager.getAdapter().getCount());
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            public void onPageSelected(int position) {
                SearchTabAdapter adapter = ((SearchTabAdapter)mViewPager.getAdapter());
                EventListFragment target = (EventListFragment)adapter.getExistingItem(mViewPager, position);
                Crashlytics.log(Log.INFO, LOG_TAG, "New page selection index " + position + ": " + target);
                target.loadSearchTab();
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
        mFetchAddress = new FetchAddress();
        mFetchAddress.onStart(this, this);
        mFetchLocation = new FetchLocation();
        mFetchLocation.onStart(this, this);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mFetchAddress != null) {
            mFetchAddress.onStop();
        }
        mFetchLocation.onStop();
    }

    public void startSearchFor(SearchOptions newSearchOptions) {
        Log.i(LOG_TAG, "startSearchFor " + newSearchOptions);
        // Can't perform fully-empty searches, so let's prompt the user
        if (newSearchOptions == null || (newSearchOptions.location.isEmpty() && newSearchOptions.keywords.isEmpty())) {
            showSearchDialog(getString(R.string.couldnt_detect_location));
            return;
        }
        mBundled.mSearchOptions = newSearchOptions;
        // We construct a new adapter and set it, which clears all the existing fragment state
        SearchTabAdapter adapter = new SearchTabAdapter(getFragmentManager(), this, getResources(), mTwoPane);
        mViewPager.setAdapter(adapter);
        EventListFragment f = (EventListFragment)adapter.getExistingItem(mViewPager, mViewPager.getCurrentItem());
        f.startSearch();
        AnalyticsUtil.track("Search Events",
                "Location", mBundled.mSearchOptions.location,
                "Keywords", mBundled.mSearchOptions.keywords);
    }

    @Override
    public void onLocationFound(Location location) {
        // As soon as we get the geo coordinates, load the ad request
        mPublisherAdView.loadAd(AdManager.getAdRequest(location));
    }

    @Override
    public void onAddressFound(Location location, Address address) {
        // If we've already got search results, then don't bother with what the geocode returns.
        if (!mBundled.mSearchOptions.isEmpty()) {
            return;
        }
        String optionalSubLocality = (address != null) ? " (with SubLocality " + address.getSubLocality() + ")" : "";
        Crashlytics.log(Log.INFO, LOG_TAG, "Address found: " + address + optionalSubLocality);
        if (address != null) {
            String addressString = FetchAddress.formatAddress(address);
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
        HelpSystem.setupShareAppItem(menu);
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
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
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
            Bundle b = intent.getExtras();
            // If they gave us a search query, search that...
            if (b != null) {
                String query = b.getString(SearchManager.QUERY, "");
                if (!query.equals("")) {
                    startSearchFor(new SearchOptions("", query));
                }
            }
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
