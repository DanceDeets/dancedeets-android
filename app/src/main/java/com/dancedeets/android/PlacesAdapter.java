package com.dancedeets.android;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

// Based on https://developers.google.com/places/training/autocomplete-android
public class PlacesAdapter extends ArrayAdapter implements Filterable {
    private ArrayList<String> mResultList;

    private static String LOG_TAG = "PlacesAdapter";

    public PlacesAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    @Override
    public int getCount() {
        return mResultList.size();
    }

    @Override
    public String getItem(int index) {
        return mResultList.get(index);
    }


    private ArrayList<String> lookupSuggestions(String input) {
        ArrayList<String> resultList = new ArrayList<>();
        Uri.Builder builder = Uri.parse("https://maps.googleapis.com/maps/api/place/autocomplete/json").buildUpon();

        // This is a browser key, which is public information. We fake the referer, so that Google lets us in.
        // Since android keys don't work, the alternative would be to proxy this through our dancedeets.com server,
        // and passing along a user id (or user id auth token) to verify the android app is legitimate,
        // and performing the autocomplete query on the client's behalf using the server key.
        // But this seems like a lot of unnecessary complexity, so we fake the referer and use a browser key here.
        builder.appendQueryParameter("key", "AIzaSyDEHGAeT9NkW-CvcaDMLbz4B6-abdvPi4I");
        builder.appendQueryParameter("input", input);
        builder.appendQueryParameter("types", "(regions)");
        builder.appendQueryParameter("language", Locale.getDefault().getLanguage());
        Uri uri = builder.build();

        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            URL url = new URL(uri.toString());
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Referer", "http://android-app.dancedeets.com/");
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // Load the results into a StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Error processing Places API URL", e);
            return resultList;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error connecting to Places API", e);
            return resultList;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {
            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = new JSONObject(jsonResults.toString());
            if (jsonObj.optString("error_message", null) != null) {
                Log.e(LOG_TAG, "Autocomplete Error (likely no results): " + jsonResults.toString());
            }
            JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");

            // Extract the Place descriptions from the results
            resultList = new ArrayList<>(predsJsonArray.length());
            for (int i = 0; i < predsJsonArray.length(); i++) {
                resultList.add(predsJsonArray.getJSONObject(i).getString("description"));
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Cannot process JSON results", e);
        }
        return resultList;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if (constraint != null) {
                    // Retrieve the google places results.
                    // TODO(lambert): We shouldn't be modifying mResultList from inside this thread,
                    // and should only do that from publishResults.
                    mResultList = lookupSuggestions(constraint.toString());

                    // Assign the data to the FilterResults
                    filterResults.values = mResultList;
                    filterResults.count = mResultList.size();
                }
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    notifyDataSetChanged();
                }
                else {
                    notifyDataSetInvalidated();
                }
            }};
    }
}