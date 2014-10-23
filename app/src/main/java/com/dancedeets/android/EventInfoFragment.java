package com.dancedeets.android;

import android.app.Fragment;
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
import android.view.animation.AnimationUtils;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.NetworkImageView;
import com.dancedeets.android.models.FullEvent;
import com.dancedeets.android.models.IdEvent;
import com.dancedeets.android.models.NamedPerson;
import com.dancedeets.android.models.Venue;
import com.dancedeets.dancedeets.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

public class EventInfoFragment extends Fragment {

    private static final String LOG_TAG = "EventInfoFragment";

    private static final String STATE_EVENT = "STATE_EVENT";

    protected FullEvent mEvent;

    protected View mRootView;
    protected View mProgressContainer;
    protected View mEventInfoContainer;
    protected ShareActionProvider mShareActionProvider;
    protected JsonObjectRequest mDataRequest;
    private OnEventReceivedListener mOnEventReceivedListener;

    public interface OnEventReceivedListener {
        public void onEventReceived(FullEvent event);
    }

    public EventInfoFragment() {
    }

    public FullEvent getEvent() {
        return mEvent;
    }

    public void setOnEventReceivedListener(OnEventReceivedListener mOnEventReceivedListener) {
        this.mOnEventReceivedListener = mOnEventReceivedListener;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        // If we restored this fragment, and grabbed mEvent from a bundle
        // then mDataRequest is never initialized.
        if (mDataRequest != null) {
            // We may have a pending data request when this gets destroyed...
            // This ensures the callback isn't called on a dead Fragment reference.
            mDataRequest.cancel();
        }
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.event_info_menu, menu);

        MenuItem shareItem = menu.findItem(R.id.action_share);
        mShareActionProvider = (ShareActionProvider) shareItem.getActionProvider();
        // Sometimes the event gets loaded before the share menu is set up,
        // so this check handles that possibility and ensures the share intent is set.
        if (mEvent != null) {
            setUpShareIntent();
        }
    }

    protected void setUpShareIntent() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        String url = formatShareText();
        intent.putExtra(Intent.EXTRA_TEXT, url);
        // Sometimes we receive an event so quickly, it happens before the menu can be shown...
        // so we cannot rely on mShareActionProvider here. Maybe we should get rid of the
        mShareActionProvider.setShareIntent(intent);
    }

    protected String formatShareText() {
        String url = mEvent.getUrl();
        return url;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            case R.id.action_view_map:
                // "geo:0,0?q=lat,lng(label)"
                // "geo:0,0?q=my+street+address"
                Venue venue = mEvent.getVenue();
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
                Uri facebookUrl = Uri.parse(mEvent.getFacebookUrl());
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
                intent.putExtra(CalendarContract.Events.EVENT_LOCATION, mEvent.getLocation());
                intent.putExtra(CalendarContract.Events.TITLE, mEvent.getTitle());
                intent.putExtra(
                        CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                        mEvent.getStartTimeLong());
                intent.putExtra(
                        CalendarContract.EXTRA_EVENT_END_TIME,
                        mEvent.getEndTimeLong());
                intent.putExtra(
                        CalendarContract.Events.DESCRIPTION,
                        mEvent.getDescription());
                if (isAvailable(getActivity(), intent)) {
                    startActivity(intent);
                } else {
                    Toast.makeText(getActivity(), "Could not find your Calendar!", Toast.LENGTH_SHORT).show();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void translatePage() {
        Uri translateUrl = Uri.parse("https://translate.google.com/translate_a/t?client=at&v=1.0").buildUpon()
                .appendQueryParameter("sl", "auto")
                .appendQueryParameter("tl", Locale.getDefault().getLanguage())
                .build();// android language
        RequestQueue queue = VolleySingleton.getInstance().getRequestQueue();
        TextView titleView = (TextView) getView().findViewById(R.id.title);
        TextView descriptionView = (TextView) getView().findViewById(R.id.description);
        String body = "q=" + Uri.encode(titleView.getText().toString())
                + "&q=" + Uri.encode(descriptionView.getText().toString());

        JsonArrayRequest jsonRequest = new JsonArrayRequest(
                Request.Method.POST,
                translateUrl.toString(),
                body,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Log.i(LOG_TAG, "YAY" + response);
                        TextView titleView = (TextView) getView().findViewById(R.id.title);
                        TextView descriptionView = (TextView) getView().findViewById(R.id.description);
                        try {
                            titleView.setText(response.getJSONArray(0).getString(0));
                            descriptionView.setText(response.getJSONArray(1).getString(0));
                        } catch (JSONException error) {
                            Log.e(LOG_TAG, "Translation failed: " + error);
                            Toast.makeText(getActivity().getBaseContext(), "Failed to translate!", Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(LOG_TAG, "Translation failed: " + error);
                        Toast.makeText(getActivity().getBaseContext(), "Failed to translate!", Toast.LENGTH_LONG).show();
                    }
                });
        jsonRequest.setShouldCache(false);
        queue.add(jsonRequest);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_event_info,
                container, false);
        mProgressContainer = mRootView.findViewById(R.id.progress_container);
        mEventInfoContainer = mRootView.findViewById(R.id.event_info);
        // Show the progress bar until we receive data
        mProgressContainer.setVisibility(View.VISIBLE);
        mEventInfoContainer.setVisibility(View.GONE);

        return mRootView;
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(STATE_EVENT, mEvent);
    }

    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            FullEvent event = (FullEvent) savedInstanceState.getSerializable(STATE_EVENT);
            // Sometimes we rotate the screen before the event can be loaded.
            // Let's not mark it as received here...but instead let natural event loading happen.
            if (event != null) {
                onEventReceived(event, false);
            } else {
                loadEventFromArguments();
            }
        } else {
            loadEventFromArguments();
        }
    }

    public void loadEventFromArguments() {
        IdEvent tempEvent = IdEvent.parse(getArguments());

        Log.i(LOG_TAG, "Retrieving: " + tempEvent.getApiDataUrl());
        mDataRequest = new JsonObjectRequest(
                tempEvent.getApiDataUrl(),
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        FullEvent event;
                        try {
                            event = FullEvent.parse(response);
                        } catch (JSONException e) {
                            Log.e(LOG_TAG, "Error reading from event api: " + e + ": " + response);
                            return;
                        }
                        onEventReceived(event, true);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(LOG_TAG, "Error retrieving data: " + error);
                    }
                });
        VolleySingleton.getInstance().getRequestQueue().add(mDataRequest);
    }

    public void onEventReceived(FullEvent event, boolean animate) {

        mEvent = event;
        if (mOnEventReceivedListener != null) {
            mOnEventReceivedListener.onEventReceived(event);
        }

        setUpView();
        // Only call this if we've set up the menu already..
        // otherwise, we set it up later, at the same time as the menu
        if (mShareActionProvider != null) {
            setUpShareIntent();
        }
        if (animate) {
            mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                    getActivity(), android.R.anim.fade_out));
            mEventInfoContainer.startAnimation(AnimationUtils.loadAnimation(
                    getActivity(), android.R.anim.fade_in));
        }
        mProgressContainer.setVisibility(View.GONE);
        mEventInfoContainer.setVisibility(View.VISIBLE);
    }

    public String getTitle() {
        if (mEvent != null) {
            return mEvent.getTitle();
        }
        return null;
    }

    public void setUpView() {
        List<NamedPerson> adminList = mEvent.getAdmins();
        Log.i(LOG_TAG, "admin list: "+adminList);
        ImageLoader photoLoader = VolleySingleton.getInstance().getPhotoLoader();
        NetworkImageView cover = (NetworkImageView) mRootView.findViewById(R.id.cover);
        cover.setImageUrl(mEvent.getCoverUrl(), photoLoader);
        cover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(LOG_TAG, "Creating intent for flyer view.");
                Intent detailIntent = new Intent(getActivity(), ViewFlyerActivity.class);
                detailIntent.putExtras(mEvent.getBundle());
                startActivity(detailIntent);
            }
        });

        TextView title = (TextView) mRootView.findViewById(R.id.title);
        title.setText(mEvent.getTitle());
        TextView location = (TextView) mRootView.findViewById(R.id.location);
        location.setText(mEvent.getLocation());
        TextView startTime  = (TextView) mRootView.findViewById(R.id.start_time);
        startTime.setText(mEvent.getStartTimeString());
        TextView description = (TextView) mRootView.findViewById(R.id.description);
        // TODO: Somehow linkify the links in description?
        // http://developer.android.com/reference/android/text/util/Linkify.html
        description.setText(mEvent.getDescription());
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
