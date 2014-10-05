package com.dancedeets.dancedeets;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.SearchView;

/**
 * Created by lambert on 2014/10/05.
 */
public class SearchDialogFragment extends DialogFragment {

    final static String LOG_TAG = "SearchDialogFragment";
    private OnSearchListener mOnClickListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(R.layout.event_search_options, null))
                // Add action buttons
                .setPositiveButton(R.string.action_search, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Dialog d = (Dialog) dialog;
                        SearchView searchLocation = ((SearchView) d.findViewById(R.id.search_location));
                        SearchView searchKeywords = ((SearchView) d.findViewById(R.id.search_keywords));

                        mOnClickListener.onSearch(
                                searchLocation.getQuery().toString(),
                                searchKeywords.getQuery().toString());
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        return builder.create();
    }

    public void setOnClickHandler(OnSearchListener onClickListener) {
        mOnClickListener = onClickListener;
    }

    public static interface OnSearchListener {
        public void onSearch(String location, String keywords);
    }
}
