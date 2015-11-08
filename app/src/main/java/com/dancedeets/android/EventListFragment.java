package com.dancedeets.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.dancedeets.android.eventlist.EventListItem;
import com.dancedeets.android.eventlist.ListItem;
import com.dancedeets.android.eventlist.OneboxListItem;
import com.dancedeets.android.models.FullEvent;
import com.dancedeets.android.models.OneboxLink;
import com.dancedeets.android.uistate.BundledState;
import com.dancedeets.android.uistate.RetainedState;
import com.dancedeets.android.uistate.StateFragment;

import java.util.ArrayList;
import java.util.List;

public class EventListFragment extends StateFragment<EventListFragment.MyBundledState, RetainedState> implements AdapterView.OnItemClickListener {

    static final String LOG_TAG = "EventListFragment";

    EventListAdapter eventAdapter;
    ListView mList;

    TextView mListDescription;
    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks = null;

    private static final String LIST_STATE = "LIST_STATE";
    private SearchOptions mSearchOptions = new SearchOptions();
    private boolean mTwoPane;

    private boolean mPendingSearch = false;

    enum VisibleState {
        PROGRESS, LIST, EMPTY, RETRY
    }

    View mEmptyContainer;
    View mProgressContainer;
    View mListContainer;
    View mRetryContainer;

    // Currently visible Container
    View mVisibleContainer;

    static protected class MyBundledState extends BundledState {
        /**
         * The current activated item position. Only used on tablets.
         */
        int mActivatedPosition = ListView.INVALID_POSITION;

        ArrayList<FullEvent> mEventList = new ArrayList<>();

        ArrayList<OneboxLink> mOneboxList = new ArrayList<>();

        boolean mInitiatedSearch = false;

        boolean mTwoPane;

        SearchOptions mSearchOptions;
        public boolean mWaitingForSearch;

        MyBundledState(boolean twoPane, SearchOptions searchOptions) {
            mTwoPane = twoPane;
            mSearchOptions = searchOptions;
        }
    }

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
        onEventListFilled(false);
    }

    protected void onEventListFilled(boolean startup) {
        eventAdapter.rebuildList(mBundled.mEventList, mBundled.mOneboxList);
        mList.setAdapter(eventAdapter);
        if (mBundled.mEventList.isEmpty()) {
            setStateShown(VisibleState.EMPTY, !startup);
        } else {
            setStateShown(VisibleState.LIST, !startup);
        }
    }

    public void prepareForSearchOptions(SearchOptions newSearchOptions) {
        Log("prepareForSearchOptions: " + newSearchOptions);
        SearchOptions searchOptions = getSearchOptions();
        searchOptions.location = newSearchOptions.location;
        searchOptions.keywords = newSearchOptions.keywords;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log("onCreateView, mBundled is " + mBundled);
        ViewGroup rootView = (ViewGroup)inflater.inflate(R.layout.search_list, container, false);

        mProgressContainer = rootView.findViewById(R.id.searchProgressContainer);
        mListContainer = rootView.findViewById(R.id.searchListContainer);
        mEmptyContainer = rootView.findViewById(R.id.searchEmptyContainer);
        mRetryContainer = rootView.findViewById(R.id.searchRetryContainer);

        mListContainer.setVisibility(View.GONE);
        mProgressContainer.setVisibility(View.GONE);
        mEmptyContainer.setVisibility(View.GONE);
        mRetryContainer.setVisibility(View.GONE);

        Button mRetryButton = (Button) mRetryContainer.findViewById(R.id.retry_button);
        mRetryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSearch();
            }
        });

        mListDescription = (TextView) rootView.findViewById(R.id.event_list_description);

        eventAdapter = new EventListAdapter(inflater.getContext());
        mList = (ListView)rootView.findViewById(android.R.id.list);
        mList.setOnItemClickListener(this);
        mList.setAdapter(null);

        if (mBundled.mEventList.size() > 0 && !mBundled.mWaitingForSearch) {
            onEventListFilled(true);
        } else {
            setStateShown(VisibleState.PROGRESS, false);
        }

        ListView listView = (ListView)rootView.findViewById(android.R.id.list);
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

    private View getContainer(VisibleState state) {
        switch (state) {
            case PROGRESS:
                return mProgressContainer;
            case LIST:
                return mListContainer;
            case EMPTY:
                return mEmptyContainer;
            case RETRY:
                return mRetryContainer;
            default:
                throw new IllegalArgumentException(state.toString());
        }
    }

    private void setStateShown(VisibleState newVisibleState, boolean animate) {
        View oldVisibleContainer = mVisibleContainer;
        View newVisibleContainer = getContainer(newVisibleState);
        mVisibleContainer = newVisibleContainer;

        // Don't animate between our own state!
        if (oldVisibleContainer == newVisibleContainer) {
            return;
        }

        if (animate) {
            newVisibleContainer.startAnimation(AnimationUtils.loadAnimation(
                    getActivity(), android.R.anim.fade_in));
        } else {
            newVisibleContainer.clearAnimation();
        }
        newVisibleContainer.setVisibility(View.VISIBLE);

        if (oldVisibleContainer != null) {
            if (animate) {
                oldVisibleContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
            } else {
                oldVisibleContainer.clearAnimation();
            }
            oldVisibleContainer.setVisibility(View.GONE);
        }
    }

    // END Derived from ListFragment

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // We don't use mList.setEmptyView(), since it will be used
        // on all null-or-empty adapters (ie, in-progress, empty, retry)
        // So instead we just handle it ourselves with our state management.

        // Reload scroll state
        if (savedInstanceState != null) {
            Parcelable savedState = savedInstanceState.getParcelable(LIST_STATE);
            if (savedState != null) {
                mList.onRestoreInstanceState(savedState);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        // Save scroll state
        if (getView() != null) {
            state.putParcelable(LIST_STATE, mList.onSaveInstanceState());
        }
    }

    public void loadSearchTab() {
        AnalyticsUtil.track("SearchTab Selected",
                "Tab", mBundled.mSearchOptions.timePeriod.toString());
        if (!mBundled.mInitiatedSearch) {
            startSearch();
        }
    }

    protected void startSearch() {
        if (getActivity() == null) {
            Log("startSearch called too early, setting mPendingSearch");
            mPendingSearch = true;
            return;
        }
        mBundled.mInitiatedSearch = true;
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

        mList.setAdapter(null);
        setStateShown(VisibleState.PROGRESS, false);

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
            listFragment.mList.setAdapter(listFragment.eventAdapter);
            listFragment.setStateShown(VisibleState.RETRY, true);
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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ListItem item = eventAdapter.getItem(position);
        if (item instanceof EventListItem) {
            FullEvent event = ((EventListItem)item).getEvent();

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
                int eventListPosition = mBundled.mEventList.indexOf(event);
                mCallbacks.onEventSelected(mBundled.mEventList, eventListPosition);
            }
        } else if (item instanceof OneboxListItem) {
            AnalyticsUtil.track("Onebox");
            OneboxLink onebox = ((OneboxListItem)item).getOnebox();
            Uri url = Uri.parse(onebox.getUrl());

            Crashlytics.log(Log.INFO, LOG_TAG, "Opening URL: " + url);

            Intent intent = new Intent(getActivity(), WebViewActivity.class);
            intent.setData(url);
            getActivity().startActivity(intent);
        }
    }
}
