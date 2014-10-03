package com.dancedeets.dancedeets;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

import java.util.List;

public class EventInfoFragment extends Fragment {

    static final String LOG_TAG = "EventInfoFragment";

    protected Event mEvent;

    public EventInfoFragment() {
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
        ShareActionProvider shareActionProvider = (ShareActionProvider) shareItem.getActionProvider();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        String url = formatShareText();
        intent.putExtra(Intent.EXTRA_TEXT, url);
        shareActionProvider.setShareIntent(intent);
    }

    public String formatShareText() {
        String url = mEvent.getUrl();
        return url;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            case R.id.action_view_facebook:
                return true;
            case R.id.action_add_to_calendar:
                Intent intent = new Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI);
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
                //case R.id.action_view_map:
            //    return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ImageLoader photoLoader = VolleySingleton.getInstance(null).getPhotoLoader();
        View rootView = inflater.inflate(R.layout.fragment_event_info,
                container, false);

        mEvent = new Event(getArguments());

        NetworkImageView cover = (NetworkImageView) rootView.findViewById(R.id.cover);
        Log.i(LOG_TAG, "Received Bundle: " + mEvent);
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

        TextView title = (TextView) rootView.findViewById(R.id.title);
        title.setText(mEvent.getTitle());
        TextView location = (TextView) rootView.findViewById(R.id.location);
        location.setText(mEvent.getLocation());
        TextView startTime  = (TextView) rootView.findViewById(R.id.start_time);
        startTime.setText(mEvent.getStartTimeString());
        TextView description = (TextView) rootView.findViewById(R.id.description);
        // TODO: Somehow linkify the links in description? Maybe I need to use a web view?
        description.setText(mEvent.getDescription());

        return rootView;
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
