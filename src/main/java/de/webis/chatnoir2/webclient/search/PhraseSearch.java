/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.search;

import de.webis.chatnoir2.webclient.resources.ConfigLoader.Config;
import org.apache.commons.math3.exception.MathParseException;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;

/**
 * Provider for pure phrase search.
 */
public class PhraseSearch extends SearchProvider
{
    /**
     * Phrase matching slop.
     */
    private int mSlop = 0;

    /**
     * Config object shortcut.
     */
    private final Config mPhraseConfig;

    /**
     * Elasticsearch response object of the last search.
     */
    private SearchResponse mResponse;

    /**
     * Whether to add explanation to search results.
     */
    private boolean mExplain = false;

    public PhraseSearch(final String[] indices)
    {
        super(indices);
        mPhraseConfig = getConf().get("search.phrase_search");
    }

    @Override
    public void doSearch(String query, int from, int size)
    {
        QueryBuilder phraseQuery = buildPreQuery(new StringBuffer(query));

        mResponse = getClient()
                .prepareSearch(getEffectiveIndices())
                .setQuery(phraseQuery)
                .setFrom(from)
                .setSize(size)
                .highlighter(new HighlightBuilder()
                        .field("title_lang." + getSearchLanguage(), getTitleLength(), 1)
                        .field("body_lang." + getSearchLanguage(), getSnippetLength(), 1)
                        .encoder("html"))
                .setExplain(mExplain)
                .setTerminateAfter(mPhraseConfig.getInteger("node_limit", 200000))
                .setProfile(false)
                .get();
    }

    @Override
    protected SearchResponse getResponse()
    {
        return mResponse;
    }

    private QueryBuilder buildPreQuery(StringBuffer queryString)
    {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        Config[] fields = mPhraseConfig.getArray("fields");
        for (Config c: fields) {
            MatchPhraseQueryBuilder matchPhraseQuery = QueryBuilders.matchPhraseQuery(
                    replaceLocalePlaceholders(c.getString("name")),
                    queryString.toString());
            matchPhraseQuery.boost(c.getFloat("boost", 1.0f));
            boolQuery.must(matchPhraseQuery);
        }

        return boolQuery;
    }

    /**
     * @return current slop setting
     */
    public int getSlop()
    {
        return mSlop;
    }

    /**
     * @param slop new slop (must be positive and cannot be more than the search.phrase_search.max_slop setting)
     */
    public void setSlop(int slop)
    {
        slop = Math.min(Math.max(0, slop), mPhraseConfig.getInteger("max_slop", 2));
        mSlop = slop;
    }

    /**
     * @return whether to add explanation to search results
     */
    public boolean isExplain()
    {
        return mExplain;
    }

    /**
     * @param explain whether to add explanation to search results
     */
    public void setExplain(boolean explain)
    {
        mExplain = explain;
    }
}
