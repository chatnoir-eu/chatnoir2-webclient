/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.search;

import java.util.*;

/**
 * Public interface for search providers.
 *
 * @author Janek Bevendorff
 */
public abstract class SearchProvider extends IndexRetrievalOperator
{
    /**
     * Search result language.
     */
    private String mSearchLanguage = "en";

    /**
     * Whether to group results by hostname.
     */
    private boolean mGroupByHostname = true;

    public SearchProvider(final String[] indices)
    {
        super(indices);
        mGroupByHostname = getConf().getBoolean("serp.group_by_hostname", true);
    }

    public SearchProvider()
    {
        this(null);
    }

    /**
     * Run a search based on given search fields.
     *
     * @param query search query
     * @param from first result to return
     * @param size number of results to return
     */
    public abstract void doSearch(String query, int from, int size);

    /**
     * Return a list of SearchResult objects for the executed search.
     *
     * @return list of search results
     */
    public abstract List<SearchResultBuilder.SearchResult> getResults();

    /**
     * Get the total number of results found for the last search.
     *
     * @return the number of results
     */
    public abstract long getTotalResultNumber();

    /**
     * @return current search language (two-characters language code)
     */
    public String getSearchLanguage() {
        return mSearchLanguage;
    }

    /**
     * @param language current search language (two-characters language code)
     */
    public void setSearchLanguage(String language) {
        mSearchLanguage = language.length() == 2 ? language.toLowerCase() : "en";
    }

    /**
     * @return whether to group results by hostname
     */
    public boolean isGroupByHostname()
    {
        return mGroupByHostname;
    }

    /**
     * @param groupByHostname whether to group results by hostname
     */
    public void setGroupByHostname(boolean groupByHostname)
    {
        this.mGroupByHostname = groupByHostname;
    }

    /**
     * Helper function to localize field names according to the current search language.
     *
     * @param fieldName field name with language placehoders
     * @return localized field name
     */
    protected String replaceLocalePlaceholders(String fieldName) {
        return fieldName.replace("%lang%", mSearchLanguage);
    }



    /**
     * Group (bucket) results by host name, but preserve ranking order of first
     * element in a group as well as the order within a group.
     *
     * @param results ungrouped ordered search results
     * @return grouped ordered search results
     */
    protected List<SearchResultBuilder.SearchResult> groupResults(List<SearchResultBuilder.SearchResult> results)
    {
        // use ordered hash map for bucketing
        LinkedHashMap<String, List<SearchResultBuilder.SearchResult>> buckets = new LinkedHashMap<>();
        for (SearchResultBuilder.SearchResult result: results) {
            boolean suggestGrouping = true;

            // first element in a group
            if (!buckets.containsKey(result.targetHostname())) {
                buckets.put(result.targetHostname(), new ArrayList<>());
                suggestGrouping = false;
            }

            result.setGroupingSuggested(suggestGrouping);
            buckets.get(result.targetHostname()).add(result);
        }

        // serialize buckets
        List<SearchResultBuilder.SearchResult> groupedResults = new ArrayList<>();
        for (String bucketKey: buckets.keySet()) {
            // suggest "more from this host" for the last result in a bucket
            List<SearchResultBuilder.SearchResult> b = buckets.get(bucketKey);
            if (b.size() > 1) {
                b.get(b.size() - 1).setMoreSuggested(true);
            }

            groupedResults.addAll(b);
        }

        return groupedResults;
    }
}
