/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dancedeets.dancedeets;


import android.app.Dialog;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;

/**
 * Search dialog. This is controlled by the 
 * SearchManager and runs in the current foreground process.
 *
 * @hide
 */
public class SearchDialog extends Dialog {

    // Debugging support
    private static final boolean DBG = false;
    private static final String LOG_TAG = "SearchDialog";

    private static final String INSTANCE_KEY_COMPONENT = "comp";
    private static final String INSTANCE_KEY_APPDATA = "data";
    private static final String INSTANCE_KEY_USER_QUERY = "uQry";

    // views & widgets
    private SearchView mSearchView;
    private View mCloseSearch;

    // interaction with searchable application
    private SearchableInfo mSearchable;
    private ComponentName mLaunchComponent;
    private Bundle mAppSearchData;

    // The query entered by the user. This is not changed when selecting a suggestion
    // that modifies the contents of the text field. But if the user then edits
    // the suggestion, the resulting string is saved.
    private String mUserQuery;


    /**
     * Constructor - fires it up and makes it look like the search UI.
     *
     * @param context Application Context we can use for system acess
     */
    public SearchDialog(Context context) {
        super(context);
    }

    /**
     * Create the search dialog and any resources that are used for the
     * entire lifetime of the dialog.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window theWindow = getWindow();
        WindowManager.LayoutParams lp = theWindow.getAttributes();
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        // taking up the whole window (even when transparent) is less than ideal,
        // but necessary to show the popup window until the window manager supports
        // having windows anchored by their parent but not clipped by them.
        //lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
        lp.gravity = Gravity.TOP | Gravity.FILL_HORIZONTAL;
        lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        theWindow.setAttributes(lp);

        // Touching outside of the search dialog will dismiss it
        setCanceledOnTouchOutside(true);
    }

    /**
     * We recreate the dialog view each time it becomes visible so as to limit
     * the scope of any problems with the contained resources.
     */
    private void createContentView() {
        // Hide the thick title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.event_search_options);

        // get the view elements for local access
        mSearchView = (SearchView) findViewById(R.id.search_view);
        mSearchView.setIconified(false);
        mSearchView.setOnCloseListener(mOnCloseListener);
        mSearchView.onActionViewExpanded();

        mCloseSearch = findViewById(android.R.id.closeButton);
        mCloseSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    /**
     * Set up the search dialog
     *
     * @return true if search dialog launched, false if not
     */
    public boolean show(String initialQuery, boolean selectInitialQuery,
                        ComponentName componentName, Bundle appSearchData) {
        boolean success = doShow(initialQuery, selectInitialQuery, componentName, appSearchData);
        return success;
    }

    /**
     * Does the rest of the work required to show the search dialog. Called by
     * {@link #show(String, boolean, ComponentName, Bundle)} and
     *
     * @return true if search dialog showed, false if not
     */
    private boolean doShow(String initialQuery, boolean selectInitialQuery,
                           ComponentName componentName, Bundle appSearchData) {
        // set up the searchable and show the dialog
        if (!show(componentName, appSearchData)) {
            return false;
        }

        // finally, load the user's initial text (which may trigger suggestions)
        setUserQuery(initialQuery);
        if (selectInitialQuery) {
            //TODO: mSearchAutoComplete.selectAll();
        }

        return true;
    }

    /**
     * Sets up the search dialog and shows it.
     *
     * @return <code>true</code> if search dialog launched
     */
    private boolean show(ComponentName componentName, Bundle appSearchData) {

        if (DBG) {
            Log.d(LOG_TAG, "show(" + componentName + ", "
                    + appSearchData + ")");
        }

        SearchManager searchManager = (SearchManager)
                getContext().getSystemService(Context.SEARCH_SERVICE);
        // Try to get the searchable info for the provided component.
        mSearchable = searchManager.getSearchableInfo(componentName);

        if (mSearchable == null) {
            return false;
        }

        mLaunchComponent = componentName;
        mAppSearchData = appSearchData;

        // show the dialog. this will call onStart().
        if (!isShowing()) {
            // Recreate the search bar view every time the dialog is shown, to get rid
            // of any bad state in the AutoCompleteTextView etc
            createContentView();
            mSearchView.setSearchableInfo(mSearchable);

            show();
        }

        return true;
    }

    @Override
    public void onStart() {
        super.onStart();

        // Register a listener for configuration change events.
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
    }

    /**
     * The search dialog is being dismissed, so handle all of the local shutdown operations.
     *
     * This function is designed to be idempotent so that dismiss() can be safely called at any time
     * (even if already closed) and more likely to really dump any memory.  No leaks!
     */
    @Override
    public void onStop() {
        super.onStop();


        // dump extra memory we're hanging on to
        mLaunchComponent = null;
        mAppSearchData = null;
        mSearchable = null;
        mUserQuery = null;
    }

    /**
     * Save the minimal set of data necessary to recreate the search
     *
     * @return A bundle with the state of the dialog, or {@code null} if the search
     *         dialog is not showing.
     */
    @Override
    public Bundle onSaveInstanceState() {
        if (!isShowing()) return null;

        Bundle bundle = new Bundle();

        // setup info so I can recreate this particular search       
        bundle.putParcelable(INSTANCE_KEY_COMPONENT, mLaunchComponent);
        bundle.putBundle(INSTANCE_KEY_APPDATA, mAppSearchData);
        bundle.putString(INSTANCE_KEY_USER_QUERY, mUserQuery);

        return bundle;
    }

    /**
     * Restore the state of the dialog from a previously saved bundle.
     *
     * @param savedInstanceState The state of the dialog previously saved by
     *     {@link #onSaveInstanceState()}.
     */
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.i(LOG_TAG, "onRestoreInstanceState:" + savedInstanceState);

        if (savedInstanceState == null) return;

        ComponentName launchComponent = savedInstanceState.getParcelable(INSTANCE_KEY_COMPONENT);
        Bundle appSearchData = savedInstanceState.getBundle(INSTANCE_KEY_APPDATA);
        String userQuery = savedInstanceState.getString(INSTANCE_KEY_USER_QUERY);

        // show the dialog.
        if (!doShow(userQuery, false, launchComponent, appSearchData)) {
            Log.i(LOG_TAG, "couldn't instantiate dialog");
            // for some reason, we couldn't re-instantiate
            return;
        }
        Log.i(LOG_TAG, "instantiated dialog");
    }


    @Override
    public void hide() {
        if (!isShowing()) return;

        // We made sure the IME was displayed, so also make sure it is closed
        // when we go away.
        InputMethodManager imm = (InputMethodManager)getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(
                    getWindow().getDecorView().getWindowToken(), 0);
        }

        super.hide();
    }

    @Override
    public void onBackPressed() {
        // If the input method is covering the search dialog completely,
        // e.g. in landscape mode with no hard keyboard, dismiss just the input method
        InputMethodManager imm = (InputMethodManager)getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && imm.isFullscreenMode() &&
                imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0)) {
            return;
        }
        // Close search dialog
        cancel();
    }

    private final SearchView.OnCloseListener mOnCloseListener = new SearchView.OnCloseListener() {

        public boolean onClose() {
            // Dismiss the dialog if close button is pressed when there's no query text
            if (TextUtils.getTrimmedLength(mSearchView.getQuery()) == 0) {
                dismiss();
                return true;
            }
            return false;
        }
    };

    /**
     * Sets the text in the query box, updating the suggestions.
     */
    private void setUserQuery(String query) {
        if (query == null) {
            query = "";
        }
        mUserQuery = query;
    }
}
