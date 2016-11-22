/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.search;

import de.webis.chatnoir2.webclient.resources.ConfigLoader;
import org.apache.lucene.queryparser.xml.builders.*;
import org.apache.lucene.search.TopScoreDocCollector;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.index.query.functionscore.fieldvaluefactor.FieldValueFactorFunctionBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.*;
import org.elasticsearch.search.aggregations.bucket.filters.Filters;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.profile.ProfileShardResult;
import org.json.simple.JSONArray;

import java.io.IOException;
import java.net.InetSocketAddress;
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

        final Settings settings = Settings.settingsBuilder().put("cluster.name", clusterName).build();
        client = new TransportClient.Builder().settings(settings).build().addTransportAddress(
                new InetSocketTransportAddress(new InetSocketAddress(hostName, port)));
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
     * @param searchParameters key-value pairs of search fields
     */
    @Override
    public void doSearch(final HashMap<String, String> searchParameters) throws InvalidSearchFieldException
    {
        if (searchParameters.get("search_query") == null ||
                searchParameters.get("start_at") == null ||
                searchParameters.get("num_results") == null) {
            throw new InvalidSearchFieldException();
        }

        final Integer from = Integer.parseInt(searchParameters.get("start_at"));
        final Integer size = Integer.parseInt(searchParameters.get("num_results"));

        // prepare aggregation
        final AggregationBuilder aggregation = AggregationBuilders.terms("hosts").
                field("warc_target_hostname_raw").order(Terms.Order.count(false)).size(1).
                subAggregation(AggregationBuilders.max("top_score").script(new Script("_score"))).
                subAggregation(AggregationBuilders.topHits("top_hosts").setFetchSource(true).setSize(5));

        // run search
        response = client.prepareSearch(indices).
                setQuery(buildQuery(searchParameters)).
                setFrom(from).
                setSize(size).
                addHighlightedField("body_lang_en", snippetLength, 1).
                addHighlightedField("title_lang_en", titleLength, 1).
                setHighlighterEncoder("html").
                setExplain(doExplain).
                setTerminateAfter(config.get("search").get("default_simple").getInteger("node_limit", 200000)).
                addAggregation(aggregation).
                setProfile(true).
                execute().
                actionGet();

        final StringTerms agg = response.getAggregations().get("hosts");
        for (final Terms.Bucket entry: agg.getBuckets()) {
            System.out.println(entry.getKeyAsString());
        }

        try {
            final Map<String, List<ProfileShardResult>> profileResults = response.getProfileResults();
            if (null != profileResults) {
                final XContentBuilder jsonBuilder = XContentFactory.contentBuilder(XContentType.JSON);
                jsonBuilder.startObject();
                for (final Map.Entry<String, List<ProfileShardResult>> e : profileResults.entrySet()) {
                    for (final ProfileShardResult p : e.getValue()) {
                        jsonBuilder.startObject(e.getKey());
                        p.toXContent(jsonBuilder, ToXContent.EMPTY_PARAMS);
                        jsonBuilder.endObject();
                    }
                }
                jsonBuilder.endObject();
                System.out.println(jsonBuilder.string());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            if (null != hit.getHighlightFields().get("body_lang_en")) {
                final Text[] fragments = hit.getHighlightFields().get("body_lang_en").fragments();
                if (1 >= fragments.length) {
                    snippet = fragments[0].string();
                }
            }

            // use meta description or first body part if no highlighted snippet available
            if (snippet.equals("") && !source.get("meta_desc_lang_en").toString().equals("")) {
                snippet = truncateSnippet(source.get("meta_desc_lang_en").toString(), snippetLength);
            } else if (snippet.equals("")) {
                snippet = truncateSnippet(source.get("body").toString(), snippetLength);
            }

            // use highlighted title if available
            String title = truncateSnippet(source.get("title_lang_en").toString(), titleLength);
            if (null != hit.getHighlightFields().get("title_lang_en")) {
                final Text[] fragments = hit.getHighlightFields().get("title_lang_en").fragments();
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
                    fullBody(source.get("body_lang_en").toString()).
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
        final SimpleQueryStringBuilder simpleQuery = QueryBuilders.simpleQueryStringQuery(userQueryString);

        final ConfigLoader.Config[] mainFields = simpleSearchConfig.getArray("main_fields");
        final ArrayList<Object[]> proximityFields = new ArrayList<>();
        for (final ConfigLoader.Config field : mainFields) {
            simpleQuery.field(field.getString("name", ""), field.getFloat("boost", 1.0f)).
                    defaultOperator(SimpleQueryStringBuilder.Operator.AND).
                    flags(SimpleQueryStringFlag.AND,
                            SimpleQueryStringFlag.OR,
                            SimpleQueryStringFlag.NOT,
                            SimpleQueryStringFlag.PHRASE,
                            SimpleQueryStringFlag.PREFIX);

            // add field to list of proximity-aware fields for later precessing
            if (field.getBoolean("proximity_matching", false)) {
                proximityFields.add(new Object[] {
                        field.getString("name"),
                        field.getInteger("proximity_slop", 1),
                        field.getFloat("proximity_boost", 1.0f),
                        field.getFloat("proximity_cutoff_frequency", 0.001f)
                });
            }
        }

        // wrap main query into function score query
        final ConfigLoader.Config functionScoreConfig = simpleSearchConfig.get("function_scores");
        final FunctionScoreQueryBuilder functionScoreQuery = QueryBuilders.functionScoreQuery(simpleQuery);
        functionScoreQuery.
                boost(functionScoreConfig.getFloat("boost", 1.0f)).
                maxBoost(functionScoreConfig.getFloat("max_boost", Float.MAX_VALUE)).
                boostMode(functionScoreConfig.getString("boost_mode", "multiply")).
                scoreMode(functionScoreConfig.getString("score_mode", "sum"));

        // add function score fields
        final ConfigLoader.Config[] scoreFields = functionScoreConfig.getArray("scoring_fields");
        for (final ConfigLoader.Config c : scoreFields) {
            FieldValueFactorFunction.Modifier modifier = mapStringToFunctionModifier(c.getString("modifier", "none"));
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

        // add range filters (e.g. to filter by minimal content length)
        BoolQueryBuilder mainQuery = QueryBuilders.boolQuery();
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
            mainQuery.filter(rangeFilter);
        }
        mainQuery.must(disMaxQuery);

        // proximity matching
        //BoolQueryBuilder proximityBoolQuery = QueryBuilders.boolQuery();
        for (Object[] o : proximityFields) {
            final MatchQueryBuilder proximityMatchQuery = QueryBuilders.matchQuery(
                    (String) o[0],
                    userQueryString
            );
            proximityMatchQuery.operator(MatchQueryBuilder.Operator.AND);
            proximityMatchQuery.slop((Integer) o[1]);
            proximityMatchQuery.boost((Float) o[2]);
            proximityMatchQuery.cutoffFrequency((Float) o[3]);
            mainQuery.should(proximityMatchQuery);
        }
        //proximityBoolQuery.must(simpleQuery);

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
