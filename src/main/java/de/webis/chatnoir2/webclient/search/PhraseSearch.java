/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.search;

import de.webis.chatnoir2.webclient.resources.ConfigLoader;
import de.webis.chatnoir2.webclient.resources.ConfigLoader.Config;
import org.elasticsearch.index.query.*;

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
    protected int getNodeLimit()
    {
        return mPhraseConfig.getInteger("node_limit", 10000);
    }

    @Override
    protected QueryBuilder buildPreQuery(StringBuffer queryString)
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

        addFilters(boolQuery);
        addBoosts(boolQuery, true);

        return boolQuery;
    }

    @Override
    protected QueryBuilder buildRescoreQuery(StringBuffer queryString)
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

        // add fields from simple search for additional scoring
        ConfigLoader.Config[] simpleSearchFields = mSimpleConfig.getArray("main_fields");
        for (final ConfigLoader.Config field : simpleSearchFields) {
            MatchQueryBuilder matchQuery = QueryBuilders.matchQuery(
                    replaceLocalePlaceholders(field.getString("name", "")),
                    queryString.toString());
            matchQuery.boost(field.getFloat("boost", 1.0f));
            boolQuery.should(matchQuery);
        }

        addFilters(boolQuery);
        addBoosts(boolQuery, false);

        // up-cast and decorate query
        QueryBuilder mainQuery = boolQuery;
        mainQuery = decorateFieldValueFactors(mainQuery);
        mainQuery = decorateNegativeBoost(mainQuery);

        return mainQuery;
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
