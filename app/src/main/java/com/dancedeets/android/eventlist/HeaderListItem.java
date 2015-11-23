package com.dancedeets.android.eventlist;

import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dancedeets.android.R;
import com.dancedeets.android.models.FullEvent;

import java.text.DateFormat;

/**
 * Created by lambert on 2015/11/08.
 */
public class HeaderListItem implements ListItem {
    private final LayoutInflater mInflater;
    private String mTitle;

    static DateFormat localizedDateFormat = DateFormat.getDateInstance(DateFormat.LONG);

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
        View view;
        if (convertView == null) {
            view = mInflater.inflate(R.layout.search_item_header, parent, false);
        } else {
            view = convertView;
        }
        TextView textView = (TextView) view.findViewById(R.id.item_header);
        textView.setText(mTitle);
        return view;
    }
}
