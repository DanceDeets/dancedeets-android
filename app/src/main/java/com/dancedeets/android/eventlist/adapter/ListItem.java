package com.dancedeets.android.eventlist.adapter;

import android.view.View;
import android.view.ViewGroup;

/**
 * Created by lambert on 2015/11/08.
 */
public interface ListItem {

    enum ItemType {
        EVENT(0),
        HEADER(1),
        ONEBOX(2);

        int value;

        ItemType(int value) {
            this.value = value;
        }
        public static int length = ListItem.ItemType.values().length;
    }

    int getItemId();
    int getItemViewType();
    View createView(View convertView, ViewGroup parent);
}
