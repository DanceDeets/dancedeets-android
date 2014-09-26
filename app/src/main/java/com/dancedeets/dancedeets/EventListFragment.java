package com.dancedeets.dancedeets;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;

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

public class EventListFragment extends ListFragment {

    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    private static final String STATE_JSON_RESPONSE = "json_response";

    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks = sDummyCallbacks;

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
        public void onItemSelected(HashMap<String, String> item);
    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(HashMap<String, String> item) {
        }
    };

    static final String LOG_TAG = "EventListFragment";

    List<HashMap<String, String>> eventMapList;
    SimpleAdapter simpleAdapter;
    JSONArray mJsonResponse;

    public EventListFragment() {
    }

    protected void parseJsonResponse(JSONArray response) {
        DateFormat format = DateFormat.getDateTimeInstance();
        for (int i = 0; i < response.length(); i++) {
            HashMap<String, String> map = new HashMap<String, String>();
            try {
                JSONObject event = response.getJSONObject(i);
                map.put("image_url", event.getString("image_url"));
                map.put("cover_url", event.getJSONObject("cover_url").getString("source"));
                ImageLoader imageLoader = VolleySingleton.getInstance(null).getImageLoader();
                imageLoader.get(event.getString("image_url"), new ImageLoader.ImageListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }

                    @Override
                    public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                    }
                });
                map.put("id", event.getString("id"));
                map.put("title", event.getString("title"));
                map.put("location", event.getString("location"));
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
        setListAdapter(simpleAdapter);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        eventMapList = new ArrayList<HashMap<String, String>>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Volley.newRequestQueue(inflater.getContext());
        Log.i(LOG_TAG, "onCreateView");
        String[] from = new String[]{"image_url", "title", "location", "start_time"};
        int[] to = new int[]{R.id.icon, R.id.title, R.id.location, R.id.datetime};

        simpleAdapter = new SimpleAdapter(inflater.getContext(),
                eventMapList, R.layout.event_row, from, to);
        simpleAdapter.setViewBinder(new SimpleAdapter.ViewBinder() {
            public boolean setViewValue(View view, Object data, String textRepresentation) {
                if (view.getId() == R.id.icon) {
                    NetworkImageView iv = (NetworkImageView) view;
                    ImageLoader imageLoader = VolleySingleton.getInstance(null).getImageLoader();
                    iv.setImageUrl((String) data, imageLoader);
                    return true;
                }
                return false;
            }
        });

        // If we saved the json, use it, otherwise fetch it from the server
        if (savedInstanceState != null) {
            try {
                String jsonData = savedInstanceState.getString(STATE_JSON_RESPONSE);
                mJsonResponse = new JSONArray(savedInstanceState.getString(STATE_JSON_RESPONSE));
                parseJsonResponse(mJsonResponse);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error processing saved json...strange!");
            }
        } else {
            final String url = "http://www.dancedeets.com/events/feed?location=nyc&keywords=&distance=10&distance_units=miles";

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
                        }
                    });

            Log.d(LOG_TAG, "Querying server feed: " + url);
            request.setShouldCache(false);
            RequestQueue queue = VolleySingleton.getInstance(null).getRequestQueue();
            queue.add(request);
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState
                    .getInt(STATE_ACTIVATED_POSITION));
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
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position,
                                long id) {
        super.onListItemClick(listView, view, position, id);

        String facebookId = eventMapList.get(position).get("id");
        Log.i(LOG_TAG, "fb id " + facebookId);

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        mCallbacks.onItemSelected(eventMapList.get(position));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
        outState.putString(STATE_JSON_RESPONSE, mJsonResponse.toString());
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
