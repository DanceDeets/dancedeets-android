package com.dancedeets.android;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.dancedeets.android.models.FullEvent;

import java.util.List;

/**
 * An Adapter for mapping Event objects to the Event ListView UI.
 */
public class EventUIAdapter extends BaseAdapter {
    static class ViewBinder {
        NetworkImageView icon;
        NetworkImageView cover;
        TextView title;
        TextView location;
        TextView startTime;
    }
    private LayoutInflater mInflater;
    private List<FullEvent> mEventList;
    private int mResource;

    public EventUIAdapter(Context context, List<FullEvent> eventBundleList, int resource) {
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mEventList = eventBundleList;
        mResource = resource;

    }
    public int getCount() {
        return mEventList.size();
    }

    @Override
    public Object getItem(int position) {
        return mEventList.get(position);
    }

    @Override
    public long getItemId(int position) {
        // The facebook ID is not guaranteed to fit into a 'long',
        // so we use the hashcode of the string, which should be good enough for uniqueness.
        return mEventList.get(position).getId().hashCode();
    }

    @Override
    public boolean hasStableIds() {
        // Dependent on the getItemId implementation above being stable.
        return true;
    }

    protected void bindView(int position, View view) {
        FullEvent event = (FullEvent)getItem(position);
        ImageLoader thumbnailLoader = VolleySingleton.getInstance().getThumbnailLoader();

        ViewBinder viewBinder = (ViewBinder)view.getTag();
        if (viewBinder.icon != null) {
            viewBinder.icon.setImageUrl(event.getThumbnailUrl(), thumbnailLoader);
        }
        if (viewBinder.cover != null) {
            viewBinder.cover.setImageUrl(event.getCoverUrl(), thumbnailLoader);
        }
        viewBinder.title.setText(event.getTitle());
        if (event.getVenue().hasName()) {
            viewBinder.location.setText(event.getVenue().getName());
        } else {
            viewBinder.location.setText("");
        }
        viewBinder.startTime.setText(event.getStartTimeString());
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createViewFromResource(position, convertView, parent, mResource);
    }

    private View createViewFromResource(int position, View convertView,
                                        ViewGroup parent, int resource) {
        View view;
        if (convertView == null) {
            view = mInflater.inflate(resource, parent, false);
            ViewBinder viewBinder = new ViewBinder();
            viewBinder.icon = (NetworkImageView )view.findViewById(R.id.event_list_icon);
            //viewBinder.cover = (NetworkImageView )view.findViewById(R.id.event_list_cover);
            viewBinder.title = (TextView)view.findViewById(R.id.event_list_title);
            viewBinder.location = (TextView)view.findViewById(R.id.event_list_location);
            viewBinder.startTime = (TextView)view.findViewById(R.id.event_list_start_time);
            view.setTag(viewBinder);
        } else {
            view = convertView;
        }

        bindView(position, view);

        return view;
    }

}
