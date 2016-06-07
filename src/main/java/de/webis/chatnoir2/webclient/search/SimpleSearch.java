/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.search;

import de.webis.chatnoir2.webclient.resources.ConfigLoader;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.index.query.functionscore.fieldvaluefactor.FieldValueFactorFunctionBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHitsAggregator;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHitsBuilder;
import org.json.simple.JSONArray;

import java.io.IOException;
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
     * Elasticsearch TransportClient.
     */
    private final TransportClient client;

    /**
     * (Default) snippet length.
     */
    private int snippetLength = 400;

    /**
     * (Default) title length.
     */
    private int titleLength = 70;

    /**
     * Array of indices to search.
     */
    private String[] indices;

    /**
     * Global configuration.
     */
    private final ConfigLoader.Config config;

    /**
     * Whether to explain query.
     */
    private boolean doExplain = false;

    /**
     * Search response of the last search.
     */
    private SearchResponse response = new SearchResponse();

    /**
     * Constructor.
     *
     * @param indices List of index names to search (null means use default from config).
     *                Indices that are not present in the config will be ignored.
     */
    public SimpleSearch(final List<String> indices)
    {
        config = new Object() {
            public ConfigLoader.Config run()
            {
                try {
                    return ConfigLoader.getInstance().getConfig();
                } catch (IOException | ConfigLoader.ParseException e) {
                    e.printStackTrace();
                    return new ConfigLoader.Config();
                }
            }
        }.run();

        final String clusterName = config.get("cluster").getString("cluster_name", "");
        final String hostName    = config.get("cluster").getString("host", "localhost");
        final int port           = config.get("cluster").getInteger("port", 9300);
        snippetLength            = config.get("serp").getInteger("snippet_length", snippetLength);
        titleLength              = config.get("serp").getInteger("title_length", titleLength);

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
                this.indices = usedIndices.toArray(new String[usedIndices.size()]);
                useDefaultIndex = false;
            }
        }
        if (useDefaultIndex) {
            this.indices = new String[]{config.get("cluster").getString("default_index", allowedIndices.get(0))};
        }

        final Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", clusterName).build();
        client = new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress(hostName, port));
    }

    public SimpleSearch()
    {
        this(null);
    }

    /**
     * Get a List of all indices that are allowed by the config and are therefore actually used.
     *
     * @return List of index names
     */
    public List<String> getEffectiveIndexList()
    {
        return Arrays.asList(this.indices);
    }

    /**
     * Set whether to explain search queries.
     *
     * @param doExplain true if query shall be explained
     */
    public void setExplain(final boolean doExplain) {
        this.doExplain = doExplain;
    }

    /**
     * Perform a simple search.
     * Expects the following fields present:
     *      'search_query' the terms to search for.
     *      'start_at'     at what result offset to start
     *      'num_results'  how many results to return
     *
     * @param searchFields key-value pairs of search fields
     */
    @Override
    public void doSearch(final HashMap<String, String> searchFields) throws InvalidSearchFieldException
    {
        if (searchFields.get("search_query") == null ||
                searchFields.get("start_at") == null ||
                searchFields.get("num_results") == null) {
            throw new InvalidSearchFieldException();
        }

        final Integer from = Integer.parseInt(searchFields.get("start_at"));
        final Integer size = Integer.parseInt(searchFields.get("num_results"));

        // prepare aggregation
        final AggregationBuilder aggregation = AggregationBuilders.terms("hosts").
                field("warc_target_hostname_raw").
                subAggregation(AggregationBuilders.topHits("top").
                        setExplain(doExplain).
                        setFrom(from).
                        setSize(1));

        // run search
        response = client.prepareSearch(indices).
                setQuery(buildQuery(searchFields)).
                setFrom(from).
                setSize(size).
                addHighlightedField("body", snippetLength, 1).
                addHighlightedField("title", titleLength, 1).
                setHighlighterEncoder("html").
                setExplain(doExplain).
                //addAggregation(aggregation).
                execute().
                actionGet();
    }

    @Override
    public void finish() {
        if (null != client) {
            client.close();
        }
    }

    @Override
    public ArrayList<SearchResultBuilder.SearchResult> getResults()
    {
        final ArrayList<SearchResultBuilder.SearchResult> results = new ArrayList<>();
        String previousHost = "";
        for (final SearchHit hit : response.getHits()) {
            final Map<String, Object> source = hit.getSource();

            String snippet = "";
            if (null != hit.getHighlightFields().get("body")) {
                final Text[] fragments = hit.getHighlightFields().get("body").fragments();
                if (1 >= fragments.length) {
                    snippet = fragments[0].string();
                }
            }

            // use meta description or first body part if no highlighted snippet available
            if (snippet.equals("") && !source.get("meta_desc").toString().equals("")) {
                snippet = truncateSnippet(source.get("meta_desc").toString(), snippetLength);
            } else if (snippet.equals("")) {
                snippet = truncateSnippet(source.get("body").toString(), snippetLength);
            }

            // use highlighted title if available
            String title = truncateSnippet(source.get("title").toString(), titleLength);
            if (null != hit.getHighlightFields().get("title")) {
                final Text[] fragments = hit.getHighlightFields().get("title").fragments();
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
            final String currentHost = (String) source.get("warc_target_hostname_raw");
            if (previousHost.equals(currentHost)) {
                doGroup = true;
            }
            previousHost = currentHost;

            final SearchResultBuilder.SearchResult result = new SearchResultBuilder().
                    id(hit.getId()).
                    trecId(source.get("warc_trec_id").toString()).
                    title(title).
                    link(source.get("warc_target_uri").toString()).
                    snippet(snippet).
                    fullBody(source.get("body").toString()).
                    addMetadata("score", hit.getScore()).
                    addMetadata("page_rank", source.get("page_rank")).
                    addMetadata("spam_rank", source.get("spam_rank")).
                    addMetadata("explanation", explanation).
                    addMetadata("has_explanation", doExplain).
                    suggestGrouping(doGroup).
                    build();
            results.add(result);
        }

        return results;
    }

    @Override
    public long getTotalResultNumber()
    {
        return response.getHits().getTotalHits();
    }


    /**
     * Helper method for assembling the search query.
     *
     * @return assembled BaseQueryBuilder object
     */
    private QueryBuilder buildQuery(final HashMap<String, String> searchFields)
    {
        final String userQueryString = searchFields.get("search_query");
        final ConfigLoader.Config simpleSearchConfig = config.get("search").get("default_simple");

        // parse query string
        final SimpleQueryStringBuilder mainQuery = QueryBuilders.simpleQueryString(userQueryString);
        final ConfigLoader.Config[] mainFields = simpleSearchConfig.getArray("main_fields");
        final ArrayList<String> proximityFields = new ArrayList<>();
        for (final ConfigLoader.Config field : mainFields) {
            mainQuery.field(field.getString("name", ""), field.getFloat("boost", 1.0f)).
                    defaultOperator(SimpleQueryStringBuilder.Operator.AND).
                    flags(SimpleQueryStringFlag.AND,
                            SimpleQueryStringFlag.OR,
                            SimpleQueryStringFlag.NOT,
                            SimpleQueryStringFlag.PHRASE,
                            SimpleQueryStringFlag.PREFIX);

            // add field to list of proximity-aware fields for later precessing
            if (field.getBoolean("proximity_matching", false)) {
                proximityFields.add(field.getString("name"));
            }
        }

        // wrap main query into function score query
        final ConfigLoader.Config functionScoreConfig      = simpleSearchConfig.get("function_scores");
        final FunctionScoreQueryBuilder functionScoreQuery = QueryBuilders.functionScoreQuery(mainQuery);
        functionScoreQuery.
                boost(functionScoreConfig.getFloat("boost", 1.0f)).
                maxBoost(functionScoreConfig.getFloat("max_boost", Float.MAX_VALUE)).
                boostMode(functionScoreConfig.getString("boost_mode", "multiply")).
                scoreMode(functionScoreConfig.getString("score_mode", "multiply"));

        // add function score fields
        final ConfigLoader.Config[] scoreFields = functionScoreConfig.getArray("scoring_fields");
        for (final ConfigLoader.Config c : scoreFields) {
            FieldValueFactorFunction.Modifier modifier                = mapStringToFunctionModifier(c.getString("modifier", "none"));
            FieldValueFactorFunctionBuilder fieldValueFunctionBuilder = ScoreFunctionBuilders.
                    fieldValueFactorFunction(c.getString("name")).
                    factor(c.getFloat("factor", 1.0f));

            // work around nasty bug with NONE enum constant
            if (FieldValueFactorFunction.Modifier.NONE != modifier) {
                fieldValueFunctionBuilder.modifier(modifier);
            }

            functionScoreQuery.add(fieldValueFunctionBuilder);
        }

        // wrap function score query into dismax query
        final DisMaxQueryBuilder disMaxQuery = QueryBuilders.disMaxQuery();
        disMaxQuery.add(functionScoreQuery);

        // mix in additional fields
        final ConfigLoader.Config[] subFields = simpleSearchConfig.getArray("additional_fields");
        for (final ConfigLoader.Config field : subFields) {
            disMaxQuery.add(
                    QueryBuilders.
                            matchQuery(field.getString("name"), userQueryString).
                            boost(field.getFloat("boost", 1.0f)).
                            operator(MatchQueryBuilder.Operator.AND)
            );
        }

        // set global tie breaker
        disMaxQuery.tieBreaker(simpleSearchConfig.getFloat("tie_breaker", 0.0f));

        // save pointer to dismax query for conditional decoration
        QueryBuilder finalQuery = disMaxQuery;

        // add proximity rankings
        if (0 != proximityFields.size()) {
            final BoolQueryBuilder proximityBoolQuery = QueryBuilders.boolQuery();
            final MultiMatchQueryBuilder proximityMatchQuery = QueryBuilders.multiMatchQuery(userQueryString);
            proximityFields.forEach(proximityMatchQuery::field);
            proximityMatchQuery.operator(MatchQueryBuilder.Operator.AND);
            proximityMatchQuery.slop(simpleSearchConfig.getInteger("proximity_slop", 1));
            proximityMatchQuery.boost(simpleSearchConfig.getFloat("proximity_boost", 1.0f));
            proximityMatchQuery.cutoffFrequency(simpleSearchConfig.getFloat("proximity_cutoff_frequency", 0.001f));
            proximityBoolQuery.must(finalQuery);
            proximityBoolQuery.should(proximityMatchQuery);

            finalQuery = proximityBoolQuery;
        }

        // add range filters (e.g. to filter by minimal content length)
        final ConfigLoader.Config[] rangeFilters = simpleSearchConfig.getArray("range_filters");
        for (final ConfigLoader.Config filterConfig : rangeFilters) {
            final RangeFilterBuilder rangeFilter = FilterBuilders.rangeFilter(filterConfig.getString("name", ""));
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
            finalQuery = QueryBuilders.filteredQuery(finalQuery, rangeFilter);
        }

        // limit per-node results for performance reasons
        final int nodeLimit = simpleSearchConfig.getInteger("node_limit", -1);
        if (0 < nodeLimit) {
            final FilterBuilder limitFilter = FilterBuilders.limitFilter(nodeLimit);
            finalQuery = QueryBuilders.filteredQuery(finalQuery, limitFilter);
        }

        return finalQuery;
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
