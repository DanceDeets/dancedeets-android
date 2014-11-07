package com.dancedeets.android;

import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewParent;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import static org.hamcrest.Matchers.is;

/**
 * Created by lambert on 2014/11/06.
 */
public class MyMatchers {
    public static Matcher<View> withResourceName(String resourceName) {
        return withResourceName(is(resourceName));
    }

    public static Matcher<View> withResourceName(final Matcher<String> resourceNameMatcher) {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("with resource name: ");
                resourceNameMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                int id = view.getId();
                return id != View.NO_ID && id != 0 && view.getResources() != null
                        && resourceNameMatcher.matches(view.getResources().getResourceName(id));
            }
        };
    }

    public static Matcher<View> isScrolledTo() {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("is scrolled-to and visible");
            }

            @Override
            public boolean matchesSafely(View view) {
                View oldViewParent = view;
                ViewParent viewParent = view.getParent();
                while (viewParent != null && !(viewParent instanceof ViewPager)) {
                    oldViewParent = (View)viewParent;
                    viewParent = viewParent.getParent();
                }
                ViewPager viewPager = (ViewPager)viewParent;
                View viewPagerElement = oldViewParent;
                if (viewPager != null && viewPagerElement != null) {
                    if (viewPager.getScrollX() <= viewPagerElement.getX() && viewPagerElement.getX() < viewPager.getScrollX() + viewPager.getWidth()) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

}
