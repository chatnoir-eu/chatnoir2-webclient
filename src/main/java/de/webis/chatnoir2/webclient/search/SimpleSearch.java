/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.search;

import de.webis.chatnoir2.webclient.resources.ConfigLoader;
import de.webis.chatnoir2.webclient.util.TextCleanser;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.rescore.QueryRescoreMode;
import org.elasticsearch.search.rescore.QueryRescorerBuilder;
import org.elasticsearch.search.rescore.RescoreBuilder;
import org.json.JSONArray;

import java.util.*;

/**
 * Provider for simple universal search.
 *
 * @author Janek Bevendorff
 * @version 1
 */
public class SimpleSearch extends SearchProvider
{
    /**
     * (Default) snippet length.
     */
    private int mSnippetLength = 400;

    /**
     * (Default) title length.
     */
    private int mTitleLength = 70;

    /**
     * Array of mIndices to search.
     */
    private String[] mIndices;

    /**
     * Whether to explain query.
     */
    private boolean mDoExplain = false;

    /**
     * Search mResponse of the last search.
     */
    private SearchResponse mResponse = new SearchResponse();

    /**
     * Constructor.
     *
     * @param indices List of index names to search (null means use default from config).
     *                Indices that are not present in the config will be ignored.
     */
    public SimpleSearch(final List<String> indices)
    {
        ConfigLoader.Config config = getConf();

        mSnippetLength = config.get("serp").getInteger("snippet_length", mSnippetLength);
        mTitleLength   = config.get("serp").getInteger("title_length", mTitleLength);

        final ArrayList<String> allowedIndices = new ArrayList<>(Arrays.asList(config.get("cluster").getStringArray("indices")));
        boolean useDefaultIndex = true;
        if (null != indices) {
            final ArrayList<String> usedIndices = new ArrayList<>();
            for (final String index : indices) {
                if (allowedIndices.contains(index.trim())) {
                    usedIndices.add(index.trim());
                }
            }
            if (!usedIndices.isEmpty()) {
                this.mIndices = usedIndices.toArray(new String[usedIndices.size()]);
                useDefaultIndex = false;
            }
        }
        if (useDefaultIndex) {
            this.mIndices = new String[]{config.get("cluster").getString("default_index", allowedIndices.get(0))};
        }
    }

    public SimpleSearch()
    {
        this(null);
    }

    /**
     * Get a List of all mIndices that are allowed by the config and are therefore actually used.
     *
     * @return List of index names
     */
    public List<String> getEffectiveIndexList()
    {
        return Arrays.asList(this.mIndices);
    }

    /**
     * Set whether to explain search queries.
     *
     * @param doExplain true if query shall be explained
     */
    public void setExplain(final boolean doExplain) {
        this.mDoExplain = doExplain;
    }

    /**
     * Perform a simple search.
     */
    @Override
    public void doSearch(String query, int from, int size)
    {
        final ConfigLoader.Config simpleSearchConfig = getConf().get("search").get("default_simple");
        StringBuffer queryBuffer = new StringBuffer(query);
        mResponse = getClient().prepareSearch(mIndices)
                .setQuery(buildPreQuery(queryBuffer))
                .setRescorer(buildRescorer(buildRescoreQuery(queryBuffer)),
                       simpleSearchConfig.getInteger("rescore_window", size))
                .setFrom(from)
                .setSize(size)
                .highlighter(new HighlightBuilder()
                        .field("title_lang.en", mTitleLength, 1)
                        .field("body_lang.en", mSnippetLength, 1)
                        .encoder("html"))
                .setExplain(mDoExplain)
                .setTerminateAfter(simpleSearchConfig.getInteger("node_limit", 200000))
                .setProfile(false)
                .get();
    }

    @Override
    public ArrayList<SearchResultBuilder.SearchResult> getResults()
    {
        final ArrayList<SearchResultBuilder.SearchResult> results = new ArrayList<>();
        String previousHost = "";
        for (final SearchHit hit : mResponse.getHits()) {
            final Map<String, Object> source = hit.getSource();

            String snippet = "";
            if (null != hit.getHighlightFields().get("body_lang.en")) {
                final Text[] fragments = hit.getHighlightFields().get("body_lang.en").fragments();
                if (1 >= fragments.length) {
                    snippet = fragments[0].string();
                }
            }

            // use meta description or first body part if no highlighted snippet available
            if (snippet.equals("") && !source.get("meta_desc_lang.en").toString().equals("")) {
                snippet = truncateSnippet(source.get("meta_desc_lang.en").toString(), mSnippetLength);
            } else if (snippet.equals("")) {
                snippet = truncateSnippet(source.get("body_lang.en").toString(), mSnippetLength);
            }

            // use highlighted title if available
            String title = truncateSnippet(source.get("title_lang.en").toString(), mTitleLength);
            if (null != hit.getHighlightFields().get("title_lang.en")) {
                final Text[] fragments = hit.getHighlightFields().get("title_lang.en").fragments();
                if (1 >= fragments.length) {
                    title = fragments[0].string();
                }
            }

            JSONArray explanation = null;
            if (null != hit.explanation()) {
                explanation = parseExplanationStringToJson(hit.explanation().toString());
            }

            // group consecutive results with same host
            boolean doGroup = false;
            final String currentHost = (String) source.get("warc_target_hostname");
            if (previousHost.equals(currentHost)) {
                doGroup = true;
            }
            previousHost = currentHost;

            String pr = String.format("%.03f", (Double) source.get("page_rank"));
            if (0.001 > (Double) source.get("page_rank")) {
                pr = String.format("%.03e", (Double) source.get("page_rank"));
            }

            final SearchResultBuilder.SearchResult result = new SearchResultBuilder()
                    .id(hit.getId())
                    .trecId(source.get("warc_trec_id").toString())
                    .title(TextCleanser.cleanse(title, true))
                    .link(source.get("warc_target_uri").toString())
                    .snippet(TextCleanser.cleanse(snippet, true))
                    .fullBody(source.get("body_lang.en").toString())
                    .addMetadata("score", String.format("%.03f", hit.getScore()))
                    .addMetadata("page_rank", pr)
                    .addMetadata("spam_rank", (0 != (Integer) source.get("spam_rank")) ? source.get("spam_rank") : "none")
                    .addMetadata("explanation", explanation)
                    .addMetadata("has_explanation", mDoExplain)
                    .suggestGrouping(doGroup)
                    .build();
            results.add(result);
        }

        return results;
    }

    @Override
    public long getTotalResultNumber()
    {
        return mResponse.getHits().getTotalHits();
    }

    /**
     * Assemble the fast pre-query for use with a rescorer.
     *
     * @return assembled pre-query
     */
    private QueryBuilder buildPreQuery(StringBuffer userQueryString)
    {
        BoolQueryBuilder mainQuery = QueryBuilders.boolQuery();
        mainQuery.filter(QueryBuilders.termQuery("lang", "en"));

        final ConfigLoader.Config simpleSearchConfig = getConf().get("search").get("default_simple");

        QueryBuilder queryStringFilter = parseQueryStringFilters(userQueryString);
        if (null != queryStringFilter) {
            mainQuery.filter(queryStringFilter);
        }

        final SimpleQueryStringBuilder searchQuery = QueryBuilders.simpleQueryStringQuery(userQueryString.toString());
        searchQuery
                .defaultOperator(Operator.AND)
                .flags(SimpleQueryStringFlag.AND,
                        SimpleQueryStringFlag.OR,
                        SimpleQueryStringFlag.NOT,
                        SimpleQueryStringFlag.WHITESPACE);

        final ConfigLoader.Config[] mainFields = simpleSearchConfig.getArray("main_fields");
        for (final ConfigLoader.Config field : mainFields) {
            searchQuery.field(field.getString("name", ""));
        }
        mainQuery.must(searchQuery);

        // add range filters (e.g. to filter by minimal content length)
        final ConfigLoader.Config[] rangeFilters = simpleSearchConfig.getArray("range_filters");
        for (final ConfigLoader.Config filterConfig : rangeFilters) {
            final RangeQueryBuilder rangeFilter = QueryBuilders.rangeQuery(filterConfig.getString("name", ""));
            if (null != filterConfig.getDouble("gt")) {
                rangeFilter.gt(filterConfig.getDouble("gt"));
            }
            if (null != filterConfig.getDouble("gte")) {
                rangeFilter.gte(filterConfig.getDouble("gte"));
            }
            if (null != filterConfig.getDouble("lt")) {
                rangeFilter.lt(filterConfig.getDouble("lt"));
            }
            if (null != filterConfig.getDouble("lte")) {
                rangeFilter.lte(filterConfig.getDouble("lte"));
            }
            final Boolean negate = filterConfig.getBoolean("negate", false);
            if (negate)
                mainQuery.mustNot(rangeFilter);
            else
                mainQuery.filter(rangeFilter);
        }

        return mainQuery;
    }

    /**
     * Parse configured filters from the query string such as site:example.com
     * and delete the filters from the given query StringBuffer.
     *
     * @param queryString user query string
     * @return filter query
     */
    private QueryBuilder parseQueryStringFilters(StringBuffer queryString)
    {
        final ConfigLoader.Config conf = getConf().get("search").get("default_simple");

        ConfigLoader.Config[] filterConf = conf.getArray("query_filters");
        if (filterConf.length == 0) {
            return null;
        }

        BoolQueryBuilder filterQuery = QueryBuilders.boolQuery();
        for (ConfigLoader.Config c: filterConf) {
            String filterKey = c.getString("keyword");
            String filterField = c.getString("field");

            int pos = queryString.indexOf(filterKey + ":");
            if (-1 != pos) {
                int filterStartPos = pos;
                pos +=  filterKey.length() + 1;
                int hostStartPos = pos;
                while (pos < queryString.length() && !Character.isWhitespace(queryString.charAt(pos))) {
                    ++pos;
                }
                String hostName = queryString.substring(hostStartPos, pos).trim();
                TermQueryBuilder termQuery = QueryBuilders.termQuery(filterField, hostName);
                filterQuery.filter(termQuery);
                queryString.replace(filterStartPos, pos, "");

                // trim whitespace
                int trimEnd = 0;
                for (int i = 0; i < queryString.length(); ++i) {
                    if (!Character.isWhitespace(queryString.charAt(i))) {
                        break;
                    }
                    ++trimEnd;
                }
                queryString.replace(0, trimEnd, "");

                int trimStart = queryString.length();
                for (int i = queryString.length(); i > 0; --i) {
                    if (!Character.isWhitespace(queryString.charAt(i - 1))) {
                        break;
                    }
                    --trimStart;
                }
                queryString.replace(trimStart, queryString.length(), "");
            }
        }

        return filterQuery;
    }

    /**
     * Build query rescorer used to run more expensive query on pre-query results.
     *
     * @param mainQuery query to rescore
     * @return assembled RescoreBuilder
     */
    private QueryRescorerBuilder buildRescorer(final QueryBuilder mainQuery)
    {
        final QueryRescorerBuilder resorer = RescoreBuilder.queryRescorer(mainQuery);
        resorer.setQueryWeight(0.1f).
                setRescoreQueryWeight(1.0f).
                setScoreMode(QueryRescoreMode.Total);
        return resorer;
    }

    /**
     * Assemble the more expensive query for rescoring the results returned by the pre-query.
     *
     * @return rescore query
     */
    private QueryBuilder buildRescoreQuery(StringBuffer userQueryString)
    {
        final ConfigLoader.Config simpleSearchConfig = getConf().get("search").get("default_simple");

        // parse query string
        final SimpleQueryStringBuilder simpleQuery = QueryBuilders.simpleQueryStringQuery(userQueryString.toString());
        simpleQuery.minimumShouldMatch("30%");

        final ConfigLoader.Config[] mainFields = simpleSearchConfig.getArray("main_fields");
        final ArrayList<Object[]> proximityFields = new ArrayList<>();
        for (final ConfigLoader.Config field : mainFields) {
            simpleQuery.field(field.getString("name", ""), field.getFloat("boost", 1.0f)).
                    defaultOperator(Operator.AND).
                    flags(SimpleQueryStringFlag.AND,
                            SimpleQueryStringFlag.OR,
                            SimpleQueryStringFlag.NOT,
                            SimpleQueryStringFlag.PHRASE,
                            SimpleQueryStringFlag.PREFIX,
                            SimpleQueryStringFlag.WHITESPACE);

            // add field to list of proximity-aware fields for later processing
            if (field.getBoolean("proximity_matching", false)) {
                proximityFields.add(new Object[] {
                        field.getString("name"),
                        field.getInteger("proximity_slop", 1),
                        field.getFloat("proximity_boost", 1.0f),
                        field.getFloat("proximity_cutoff_frequency", 0.001f)
                });
            }
        }

        // proximity matching
        BoolQueryBuilder mainQuery = QueryBuilders.boolQuery();
        for (Object[] o : proximityFields) {
            final MatchPhraseQueryBuilder proximityQuery = QueryBuilders.matchPhraseQuery(
                    (String) o[0],
                    userQueryString.toString()
            );
            proximityQuery
                    .slop((Integer) o[1])
                    .boost((Float) o[2] / 2.0f);
            mainQuery.should(proximityQuery);
        }

        mainQuery.must(simpleQuery);

        // general host boost
        SimpleQueryStringBuilder hostBooster = QueryBuilders.simpleQueryStringQuery(userQueryString.toString());
        hostBooster.field("warc_target_hostname");
        hostBooster.boost(20.0f);
        mainQuery.should(hostBooster);

        // Wikipedia boost
        TermQueryBuilder wikiBooster = QueryBuilders.termQuery("warc_target_hostname.raw", "en.wikipedia.org");
        mainQuery.should(wikiBooster);

        return mainQuery;
    }

    /**
     * Helper method for mapping a string to members of {@link FieldValueFactorFunction.Modifier}.
     *
     * @param modifier modifier String representation
     * @return correct enum value
     */
    private FieldValueFactorFunction.Modifier mapStringToFunctionModifier(String modifier)
    {
        modifier = modifier.toUpperCase();
        switch (modifier) {
            case "LOG":
            case "LOG1P":
            case "LOG2P":
            case "LN":
            case "LN1P":
            case "LN2P":
            case "SQUARE":
            case "SQRT":
            case "RECIPROCAL":
                return FieldValueFactorFunction.Modifier.valueOf(modifier);
            default:
                return FieldValueFactorFunction.Modifier.NONE;
        }
    }

    /**
     * Truncate a snippet after a certain number of characters, trying to preserve full words.
     * Will cut the string hard after the specified amount of characters if no spaces could be
     * found or cutting after words would reduce the size more than 2/3 of the desired length.
     *
     * @param snippet the snippet
     * @param numCharacters number of characters after which to truncate
     * @return the truncated snippet
     */
    private String truncateSnippet(String snippet, final int numCharacters)
    {
        if (snippet.length() > numCharacters) {
            final boolean wordEnded = (snippet.charAt(numCharacters) == ' ');
            snippet = snippet.substring(0, numCharacters);

            // get rid of incomplete words
            final int pos = snippet.lastIndexOf(' ');
            if (!wordEnded && -1 != pos) {
                // shorten snippet if it doesn't become too short then
                if ((int)(.6 * numCharacters) <= pos) {
                    snippet = snippet.substring(0, pos);
                }
            }
        }

        return snippet.trim();
    }
}
