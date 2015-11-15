package com.dancedeets.android.eventlist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dancedeets.android.models.OneboxLink;

import java.text.DateFormat;

/**
 * Created by lambert on 2015/11/08.
 */
public class OneboxListItem implements ListItem {
        private final LayoutInflater mInflater;
        private OneboxLink mOnebox;

        static DateFormat localizedDateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);

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
            TextView view;
            if (convertView == null) {
                view = (TextView) mInflater.inflate(parent.getResources().getLayout(android.R.layout.simple_list_item_1), null);
            } else {
                view = (TextView) convertView;
            }
            view.setText(mOnebox.getTitle());
            return view;
        }
    }
