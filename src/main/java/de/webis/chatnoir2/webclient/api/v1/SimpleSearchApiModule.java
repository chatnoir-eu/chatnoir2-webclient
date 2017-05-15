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
import org.json.JSONArray;
import org.json.JSONObject;

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
    @SuppressWarnings("unchecked")
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        final String searchQueryString = getTypedNestedParameter(String.class, "q", request);
        if (null == searchQueryString || searchQueryString.trim().isEmpty()) {
            final JSONObject errObj = generateErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Empty search query");
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

        final JSONObject responseObj = new JSONObject();

        final long startTime = System.currentTimeMillis();
        search.setExplain(null != getTypedNestedParameter(Boolean.class, "explain", request));
        search.doSearch(searchQueryString, from, size);
        final long elapsedTime = System.currentTimeMillis() - startTime;

        final ArrayList<SearchResultBuilder.SearchResult> results = search.getResults();
        final JSONArray resultsJson = new JSONArray();
        for (final SearchResultBuilder.SearchResult result : results) {
            final JSONObject current = new JSONObject();
            current.put("id", result.id());
            current.put("trec_id", result.trecId());
            current.put("link", result.link());
            current.put("title", result.title());
            current.put("snippet", result.snippet());
            final HashMap<String, Object> resMeta = result.metaData();
            for (final String key : resMeta.keySet()) {
                if (key.equals("explanation") && null == resMeta.get(key)) {
                    continue;
                }
                current.put(key, resMeta.get(key));
            }
            resultsJson.put(current);
        }

        final JSONObject searchMeta = new JSONObject();
        searchMeta.put("query_time", elapsedTime);
        searchMeta.put("total_results", search.getTotalResultNumber());
        searchMeta.put("searched_indices", search.getEffectiveIndices());

        responseObj.put("results", resultsJson);
        responseObj.put("meta", searchMeta);

        writeResponse(response, responseObj);
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