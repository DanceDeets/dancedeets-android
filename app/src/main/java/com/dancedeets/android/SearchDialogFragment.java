package com.dancedeets.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;

import com.dancedeets.android.uistate.BundledState;
import com.dancedeets.android.uistate.RetainedState;
import com.dancedeets.android.uistate.StateDialogFragment;

/**
 * A dialog for searching events on the list view.
 */
public class SearchDialogFragment extends StateDialogFragment<SearchDialogFragment.MyBundledState, SearchDialogFragment.MyRetainedState> {

    static protected class MyBundledState extends BundledState {
        SearchOptions mSearchOptions;
    }

    static public class MyRetainedState extends RetainedState {
        OnSearchListener mOnSearchListener;
    }
    private static final String LOG_TAG = "SearchDialogFragment";

    public static final String ARG_SEARCH_OPTIONS = "SEARCH_OPTIONS";

    public static final String ARG_MESSAGE = "MESSAGE";

    // This is temporary for the constructor to save state. When onAttach is called, it can be copied into MyRetainedState.
    private OnSearchListener mTempOnSearchListener;

    @Override
    public MyBundledState buildBundledState() {
        return new MyBundledState();
    }

    @Override
    public MyRetainedState buildRetainedState() {
        return new MyRetainedState();
    }

    @Override
    public String getUniqueTag() {
        return LOG_TAG;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Don't overwrite the retainedstate's searchlistener with our empty one, if we're being recreated9
        if (mTempOnSearchListener != null) {
            mRetained.mOnSearchListener = mTempOnSearchListener;
        }
        Log.i(LOG_TAG, "get retained state " + mRetained + ", searchlistener " + mRetained.mOnSearchListener);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.i(LOG_TAG, "onCreateDialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.event_search_options, null);
        AutoCompleteTextView searchLocation = (AutoCompleteTextView) view.findViewById(R.id.search_location);
        EditText searchKeywords = (EditText) view.findViewById(R.id.search_keywords);

        mBundled.mSearchOptions = (SearchOptions)getArguments().getSerializable(ARG_SEARCH_OPTIONS);
        searchLocation.setText(mBundled.mSearchOptions.location);
        searchKeywords.setText(mBundled.mSearchOptions.keywords);

        // We explicitly set the searchLocation adapter after calling setText, so setText dosen't trigger the dropdown.
        searchLocation.setAdapter(new PlacesAdapter(getActivity(), android.R.layout.simple_list_item_1));

        TextView messageView = (TextView) view.findViewById(R.id.search_message);
        String message = getArguments().getString(ARG_MESSAGE);
        if (message != null && !message.isEmpty()) {
            messageView.setText(message);
            int borderDp = 20;
            float scale = getResources().getDisplayMetrics().density;
            int dpAsPixels = (int) (borderDp*scale + 0.5f);
            messageView.setPadding(0, 0, 0, dpAsPixels);
        }

        builder.getContext();
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(view)
                // Add action buttons
                .setPositiveButton(R.string.action_search, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Dialog d = (Dialog) dialog;
                        EditText searchLocation = ((EditText) d.findViewById(R.id.search_location));
                        EditText searchKeywords = ((EditText) d.findViewById(R.id.search_keywords));

                        mRetained.mOnSearchListener.onSearch(
                                searchLocation.getText().toString(),
                                searchKeywords.getText().toString());
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        return builder.create();
    }

    public void setOnClickHandler(OnSearchListener onSearchListener) {
        if (mRetained != null) {
            mRetained.mOnSearchListener = onSearchListener;
        } else {
            mTempOnSearchListener = onSearchListener;
        }
    }

    public static interface OnSearchListener {
        public void onSearch(String location, String keywords);
    }
}
