package com.dancedeets.android.eventlist.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dancedeets.android.R;
import com.dancedeets.android.models.OneboxLink;

/**
 * Created by lambert on 2015/11/08.
 */
public class OneboxListItem implements ListItem {
    private final LayoutInflater mInflater;
    private OneboxLink mOnebox;

    public OneboxListItem(LayoutInflater inflater, OneboxLink onebox) {
        mInflater = inflater;
        mOnebox = onebox;
    }

    public OneboxLink getOnebox() {
        return mOnebox;
    }

    public int getItemId() {
        return Math.abs(mOnebox.hashCode());
    }

    public int getItemViewType() {
        return ItemType.ONEBOX.value;
    }

    @Override
    public View createView(View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = mInflater.inflate(R.layout.search_item_onebox, parent, false);
        } else {
            view = convertView;
        }
        TextView textView = (TextView) view.findViewById(R.id.item_header);
        textView.setText(mOnebox.getTitle());
        return view;
    }
}
