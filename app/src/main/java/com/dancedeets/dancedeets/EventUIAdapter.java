package com.dancedeets.dancedeets;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

import java.util.List;

/**
 * Created by lambert on 2014/10/02.
 */
public class EventUIAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private List<Bundle> mEventBundleList;
    private int mResource;

    public EventUIAdapter(Context context, List<Bundle> eventBundleList, int resource) {
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mEventBundleList = eventBundleList;
        mResource = resource;

    }
    public int getCount() {
        return mEventBundleList.size();
    }

    @Override
    public Object getItem(int position) {
        return mEventBundleList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position; // TODO: do we want to return the FB ID of the item itself? If we do, override hasStableIds()
    }

    protected void bindView(int position, View view) {
        Bundle eventBundle = (Bundle)getItem(position);
        ImageLoader thumbnailLoader = VolleySingleton.getInstance(null).getThumbnailLoader();

        // TODO: Maybe use a ViewHolder?
        // http://developer.android.com/training/improving-layouts/smooth-scrolling.html#ViewHolder

        NetworkImageView iconView = (NetworkImageView )view.findViewById(R.id.icon);
        iconView.setImageUrl(eventBundle.getString("image_url"), thumbnailLoader);

        TextView titleView = (TextView)view.findViewById(R.id.title);
        titleView.setText(eventBundle.getString("title"));

        TextView locationView = (TextView)view.findViewById(R.id.location);
        locationView.setText(eventBundle.getString("location"));

        TextView dateView = (TextView)view.findViewById(R.id.datetime);
        dateView.setText(eventBundle.getString("start_time"));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createViewFromResource(position, convertView, parent, mResource);
    }

    private View createViewFromResource(int position, View convertView,
                                        ViewGroup parent, int resource) {
        View v;
        if (convertView == null) {
            v = mInflater.inflate(resource, parent, false);
        } else {
            v = convertView;
        }

        bindView(position, v);

        return v;
    }

}
