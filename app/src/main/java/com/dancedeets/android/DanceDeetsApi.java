package com.dancedeets.android;

import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.dancedeets.android.models.FullEvent;
import com.facebook.Session;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Created by lambert on 2014/12/24.
 */
public class DanceDeetsApi {

    private static String LOG_TAG = "DanceDeetsApi";

    private static String VERSION = "v1.0";

    private static DateFormat isoDateTimeFormatWithTZ = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    private static String getApiUrl(String apiPath) {
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
}
