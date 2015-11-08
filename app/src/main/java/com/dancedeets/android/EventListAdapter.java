package com.dancedeets.android;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.dancedeets.android.models.CoverData;
import com.dancedeets.android.models.FullEvent;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * An Adapter for mapping Event objects to the Event ListView UI.
 */
public class EventListAdapter extends BaseAdapter {

    private static String LOG_TAG = "EventUIAdapter";

    static class ViewBinder {
        NetworkImageView icon;
        PlaceholderNetworkImageView cover;
        TextView title;
        TextView location;
        TextView startTime;
        TextView categories;
    }
    private LayoutInflater mInflater;
    private List<Object> mSectionedEventList = new ArrayList();
    private List<Integer> mMapping;
    private static int mSectionResource = 0;
    private static int mResource = R.layout.search_event_item;

    public enum ItemType {
        HEADER(0),
        EVENT(1);

        private final int queryArg;
        ItemType(int queryArg) {
            this.queryArg = queryArg;
        }
        public int value() {
            return queryArg;
        }
    }

    static DateFormat localizedDateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);

    public EventListAdapter(Context context) {
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void rebuildList(List<FullEvent> eventBundleList) {
        mMapping = new ArrayList<>();
        mSectionedEventList = fillSectionedList(eventBundleList);
    }

    private List<Object> fillSectionedList(List<FullEvent> eventList) {
        Calendar lastCal = Calendar.getInstance();
        Calendar curCal = Calendar.getInstance();
        List<Object> sectionedList = new ArrayList<>();
        for (int i = 0; i < eventList.size(); i++) {
            FullEvent event = eventList.get(i);
            curCal.setTime(event.getStartTime());
            if (i == 0 ||
                    curCal.get(Calendar.YEAR) != lastCal.get(Calendar.YEAR) ||
                    curCal.get(Calendar.DAY_OF_YEAR) != lastCal.get(Calendar.DAY_OF_YEAR)
                    ) {
                sectionedList.add(localizedDateFormat.format(event.getStartTimeLong()));
                mMapping.add(-1);
            }
            sectionedList.add(event);
            lastCal.setTime(event.getStartTime());
            mMapping.add(i);
        }
        return sectionedList;
    }

    public int translatePosition(int position) {
        return mMapping.get(position);
    }

    public int getCount() {
        return mSectionedEventList.size();
    }

    @Override
    public Object getItem(int position) {
        return mSectionedEventList.get(position);
    }

    @Override
    public long getItemId(int position) {
        // The facebook ID is not guaranteed to fit into a 'long',
        // so we use the hashcode of the string, which should be good enough for uniqueness.
        if (getItemViewType(position) == ItemType.HEADER.value()) {
            return Math.abs(mSectionedEventList.get(position).hashCode());
        } else {
            return Math.abs(((FullEvent) mSectionedEventList.get(position)).getId().hashCode());
        }
    }

    @Override
    public boolean hasStableIds() {
        // Dependent on the getItemId implementation above being stable.
        return true;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    protected ItemType getItemType(int position) {
        Object o = mSectionedEventList.get(position);
        if (o instanceof FullEvent) {
            return ItemType.EVENT;
        }
        else {
            return ItemType.HEADER;
        }
    }

    @Override
    public int getItemViewType(int position) {
        return getItemType(position).value();
    }

    protected void bindView(int position, View view) {
        FullEvent event = (FullEvent)getItem(position);
        ImageLoader thumbnailLoader = VolleySingleton.getInstance().getThumbnailLoader();
        ImageLoader photoLoader = VolleySingleton.getInstance().getPhotoLoader();

        ViewBinder viewBinder = (ViewBinder)view.getTag();
        if (viewBinder.icon != null) {
            viewBinder.icon.setImageUrl(event.getThumbnailUrl(), thumbnailLoader);
        }
        if (viewBinder.cover != null) {
            CoverData coverData = event.getCoverData();
            if (coverData != null) {
                viewBinder.cover.setCoverImage(coverData, photoLoader);
            } else {
                viewBinder.cover.setImageDrawable(null);
            }
        }
        viewBinder.title.setText(event.getTitle());
        if (event.getVenue().hasName()) {
            viewBinder.location.setText(event.getVenue().getName() + ", " + event.getVenue().getCityStateCountry());
        } else {
            viewBinder.location.setText("");
        }
        viewBinder.startTime.setText(event.getStartTimeString());
        viewBinder.categories.setText("(" + event.getCategoriesAsString() + ")");
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (getItemType(position) == ItemType.EVENT) {
            return createEventViewFromResource(position, convertView, parent);
        } else {
            return createSectionViewFromResource(position, convertView, parent);
        }
    }

    private View createSectionViewFromResource(int position, View convertView,
                                             ViewGroup parent) {
        TextView view;
        if (convertView == null) {
            view = (TextView) mInflater.inflate(parent.getResources().getLayout(android.R.layout.simple_list_item_1), null);
            view.setTag("sticky");
        } else {
            view = (TextView) convertView;
        }
        view.setText((String)mSectionedEventList.get(position));

        return view;
    }

    private View createEventViewFromResource(int position, View convertView,
                                             ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = mInflater.inflate(mResource, parent, false);
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

        bindView(position, view);

        return view;
    }

}
