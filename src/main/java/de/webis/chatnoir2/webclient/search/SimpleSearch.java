/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.search;

import de.webis.chatnoir2.webclient.resources.ConfigLoader;
import de.webis.chatnoir2.webclient.util.TextCleanser;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction;
import org.elasticsearch.common.lucene.search.function.FiltersFunctionScoreQuery;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.functionscore.FieldValueFactorFunctionBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.profile.ProfileShardResult;
import org.elasticsearch.search.rescore.QueryRescoreMode;
import org.elasticsearch.search.rescore.QueryRescorerBuilder;
import org.elasticsearch.search.rescore.RescoreBuilder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.json.JSONArray;

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

        final Settings settings = Settings.builder()
                .put("cluster.name", clusterName)
                .put("client.transport.sniff", true)
                .build();
        client = new PreBuiltTransportClient(settings).addTransportAddress(
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
        // unfortunately still not working with rescoring in Elasticsearch 2.4.3
        /*final AggregationBuilder aggregation = AggregationBuilders.terms("hosts")
                .field("warc_target_hostname_raw")
                .order(Terms.Order.aggregation("top_score", false))
                .subAggregation(AggregationBuilders.topHits("top_sites").setSize(4))
                .subAggregation(AggregationBuilders.max("top_score").field("_score"));*/


        final ConfigLoader.Config simpleSearchConfig = config.get("search").get("default_simple");

        // run search
        response = client.prepareSearch(indices)
                .setQuery(buildPreQuery(searchParameters))
                .setRescorer(buildRescorer(buildRescoreQuery(searchParameters)),
                       simpleSearchConfig.getInteger("rescore_window", size))
                .setFrom(from)
                .setSize(size)
                .highlighter(new HighlightBuilder()
                        .field("title_lang_en", titleLength, 1)
                        .field("body_lang_en", snippetLength, 1)
                        .encoder("html"))
                .setExplain(doExplain)
                .setTerminateAfter(simpleSearchConfig.getInteger("node_limit", 200000))
                //.addAggregation(aggregation)
                .setProfile(false)
                .execute()
                .actionGet();

        /*final Terms agg = response.getAggregations().get("hosts");
        for (final Terms.Bucket bucket: agg.getBuckets()) {
            System.out.println(bucket.getKeyAsString());
            TopHits ts = bucket.getAggregations().get("top_sites");
            for (SearchHit h: ts.getHits().hits()) {
                System.out.printf(" >   %s%n", h.sourceAsMap().get("warc_target_uri").toString());
            }
        }*/

        /*
        try {
            final Map<String, ProfileShardResult> profileResults = response.getProfileResults();
            if (null != profileResults) {
                final XContentBuilder jsonBuilder = XContentFactory.contentBuilder(XContentType.JSON);
                jsonBuilder.startObject();
                for (final Map.Entry<String, ProfileShardResult> e : profileResults.entrySet()) {
                    for (final ProfileShardResult p : e.getValue()) {
                        jsonBuilder.startObject(e.getKey());
                        p.toXContent(jsonBuilder, ToXContent.EMPTY_PARAMS);
                        jsonBuilder.endObject();
                    }
                }
                jsonBuilder.endObject();
                System.out.println(jsonBuilder.string());

                final XContentBuilder jsonBuilder2 = XContentFactory.contentBuilder(XContentType.JSON);
                jsonBuilder2.startObject();
                response.toXContent(jsonBuilder2, ToXContent.EMPTY_PARAMS);
                jsonBuilder2.endObject();
                System.out.println(jsonBuilder2.string());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
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
                snippet = truncateSnippet(source.get("body_lang_en").toString(), snippetLength);
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

            String pr = String.format("%.03f", (Double) source.get("page_rank"));
            if (0.001 > (Double) source.get("page_rank")) {
                pr = String.format("%.03e", (Double) source.get("page_rank"));
            }

            final SearchResultBuilder.SearchResult result = new SearchResultBuilder().
                    id(hit.getId()).
                    trecId(source.get("warc_trec_id").toString()).
                    title(TextCleanser.cleanse(title, true)).
                    link(source.get("warc_target_uri").toString()).
                    snippet(TextCleanser.cleanse(snippet, true)).
                    fullBody(source.get("body_lang_en").toString()).
                    addMetadata("score", String.format("%.03f", hit.getScore())).
                    addMetadata("page_rank", pr).
                    addMetadata("spam_rank", (0 != (Integer) source.get("spam_rank")) ? source.get("spam_rank") : "none").
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
     * Assemble the fast pre-query for use with a rescorer.
     *
     * @return assembled pre-query
     */
    private QueryBuilder buildPreQuery(final HashMap<String, String> searchFields)
    {
        BoolQueryBuilder mainQuery = QueryBuilders.boolQuery();

        final String userQueryString = searchFields.get("search_query");
        final ConfigLoader.Config simpleSearchConfig = config.get("search").get("default_simple");

        final SimpleQueryStringBuilder searchQuery = QueryBuilders.simpleQueryStringQuery(userQueryString);
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
    private QueryBuilder buildRescoreQuery(final HashMap<String, String> searchFields)
    {
        final String userQueryString = searchFields.get("search_query");
        final ConfigLoader.Config simpleSearchConfig = config.get("search").get("default_simple");

        // parse query string
        final SimpleQueryStringBuilder simpleQuery = QueryBuilders.simpleQueryStringQuery(userQueryString);
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
                    userQueryString
            );
            proximityQuery
                    .slop((Integer) o[1])
                    .boost((Float) o[2] / 2.0f);
            mainQuery.should(proximityQuery);
        }

        mainQuery.must(simpleQuery);

        // general host boost
        SimpleQueryStringBuilder hostBooster = QueryBuilders.simpleQueryStringQuery(userQueryString);
        hostBooster.field("warc_target_hostname");
        hostBooster.boost(20.0f);
        mainQuery.should(hostBooster);

        // Wikipedia boost
        TermQueryBuilder wikiBooster = QueryBuilders.termQuery("warc_target_hostname_raw", "en.wikipedia.org");
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
