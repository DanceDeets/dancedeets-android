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

        // Tell analytics if someone hits the share button
/*
        ShareActionProvider shareActionProvider =
        shareActionProvider.setOnShareTargetSelectedListener(
                new ShareActionProvider.OnShareTargetSelectedListener() {
                    @Override
                    public boolean onShareTargetSelected(ShareActionProvider shareActionProvider,
                                                         Intent intent) {
                        //AnalyticsUtil.logAction(
                        //        MainActivity .this, "sharing", "main-activity-action-bar");
                        //return false;
                    }
                });
*/
        shareItem.setActionProvider(new ShareActionProvider(getActivity()));

        // Set up ShareActionProvider shareIntent
        ShareActionProvider shareActionProvider = (ShareActionProvider) shareItem.getActionProvider();
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
        Venue.LatLong latLong = venue.getLatLong();
        /**
         * We must support a few use cases:
         * 1) Venue Name: Each One Teach One
         * Street/City/State/Zip/Country: Lehman College 250 Bedford Prk Blvd Speech & Theatre Bldg the SET Room B20, Bronx, NY, 10468, United States
         * Lat, Long: 40.8713753364, -73.8879763323
         * 2) Venue Name: Queens Theatre in the Park
         * Street/City/State/Zip/Country: New York, NY, 11368, United States
         * Lat, Long: 40.7441611111, -73.8444222222
         * 3) More normal scenarios, like a good venue and street address
         *
         * Given this, our most reliable source is lat/long.
         * We don't want to do a search around it because of #1 and #2 will search for the wrong things.
         * So instead, the best we can do is to label the lat/long point
         **/
        Uri mapUrl = Uri.parse("geo:0,0?q=" + latLong.getLatitude() + "," + latLong.getLongitude() + "(" + Uri.encode(venue.getName())+")");
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
                openLocationOnMap();
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
        // Set location View to be linkable text

        SpannableString ss = new SpannableString(event.getLocation());
        ss.setSpan(new URLSpan("#"), 0, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        location.setText(ss, TextView.BufferType.SPANNABLE);
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
