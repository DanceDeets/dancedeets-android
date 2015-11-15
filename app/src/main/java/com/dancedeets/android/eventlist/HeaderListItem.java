package com.dancedeets.android.eventlist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dancedeets.android.models.FullEvent;

import java.text.DateFormat;

/**
 * Created by lambert on 2015/11/08.
 */
public class HeaderListItem implements ListItem {
    private final LayoutInflater mInflater;
    private String mTitle;

    static DateFormat localizedDateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);

    public HeaderListItem(LayoutInflater inflater, FullEvent event) {
        mInflater = inflater;
        mTitle = localizedDateFormat.format(event.getStartTimeLong());
    }

    public int getItemId() {
        return Math.abs(mTitle.hashCode());
    }

    public int getItemViewType() {
        return ItemType.HEADER.value;
    }

    @Override
    public View createView(View convertView, ViewGroup parent) {
        TextView view;
        if (convertView == null) {
            view = (TextView) mInflater.inflate(parent.getResources().getLayout(android.R.layout.simple_list_item_1), null);
            view.setTag("sticky");
        } else {
            view = (TextView) convertView;
        }
        view.setText(mTitle);
        return view;
    }
}
