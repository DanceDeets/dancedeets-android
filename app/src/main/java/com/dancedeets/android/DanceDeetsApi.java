package com.dancedeets.android;

import android.net.Uri;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.*;
import com.dancedeets.android.models.Event;
import com.dancedeets.android.models.FullEvent;
import com.facebook.Session;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lambert on 2014/12/24.
 */
public class DanceDeetsApi {

    private static String LOG_TAG = "DanceDeetsApi";

    private static String VERSION = "v1.0";

    private static DateFormat isoDateTimeFormatWithTZ = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    private static String getApiUrl(String apiPath) {
        //return "http://www.dancedeets.com/api/" + apiPath;
        return "http://www.dancedeets.com/api/" + VERSION + "/" + apiPath;
    }

    public static void sendAuth(Session session, String location) {
        Log.i(LOG_TAG, "sendAuth with location: " + location);
        JSONObject jsonPayload = new JSONObject();
        try {
            jsonPayload.put("access_token", session.getAccessToken());
            jsonPayload.put("access_token_expires", isoDateTimeFormatWithTZ.format(session.getExpirationDate()));
            jsonPayload.put("location", location);
            jsonPayload.put("client", "android");
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error constructing request: " + e);
            return;
        }
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                getApiUrl("auth"),
                jsonPayload,
                new com.android.volley.Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(LOG_TAG, "Successfully called /api/auth: " + response);
                    }
                },
                new com.android.volley.Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(LOG_TAG, "Error calling /api/auth: " + error);
                    }
                });
        VolleySingleton.getInstance().getRequestQueue().add(request);
    }


    public interface OnEventReceivedListener {
        public void onEventReceived(FullEvent event);
        public void onError(Exception exception);
    }

    public static void getEvent(String id, final OnEventReceivedListener onEventReceivedListener) {
        JsonObjectRequest request = new JsonObjectRequest(
                getApiUrl("events/" + id),
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        FullEvent event;
                        try {
                            event = FullEvent.parse(response);
                        } catch (JSONException e) {
                            Log.e(LOG_TAG, "Error reading from event api: " + e + ": " + response);
                            if (onEventReceivedListener != null) {
                                onEventReceivedListener.onError(e);
                            }
                            return;
                        }
                        if (onEventReceivedListener != null) {
                            onEventReceivedListener.onEventReceived(event);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(LOG_TAG, "Error retrieving data: " + error);
                        if (onEventReceivedListener != null) {
                            onEventReceivedListener.onError(error);
                        }
                    }
                });
        VolleySingleton.getInstance().getRequestQueue().add(request);
    }


    public interface OnResultsReceivedListener {
        public void onResultsReceived(List<FullEvent> eventList);
        public void onError(Exception exception);
    }

    static class SearchProcessor implements Response.Listener<JSONObject> {

        private final OnResultsReceivedListener mOnResultsReceivedListener;

        public SearchProcessor(OnResultsReceivedListener onResultsReceivedListener)
        {
            mOnResultsReceivedListener = onResultsReceivedListener;
        }
        @Override
        public void onResponse(JSONObject response) {
            List<FullEvent> eventList = new ArrayList<FullEvent>();
            JSONArray jsonEventList;
            try {
                jsonEventList = response.getJSONArray("results");
            } catch (JSONException e) {
                Log.e(LOG_TAG, "JSONException: " + e);
                mOnResultsReceivedListener.onError(e);
                return;
            }
            for (int i = 0; i < jsonEventList.length(); i++) {
                FullEvent event = null;
                try {
                    JSONObject jsonEvent = jsonEventList.getJSONObject(i);
                    event = FullEvent.parse(jsonEvent);
                    // Prefetch images so scrolling "just works"
                    Log.e(LOG_TAG, event.getId() + ": " + event.getThumbnailUrl());
                    VolleySingleton volley = VolleySingleton.getInstance();
                    volley.prefetchThumbnail(event.getThumbnailUrl());
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "JSONException: " + e);
                }
                eventList.add(event);
            }
            if (mOnResultsReceivedListener != null) {
                mOnResultsReceivedListener.onResultsReceived(eventList);
            }
        }
    }

    public static void runSearch(SearchOptions searchOptions, final OnResultsReceivedListener onResultsReceivedListener) {
        Uri.Builder builder = Uri.parse(getApiUrl("search")).buildUpon();
        builder.appendQueryParameter("location", searchOptions.location);
        builder.appendQueryParameter("keywords", searchOptions.keywords);
        builder.appendQueryParameter("distance", "10");
        builder.appendQueryParameter("distance_units", "miles");
        final Uri searchUri = builder.build();

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (onResultsReceivedListener != null) {
                    onResultsReceivedListener.onError(error);
                }
            }
        };

        JsonObjectRequest request = new JsonObjectRequest(
                searchUri.toString(),
                null,
                new SearchProcessor(onResultsReceivedListener),
                errorListener);

        Log.d(LOG_TAG, "Querying server feed: " + searchUri);
        request.setShouldCache(false);
        RequestQueue queue = VolleySingleton.getInstance().getRequestQueue();
        queue.add(request);
    }
}
