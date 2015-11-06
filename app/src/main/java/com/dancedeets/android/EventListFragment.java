package com.dancedeets.android;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.dancedeets.android.models.FullEvent;
import com.dancedeets.android.models.OneboxLink;
import com.dancedeets.android.uistate.BundledState;
import com.dancedeets.android.uistate.RetainedState;
import com.dancedeets.android.uistate.StateFragment;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.ArrayList;
import java.util.List;

public class EventListFragment extends StateFragment<EventListFragment.MyBundledState, RetainedState> implements SearchTarget {

    final private Handler mHandler = new Handler();

    final private Runnable mRequestFocus = new Runnable() {
        public void run() {
            mList.focusableViewAvailable(mList);
        }
    };

    final private AdapterView.OnItemClickListener mOnClickListener
            = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            onListItemClick((ListView)parent, v, position, id);
        }
    };

    private static final String LIST_STATE = "LIST_STATE";
    private SearchOptions mSearchOptions = new SearchOptions();
    private boolean mTwoPane;

    private boolean mPendingSearch = false;

    ListAdapter mAdapter;
    ListView mList;
    View mEmptyView;
    TextView mStandardEmptyView;
    View mProgressContainer;
    View mListContainer;
    CharSequence mEmptyText;
    boolean mListShown;

    static protected class MyBundledState extends BundledState {
        /**
         * The current activated item position. Only used on tablets.
         */
        int mActivatedPosition = ListView.INVALID_POSITION;

        ArrayList<FullEvent> mEventList = new ArrayList<>();

        ArrayList<OneboxLink> mOneboxList = new ArrayList<>();

        boolean mDirty = true; // Start out dirty

        boolean mTwoPane;

        SearchOptions mSearchOptions;
        public boolean mWaitingForSearch;

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
    View mEmptyTextView;
    Button mRetryButton;
    TextView mListDescription;

    GoogleApiClient mGoogleApiClient;

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

    protected void handleEventList(List<FullEvent> eventList, List<OneboxLink> oneboxList) {
        mBundled.mWaitingForSearch = false;
        mBundled.mEventList.clear();
        mBundled.mEventList.addAll(eventList);
        mBundled.mOneboxList.clear();
        mBundled.mOneboxList.addAll(oneboxList);
        onEventListFilled();
    }

    protected void onEventListFilled() {
        if (mBundled.mEventList.isEmpty()) {
            mEmptyTextView.setVisibility(View.VISIBLE);
        } else {
            mEmptyTextView.setVisibility(View.GONE);
        }
        mRetryButton.setVisibility(View.GONE);
        eventAdapter.rebuildList(mBundled.mEventList);
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
        ViewGroup rootView = (ViewGroup)inflater.inflate(R.layout.search_list, container, false);

        mEmptyListView = inflater.inflate(R.layout.search_empty_list,
                container, false);
        mEmptyTextView = mEmptyListView.findViewById(R.id.empty_events_list_text);
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
        ListView listView = (ListView)rootView.findViewById(android.R.id.list);
        ViewParent listContainerView = listView.getParent();
        ((ViewGroup) listContainerView).addView(mEmptyListView);

        mListDescription = (TextView) rootView.findViewById(R.id.event_list_description);

        eventAdapter = new EventUIAdapter(inflater.getContext());
        setListAdapter(null);

        if (mBundled.mEventList.size() > 0 && !mBundled.mWaitingForSearch) {
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

    public void setListShown(boolean shown) {
        setListShown(shown, true);
    }
    public void setListShownNoAnimation(boolean shown) {
        setListShown(shown, false);
    }

    private void setListShown(boolean shown, boolean animate) {
        ensureList();
        if (mProgressContainer == null) {
            throw new IllegalStateException("Can't be used with a custom content view");
        }
        if (mListShown == shown) {
            return;
        }
        mListShown = shown;
        if (shown) {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
            } else {
                mProgressContainer.clearAnimation();
                mListContainer.clearAnimation();
            }
            mProgressContainer.setVisibility(View.GONE);
            mListContainer.setVisibility(View.VISIBLE);
        } else {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
            } else {
                mProgressContainer.clearAnimation();
                mListContainer.clearAnimation();
            }
            mProgressContainer.setVisibility(View.VISIBLE);
            mListContainer.setVisibility(View.GONE);
        }
    }

    public ListAdapter getListAdapter() {
        return mAdapter;
    }

    private void ensureList() {
        if (mList != null) {
            return;
        }
        View root = getView();
        if (root == null) {
            throw new IllegalStateException("Content view not yet created");
        }
        if (root instanceof ListView) {
            mList = (ListView)root;
        } else {
            mStandardEmptyView = (TextView)root.findViewById(
                    R.id.emptyList);
            if (mStandardEmptyView == null) {
                mEmptyView = root.findViewById(android.R.id.empty);
            } else {
                mStandardEmptyView.setVisibility(View.GONE);
            }
            mProgressContainer = root.findViewById(R.id.progressContainer);
            mListContainer = root.findViewById(R.id.listContainer);
            View rawListView = root.findViewById(android.R.id.list);
            if (!(rawListView instanceof ListView)) {
                throw new RuntimeException(
                        "Content has view with id attribute 'android.R.id.list' "
                                + "that is not a ListView class");
            }
            mList = (ListView)rawListView;
            if (mList == null) {
                throw new RuntimeException(
                        "Your content must have a ListView whose id attribute is " +
                                "'android.R.id.list'");
            }
            if (mEmptyView != null) {
                mList.setEmptyView(mEmptyView);
            } else if (mEmptyText != null) {
                mStandardEmptyView.setText(mEmptyText);
                mList.setEmptyView(mStandardEmptyView);
            }
        }
        mListShown = true;
        mList.setOnItemClickListener(mOnClickListener);
        if (mAdapter != null) {
            ListAdapter adapter = mAdapter;
            mAdapter = null;
            setListAdapter(adapter);
        } else {
            // We are starting without an adapter, so assume we won't
            // have our data right away and start with the progress indicator.
            if (mProgressContainer != null) {
                setListShown(false, false);
            }
        }
        mHandler.post(mRequestFocus);
    }

    public ListView getListView() {
        ensureList();
        return mList;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ensureList();
        getListView().setEmptyView(mEmptyListView);

        // Reload scroll state
        if (savedInstanceState != null) {
            Parcelable savedState = savedInstanceState.getParcelable(LIST_STATE);
            if (savedState != null) {
                getListView().onRestoreInstanceState(savedState);
            }
        }
    }

    public void setListAdapter(ListAdapter adapter) {
        boolean hadAdapter = mAdapter != null;
        mAdapter = adapter;
        if (mList != null) {
            mList.setAdapter(adapter);
            if (!mListShown && !hadAdapter) {
                // The list was hidden, and previously didn't have an
                // adapter.  It is now time to show it.
                setListShown(true, getView().getWindowToken() != null);
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
        mBundled.mWaitingForSearch = true;
        DanceDeetsApi.runSearch(mBundled.mSearchOptions, new ResultsReceivedHandler(mRetained));
    }

    public static class ResultsReceivedHandler implements DanceDeetsApi.OnResultsReceivedListener {
        private RetainedState mRetained;

        public ResultsReceivedHandler(RetainedState retainedState) {
            mRetained = retainedState;
        }

        @Override
        public void onResultsReceived(List<FullEvent> eventList, List<OneboxLink> oneboxList) {
            EventListFragment listFragment = (EventListFragment)mRetained.getTargetFragment();
            listFragment.handleEventList(eventList, oneboxList);
        }

        @Override
        public void onError(Exception exception) {
            EventListFragment listFragment = (EventListFragment)mRetained.getTargetFragment();
            Crashlytics.log(Log.ERROR, LOG_TAG, "Error retrieving search results, with error: " + exception.toString());
            listFragment.mEmptyTextView.setVisibility(View.GONE);
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

    public void onListItemClick(ListView listView, View view, int position,
                                long id) {
        int translatedPosition = eventAdapter.translatePosition(position);
        if (translatedPosition < 0) {
            return;
        }
        FullEvent event = mBundled.mEventList.get(translatedPosition);

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
            mCallbacks.onEventSelected(mBundled.mEventList, translatedPosition);
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
