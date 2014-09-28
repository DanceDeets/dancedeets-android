package com.dancedeets.dancedeets;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Intent;
import android.location.Location;
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
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
        public void onEventSelected(Bundle bundle);
    }

    static final String LOG_TAG = "EventListFragment";

    String mLocation;

    List<HashMap<String, String>> eventMapList;
    SimpleAdapter simpleAdapter;
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
        DateFormat format = DateFormat.getDateTimeInstance();

        VolleySingleton volley = VolleySingleton.getInstance(null);


        for (int i = 0; i < response.length(); i++) {
            HashMap<String, String> map = new HashMap<String, String>();
            try {
                JSONObject event = response.getJSONObject(i);
                map.put("image_url", event.getString("image_url"));
                if (!event.isNull("cover_url")) {
                    map.put("cover_url", event.getJSONObject("cover_url").getString("source"));
                }
                // Prefetch images so scrolling "just works"
                volley.prefetchThumbnail(event.getString("image_url"));

                map.put("id", event.getString("id"));
                map.put("title", event.getString("title"));
                map.put("location", event.getString("location"));
                map.put("description", event.getString("description"));
                DateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                try {
                    Date date = isoDateFormat.parse(event.getString("start_time"));
                    map.put("start_time", format.format(date));
                } catch (ParseException e) {
                    Log.e(LOG_TAG, "Date ParseException: " + e);
                }
            } catch (JSONException e) {
                Log.e(LOG_TAG, "JSONException: " + e);
            }
            eventMapList.add(map);
        }
        mEmptyText.setVisibility(View.GONE);
        mRetryButton.setVisibility(View.VISIBLE);
        setListAdapter(simpleAdapter);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(LOG_TAG, "onCreate");
        eventMapList = new ArrayList<HashMap<String, String>>();
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
        // We reconnect every time the app wakes up, but we only need
        // to fetch on start if we have no location data (ie, app startup).
        if (mLocation == null) {
            Log.i(LOG_TAG, "GoogleApiClient.onConnected: " + bundle);
            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            Log.i(LOG_TAG, "Loc is " + location.getLatitude() + ", " + location.getLongitude());
            mLocation = location.getLatitude() + "," + location.getLongitude();
            fetchJsonData();
        }
    }
    public void onConnectionSuspended(int cause) {
        Log.i(LOG_TAG, "GoogleApiClient connection has been suspend");
    }

    @Override
    public void onCreateOptionsMenu (Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.events_list, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextSubmit(String s) {
                Log.i(LOG_TAG, "Search for " + s);
                mLocation = s;
                fetchJsonData();
                return false;
            }
        });
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
        String[] from = new String[]{"image_url", "title", "location", "start_time"};
        int[] to = new int[]{R.id.icon, R.id.title, R.id.location, R.id.datetime};

        //TODO: get rid of this intermediary string-labels that requires a map<string, string>, and have it all operate on bundles
        // TODO: Probably will make sense to operate with a ViewHolder object at that point?
        // http://developer.android.com/training/improving-layouts/smooth-scrolling.html#ViewHolder
        simpleAdapter = new SimpleAdapter(inflater.getContext(),
                eventMapList, R.layout.event_row, from, to);
        simpleAdapter.setViewBinder(new SimpleAdapter.ViewBinder() {
            public boolean setViewValue(View view, Object data, String textRepresentation) {
                if (view.getId() == R.id.icon) {
                    NetworkImageView iv = (NetworkImageView) view;
                    ImageLoader thumbnailLoader = VolleySingleton.getInstance(null).getThumbnailLoader();
                    iv.setImageUrl((String) data, thumbnailLoader);
                    return true;
                }
                return false;
            }
        });
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
        eventMapList.clear();

        final String url = "http://www.dancedeets.com/events/feed?location="+mLocation+"&keywords=&distance=10&distance_units=miles";

        JsonArrayRequest request = new JsonArrayRequest
                (url, new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        mJsonResponse = response;
                        parseJsonResponse(response);
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(LOG_TAG, "Error retrieving URL " + url + ", with error: " + error.toString());
                        mEmptyText.setVisibility(View.GONE);
                        mRetryButton.setVisibility(View.VISIBLE);
                        setListAdapter(simpleAdapter);
                    }
                });

        Log.d(LOG_TAG, "Querying server feed: " + url);
        request.setShouldCache(false);
        RequestQueue queue = VolleySingleton.getInstance(null).getRequestQueue();
        queue.add(request);
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

        String facebookId = eventMapList.get(position).get("id");
        Log.i(LOG_TAG, "fb id " + facebookId);

        HashMap<String, String> item = eventMapList.get(position);
        Bundle arguments = new Bundle();
        arguments.putString("id", item.get("id"));
        arguments.putString("cover", item.get("cover_url"));
        if (item.containsKey("cover_url")) {
            VolleySingleton volley = VolleySingleton.getInstance(null);
            volley.prefetchPhoto(item.get("cover_url"));
        }
        arguments.putString("title", item.get("title"));
        arguments.putString("location", item.get("location"));
        arguments.putString("description", item.get("description"));
        arguments.putLong("start_time", 0);
        arguments.putLong("end_time", 0);

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        if (mCallbacks != null) {
            mCallbacks.onEventSelected(arguments);
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
            outState.putString(STATE_LOCATION, mLocation);
        }
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
