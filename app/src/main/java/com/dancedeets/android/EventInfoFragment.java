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
import com.dancedeets.android.uistate.BundledState;
import com.dancedeets.android.uistate.RetainedState;
import com.dancedeets.android.uistate.StateFragment;
import com.dancedeets.dancedeets.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

public class EventInfoFragment extends StateFragment<
        EventInfoFragment.MyBundledState,
        EventInfoFragment.MyRetainedState> {

    static protected class MyBundledState extends BundledState {
        FullEvent mEvent;
    }
    static public class MyRetainedState extends RetainedState {
        JsonObjectRequest mDataRequest;
    }


    protected View mRootView;
    protected View mProgressContainer;
    protected View mEventInfoContainer;
    protected ShareActionProvider mShareActionProvider;

    private OnEventReceivedListener mOnEventReceivedListener;

    private static final String LOG_TAG = "EventInfoFragment";

    public interface OnEventReceivedListener {
        public void onEventReceived(FullEvent event);
    }

    public EventInfoFragment() {
    }

    public FullEvent getEvent() {
        return mBundled.mEvent;
    }

    public void setOnEventReceivedListener(OnEventReceivedListener onEventReceivedListener) {
        mOnEventReceivedListener = onEventReceivedListener;
    }


    @Override
    public MyBundledState buildBundledState() {
        return new MyBundledState();
    }

    @Override
    public MyRetainedState buildRetainedState() {
        return new MyRetainedState();
    }

    @Override
    public String getUniqueTag() {
        IdEvent tempEvent = IdEvent.parse(getArguments());
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
        mShareActionProvider = (ShareActionProvider) shareItem.getActionProvider();
        // Sometimes the event gets loaded before the share menu is set up,
        // so this check handles that possibility and ensures the share intent is set.
        if (getEvent() != null) {
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
        String url = getEvent().getUrl();
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

    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getEvent() != null) {
            onEventReceived(getEvent(), false);
        } else {
            loadEventFromArguments();
        }
    }

    // This is done in a static method, so there are no references to this Fragment leaked
    private static JsonObjectRequest constructEventRequest(String url, final RetainedState retainedState) {
        return new JsonObjectRequest(
                url,
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
                        // Sometimes the retainedState keeps a targetFragment even after it's detached,
                        // since I can't hook into the lifecycle at the right point in time.
                        // So double-check it's safe here first...
                        if (retainedState.getTargetFragment().getActivity() != null) {
                            ((EventInfoFragment) retainedState.getTargetFragment()).onEventReceived(event, true);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(LOG_TAG, "Error retrieving data: " + error);
                    }
                });
    }

    public void loadEventFromArguments() {
        IdEvent tempEvent = IdEvent.parse(getArguments());

        Log.i(LOG_TAG, "Retrieving: " + tempEvent.getApiDataUrl());
        mRetained.mDataRequest = constructEventRequest(tempEvent.getApiDataUrl(), mRetained);
        VolleySingleton.getInstance().getRequestQueue().add(mRetained.mDataRequest);
    }

    public void onEventReceived(FullEvent event, boolean animate) {
        mBundled.mEvent = event;
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
        if (getEvent() != null) {
            return getEvent().getTitle();
        }
        return null;
    }

    public void setUpView() {
        List<NamedPerson> adminList = getEvent().getAdmins();
        Log.i(LOG_TAG, "admin list: "+adminList);
        ImageLoader photoLoader = VolleySingleton.getInstance().getPhotoLoader();
        NetworkImageView cover = (NetworkImageView) mRootView.findViewById(R.id.cover);
        cover.setImageUrl(getEvent().getCoverUrl(), photoLoader);
        cover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(LOG_TAG, "Creating intent for flyer view.");
                Intent detailIntent = new Intent(getActivity(), ViewFlyerActivity.class);
                detailIntent.putExtras(getEvent().getBundle());
                startActivity(detailIntent);
            }
        });

        TextView title = (TextView) mRootView.findViewById(R.id.title);
        title.setText(getEvent().getTitle());
        TextView location = (TextView) mRootView.findViewById(R.id.location);
        location.setText(getEvent().getLocation());
        TextView startTime  = (TextView) mRootView.findViewById(R.id.start_time);
        startTime.setText(getEvent().getStartTimeString());
        TextView description = (TextView) mRootView.findViewById(R.id.description);
        description.setText(getEvent().getDescription());
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
