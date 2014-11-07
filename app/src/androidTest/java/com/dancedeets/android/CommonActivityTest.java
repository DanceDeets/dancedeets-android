package com.dancedeets.android;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.View;

import com.android.volley.ExecutorDelivery;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.NoCache;
import com.google.android.apps.common.testing.testrunner.ActivityLifecycleCallback;
import com.google.android.apps.common.testing.testrunner.ActivityLifecycleMonitorRegistry;
import com.google.android.apps.common.testing.testrunner.Stage;
import com.google.android.apps.common.testing.ui.espresso.Espresso;

import org.hamcrest.Matcher;

import java.util.Random;

import static com.dancedeets.android.MyMatchers.isScrolledTo;
import static org.hamcrest.Matchers.allOf;

/**
 * Created by lambert on 2014/11/06.
 */
public class CommonActivityTest<T extends Activity> extends ActivityInstrumentationTestCase2<T> {
    private VolleyDiskBasedHttpStack mHttpStack;
    private VolleyIdlingResource mIdlingResource;
    private CurrentActivityTracker mCurrentActivityTracker;

    public CommonActivityTest(Class<T> activityClass) {
        super(activityClass);
    }

    @SuppressWarnings("unchecked")
    public static Matcher<View> withinActivePager(Matcher<View> matcher) {
        return allOf(matcher, isScrolledTo());
    }

    private static class CurrentActivityTracker implements ActivityLifecycleCallback {
        Activity mCurrentActivity;

        @Override
        public void onActivityLifecycleChanged(Activity activity, Stage stage) {
            Log.i("TEST", "A " + activity + ", stage " + stage);
            if (stage == Stage.RESUMED) {
                mCurrentActivity = activity;
            } else if (stage == Stage.PAUSED && mCurrentActivity == activity) {
                mCurrentActivity = null;
            }
        }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        createVolleyForEspresso();

        Log.i("TEST", "Hi");
        mCurrentActivityTracker = new CurrentActivityTracker();
        ActivityLifecycleMonitorRegistry.getInstance().addLifecycleCallback(mCurrentActivityTracker);
    }


    @Override
    public void tearDown() throws Exception {
        // We need to "kill" the IdlingResource since we don't need it anymore.
        // But Espresso does not currently have an unregisterIdlingResources() to call.
        // So instead we make it impotent, so it doesn't affect anything going forward.
        mIdlingResource.makeImpotent();
        super.tearDown();
    }


    public Activity getCurrentActivity() {
        return mCurrentActivityTracker.mCurrentActivity;
    }

    protected void createVolleyForEspresso() throws NoSuchFieldException {
        // Construct our own queue, with a proper VolleyIdlingResource handler
        mHttpStack = new VolleyDiskBasedHttpStack();
        Network network = new BasicNetwork(mHttpStack);
        String randomString = Integer.toString(new Random().nextInt());
        mIdlingResource = new VolleyIdlingResource("VolleyRequestQueue_" + randomString);

        Handler mHandler = new Handler(Looper.getMainLooper());
        // Wrap our normal ExecutorDelivery(Handler(MainLooper)) with one that notifies our idlingResource.
        ExecutorDelivery mDispatch = mIdlingResource.getExecutorDeliveryWrapper(mHandler);
        RequestQueue queue = new RequestQueue(new NoCache(), network, 4, mDispatch);

        VolleySingleton.createInstance(queue);
        Espresso.registerIdlingResources(mIdlingResource);
        queue.start();
    }

    protected void waitForVolley(boolean runVolley) {
        mHttpStack.setBlockResponses(!runVolley);
        mIdlingResource.setWaitForVolley(runVolley);
    }
}
