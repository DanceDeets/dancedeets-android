package com.dancedeets.android;

import android.net.Uri;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.crashlytics.android.Crashlytics;
import com.dancedeets.android.models.FullEvent;
import com.dancedeets.android.models.OneboxLink;
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
 * Wrapper for all API calls to the DanceDeets server.
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
            Crashlytics.log(Log.ERROR, LOG_TAG, "Error constructing request: " + e);
            Crashlytics.logException(e);
            return;
        }
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                builder.toString(),
                jsonPayload,
                new com.android.volley.Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Crashlytics.log(Log.INFO, LOG_TAG, "Successfully called /api/auth: " + response);
                    }
                },
                new com.android.volley.Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Crashlytics.log(Log.ERROR, LOG_TAG, "Error calling /api/auth: " + error);
                    }
                });
        VolleySingleton.getInstance().getRequestQueue().add(request);
    }


    public interface OnEventReceivedListener {
        void onEventReceived(FullEvent event);
        void onError(Exception exception);
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
                            Crashlytics.log(Log.ERROR, LOG_TAG, "Error reading from event api: " + e + ": " + response);
                            Crashlytics.logException(e);
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
                        Crashlytics.log(Log.ERROR, LOG_TAG, "Error retrieving data: " + error);
                        Crashlytics.logException(error);
                        if (onEventReceivedListener != null) {
                            onEventReceivedListener.onError(error);
                        }
                    }
                });
        VolleySingleton.getInstance().getRequestQueue().add(request);
    }


    public interface OnResultsReceivedListener {
        void onResultsReceived(List<FullEvent> eventList, List<OneboxLink> oneboxList);
        void onError(Exception exception);
    }

    static class SearchProcessor implements Response.Listener<JSONObject>, Response.ErrorListener {

        private final OnResultsReceivedListener mOnResultsReceivedListener;

        public SearchProcessor(OnResultsReceivedListener onResultsReceivedListener)
        {
            mOnResultsReceivedListener = onResultsReceivedListener;
        }
        @Override
        public void onResponse(JSONObject response) {
            List<FullEvent> eventList = new ArrayList<>();
            JSONArray jsonEventList;
            try {
                jsonEventList = response.getJSONArray("results");
            } catch (JSONException e) {
                Crashlytics.log(Log.ERROR, LOG_TAG, "JSONException: " + e);
                Crashlytics.logException(e);
                mOnResultsReceivedListener.onError(e);
                return;
            }
            for (int i = 0; i < jsonEventList.length(); i++) {
                FullEvent event = null;
                try {
                    JSONObject jsonEvent = jsonEventList.getJSONObject(i);
                    event = FullEvent.parse(jsonEvent);
                    // Prefetch the first few images, so scrolling "just works"
                    if (i < 20) {
                        VolleySingleton volley = VolleySingleton.getInstance();
                        volley.prefetchThumbnail(event.getThumbnailUrl());
                    }
                } catch (JSONException e) {
                    String eventId = "";
                    try {
                        eventId = " " + jsonEventList.getJSONObject(i).getString("id");
                    } catch (JSONException e2) {
                    }
                    Crashlytics.log(Log.ERROR, LOG_TAG, "JSONException on event" + eventId + ": " + e);
                    Crashlytics.logException(e);
                }
                eventList.add(event);
            }

            //TODO: Return a full Results object containing events and oneboxes
            List<OneboxLink> oneboxList = new ArrayList<>();
            JSONArray jsonOneboxList;
            try {
                jsonOneboxList = response.getJSONArray("onebox_links");
            } catch (JSONException e) {
                Crashlytics.log(Log.ERROR, LOG_TAG, "JSONException: " + e);
                Crashlytics.logException(e);
                // The oneboxes are optional, report it to the server but let it proceed here...
                jsonOneboxList = new JSONArray();
            }
            for (int i = 0; i < jsonOneboxList.length(); i++) {
                OneboxLink link = null;
                try {
                    JSONObject jsonObject = jsonOneboxList.getJSONObject(i);
                    link = OneboxLink.parse(jsonObject);
                } catch (JSONException e) {
                    String data = "";
                    try {
                        data = jsonOneboxList.getJSONObject(i).toString();
                    } catch (JSONException e2) {
                    }
                    Crashlytics.log(Log.ERROR, LOG_TAG, "JSONException on object " + data + ": " + e);
                    Crashlytics.logException(e);
                }
                oneboxList.add(link);
            }

            if (mOnResultsReceivedListener != null) {
                Crashlytics.log(Log.INFO, LOG_TAG, "Received " + eventList.size() + " results from server");
                mOnResultsReceivedListener.onResultsReceived(eventList, oneboxList);
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

        Crashlytics.log(Log.INFO, LOG_TAG, "Querying server feed: " + searchUri);
        request.setShouldCache(false);
        RequestQueue queue = VolleySingleton.getInstance().getRequestQueue();
        queue.add(request);
    }
}
