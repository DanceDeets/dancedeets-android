package com.dancedeets.android;

import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.google.android.apps.common.testing.ui.espresso.IdlingResource;

import java.lang.reflect.Field;
import java.util.Set;

import static com.google.android.apps.common.testing.testrunner.util.Checks.checkNotNull;

/**
 * Created by lambert on 2014/10/28.
 */
public final class VolleyIdlingResource implements IdlingResource {
    private static final String TAG = "VolleyIdlingResource";
    private final String resourceName;

    // written from main thread, read from any thread.
    private volatile ResourceCallback resourceCallback;

    private Field mCurrentRequests;
    private final RequestQueue mVolleyRequestQueue;

    public VolleyIdlingResource(String resourceName) throws SecurityException, NoSuchFieldException {
        this.resourceName = checkNotNull(resourceName);

        mVolleyRequestQueue = VolleySingleton.getInstance().getRequestQueue();

        mCurrentRequests = RequestQueue.class.getDeclaredField("mCurrentRequests");
        mCurrentRequests.setAccessible(true);
    }

    @Override
    public String getName() {
        return resourceName;
    }

    @Override
    public boolean isIdleNow() {
        try {
            synchronized (mVolleyRequestQueue) {
                Set<Request> set = (Set<Request>) mCurrentRequests.get(mVolleyRequestQueue);
                int count = set.size();
                if (count == 0) {
                    Log.d(TAG, "Volley is now idle");
                    resourceCallback.onTransitionToIdle();
                } else {
                    Log.d(TAG, "Volley has " + count + " requests in-flight");
                }
                return count == 0;
            }
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Failed to find mVolleyRequestQueue.mCurrentRequests");
            return true;
        }
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
        this.resourceCallback = resourceCallback;
    }
}