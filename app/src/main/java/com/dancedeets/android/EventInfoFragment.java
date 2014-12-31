package com.dancedeets.android;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.dancedeets.android.models.FullEvent;
import com.dancedeets.android.models.NamedPerson;
import com.dancedeets.android.models.Venue;
import com.dancedeets.android.uistate.BundledState;
import com.dancedeets.android.uistate.RetainedState;
import com.dancedeets.android.uistate.StateFragment;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;
import java.util.Locale;

public class EventInfoFragment extends StateFragment<
        EventInfoFragment.MyBundledState,
        RetainedState> {

    static protected class MyBundledState extends BundledState {
        FullEvent mEvent;
    }

    private static final String LOG_TAG = "EventInfoFragment";

    public EventInfoFragment() {
    }

    public FullEvent getEvent() {
        return mBundled.mEvent;
    }

    @Override
    public MyBundledState buildBundledState() {
        return new MyBundledState();
    }

    @Override
    public RetainedState buildRetainedState() {
        return new RetainedState();
    }

    @Override
    public String getUniqueTag() {
        FullEvent tempEvent = FullEvent.parse(getArguments());
        return LOG_TAG + tempEvent.getId();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.event_info_menu, menu);

        MenuItem shareItem = menu.findItem(R.id.action_share);

        // Set up ShareActionProvider shareIntent
        ShareActionProvider shareActionProvider = (ShareActionProvider) shareItem.getActionProvider();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        String url = formatShareText();
        intent.putExtra(Intent.EXTRA_TEXT, url);
        shareActionProvider.setShareIntent(intent);
    }

    protected String formatShareText() {
        String url = getEvent().getUrl();
        return url;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_view_map:
                // "geo:0,0?q=lat,lng(label)"
                // "geo:0,0?q=my+street+address"
                Venue venue = getEvent().getVenue();
                Venue.LatLong latLong = venue.getLatLong();
                String name = venue.getName();
                Uri mapUrl = Uri.parse("geo:" + latLong.getLatitude() + "," + latLong.getLongitude() + "?q=" + Uri.encode(name));
                Log.i(LOG_TAG, "map url is " + mapUrl);
                intent = new Intent(Intent.ACTION_VIEW, mapUrl);
                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                }
                return true;
            case R.id.action_view_facebook:
                Uri facebookUrl = Uri.parse(getEvent().getFacebookUrl());
                intent = new Intent(Intent.ACTION_VIEW, facebookUrl);
                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                }
                return true;
            case R.id.action_translate:
                translatePage();
                return true;
            case R.id.action_add_to_calendar:
                intent = new Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI);
                intent.putExtra(CalendarContract.Events.EVENT_LOCATION, getEvent().getLocation());
                intent.putExtra(CalendarContract.Events.TITLE, getEvent().getTitle());
                intent.putExtra(
                        CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                        getEvent().getStartTimeLong());
                intent.putExtra(
                        CalendarContract.EXTRA_EVENT_END_TIME,
                        getEvent().getEndTimeLong());
                intent.putExtra(
                        CalendarContract.Events.DESCRIPTION,
                        getEvent().getDescription());
                if (isAvailable(getActivity(), intent)) {
                    startActivity(intent);
                } else {
                    Toast.makeText(getActivity(), "Could not find your Calendar!", Toast.LENGTH_SHORT).show();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    static class TranslateEvent implements Response.Listener<JSONArray>, Response.ErrorListener {

        RetainedState mRetainedState;

        public TranslateEvent(RetainedState retainedState) {
            mRetainedState = retainedState;
        }

        @Override
        public void onResponse(JSONArray response) {
            EventInfoFragment eventInfoFragment = (EventInfoFragment) mRetainedState.getTargetFragment();
            TextView titleView = (TextView) eventInfoFragment.getView().findViewById(R.id.title);
            TextView descriptionView = (TextView) eventInfoFragment.getView().findViewById(R.id.description);
            try {
                titleView.setText(response.getJSONArray(0).getString(0));
                descriptionView.setText(response.getJSONArray(1).getString(0));
            } catch (JSONException error) {
                Log.e(LOG_TAG, "Translation failed: " + error);
                Toast.makeText(mRetainedState.getActivity().getBaseContext(), "Failed to translate!", Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            Log.e(LOG_TAG, "Translation failed: " + error);
            Toast.makeText(mRetainedState.getActivity().getBaseContext(), "Failed to translate!", Toast.LENGTH_LONG).show();
        }
    }

    public void translatePage() {
        Uri translateUrl = Uri.parse("https://translate.google.com/translate_a/t").buildUpon()
                // I believe this stands for Translate Android
                .appendQueryParameter("client", "ta")
                // version 1.0 returns simple json output, while 2.0 returns more complex data used by the Translate app
                .appendQueryParameter("v", "1.0")
                // This is necessary, or Google Translate will mistakenly interpret utf8-encoded text as Shift_JIS, and return garbage.
                .appendQueryParameter("ie", "UTF-8")
                // And really, there's no reason to return data as Shift_JIS either, just creates more room for error.
                .appendQueryParameter("oe", "UTF-8")
                // Source language unknown, and target language is always the locale's language
                .appendQueryParameter("sl", "auto")
                .appendQueryParameter("tl", Locale.getDefault().getLanguage())
                .build();// android language
        RequestQueue queue = VolleySingleton.getInstance().getRequestQueue();
        TextView titleView = (TextView) getView().findViewById(R.id.title);
        TextView descriptionView = (TextView) getView().findViewById(R.id.description);
        String body = "q=" + Uri.encode(titleView.getText().toString())
                + "&q=" + Uri.encode(descriptionView.getText().toString());

        TranslateEvent translateEventListener = new TranslateEvent(mRetained);
        JsonArrayRequest jsonRequest = new JsonArrayRequest(
                Request.Method.POST,
                translateUrl.toString(),
                body,
                translateEventListener,
                translateEventListener);
        jsonRequest.setShouldCache(false);
        queue.add(jsonRequest);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_event_info,
                container, false);
        FullEvent event = FullEvent.parse(getArguments());
        mBundled.mEvent = event;
        fillOutView(rootView, event);
        return rootView;
    }

    public void fillOutView(View rootView, FullEvent event) {
        List<NamedPerson> adminList = event.getAdmins();
        Log.i(LOG_TAG, "admin list: "+adminList);
        ImageLoader photoLoader = VolleySingleton.getInstance().getPhotoLoader();
        NetworkImageView cover = (NetworkImageView) rootView.findViewById(R.id.cover);
        cover.setImageUrl(event.getCoverUrl(), photoLoader);
        cover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(LOG_TAG, "Creating intent for flyer view.");
                Intent detailIntent = new Intent(getActivity(), ViewFlyerActivity.class);
                detailIntent.putExtras(getEvent().getBundle());
                startActivity(detailIntent);
            }
        });

        TextView title = (TextView) rootView.findViewById(R.id.title);
        title.setText(event.getTitle());
        TextView location = (TextView) rootView.findViewById(R.id.location);
        location.setText(event.getLocation());
        TextView startTime  = (TextView) rootView.findViewById(R.id.start_time);
        startTime.setText(event.getStartTimeString());
        TextView description = (TextView) rootView.findViewById(R.id.description);
        description.setText(event.getDescription());
    }

    /* check if intent is available */
    public static boolean isAvailable(Context ctx, Intent intent) {
        final PackageManager mgr = ctx.getPackageManager();
        List<ResolveInfo> list =
        mgr.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

}
