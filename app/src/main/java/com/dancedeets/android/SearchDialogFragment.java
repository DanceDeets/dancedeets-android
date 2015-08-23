package com.dancedeets.android;

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
public class SearchDialogFragment extends StateDialogFragment<SearchDialogFragment.MyBundledState, RetainedState> {

    static protected class MyBundledState extends BundledState {
        SearchOptions mSearchOptions;
    }

    private static final String LOG_TAG = "SearchDialogFragment";

    public static final String ARG_SEARCH_OPTIONS = "SEARCH_OPTIONS";

    public static final String ARG_MESSAGE = "MESSAGE";


    @Override
    public MyBundledState buildBundledState() {
        return new MyBundledState();
    }

    @Override
    public RetainedState buildRetainedState() {
        return null;
    }

    @Override
    public String getUniqueTag() {
        return LOG_TAG;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.i(LOG_TAG, "onCreateDialog");
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

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.getContext();
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(view)
                // Add action buttons
                .setPositiveButton(R.string.action_search, new DialogInterface.OnClickListener() {
                    // We don't need to use a separate static class and pass mRetained,
                    // because this code is recreated each time (with no long lasting references)
                    // Also mRetained is not set yet at this point (sometimes),
                    // and so delaying our access to mRetained avoids null pointer accesses.
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Dialog d = (Dialog) dialog;
                        EditText searchLocation = ((EditText) d.findViewById(R.id.search_location));
                        EditText searchKeywords = ((EditText) d.findViewById(R.id.search_keywords));

                        ((OnSearchListener)getActivity()).onSearch(
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

    public interface OnSearchListener {
        void onSearch(String location, String keywords);
    }
}
