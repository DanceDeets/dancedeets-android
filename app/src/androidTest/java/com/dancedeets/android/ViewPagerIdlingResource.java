package com.dancedeets.android;

import android.app.Activity;
import android.content.ComponentName;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.widget.Scroller;

import com.google.android.apps.common.testing.testrunner.ActivityLifecycleCallback;
import com.google.android.apps.common.testing.testrunner.Stage;
import com.google.android.apps.common.testing.ui.espresso.IdlingResource;

import java.lang.reflect.Field;

import static com.google.android.apps.common.testing.testrunner.util.Checks.checkNotNull;

/**
 * Created by lambert on 2014/11/06.
 */
public class ViewPagerIdlingResource implements IdlingResource, ActivityLifecycleCallback {
    private static final String LOG_TAG = "ViewPagerIdlingResource";
    private final String mResourceName;
    private final int mViewPagerId;
    private final Class mActivityClass;

    private final Field mScroller;
    private final Field mOnPageChangeListener;

    private ViewPager mViewPager;
    private ResourceCallback mCallback;

    public ViewPagerIdlingResource(String resourceName, Class activityClass, int viewPagerId) throws NoSuchFieldException {
        mResourceName = checkNotNull(resourceName);
        mActivityClass = activityClass;
        mViewPagerId = viewPagerId;

        mScroller = ViewPager.class.getDeclaredField("mScroller");
        mScroller.setAccessible(true);

        mOnPageChangeListener = ViewPager.class.getDeclaredField("mOnPageChangeListener");
        mOnPageChangeListener.setAccessible(true);

    }

    @Override
    public String getName() {
        return mResourceName;
    }

    @Override
    public boolean isIdleNow() {
        if (mViewPager == null) {
            return true;
        }
        try {
            Scroller scroller = (Scroller)mScroller.get(mViewPager);
            return scroller.isFinished();
        } catch (IllegalAccessException e) {
            Log.e(LOG_TAG, "Failed to find mViewPager.mScroller");
            return true;
        }
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback callback) {
        mCallback = callback;
    }


    @Override
    public void onActivityLifecycleChanged(Activity activity, Stage stage) {
        ComponentName websiteActivityComponentName =
                new ComponentName(activity, mActivityClass.getName());
        if (!activity.getComponentName().equals(websiteActivityComponentName)) return;
        Log.i(LOG_TAG, "Activity: " + activity + ", Stage: " + stage);

        switch (stage) {
            case CREATED:
                mViewPager = (ViewPager) activity.findViewById(mViewPagerId);

                // We attempt this privacy hack to find the current OnPageChangeListener,
                // so that our test can setOnPageChangeListener while still calling through to the original
                ViewPager.OnPageChangeListener pageChangeListener = null;
                try {
                    pageChangeListener = (ViewPager.OnPageChangeListener) mOnPageChangeListener.get(mViewPager);
                } catch (IllegalAccessException e) {
                    Log.e(LOG_TAG, "Failed to find mViewPager.mOnPageChangeListener");
                }

                final ViewPager.OnPageChangeListener finalPageChangeListener = pageChangeListener;

                mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                    @Override
                    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                        if (finalPageChangeListener != null) {
                            finalPageChangeListener.onPageScrolled(position, positionOffset, positionOffsetPixels);
                        }
                    }

                    @Override
                    public void onPageSelected(int position) {
                        if (finalPageChangeListener != null) {
                            finalPageChangeListener.onPageSelected(position);
                        }
                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {
                        if (finalPageChangeListener != null) {
                            finalPageChangeListener.onPageScrollStateChanged(state);
                        }
                        if (state == ViewPager.SCROLL_STATE_IDLE) {
                            mCallback.onTransitionToIdle();
                        }
                    }
                });
                break;
            case STOPPED:
                // Clean up reference
                if (activity.isFinishing()) {
                    mViewPager = null;
                }
                break;
        }
    }
}
