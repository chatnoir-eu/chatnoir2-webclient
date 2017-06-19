/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.search;

import de.webis.chatnoir2.webclient.resources.ConfigLoader;
import de.webis.chatnoir2.webclient.resources.ConfigLoader.Config;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.collapse.CollapseBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.rescore.QueryRescorerBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Provider for pure phrase search.
 */
public class PhraseSearch extends SimpleSearch
{
    /**
     * Phrase matching slop.
     */
    private int mSlop = 0;

    /**
     * Config object shortcut to phrase search config.
     */
    private final Config mPhraseConfig;

    /**
     * Config object shortcut to simple search config.
     */
    private final Config mSimpleConfig;

    public PhraseSearch(final String[] indices)
    {
        super(indices);
        mPhraseConfig = getConf().get("search.phrase_search");
        mSimpleConfig = getConf().get("search.default_simple");
    }

    @Override
    protected SearchRequestBuilder buildSearchRequest(StringBuffer queryString, int from, int size)
    {
        return super.buildSearchRequest(queryString, from, size)
                .setCollapse(new CollapseBuilder("warc_target_hostname.raw"));
    }

    @Override
    protected int getNodeLimit()
    {
        return mPhraseConfig.getInteger("node_limit", 10000);
    }

    @Override
    protected HighlightBuilder buildFieldHighlighter()
    {
        return new HighlightBuilder()
                .field("body_lang." + getSearchLanguage(), getSnippetLength(), 1)
                .encoder("html");
    }

    @Override
    protected QueryBuilder buildPreQuery(StringBuffer queryString)
    {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        Config[] fields = mPhraseConfig.getArray("fields");
        List<String> phraseFields = new ArrayList<>();
        for (Config c: fields) {
            String fieldName = replaceLocalePlaceholders(c.getString("name"));
            MatchPhraseQueryBuilder matchPhraseQuery = QueryBuilders.matchPhraseQuery(fieldName, queryString.toString());
            matchPhraseQuery.boost(c.getFloat("boost", 1.0f));

            boolQuery.must(matchPhraseQuery);
            phraseFields.add(fieldName);
        }

        addSimpleSearchFields(boolQuery, queryString, phraseFields);

        addFilters(boolQuery);
        addBoosts(boolQuery, true);

        return boolQuery;
    }

    @Override
    protected QueryRescorerBuilder buildRescorer(QueryBuilder query)
    {
        // rescoring is incomatible with field collapsing
        return null;
    }

    /**
     * Add configured fields from simple search as should clauses.
     *
     * @param query query to add should clauses to
     * @param queryString user query string
     * @param blackList list of fields that are to be skipped
     */
    protected void addSimpleSearchFields(BoolQueryBuilder query, StringBuffer queryString, List<String> blackList)
    {
        ConfigLoader.Config[] simpleSearchFields = mSimpleConfig.getArray("main_fields");
        for (ConfigLoader.Config field : simpleSearchFields) {
            String fieldName = replaceLocalePlaceholders(field.getString("name"));
            if (blackList.contains(fieldName)) {
                // we already scored by this field
                continue;
            }

            MatchQueryBuilder matchQuery = QueryBuilders.matchQuery(fieldName, queryString.toString());
            matchQuery.boost(field.getFloat("boost", 1.0f));
            query.should(matchQuery);
        }
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
}
