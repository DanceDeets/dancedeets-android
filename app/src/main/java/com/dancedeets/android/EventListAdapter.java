package com.dancedeets.android;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.dancedeets.android.eventlist.EventListItem;
import com.dancedeets.android.eventlist.HeaderListItem;
import com.dancedeets.android.eventlist.ListItem;
import com.dancedeets.android.models.FullEvent;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * An Adapter for mapping Event objects to the Event ListView UI.
 */
public class EventListAdapter extends BaseAdapter {

    private static String LOG_TAG = "EventListAdapter";

    private LayoutInflater mInflater;
    private List<ListItem> mList = new ArrayList();

    public EventListAdapter(Context context) {
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void rebuildList(List<FullEvent> eventBundleList) {
        mList = fillSectionedList(eventBundleList);
    }

    private List<ListItem> fillSectionedList(List<FullEvent> eventList) {
        Calendar lastCal = Calendar.getInstance();
        Calendar curCal = Calendar.getInstance();
        List<ListItem> sectionedList = new ArrayList<>();
        for (int i = 0; i < eventList.size(); i++) {
            FullEvent event = eventList.get(i);
            curCal.setTime(event.getStartTime());
            if (i == 0 ||
                    curCal.get(Calendar.YEAR) != lastCal.get(Calendar.YEAR) ||
                    curCal.get(Calendar.DAY_OF_YEAR) != lastCal.get(Calendar.DAY_OF_YEAR)
                    ) {
                sectionedList.add(new HeaderListItem(mInflater, event));
            }
            sectionedList.add(new EventListItem(mInflater, event));
            lastCal.setTime(event.getStartTime());
        }
        return sectionedList;
    }

    public int getCount() {
        return mList.size();
    }

    @Override
    public ListItem getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getItemId();
    }

    @Override
    public boolean hasStableIds() {
        // Dependent on the getItemId implementation above being stable.
        return true;
    }

    @Override
    public int getViewTypeCount() {
        return ListItem.ItemType.length;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getItemViewType();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getItem(position).createView(convertView, parent);
    }
}
