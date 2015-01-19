package com.dancedeets.android;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.URLSpan;
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
import com.dancedeets.android.models.LatLong;
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
        String mTranslatedTitle;
        String mTranslatedDescription;
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

        ShareActionProvider shareActionProvider = (ShareActionProvider)shareItem.getActionProvider();
        // Track share item clicks
        shareActionProvider.setOnShareTargetSelectedListener(
                new ShareActionProvider.OnShareTargetSelectedListener() {
                    @Override
                    public boolean onShareTargetSelected(ShareActionProvider shareActionProvider,
                                                         Intent intent) {
                        AnalyticsUtil.trackEvent("Share Event", mBundled.mEvent);
                        return false;
                    }
                });

        // Set up ShareActionProvider shareIntent
        Intent intent = new Intent(Intent.ACTION_SEND);
        // We need to keep this as text/plain, not text/html, so we get the full set of apps to share to.
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, EventSharing.getTitle(getEvent()));
        intent.putExtra(Intent.EXTRA_TEXT, EventSharing.getBodyText(getEvent()));
        shareActionProvider.setShareIntent(intent);
    }

    public void openLocationOnMap() {
        // "geo:lat,lng?q=query
        // "geo:0,0?q=lat,lng(label)"
        // "geo:0,0?q=my+street+address"
        Venue venue = getEvent().getVenue();
        LatLong latLong = venue.getLatLong();
        /**
         * We must support a few use cases:
         * 1) Venue Name: Each One Teach One
         * Street/City/State/Zip/Country: Lehman College 250 Bedford Prk Blvd Speech & Theatre Bldg the SET Room B20, Bronx, NY, 10468, United States
         * Lat, Long: 40.8713753364, -73.8879763323
         * 2) Venue Name: Queens Theatre in the Park
         * Street/City/State/Zip/Country: New York, NY, 11368, United States
         * Lat, Long: 40.7441611111, -73.8444222222
         * 3) Venue Name: "Hamburg"
         * Street/City/State/Zip/Country: null
         * Lat, Long: null
         * 4) More normal scenarios, like a good venue and street address
         *
         * Given this, our most reliable source is lat/long.
         * We don't want to do a search around it because of #1 and #2 will search for the wrong things.
         * So instead, the best we can do is to label the lat/long point
         **/
        Uri mapUrl;
        if (latLong != null) {
            mapUrl = Uri.parse("geo:0,0?q=" + latLong.getLatitude() + "," + latLong.getLongitude() + "(" + Uri.encode(getEvent().getVenue().getName()) + ")");
        } else {
            mapUrl = Uri.parse("geo:0,0?q=" + Uri.encode(getEvent().getVenue().getName()));
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, mapUrl);
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_view_map:
                AnalyticsUtil.trackEvent("View on Map", mBundled.mEvent);
                openLocationOnMap();
                return true;
            case R.id.action_view_facebook:
                AnalyticsUtil.trackEvent("Open in Facebook", mBundled.mEvent);
                Uri facebookUrl = Uri.parse(getEvent().getFacebookUrl());
                intent = new Intent(Intent.ACTION_VIEW, facebookUrl);
                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                }
                return true;
            case R.id.action_translate:
                AnalyticsUtil.trackEvent("Translate", mBundled.mEvent);
                translatePage();
                return true;
            case R.id.action_add_to_calendar:
                AnalyticsUtil.trackEvent("Add to Calendar", mBundled.mEvent);
                intent = new Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI);
                Log.i(LOG_TAG, "Loc " + getEvent().getVenue() + ": " + getEvent().getVenue().hasName() + " : " + getEvent().getVenue().getName());

                String address = getEvent().getVenue().getAddress();
                if (getEvent().getVenue().hasName()) {
                    address = getEvent().getVenue().getName() + ", " + address;
                }
                intent.putExtra(CalendarContract.Events.EVENT_LOCATION, address);
                intent.putExtra(CalendarContract.Events.TITLE, getEvent().getTitle());
                Log.i(LOG_TAG, "Start time " + getEvent().getStartTimeString() + ": " + getEvent().getStartTimeLong());
                intent.putExtra(
                        CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                        getEvent().getStartTimeLong());
                if (getEvent().getEndTimeLong() != 0) {
                    intent.putExtra(
                            CalendarContract.EXTRA_EVENT_END_TIME,
                            getEvent().getEndTimeLong());
                } else {
                    // HACK to force an end-time, since Sunrise Calendaring doesn't handle missing-end-time intents very well
                    intent.putExtra(
                            CalendarContract.EXTRA_EVENT_END_TIME,
                            getEvent().getStartTimeLong() + 2*60*60*1000);
                }
                String description = getEvent().getUrl() + "\n\n" + getEvent().getDescription();
                intent.putExtra(
                        CalendarContract.Events.DESCRIPTION,
                        description);
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
            try {
                eventInfoFragment.mBundled.mTranslatedTitle = response.getJSONArray(0).getString(0);
                eventInfoFragment.mBundled.mTranslatedDescription = response.getJSONArray(1).getString(0);
                eventInfoFragment.swapTitleAndDescription();
            } catch (JSONException error) {
                Log.e(LOG_TAG, "Translation failed: " + error);
                Toast.makeText(mRetainedState.getActivity().getBaseContext(), "Failed to translate! " + error, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            Log.e(LOG_TAG, "Translation failed: " + error);
            Toast.makeText(mRetainedState.getActivity().getBaseContext(), "Failed to translate! " + error, Toast.LENGTH_LONG).show();
        }
    }

    public void swapTitleAndDescription() {
        TextView titleView = (TextView) getView().findViewById(R.id.title);
        TextView descriptionView = (TextView) getView().findViewById(R.id.description);

        CharSequence oldTitle = titleView.getText();
        CharSequence oldDescription = descriptionView.getText();

        titleView.setText(mBundled.mTranslatedTitle);
        descriptionView.setText(mBundled.mTranslatedDescription);

        mBundled.mTranslatedTitle = oldTitle.toString();
        mBundled.mTranslatedDescription = oldDescription.toString();
    }

    public void translatePage() {
        if (mBundled.mTranslatedTitle != null) {
            // Restore old translated content
            swapTitleAndDescription();
        } else {
            Uri translateUrl = Uri.parse("https://translate.google.com/translate_a/t").buildUpon()
                    // I believe this stands for Translate Android
                    .appendQueryParameter("client", "ta")
                    // version 1.0 returns simple json output, while 2.0 returns more complex data used by the Translate app
                    .appendQueryParameter("v", "1.0")
                    // This is necessary, or Google Translate will mistakenly interpret utf8-encoded text as Shift_JIS, and return garbage.
                    .appendQueryParameter("ie", "UTF-8")
                    // And really, there's no reason to return data as Shift_JIS either, just creates more room for error.
                    .appendQueryParameter("oe", "UTF-8")
                    // Source language unknown
                    .appendQueryParameter("sl", "auto")
                    // And target language is always the locale's language
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
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_event_info,
                container, false);
        FullEvent event = FullEvent.parse(getArguments());
        mBundled.mEvent = event;
        AnalyticsUtil.trackEvent("View Event", mBundled.mEvent);
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

        if (event.getVenue().hasName()) {
            // Set location View to be linkable text
            SpannableString ss = new SpannableString(event.getVenue().getName());
            ss.setSpan(new URLSpan("#"), 0, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            location.setText(ss, TextView.BufferType.SPANNABLE);
        } else {
            location.setText("");
        }
        location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLocationOnMap();
            }
        });
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
