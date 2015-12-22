package com.dancedeets.android.geo;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Based on https://developers.google.com/places/training/autocomplete-android
public class PlacesAdapter extends BaseAdapter implements Filterable {
    private final LayoutInflater mInflater;
    private final int mResource;
    private List<String> mResultList;

    private static String LOG_TAG = "PlacesAdapter";

    public PlacesAdapter(Context context, int resource) {
        super();
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mResource = resource;
        mResultList = new ArrayList<>();
    }

    @Override
    public int getCount() {
        return mResultList.size();
    }

    @Override
    public String getItem(int index) {
        return mResultList.get(index);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createViewFromResource(position, convertView, parent, mResource);
    }

    private View createViewFromResource(int position, View convertView, ViewGroup parent,
                                        int resource) {
        View view;

        if (convertView == null) {
            view = mInflater.inflate(resource, parent, false);
        } else {
            view = convertView;
        }

        String item = getItem(position);
        ((TextView) view).setText(item);
        return view;
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
            Crashlytics.log(Log.ERROR, LOG_TAG, "Cannot process JSON results: " + e);
            Crashlytics.logException(e);
        }
        return resultList;
    }

    @Override
    public Filter getFilter() {
        // This Filter runs in another thread, which allows us to do long-running operations
        // to fetch the filtered results. (In this case, by loading from Google Places API).
        // But this background thread cannot touch mResultList, but must instead communicate
        // via returning from performFiltering to pass to publishResults.
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if (constraint != null) {
                    // Retrieve the Places suggestions.
                    // Don't modify mResultList, let publishResults do that in the UI thread.
                    List<String> resultList = lookupSuggestions(constraint.toString());

                    // Assign the data to the FilterResults
                    filterResults.values = resultList;
                    filterResults.count = resultList.size();
                } else {
                    // Don't allow null lists to leak out to publishResults when it sets mResultList,
                    // or it will cause a crash later when getCount is called.
                    filterResults.values = new ArrayList<>();
                    filterResults.count = 0;
                }
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                //noinspection unchecked
                mResultList = (List<String>) results.values;
                if (results.count > 0) {
                    notifyDataSetChanged();
                }
                else {
                    notifyDataSetInvalidated();
                }
            }
        };
    }
}