package com.dancedeets.android;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

import com.android.volley.ExecutorDelivery;
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
    private static final String LOG_TAG = "VolleyIdlingResource";
    private final String mResourceName;

    private boolean mWaitForVolley;

    // written from main thread, read from any thread.
    private volatile ResourceCallback resourceCallback;
    private Field mCurrentRequests;

    public VolleyIdlingResource(String resourceName) throws NoSuchFieldException {
        mResourceName = checkNotNull(resourceName);

        mCurrentRequests = RequestQueue.class.getDeclaredField("mCurrentRequests");
        mCurrentRequests.setAccessible(true);
        setWaitForVolley(true);
    }

    public void makeImpotent() {
        mWaitForVolley = false;
    }

    @Override
    public String getName() {
        return mResourceName;
    }

    public boolean isWaitForVolley() {
        return mWaitForVolley;
    }

    public void setWaitForVolley(boolean waitForVolley) {
        this.mWaitForVolley = waitForVolley;
    }


    @Override
    public boolean isIdleNow() {
        if (!isWaitForVolley()) {
            return true;
        }
        // Always use the current request queue, as opposed to grabbing the one in use when this class is constructed
        final RequestQueue volleyRequestQueue = VolleySingleton.getInstance().getRequestQueue();
        try {
            Set<Request> set = (Set<Request>) mCurrentRequests.get(volleyRequestQueue);
            synchronized (set) {
                int count = set.size();
                if (count == 0) {
                    Log.d(LOG_TAG, "Volley is now idle");
                } else {
                    Log.d(LOG_TAG, "Volley has " + count + " requests in-flight:");
                    for (Request request: set) {
                        Log.d(LOG_TAG, "  Request: " + request);
                    }
                }
                return count == 0;
            }
        } catch (IllegalAccessException e) {
            Log.e(LOG_TAG, "Failed to find mVolleyRequestQueue.mCurrentRequests");
            return true;
        }
    }

    private void notifyIfIdleNow() {
        if (isIdleNow()) {
            if (resourceCallback != null) {
                resourceCallback.onTransitionToIdle();
            } else {
                Log.e(LOG_TAG, "Would notify that we are idle, but no resourceCallback.");
            }
        }
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
        this.resourceCallback = resourceCallback;
    }

    public ExecutorDelivery getExecutorDeliveryWrapper(final Handler handler) {
        // An ExecutorDelivery passes all messages in to the Handler to be evaluated on the main thread.
        // This is a proxy Handler, that executes a normal message,
        // but also guarantees a call to notifyIfIdleNow immediately afterwards.
        // This guarantees sequential operation, so that we check if it's empty,
        // immediately after we've run code that potentially empties the queue.
        // This guarantees we notify Espresso immediately after the queue empties,
        // instead of waiting for for Espresso to ask us after its multi-second timeout fires.
        return new ExecutorDelivery(new Handler() {
            public boolean sendMessageAtTime(@NonNull final Message msg, long uptimeMillis) {
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        msg.getCallback().run();
                        notifyIfIdleNow();
                    }
                };
                return handler.postAtTime(r, uptimeMillis);
            }
        });
    }
}