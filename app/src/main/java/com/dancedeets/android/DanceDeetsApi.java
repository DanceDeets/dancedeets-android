package com.dancedeets.android;

import android.net.Uri;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.dancedeets.android.models.FullEvent;
import com.facebook.AccessToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by lambert on 2014/12/24.
 */
public class DanceDeetsApi {

    private static String LOG_TAG = "DanceDeetsApi";

    private static String VERSION = "v1.1";

    private static DateFormat isoDateTimeFormatWithTZ = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    private static Uri.Builder generateApiBuilderFor(String apiPath) {
        String url = "http://www.dancedeets.com/api/" + VERSION + "/" + apiPath;
        Uri.Builder builder = Uri.parse(url).buildUpon();
        builder.appendQueryParameter("hl", Locale.getDefault().getLanguage());
        //TODO: Figure out if we want to use json payloads, query-param payloads, or get params
        // But for now, this is the easiest way to pass this info on every request.
        builder.appendQueryParameter("client", "android");
        return builder;
    }

    public static void sendAuth(AccessToken accessToken, String location) {
        Log.i(LOG_TAG, "sendAuth with location: " + location);
        Uri.Builder builder = generateApiBuilderFor("auth");
        JSONObject jsonPayload = new JSONObject();
        try {
            jsonPayload.put("access_token", accessToken.getToken());
            // Sometimes the cached payload has a far-future expiration date. Not really sure why...
            // Best I can come up with is AccessToken.getBundleLongAsDate()'s Long.MAX_VALUE result
            // must somehow have gotten cached to disk with that large value for the expiration.
            // Alternately, these may represent infinite-duration tokens that never expire.
            if (!accessToken.getExpires().equals(new Date(Long.MAX_VALUE))) {
                jsonPayload.put("access_token_expires", isoDateTimeFormatWithTZ.format(accessToken.getExpires()));
            } else {
                Log.e(LOG_TAG, "Somehow had far-future expiration date, ignoring: " + accessToken.getExpires());
            }
            jsonPayload.put("location", location);
            jsonPayload.put("client", "android");
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error constructing request: " + e);
            return;
        }
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                builder.toString(),
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
        Uri.Builder builder = generateApiBuilderFor("events/" + id);
        JsonObjectRequest request = new JsonObjectRequest(
                builder.toString(),
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

    static class SearchProcessor implements Response.Listener<JSONObject>, Response.ErrorListener {

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
                    // Prefetch the first few images, so scrolling "just works"
                    if (i < 100) {
                        VolleySingleton volley = VolleySingleton.getInstance();
                        volley.prefetchThumbnail(event.getThumbnailUrl());
                    }
                } catch (JSONException e) {
                    String eventId = "";
                    try {
                        eventId = " " + jsonEventList.getJSONObject(i).getString("id");
                    } catch (JSONException e2) {
                    }
                    Log.e(LOG_TAG, "JSONException on event" + eventId + ": " + e);
                }
                eventList.add(event);
            }
            if (mOnResultsReceivedListener != null) {
                Log.i(LOG_TAG, "Received " + eventList.size() + " results from server");
                mOnResultsReceivedListener.onResultsReceived(eventList);
            }
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            if (mOnResultsReceivedListener != null) {
                mOnResultsReceivedListener.onError(error);
            }
        }
    }

    public static void runSearch(SearchOptions searchOptions, final OnResultsReceivedListener onResultsReceivedListener) {
        Uri.Builder builder = generateApiBuilderFor("search");
        builder.appendQueryParameter("location", searchOptions.location);
        builder.appendQueryParameter("keywords", searchOptions.keywords);
        builder.appendQueryParameter("time_period", searchOptions.timePeriod.toString());
        builder.appendQueryParameter("distance", "10");
        builder.appendQueryParameter("distance_units", "miles");
        final Uri searchUri = builder.build();

        SearchProcessor searchProcessor = new SearchProcessor(onResultsReceivedListener);

        JsonObjectRequest request = new JsonObjectRequest(
                searchUri.toString(),
                null,
                searchProcessor,
                searchProcessor);

        Log.d(LOG_TAG, "Querying server feed: " + searchUri);
        request.setShouldCache(false);
        RequestQueue queue = VolleySingleton.getInstance().getRequestQueue();
        queue.add(request);
    }
}
