/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2015-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.api.v1;

import de.webis.chatnoir2.webclient.api.ApiModuleBase;
import de.webis.chatnoir2.webclient.api.ApiModuleV1;
import de.webis.chatnoir2.webclient.resources.ConfigLoader;
import de.webis.chatnoir2.webclient.search.PhraseSearch;
import de.webis.chatnoir2.webclient.search.SearchResultBuilder;
import de.webis.chatnoir2.webclient.util.Configured;
import de.webis.chatnoir2.webclient.search.ExplanationXContent;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.json.JSONArray;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * ChatNoir API module for pure phrase search.
 */
@ApiModuleV1("_phrases")
public class PhraseSearchApiModule extends ApiModuleBase
{
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        ConfigLoader.Config config = Configured.getInstance().getConf();

        String searchQueryString = getTypedNestedParameter(String.class, "query", request);
        if (null == searchQueryString) {
            searchQueryString = getTypedNestedParameter(String.class, "q", request);
        }

        if (null == searchQueryString || searchQueryString.trim().isEmpty()) {
            final XContentBuilder errObj = generateErrorResponse(request,
                    HttpServletResponse.SC_BAD_REQUEST, "Empty search query");
            writeResponse(response, errObj, HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        final JSONArray indices = getTypedNestedParameter(JSONArray.class, "index", request);
        String[] indicesStr = null;
        if (null != indices) {
            indicesStr = new String[indices.length()];
            for (int i = 0; i < indices.length(); ++i) {
                indicesStr[i] = indices.getString(i);
            }
        }

        Integer from = getTypedNestedParameter(Integer.class, "from", request);
        Integer size = getTypedNestedParameter(Integer.class, "size", request);
        Integer slop = getTypedNestedParameter(Integer.class, "slop", request);
        boolean doExplain = isNestedParameterSet("explain", request);
        boolean snippetOnly = isNestedParameterSet("snippet_only", request);
        if (null == from || from < 0) {
            from = 0;
        }
        if (null == size || size < 1) {
            size = config.getInteger("serp.results_per_page");
        }
        if (null == slop || slop < 0 || slop > config.getInteger("search.phrase_search.max_slop")) {
            slop =  config.getInteger("search.phrase_search.slop");
        }

        final PhraseSearch search = new PhraseSearch(indicesStr);
        final long startTime = System.currentTimeMillis();
        search.setSlop(slop);
        search.setExplain(doExplain);
        search.doSearch(searchQueryString, from, size);
        final long elapsedTime = System.currentTimeMillis() - startTime;

        final List<SearchResultBuilder.SearchResult> results = search.getResults();

        // write query log
        if (from == 0) {
            writeQueryLog(search, request, searchQueryString, false);
        }

        final XContentBuilder builder = getResponseBuilder(request);
        builder.startObject()
            .startObject("meta")
                .field("query_time", elapsedTime)
                .field("total_results", search.getTotalResultNumber())
                .array("indices", search.getEffectiveIndices())
            .endObject()
            .startArray("results");
                for (final SearchResultBuilder.SearchResult result : results) {
                    builder.startObject();
                        builder.field("score", result.score());

                        if (!snippetOnly) {
                            builder.field("uuid", result.documentId())
                                    .field("index", result.index())
                                    .field("trec_id", result.trecId())
                                    .field("target_hostname", result.targetHostname())
                                    .field("target_uri", result.targetUri())
                                    .field("page_rank", result.pageRank())
                                    .field("spam_rank", result.spamRank())
                                    .field("title", result.title());
                        }

                        builder.field("snippet", result.snippet());

                        if (!snippetOnly || doExplain) {
                            builder.field("explanation");
                            new ExplanationXContent(result.explanation()).toXContent(builder, ToXContent.EMPTY_PARAMS);
                        }
                    builder.endObject();
                }
            builder.endArray()
        .endObject();

        writeResponse(response, builder);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        doGet(request, response);
    }
}