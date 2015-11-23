package com.dancedeets.android.eventlist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.dancedeets.android.PlaceholderNetworkImageView;
import com.dancedeets.android.R;
import com.dancedeets.android.VolleySingleton;
import com.dancedeets.android.models.CoverData;
import com.dancedeets.android.models.FullEvent;

/**
 * Created by lambert on 2015/11/08.
 */
public class EventListItem implements ListItem {

    private final LayoutInflater mInflater;
    private FullEvent mEvent;

    static class ViewBinder {
        NetworkImageView icon;
        PlaceholderNetworkImageView cover;
        TextView title;
        TextView location;
        TextView startTime;
        TextView categories;
    }

    public EventListItem(LayoutInflater inflater, FullEvent event) {
        mInflater = inflater;
        mEvent = event;
    }

    public int getItemId() {
        // The facebook ID is not guaranteed to fit into a 'long',
        // so we use the hashcode of the string, which should be good enough for uniqueness.
        return Math.abs(mEvent.getId().hashCode());
    }

    public FullEvent getEvent() {
        return mEvent;
    }

    public int getItemViewType() {
        return ItemType.EVENT.value;
    }

    protected void bindView(View view) {
        ImageLoader thumbnailLoader = VolleySingleton.getInstance().getThumbnailLoader();
        ImageLoader photoLoader = VolleySingleton.getInstance().getPhotoLoader();

        ViewBinder viewBinder = (ViewBinder)view.getTag();
        if (viewBinder.icon != null) {
            viewBinder.icon.setImageUrl(mEvent.getThumbnailUrl(), thumbnailLoader);
        }
        if (viewBinder.cover != null) {
            CoverData coverData = mEvent.getCoverData();
            if (coverData != null) {
                viewBinder.cover.setCoverImage(coverData, photoLoader);
            } else {
                viewBinder.cover.setImageDrawable(null);
            }
        }

        viewBinder.title.setText(mEvent.getTitle());

        if (mEvent.getVenue().hasName()) {
            viewBinder.location.setText(mEvent.getVenue().getName() + ", " + mEvent.getVenue().getCityStateCountry());
        } else {
            viewBinder.location.setText("");
        }
        viewBinder.startTime.setText(mEvent.getStartTimeString());
        viewBinder.categories.setText("(" + mEvent.getCategoriesAsString() + ")");
    }

    public View createView(View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = mInflater.inflate(R.layout.search_item_event, parent, false);
            ViewBinder viewBinder = new ViewBinder();
            //viewBinder.icon = (NetworkImageView)view.findViewById(R.id.event_list_icon);
            viewBinder.cover = (PlaceholderNetworkImageView)view.findViewById(R.id.event_list_cover);
            viewBinder.title = (TextView)view.findViewById(R.id.event_list_title);
            viewBinder.location = (TextView)view.findViewById(R.id.event_list_location);
            viewBinder.startTime = (TextView)view.findViewById(R.id.event_list_start_time);
            viewBinder.categories = (TextView)view.findViewById(R.id.event_list_categories);
            view.setTag(viewBinder);
        } else {
            view = convertView;
        }

        bindView(view);

        return view;
    }
}
