package com.dancedeets.android;

import android.app.Activity;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.dancedeets.android.models.FullEvent;
import com.dancedeets.android.uistate.BundledState;
import com.dancedeets.android.uistate.RetainedState;
import com.dancedeets.android.uistate.StateListFragment;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.ArrayList;
import java.util.List;

public class EventListFragment extends StateListFragment<EventListFragment.MyBundledState, RetainedState> implements SearchTarget {

    private static final String LIST_STATE = "LIST_STATE";
    private SearchOptions mSearchOptions = new SearchOptions();
    private boolean mTwoPane;

    private boolean mPendingSearch = false;

    static protected class MyBundledState extends BundledState {
        /**
         * The current activated item position. Only used on tablets.
         */
        int mActivatedPosition = ListView.INVALID_POSITION;

        ArrayList<FullEvent> mEventList = new ArrayList<>();

        boolean mDirty = true; // Start out dirty

        boolean mTwoPane;

        SearchOptions mSearchOptions;

        MyBundledState(boolean twoPane, SearchOptions searchOptions) {
            mTwoPane = twoPane;
            mSearchOptions = searchOptions;
        }
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
        void onEventSelected(ArrayList<FullEvent> allEvents, int positionSelected);
    }

    static final String LOG_TAG = "EventListFragment";


    EventUIAdapter eventAdapter;

    View mEmptyListView;
    View mEmptyText;
    Button mRetryButton;
    TextView mListDescription;

    GoogleApiClient mGoogleApiClient;

    // These are exposed as member variables for the sake of testing.
    LocationManager mLocationManager;

    public EventListFragment() {
    }

    public void setTwoPane(boolean twoPane) {
        if (mBundled != null) {
            mBundled.mTwoPane = twoPane;
        } else {
            mTwoPane = twoPane;
        }
    }

    public void setEventSearchType(SearchOptions.TimePeriod eventSearchType) {
        getSearchOptions().timePeriod = eventSearchType;
    }

    protected SearchOptions getSearchOptions() {
        if (mBundled != null) {
            return mBundled.mSearchOptions;
        } else {
            return mSearchOptions;
        }
    }

    @Override
    public MyBundledState buildBundledState() {
        /**
         *
         * Before this function is called, we read/set mTwoPane/mSearchOptions directly on this class.
         * This allows us to initialize this class with its identity and index as a tab.
         * This is used by getUniqueTag to construct an correctly named Retained fragment onAttach.
         * Then we construct a Bundled object here, copying over the relevant fields.
         * After this function is called, we use the fields on the persistable mBundled object directly.
         * Then we rely on persisting through the Bundled object.
         */

        return new MyBundledState(mTwoPane, mSearchOptions);
    }

    @Override
    public String getUniqueTag() {
        return LOG_TAG + "." + getSearchOptions().timePeriod;
    }

    @Override
    public RetainedState buildRetainedState() {
        return new RetainedState();
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
    public void prepareForSearchOptions(SearchOptions newSearchOptions) {
        Log("prepareForSearchOptions: " + newSearchOptions);
        SearchOptions searchOptions = getSearchOptions();
        searchOptions.location = newSearchOptions.location;
        searchOptions.keywords = newSearchOptions.keywords;
        if (mBundled != null) {
            mBundled.mDirty = true;
        } // If mBundled is empty (for instantiation), then when it is constructed, it will default to true anyway
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
    Log("Final location is " + location);
    if (location != null) {
        // TODO: Location: Sometimes this times out too, just randomly.
        // Should we store prefs for the final geocoded location too?
    }
    */

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log("onCreateView, mBundled is " + mBundled);
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
        mEmptyText = mEmptyListView.findViewById(R.id.empty_events_list_text);
        mRetryButton = (Button) mEmptyListView.findViewById(R.id.retry_button);
        mRetryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSearch();
            }
        });

        /* We need to add the emptyListView as a sibling to the List,
         * as suggested by ListFragment.onCreateView documentation.
         * Then setting/unsetting the ListFragment's Adapter triggers
         * the ProgressBar and ListContainer(List+EmptyView) to alternate.
         * And within the List, it will then alternate with the EmptyView.
         */
        ListView listView = (ListView)listRootView.findViewById(android.R.id.list);
        ViewParent listContainerView = listView.getParent();
        ((ViewGroup) listContainerView).addView(mEmptyListView);

        mListDescription = (TextView) rootView.findViewById(R.id.event_list_description);

        eventAdapter = new EventUIAdapter(inflater.getContext(), mBundled.mEventList, R.layout.event_row);

        if (savedInstanceState != null) {
            onEventListFilled();
        }

        // In two-pane mode, list items should be given the
        // 'activated' state when touched.
        if (mBundled.mTwoPane) {
            // When setting CHOICE_MODE_SINGLE, ListView will automatically
            // give items the 'activated' state when touched.
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        } else {
            listView.setChoiceMode(ListView.CHOICE_MODE_NONE);
        }


        return rootView;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setEmptyView(mEmptyListView);

        // Reload scroll state
        if (savedInstanceState != null) {
            Parcelable savedState = savedInstanceState.getParcelable(LIST_STATE);
            if (savedState != null) {
                getListView().onRestoreInstanceState(savedState);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        // Save scroll state
        if (getView() != null) {
            state.putParcelable(LIST_STATE, getListView().onSaveInstanceState());
        }
    }

    public void loadSearchTab() {
        AnalyticsUtil.track("SearchTab Selected",
                "Tab", mBundled.mSearchOptions.timePeriod.toString());
        if (mBundled.mDirty) {
            mBundled.mDirty = false;
            startSearch();
        }
    }

    protected void startSearch() {
        if (getActivity() == null) {
            Log("startSearch called too early, setting mPendingSearch");
            mPendingSearch = true;
            return;
        }
        SearchOptions searchOptions = getSearchOptions();
        Log("startSearch: " + searchOptions);
        // Our layout sets android:freezesText="true" , which ensures this is retained across device rotations.
        String listDescription;
        if (searchOptions.keywords.isEmpty()) {
            listDescription = String.format(getString(R.string.events_near), searchOptions.location);
        } else if (searchOptions.location.isEmpty()) {
            listDescription = String.format(getString(R.string.events_with_keyword), searchOptions.keywords);
        } else {
            listDescription = String.format(getString(R.string.events_near_with_keyword), searchOptions.location, searchOptions.keywords);
        }
        mListDescription.setText(listDescription);

        // Show the progress bar
        setListAdapter(null);
        /* We need to call setListShown after setListAdapter,
         * because otherwise setListAdapter thinks it has an adapter
         * and tries to show the un-initialized list view. Reported in:
         * https://code.google.com/p/android/issues/detail?id=76779
         */
        setListShown(false);
        mBundled.mEventList.clear();
        DanceDeetsApi.runSearch(mBundled.mSearchOptions, new ResultsReceivedHandler(mRetained));
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
            Crashlytics.log(Log.ERROR, LOG_TAG, "Error retrieving search results, with error: " + exception.toString());
            listFragment.mEmptyText.setVisibility(View.GONE);
            listFragment.mRetryButton.setVisibility(View.VISIBLE);
            listFragment.setListAdapter(listFragment.eventAdapter);
        }
    }

    protected void Log(String log) {
        Crashlytics.log(Log.INFO, LOG_TAG, getSearchOptions().timePeriod.toString() +  ": " + log);
    }
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        Log("onAttach " + this + ": " + activity);
        if (mPendingSearch) {
            mPendingSearch = false;
            startSearch();
        }

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
        Log("onListItemClick: fb event id: " + event.getId());

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

    private void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(mBundled.mActivatedPosition, false);
        } else {
            getListView().setItemChecked(position, true);
        }

        mBundled.mActivatedPosition = position;
    }
}
