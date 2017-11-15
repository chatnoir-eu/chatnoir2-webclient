/*
 * ChatNoir 2 Web Frontend.
 * Copyright (C) 2014-2017 Janek Bevendorff, Webis Group
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package de.webis.chatnoir2.webclient.api.v1;

import de.webis.chatnoir2.webclient.api.ApiBootstrap;
import de.webis.chatnoir2.webclient.api.ApiErrorModule;
import de.webis.chatnoir2.webclient.api.ApiModuleBase;
import de.webis.chatnoir2.webclient.search.SearchResultBuilder;
import de.webis.chatnoir2.webclient.search.SimpleSearch;
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
 * ChatNoir API module for simple search.
 */
@ApiModuleV1("_search")
public class SimpleSearchApiModule extends ApiModuleBase
{
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        String searchQueryString = getTypedNestedParameter(String.class, "query", request);
        if (null == searchQueryString) {
            searchQueryString = getTypedNestedParameter(String.class, "q", request);
        }

        if (null == searchQueryString || searchQueryString.trim().isEmpty()) {
            ApiBootstrap.handleApiError(request, response, ApiErrorModule.SC_BAD_REQUEST,
                    "Empty search query");
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
        boolean doExplain = isNestedParameterSet("explain", request);
        if (null == from || from < 0) {
            from = 0;
        }
        if (null == size || size < 1) {
            size = Configured.getConf().getInteger("serp.results_per_page");
        }

        final SimpleSearch search = new SimpleSearch(indicesStr);
        final long startTime = System.currentTimeMillis();
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
                    builder.startObject()
                        .field("score", result.score())
                        .field("uuid", result.documentId())
                        .field("index", result.index())
                        .field("trec_id", result.trecId())
                        .field("target_hostname", result.targetHostname())
                        .field("target_uri", result.targetUri())
                        .field("page_rank", result.pageRank())
                        .field("spam_rank", result.spamRank())
                        .field("title", result.title())
                        .field("snippet", result.snippet())
                        .field("explanation");
                            new ExplanationXContent(result.explanation()).toXContent(builder, ToXContent.EMPTY_PARAMS);
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