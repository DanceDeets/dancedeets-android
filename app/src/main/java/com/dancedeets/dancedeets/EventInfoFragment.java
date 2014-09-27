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

    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    static final String LOG_TAG = "EventInfoFragment";

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
        ShareActionProvider shareActionProvider = (ShareActionProvider)shareItem.getActionProvider();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        String url = "Test URL"; //TODO
        intent.putExtra(Intent.EXTRA_TEXT, url);
        shareActionProvider.setShareIntent(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            case R.id.action_view_facebook:
                return true;
            case R.id.action_add_to_calendar:
                Bundle b = getArguments();
                Intent intent = new Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI);
                intent.putExtra(CalendarContract.Events.EVENT_LOCATION, b.getString("location"));
                intent.putExtra(CalendarContract.Events.TITLE, b.getString("title"));
                intent.putExtra(
                        CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                        b.getLong("begin_time"));
                intent.putExtra(
                        CalendarContract.EXTRA_EVENT_END_TIME,
                        b.getLong("end_time"));
                intent.putExtra(
                        CalendarContract.Events.DESCRIPTION,
                        b.getString("description"));
                if (isAvailable(getActivity(), intent)) {
                    startActivity(intent);
                } else {
                    Toast.makeText(getActivity(), "Could not find your Calendar!", Toast.LENGTH_SHORT);
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
        ImageLoader imageLoader = VolleySingleton.getInstance(null).getImageLoader();

        View rootView = inflater.inflate(R.layout.fragment_event_info,
                container, false);

        Bundle b = getArguments();

        NetworkImageView cover = (NetworkImageView)rootView.findViewById(R.id.cover);
        Log.i(LOG_TAG, "Received Bundle: " + b);
        cover.setImageUrl(b.getString("cover"), imageLoader);

        TextView title = (TextView)rootView.findViewById(R.id.title);
        title.setText(b.getString("title"));
        TextView location = (TextView)rootView.findViewById(R.id.location);
        location.setText(b.getString("location"));
        TextView description = (TextView)rootView.findViewById(R.id.description);
        description.setText(b.getString("description"));

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
