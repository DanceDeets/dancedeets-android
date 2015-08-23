package com.dancedeets.android;

/**
 * SearchListActivity expects this API of the various Fragments in the PagerAdapter
 */
public interface SearchTarget {
    void prepareForSearchOptions(SearchOptions searchOptions);

    void loadSearchTab();
}