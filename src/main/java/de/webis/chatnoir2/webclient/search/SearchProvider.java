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
    private String mSearchLanguage = "en";

    public SearchProvider(final String[] indices)
    {
        super(indices);
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
    public abstract ArrayList<SearchResultBuilder.SearchResult> getResults();

    /**
     * Get the total number of results found for the last search.
     *
     * @return the number of results
     */
    public abstract long getTotalResultNumber();

    /**
     * @return Current search language
     */
    public String getSearchLanguage() {
        return mSearchLanguage;
    }

    /**
     * Set current search language
     *
     * @param language two-characters language code
     */
    public void setSearchLanguage(String language) {
        mSearchLanguage = language.length() == 2 ? language.toLowerCase() : "en";
    }

    /**
     * Helper function to localize field names according to the current search language.
     *
     * @param fieldName field name with language placehoders
     * @return localized field name
     */
    protected String localizeFieldName(String fieldName) {
        return fieldName.replace("%lang%", mSearchLanguage);
    }
}
