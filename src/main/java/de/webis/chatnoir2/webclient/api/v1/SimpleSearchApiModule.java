/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2015-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.api.v1;

import de.webis.chatnoir2.webclient.api.ApiModuleV1;
import de.webis.chatnoir2.webclient.search.SearchResultBuilder;
import de.webis.chatnoir2.webclient.search.SimpleSearch;
import de.webis.chatnoir2.webclient.util.Configured;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.json.JSONArray;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Base class for ChatNoir REST API modules.
 */
@ApiModuleV1("_search")
public class SimpleSearchApiModule extends ApiModuleBase
{
    /**
     * Handle GET request to API endpoint.
     * If GET is not supported for this request, {@link #rejectMethod(HttpServletResponse)} should be used to
     * generate and write an appropriate error response.
     *
     * @param request HTTP request
     * @param response HTTP response
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        final String searchQueryString = getTypedNestedParameter(String.class, "q", request);
        if (null == searchQueryString || searchQueryString.trim().isEmpty()) {
            final XContentBuilder errObj = generateErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Empty search query");
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

        final SimpleSearch search = new SimpleSearch(indicesStr);

        Integer from = getTypedNestedParameter(Integer.class, "from", request);
        Integer size = getTypedNestedParameter(Integer.class, "size", request);
        if (null == from || from < 1) {
            from = 1;
        }
        if (null == size || size < 1) {
            size = new Configured().getConf().get("serp").get("pagination").getInteger("results_per_page");
        }

        final long startTime = System.currentTimeMillis();
        search.setExplain(null != getTypedNestedParameter(Boolean.class, "explain", request));
        search.doSearch(searchQueryString, from, size);
        final long elapsedTime = System.currentTimeMillis() - startTime;

        final ArrayList<SearchResultBuilder.SearchResult> results = search.getResults();

        final XContentBuilder jb = getResponseBuilder();
        jb.startObject()
                .startObject("meta")
                    .field("query_time", elapsedTime)
                    .field("total_results", search.getTotalResultNumber())
                    .array("indices", search.getEffectiveIndices())
                .endObject()
                .startArray("results");

                    for (final SearchResultBuilder.SearchResult result : results) {
                        jb.startObject()
                                .field("uuid", result.id())
                                .field("trec_id", result.trecId())
                                .field("hostname", result.hostname())
                                .field("link", result.link())
                                .field("title", result.title())
                                .field("snippet", result.snippet())
                                .startObject("meta_data");
                                    final HashMap<String, Object> resMeta = result.metaData();
                                    for (final String key : resMeta.keySet()) {
                                        if (key.equals("explanation") && null == resMeta.get(key)) {
                                            continue;
                                        }
                                        jb.field(key, resMeta.get(key));
                                    }
                                jb.endObject();
                        jb.endObject();
                    }
                jb.endArray()
        .endObject();

        writeResponse(response, jb);
    }

    /**
     * Handle POST request to API endpoint.
     * If POST is not supported for this request, {@link #rejectMethod(HttpServletResponse)} should be used to
     * generate and write an appropriate error response.
     *
     * @param request HTTP request
     * @param response HTTP response
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        doGet(request, response);
    }
}