package com.dancedeets.dancedeets;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/**
 * Created by lambert on 2014/10/05.
 */
public class SearchDialogFragment extends DialogFragment {

    private static final String LOG_TAG = "SearchDialogFragment";

    private OnSearchListener mOnClickListener;
    private SearchOptions mSearchOptions;

    public void setSearchOptions(SearchOptions searchOptions) {
        mSearchOptions = searchOptions;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.event_search_options, null);
        final EditText searchLocation = (EditText) view.findViewById(R.id.search_location);
        searchLocation.setText(mSearchOptions.location);
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

                        mOnClickListener.onSearch(
                                searchLocation.getText().toString(),
                                searchKeywords.getText().toString());
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        searchLocation.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                searchLocation.post(new Runnable() {
                    @Override
                    public void run() {
                        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputMethodManager.showSoftInput(searchLocation, InputMethodManager.SHOW_IMPLICIT);
                    }
                });
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
