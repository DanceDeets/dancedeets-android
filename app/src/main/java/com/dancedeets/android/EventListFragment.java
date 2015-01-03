package com.dancedeets.android;

import android.app.Activity;
import android.content.Intent;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dancedeets.android.models.FullEvent;
import com.dancedeets.android.uistate.BundledState;
import com.dancedeets.android.uistate.RetainedState;
import com.dancedeets.android.uistate.StateListFragment;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

public class EventListFragment extends StateListFragment<EventListFragment.MyBundledState, EventListFragment.MyRetainedState> implements FetchLocation.AddressListener {

    static protected class MyBundledState extends BundledState {

        /**
         * The current activated item position. Only used on tablets.
         */
        int mActivatedPosition = ListView.INVALID_POSITION;

        ArrayList<FullEvent> mEventList = new ArrayList<>();

        SearchOptions mSearchOptions = new SearchOptions();
    }

    static public class MyRetainedState extends RetainedState {
        private FetchLocationWithDialog mFetchLocationWithDialog;

    }

    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks = null;

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        public void onEventSelected(ArrayList<FullEvent> allEvents, int positionSelected);
    }

    static final String LOG_TAG = "EventListFragment";


    EventUIAdapter eventAdapter;

    View mEmptyListView;
    TextView mEmptyText;
    Button mRetryButton;
    TextView mListDescription;

    GoogleApiClient mGoogleApiClient;

    // These are exposed as member variables for the sake of testing.
    SearchDialogFragment mSearchDialog;
    FusedLocationProviderApi mLocationProviderApi = LocationServices.FusedLocationApi;

    public EventListFragment() {
    }


    @Override
    public MyBundledState buildBundledState() {
        return new MyBundledState();
    }

    @Override
    public MyRetainedState buildRetainedState() {
        return new MyRetainedState();
    }

    @Override
    public String getUniqueTag() {
        return LOG_TAG;
    }

    @Override
    public void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        if (mRetained.mFetchLocationWithDialog != null) {
            mRetained.mFetchLocationWithDialog.onActivityResult(getActivity(), requestCode, resultCode, data);
        }
    }

    protected void handleEventList(List<FullEvent> eventList) {
        mBundled.mEventList.clear();
        mBundled.mEventList.addAll(eventList);
        onEventListFilled();
    }

    protected void onEventListFilled() {
        if (mBundled.mEventList.isEmpty()) {
            mEmptyText.setVisibility(View.VISIBLE);
        } else {
            mEmptyText.setVisibility(View.GONE);
        }
        mRetryButton.setVisibility(View.GONE);
        setListAdapter(eventAdapter);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(LOG_TAG, "onCreate");
        // Restore the previously serialized activated item position.
        setHasOptionsMenu(true);
    }


    public void onStart() {
        super.onStart();
        if (mBundled.mSearchOptions.location.isEmpty()) {
            mRetained.mFetchLocationWithDialog = new FetchLocationWithDialog();
            mRetained.mFetchLocationWithDialog.onStart(getActivity(), this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mRetained.mFetchLocationWithDialog != null) {
            mRetained.mFetchLocationWithDialog.onStop();
        }
    }

    @Override
    public void onAddressFound(Location location, Address address) {
        Log.i(LOG_TAG, "Address found: " + address);
        if (address != null) {
            String addressString;
            if (address.getLocality() != null) {
                addressString = address.getLocality() + ", " + address.getAdminArea() + ", " + address.getCountryCode();
            } else {
                addressString = address.getAdminArea() + ", " + address.getCountryCode();
            }
            startSearchFor(addressString, "");
        } else {
            if (location == null) {
                // Both are null. No GPS, but perhaps there still is network connectivity...
                // Perhaps we can return a list of selected cities/locations?
                startSearchFor("", "");
            } else {
                // We have GPS, but our reverse geocode from the GMaps API failed.
                // Could be without network connectivity, or just a transient failure.
                // Is their cached data we can use? Or just use the lat/long directly?
                Toast.makeText(getActivity(), "Google Geocoder Request failed.", Toast.LENGTH_LONG).show();
                Log.e(LOG_TAG, "No address returned from FetchCityTask, fetching with empty location.");
                startSearchFor("", "");
            }
        }
    }

    public void startSearchFor(String location, String keywords) {
        SearchOptions searchOptions = mBundled.mSearchOptions;
        searchOptions.location = location;
        searchOptions.keywords = keywords;
        // Our layout sets android:freezesText="true" , which ensures this is retained across device rotations.
        if (searchOptions.keywords.isEmpty()) {
            mListDescription.setText("Events near " + searchOptions.location);
        } else {
            mListDescription.setText("Events near " + searchOptions.location + " with keywords '" + searchOptions.keywords + "'");
        }
        if (searchOptions.location.isEmpty() && searchOptions.keywords.isEmpty()) {
            showSearchDialog("Could not detect your location. Enter your location here.");
        } else {
            fetchJsonData();
        }
    }

    //TODO: Add caching to the new code:
    /*
    SharedPreferences pref = getActivity().getSharedPreferences(DanceDeetsApp.SAVED_DATA_FILENAME, Context.MODE_PRIVATE);
    if (location != null) {
        pref.edit()
                .putFloat("latitude", (float)location.getLatitude())
                .putFloat("longitude", (float)location.getLongitude())
                .apply();
    } else if (pref.getFloat("latitude", -1) != -1) {
        location = new Location("Saved Preference File");
        location.setLatitude(pref.getFloat("latitude", -1));
        location.setLongitude(pref.getFloat("longitude", -1));
    }
    Log.i(LOG_TAG, "Final location is " + location);
    if (location != null) {
        // TODO: Location: Sometimes this times out too, just randomly.
        // Should we store prefs for the final geocoded location too?
    }
    */

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.events_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_search:
                showSearchDialog("");
                return true;
            case R.id.action_refresh:
                startSearchFor(mBundled.mSearchOptions.location, mBundled.mSearchOptions.keywords);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    public static class SearchListener implements SearchDialogFragment.OnSearchListener {

        private final MyRetainedState mRetained;

        public SearchListener(MyRetainedState retainedState) {
            mRetained = retainedState;
        }

        @Override
        public void onSearch(String location, String keywords) {
            Log.i(LOG_TAG, "Search: " + location + ", " + keywords);
            EventListFragment listFragment = (EventListFragment)mRetained.getTargetFragment();
            listFragment.startSearchFor(location, keywords);
        }
    }

    public void showSearchDialog(String message) {
        mSearchDialog = new SearchDialogFragment();
        Bundle b = new Bundle();
        b.putSerializable(SearchDialogFragment.ARG_SEARCH_OPTIONS, mBundled.mSearchOptions);
        b.putString(SearchDialogFragment.ARG_MESSAGE, message);
        mSearchDialog.setArguments(b);
        mSearchDialog.setOnClickHandler(new SearchListener(mRetained));
        mSearchDialog.show(getFragmentManager(), "search");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreateView");
        ViewGroup rootView = (ViewGroup)inflater.inflate(R.layout.event_list_layout, container, false);

        // Construct the ListFragment's UI objects in super, and stick them inside our rootView in the appropriate place.
        View listRootView = super.onCreateView(inflater, rootView, savedInstanceState);
        View eventListMagicView = rootView.findViewById(R.id.event_list_magic);
        int eventListMagicIndex = rootView.indexOfChild(eventListMagicView);
        ViewGroup.LayoutParams eventListMagicParams = eventListMagicView.getLayoutParams();
        rootView.removeView(eventListMagicView);
        rootView.addView(listRootView, eventListMagicIndex, eventListMagicParams);

        mEmptyListView = inflater.inflate(R.layout.event_list_empty_view,
                container, false);
        mEmptyText = (TextView) mEmptyListView.findViewById(R.id.empty_events_list_text);
        mRetryButton = (Button) mEmptyListView.findViewById(R.id.retry_button);
        mRetryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fetchJsonData();
            }
        });

        /* We need to add the emptyListView as a sibling to the List,
         * as suggested by ListFragment.onCreateView documentation.
         * Then setting/unsetting the ListFragment's Adapter triggers
         * the ProgressBar and ListContainer(List+EmptyView) to alternate.
         * And within the List, it will then alternate with the EmptyView.
         */
        ViewParent listContainerView = listRootView.findViewById(android.R.id.list).getParent();
        ((ViewGroup) listContainerView).addView(mEmptyListView);

        mListDescription = (TextView) rootView.findViewById(R.id.event_list_description);

        eventAdapter = new EventUIAdapter(inflater.getContext(), mBundled.mEventList, R.layout.event_row);

        if (savedInstanceState != null) {
            onEventListFilled();
        }

        return rootView;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i(LOG_TAG, "onViewCreated");
        getListView().setEmptyView(mEmptyListView);
    }

    public void fetchJsonData() {
        // Show the progress bar
        setListAdapter(null);
        /* We need to call setListShown after setListAdapter,
         * because otherwise setListAdapter thinks it has an adapter
         * and tries to show the un-initialized list view. Reported in:
         * https://code.google.com/p/android/issues/detail?id=76779
         */
        setListShown(false);
        mBundled.mEventList.clear();
        Log.i(LOG_TAG, "fetchJsonData");
        DanceDeetsApi.runSearch(mBundled.mSearchOptions, new ResultsReceivedHandler(mRetained));

        AnalyticsUtil.track("Search Events",
                "Location", mBundled.mSearchOptions.location,
                "Keywords", mBundled.mSearchOptions.keywords);
    }

    public static class ResultsReceivedHandler implements DanceDeetsApi.OnResultsReceivedListener {
        private RetainedState mRetained;

        public ResultsReceivedHandler(RetainedState retainedState) {
            mRetained = retainedState;
        }

        @Override
        public void onResultsReceived(List<FullEvent> eventList) {
            EventListFragment listFragment = (EventListFragment)mRetained.getTargetFragment();
            listFragment.handleEventList(eventList);
        }

        @Override
        public void onError(Exception exception) {
            EventListFragment listFragment = (EventListFragment)mRetained.getTargetFragment();
            Log.e(LOG_TAG, "Error retrieving search results, with error: " + exception.toString());
            listFragment.mEmptyText.setVisibility(View.GONE);
            listFragment.mRetryButton.setVisibility(View.VISIBLE);
            listFragment.setListAdapter(listFragment.eventAdapter);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException(
                    "Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = null;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position,
                                long id) {
        super.onListItemClick(listView, view, position, id);

        FullEvent event = mBundled.mEventList.get(position);
        Log.i(LOG_TAG, "onListItemClick: fb event id: " + event.getId());

        VolleySingleton volley = VolleySingleton.getInstance();
        // Prefetch Images
        if (event.getCoverUrl() != null) {
            volley.prefetchPhoto(event.getCoverUrl());
        }
        // Prefetch API data too
        DanceDeetsApi.getEvent(event.getId(), null);

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        if (mCallbacks != null) {
            mCallbacks.onEventSelected(mBundled.mEventList, position);
        }
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be
     * given the 'activated' state when touched.
     */
    public void setActivateOnItemClick(boolean activateOnItemClick) {
        if (activateOnItemClick) {
            // When setting CHOICE_MODE_SINGLE, ListView will automatically
            // give items the 'activated' state when touched.
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        } else {
            getListView().setChoiceMode(ListView.CHOICE_MODE_NONE);
        }
    }

    private void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(mBundled.mActivatedPosition, false);
        } else {
            getListView().setItemChecked(position, true);
        }

        mBundled.mActivatedPosition = position;
    }
}
