package com.dancedeets.dancedeets;

import android.app.Activity;
import android.app.ListFragment;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
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
import android.widget.SearchView;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class EventListFragment extends ListFragment implements GoogleApiClient.ConnectionCallbacks {

    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    private static final String STATE_JSON_RESPONSE = "json_response";
    private static final String STATE_LOCATION = "location";

    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks = null;

    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        public void onEventSelected(Event event);
    }

    static final String LOG_TAG = "EventListFragment";

    String mLocation;

    List<Event> eventList;
    EventUIAdapter eventAdapter;
    JSONArray mJsonResponse;

    View mEmptyListView;
    TextView mEmptyText;
    Button mRetryButton;
    GoogleApiClient mGoogleApiClient;

    public EventListFragment() {
    }

    /*
     * Handle results returned to the FragmentActivity
     * by Google Play services
     */
    @Override
    public void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        // Decide what to do based on the original request code
        switch (requestCode) {
            case GooglePlayUtil.CONNECTION_FAILURE_RESOLUTION_REQUEST :
            /*
             * If the result code is Activity.RESULT_OK, try
             * to connect again
             */
                switch (resultCode) {
                    case Activity.RESULT_OK :
                    /*
                     * Try the request again
                     */
                        initializeFromLocation();
                        break;
                }
        }
    }

    protected void parseJsonResponse(JSONArray response) {
        Log.i(LOG_TAG, "Parsing JSON Response");

        VolleySingleton volley = VolleySingleton.getInstance(null);


        for (int i = 0; i < response.length(); i++) {
            Bundle eventBundle = new Bundle();
            Event event = null;
            try {
                JSONObject jsonEvent = response.getJSONObject(i);
                event = new Event(jsonEvent);
                // Prefetch images so scrolling "just works"
                volley.prefetchThumbnail(event.getThumbnailUrl());
            } catch (JSONException e) {
                Log.e(LOG_TAG, "JSONException: " + e);
            }
            eventList.add(event);
        }
        mEmptyText.setVisibility(View.GONE);
        mRetryButton.setVisibility(View.VISIBLE);
        setListAdapter(eventAdapter);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(LOG_TAG, "onCreate");
        eventList = new ArrayList<Event>();
        setHasOptionsMenu(true);
        initializeFromLocation();
    }

    protected void initializeFromLocation() {
        if (GooglePlayUtil.servicesConnected(getActivity())) {
            mGoogleApiClient = new GoogleApiClient.Builder(getActivity().getBaseContext())
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .build();
        } else {
            Log.i(LOG_TAG, "Unable to connect to Google Play Services");
        }
    }
    @Override
    public void onStart() {
        super.onStart();
        Log.i(LOG_TAG, "onStart");
        // Connect the client.
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        Log.i(LOG_TAG, "onStop");
        // Disconnecting the client invalidates it.
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(LOG_TAG, "GoogleApiClient.onConnected: " + bundle);
        // We reconnect every time the app wakes up, but we only need
        // to fetch on start if we have no location data (ie, app startup).
        if (mLocation == null) {
            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            Log.i(LOG_TAG, "Loc is " + location.getLatitude() + ", " + location.getLongitude());
            mLocation = location.getLatitude() + "," + location.getLongitude();
            fetchJsonData();
        }
    }
    public void onConnectionSuspended(int cause) {
        Log.i(LOG_TAG, "GoogleApiClient connection has been suspended");
    }

    @Override
    public void onCreateOptionsMenu (Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.events_list, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchManager searchManager =
                (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        searchView.setIconifiedByDefault(false);
        Log.i(LOG_TAG, "onCreateOptionsMenu, with Location: " + mLocation);
        if (mLocation != null) {
            searchView.setQuery(mLocation, true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        mEmptyListView = inflater.inflate(R.layout.event_list_empty_view,
                container, false);
        mEmptyText = (TextView)mEmptyListView.findViewById(R.id.empty_events_list_text);
        mRetryButton = (Button)mEmptyListView.findViewById(R.id.retry_button);
        mRetryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fetchJsonData();
            }
        });

        Volley.newRequestQueue(inflater.getContext());
        Log.d(LOG_TAG, "onCreateView");

        eventAdapter = new EventUIAdapter(inflater.getContext(), eventList, R.layout.event_row);
        return rootView;
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
        eventList.clear();
        Log.i(LOG_TAG, "fetchJsonData");

        boolean SANS_INTERNET = false;
        if (SANS_INTERNET) {

            final String jsonString = "[{\"city\": \"EXPG New York, New York, NY, US\", \"end_time\": \"2014-09-29T20:30:00Z\", \"image_url\": \"https://scontent-a.xx.fbcdn.net/hphotos-xap1/v/t1.0-9/c0.0.200.200/p200x200/10635707_10152646002551066_7530276650584086219_n.jpg?oh=33a51a2e0ad11b5ea42ee1f083d39233&oe=548B9F4D\", \"description\": \"Weekly House Dance class in New York City at EXPG-NYC studio, every Monday night (7-8:30PM).\\n\\nIt's a beginner, open class. We talk about the club culture, the history of House dance and music, learn foundation steps, how to get creative with them & be able to freestyle. Emphasis is made on how to connect the movement with the music. \\n\\n\\nEvery Monday\\nFrom 7:00PM to 8:30PM\\n@EXPG New York 27 2nd avenue, NY, NY 10003.\\n\\nFor more info, please visit www.expg-ny.com.\", \"title\": \"House Dance class with Mai L\\u00ea\", \"keywords\": \"class, club, house dance\", \"start_time\": \"2014-09-29T19:00:00Z\", \"id\": \"781192871949041\", \"cover_url\": {\"source\": \"https://scontent-a.xx.fbcdn.net/hphotos-xap1/v/t1.0-9/10635707_10152646002551066_7530276650584086219_n.jpg?oh=26b884cb2af327e81336b27e9981f08c&oe=5485D972\", \"height\": 756, \"width\": 945}, \"location\": \"New York, NY, US\"}]";
            try {
                mJsonResponse = new JSONArray(jsonString);
            } catch (JSONException exception) {
                Log.e(LOG_TAG, "Error faking json response: " + exception);
            }
            parseJsonResponse(mJsonResponse);
        } else {
            Uri.Builder builder = Uri.parse("http://www.dancedeets.com/events/feed").buildUpon();
            builder.appendQueryParameter("location", mLocation);
            builder.appendQueryParameter("keywords", "");
            builder.appendQueryParameter("distance", "10");
            builder.appendQueryParameter("distance_units", "miles");
            final Uri uri = builder.build();

            JsonArrayRequest request = new JsonArrayRequest
                    (uri.toString(), new Response.Listener<JSONArray>() {

                        @Override
                        public void onResponse(JSONArray response) {
                            mJsonResponse = response;
                            parseJsonResponse(response);
                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(LOG_TAG, "Error retrieving URL " + uri + ", with error: " + error.toString());
                            mEmptyText.setVisibility(View.GONE);
                            mRetryButton.setVisibility(View.VISIBLE);
                            setListAdapter(eventAdapter);
                        }
                    });

            Log.d(LOG_TAG, "Querying server feed: " + uri);
            request.setShouldCache(false);
            RequestQueue queue = VolleySingleton.getInstance(null).getRequestQueue();
            queue.add(request);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i(LOG_TAG, "onViewCreated");

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
                setActivatedPosition(savedInstanceState
                        .getInt(STATE_ACTIVATED_POSITION));
            }
            // If we saved the json, use it, otherwise fetch it from the server
            String jsonData = savedInstanceState.getString(STATE_JSON_RESPONSE);
            if (jsonData != null) {
                try {
                    mJsonResponse = new JSONArray(jsonData);
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error processing saved json...strange!");
                }
            }
            // If we saved the json, use it, otherwise fetch it from the server
            if (savedInstanceState.containsKey(STATE_LOCATION)) {
                mLocation = savedInstanceState.getString(STATE_LOCATION);
                Log.i(LOG_TAG, "Loading bundle-saved location: " + mLocation);
                //TODO: save this somewhere, and figure out if we want to do this in the activity or the fragment
            }
        }
        Log.d(LOG_TAG, "mJsonResponse is " + mJsonResponse);

        if (mJsonResponse != null) {
            parseJsonResponse(mJsonResponse);
        } else {
            // Don't fetch data, let the Google Play Services connect,
            // and then grab the most recent location information,
            // and use that to fetch the results.
        }

        /* We need to add the emptyListView as a sibling to the List,
         * as suggested by ListFragment.onCreateView documentation.
         * Then setting/unsetting the ListFragment's Adapter triggers
         * the ProgressBar and ListContainer(List+EmptyView) to alternate.
         * And within the List, it will then alternate with the EmptyView.
         */
        ViewParent listContainerView = view.findViewById(android.R.id.list).getParent();
        ((ViewGroup)listContainerView).addView(mEmptyListView);
        getListView().setEmptyView(mEmptyListView);
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

        Event event = eventList.get(position);
        String facebookId = event.getId();
        Log.i(LOG_TAG, "fb id " + facebookId);

        if (event.getCoverUrl() != null) {
            VolleySingleton volley = VolleySingleton.getInstance(null);
            volley.prefetchPhoto(event.getCoverUrl());
        }

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        if (mCallbacks != null) {
            mCallbacks.onEventSelected(event);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(LOG_TAG, "onSaveInstanceState");
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
        if (mJsonResponse != null) {
            outState.putString(STATE_JSON_RESPONSE, mJsonResponse.toString());
        }
        if (mLocation != null) {
            Log.d(LOG_TAG, "Location is " + mLocation);
            outState.putString(STATE_LOCATION, mLocation);
        }
        Log.d(LOG_TAG, "Bundle saved is " + outState);
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be
     * given the 'activated' state when touched.
     */
    public void setActivateOnItemClick(boolean activateOnItemClick) {
        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        getListView().setChoiceMode(
                activateOnItemClick ? ListView.CHOICE_MODE_SINGLE
                        : ListView.CHOICE_MODE_NONE);
    }

    private void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(mActivatedPosition, false);
        } else {
            getListView().setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }
}
