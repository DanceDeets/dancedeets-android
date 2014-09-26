package com.dancedeets.dancedeets;

import android.app.ListFragment;
import android.content.Intent;
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

    static final String LOG_TAG = "EventListFragment";

    List<HashMap<String, String>> eventMapList;
    SimpleAdapter simpleAdapter;

    public EventListFragment() {
        eventMapList = new ArrayList<HashMap<String, String>>();
    }

    protected void onJsonResponse(JSONArray response) {
        DateFormat format = DateFormat.getDateTimeInstance();
        for (int i = 0; i < response.length(); i++) {
            HashMap<String, String> map = new HashMap<String, String>();
            try {
                JSONObject event = response.getJSONObject(i);
                map.put("image_url", event.getString("image_url"));
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
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Volley.newRequestQueue(inflater.getContext());

        final String url = "http://www.dancedeets.com/events/feed?location=nyc&keywords=&distance=10&distance_units=miles";

        JsonArrayRequest request = new JsonArrayRequest
                (url, new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        onJsonResponse(response);
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(LOG_TAG, "Error retrieving URL " + url + ", with error: " + error.toString());
                    }
                });

        request.setShouldCache(false);
        RequestQueue queue = VolleySingleton.getInstance(inflater.getContext()).getRequestQueue();
        queue.add(request);

        String[] from = new String[]{"image_url", "title", "location", "start_time"};
        int[] to = new int[]{R.id.icon, R.id.title, R.id.location, R.id.datetime};

        simpleAdapter = new SimpleAdapter(inflater.getContext(),
                eventMapList, R.layout.event_row, from, to);
        simpleAdapter.setViewBinder(new SimpleAdapter.ViewBinder() {
            public boolean setViewValue(View view, Object data, String textRepresentation) {
                if (view.getId() == R.id.icon) {
                    NetworkImageView iv = (NetworkImageView) view;
                    ImageLoader imageLoader = VolleySingleton.getInstance(inflater.getContext()).getImageLoader();
                    iv.setImageUrl((String) data, imageLoader);
                    return true;
                }
                return false;
            }
        });

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    public void onListItemClick(ListView l, View v, int position, long id) {
        String facebookId = eventMapList.get(position).get("id");
        Log.i(LOG_TAG, "fb id " + facebookId);
        Intent showContent = new Intent(getActivity().getApplicationContext(),
                EventInfoActivity.class);
        //showContent.setData(data);
        startActivity(showContent);
    }
}
