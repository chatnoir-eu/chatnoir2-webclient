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

package de.webis.chatnoir2.webclient;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;

import de.webis.chatnoir2.webclient.resources.ConfigLoader;
import de.webis.chatnoir2.webclient.response.Renderer;
import de.webis.chatnoir2.webclient.search.SearchResultBuilder;
import de.webis.chatnoir2.webclient.search.SimpleSearch;
import de.webis.chatnoir2.webclient.util.Configured;

/**
 * ChatNoir 2 main search servlet.
 */
@WebServlet(SearchServlet.ROUTE)
public class SearchServlet extends ChatNoirServlet
{
    /**
     * URL Routing for this servlet.
     */
    public static final String ROUTE = "/search/*";

    /**
     * Default Mustache template.
     */
    private static final String TEMPLATE_INDEX = "/templates/chatnoir2-search.mustache";

    /**
     * Number results to show per page.
     */
    private int mResultsPerPage = 10;

    /**
     * Initialize servlet.
     */
    @Override
    public void init()
    {
        mResultsPerPage = Configured.getInstance().getConf().getInteger("serp.results_per_page", mResultsPerPage);
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        // only allow access via index servlet forward
        if (!isForwardedForm(request, IndexServlet.ROUTE)) {
            forwardError(request, response, HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        final String searchQueryString = request.getParameter("q");
        if (null == searchQueryString || searchQueryString.trim().isEmpty()) {
            redirect(request, response, IndexServlet.ROUTE);
            return;
        }

        String[] indices = request.getParameterValues("index");

        final HashMap<String, Object> templateVars = new HashMap<>();
        templateVars.put("searchQuery", searchQueryString);
        templateVars.put("searchQueryUrlEnc", URLEncoder.encode(searchQueryString, "UTF-8"));
        templateVars.put("queryString", request.getAttribute("javax.servlet.forward.query_string"));

        final SimpleSearch search = new SimpleSearch(indices);

        int currentPage = 1;
        final String pageNumber = request.getParameter("p");
        if (null != pageNumber) {
            try {
                currentPage = Math.max(Integer.parseInt(pageNumber), 1);
            } catch (NumberFormatException ignored) { }
        }

        final long startTime = System.nanoTime();
        search.setExplain(null != request.getParameter("explain"));
        search.doSearch(searchQueryString, (currentPage - 1) * mResultsPerPage, mResultsPerPage);
        final long elapsedTime = System.nanoTime() - startTime;
        templateVars.put("queryTime", String.format("%.1fms", elapsedTime * 0.000001));

        // list effective and allowed indices
        List<Map<String, Object>> allowedIndices = new ArrayList<>();
        String[] allowedArr = search.getAllowedIndices();
        List<String> effectiveArr = Arrays.asList(search.getEffectiveIndices());

        // get index display names
        ConfigLoader.Config[] aliases = Configured.getInstance().getConf().getArray("cluster.index_aliases");
        for (String allowed: allowedArr) {
            Map<String, Object> m = new HashMap<>();

            for (ConfigLoader.Config c: aliases) {
                if (!allowed.equals(c.getString("alias")) && !allowed.equals(c.getString("index"))) {
                    continue;
                }
                String displayName = c.getString("display_name");
                if (null != displayName) {
                    m.put("displayName", displayName);
                } else {
                    m.put("displayName", allowed);
                }
            }
            m.put("name", allowed);
            m.put("selected", effectiveArr.contains(allowed));
            allowedIndices.add(m);
        }
        templateVars.put("allowedIndices", allowedIndices);
        templateVars.put("indices", effectiveArr);
        templateVars.put("isSearch", true);

        long numResults = search.getTotalResultNumber();
        final long currentPageCapped = Math.max(1, Math.min((long) Math.ceil((double) numResults / mResultsPerPage), currentPage));

        // if user navigated past last page
        if (currentPage != currentPageCapped) {
            response.sendRedirect(String.format("%s?q=%s&p=%d",
                    request.getAttribute("javax.servlet.forward.request_uri"),
                    URLEncoder.encode(request.getParameter("q"), "UTF-8"),
                    currentPageCapped));
            return;
        }

        // write query log
        if (currentPage == 1) {
            writeQueryLog(search, request, searchQueryString, true);
        }

        final SERPContext serpContext = new SERPContext();
        serpContext.setResults(search.getResults());
        serpContext.setPagination(numResults, mResultsPerPage, currentPage);
        serpContext.setTerminatedEarly(search.isTerminatedEarly());

        Renderer.render(getServletContext(), request, response, TEMPLATE_INDEX, templateVars, serpContext);
    }

    /**
     * Mustache context class for search results page.
     */
    @SuppressWarnings("unused")
    public static class SERPContext
    {
        /**
         * The registered individual results.
         */
        private List<SearchResultBuilder.SearchResult> mResults = new ArrayList<>();

        /**
         * Total number of results.
         */
        private long mNumResults;

        /**
         * How many results to show per page.
         */
        private int mResultsPerPage;

        /**
         * Number of the current page.
         */
        private long mCurrentPage;

        /**
         * Whether search terminated early.
         */
        private boolean mTerminatedEarly;

        /**
         * Mustache accessor for search results.
         * @return list of search results
         */
        public List<SearchResultBuilder.SearchResult> searchResults()
        {
            return mResults;
        }

        /**
         * Mustache accessor for pagination.
         * @return hashmap list with pagination info, null if there are 0 results
         */
        public List<HashMap<String, String>> pagination()
        {
            if (0 == mNumResults) {
                return null;
            }

            // default pagination hard limit by Elasticsearch
            final int maxPage = 10000 / mResultsPerPage;

            final List<HashMap<String, String>> pagination = new ArrayList<>();
            int numPages      = Math.min((int) Math.ceil((double) mNumResults / mResultsPerPage), maxPage);
            long displayPages = Math.min(Math.min(mCurrentPage + 5, numPages), maxPage);
            long startingPage = Math.min(Math.max(mCurrentPage - 4, 1), maxPage - 4);

            // go to first page
            if (5 < mCurrentPage) {
                final HashMap<String, String> page = new HashMap<>();
                page.put("pageNumber", Integer.toString(1));
                page.put("ariaHiddenLabel", "←");
                page.put("hiddenLabel", "First");
                pagination.add(page);
            }

            // go to previous page
            if (1 != mCurrentPage) {
                final HashMap<String, String> page = new HashMap<>();
                page.put("pageNumber", Long.toString(mCurrentPage - 1));
                page.put("ariaHiddenLabel", "«");
                page.put("hiddenLabel", "Previous");
                pagination.add(page);
            }

            // page numbers
            for (long i = startingPage; i <= displayPages; ++i) {
                final HashMap<String, String> page = new HashMap<>();
                page.put("pageNumber", Long.toString(i));
                page.put("label", Long.toString(i));
                if (i == mCurrentPage) {
                    page.put("active", "1");
                }
                pagination.add(page);
            }

            // go to next page
            if (numPages != mCurrentPage) {
                final HashMap<String, String> page = new HashMap<>();
                page.put("pageNumber", Long.toString(mCurrentPage + 1));
                page.put("ariaHiddenLabel", "»");
                page.put("hiddenLabel", "Next");
                pagination.add(page);
            }

            return pagination;
        }

        /**
         * Mustache accessor for current page.
         *
         * @return page number
         */
        public long currentPage()
        {
            return mCurrentPage;
        }

        /**
         * Whether search terminated early.
         */
        public boolean terminatedEarly()
        {
            return mTerminatedEarly;
        }

        /**
         * Set whether search terminated early.
         */
        public void setTerminatedEarly(boolean terminatedEarly)
        {
            mTerminatedEarly = terminatedEarly;
        }

        /**
         * Mustache accessor returning true if search results have explanations.
         *
         * @return true if explain mode is on
         */
        public boolean isExplainMode()
        {
            return 0 != mResults.size() && mResults.get(0).explanation() != null;
        }

        /**
         * Mustache accessor for general pagination info.
         * @return meta information for pagination
         */
        public HashMap<String, String> paginationInfo()
        {
            HashMap<String, String> paginationInfo = new HashMap<>();
            paginationInfo.put("currentPage", Long.toString(mCurrentPage));
            paginationInfo.put("resultsRangeStart", Long.toString(1 + (mCurrentPage - 1) * mResultsPerPage));
            paginationInfo.put("resultsRangeEnd", Long.toString(Math.min((mCurrentPage - 1) * mResultsPerPage + mResultsPerPage, mNumResults)));
            paginationInfo.put("numResults", String.format("%,d%s", mNumResults, terminatedEarly() ? "+" : ""));

            return paginationInfo;
        }

        /**
         * Mustache accessor for whether there are any search results.
         */
        public boolean resultsFound()
        {
            return mNumResults != 0;
        }

        /**
         * Add search result.
         *
         * @param result the result
         */
        public void addResult(final SearchResultBuilder.SearchResult result)
        {
            mResults.add(result);
        }

        /**
         * Set search results. Overwrites any existing results
         *
         * @param results the result
         */
        public void setResults(final List<SearchResultBuilder.SearchResult> results)
        {
            mResults = results;
        }

        /**
         * Set pagination info.
         *
         * @param numResults     total number of results
         * @param resultsPerPage number of results to show per page
         * @param currentPage    the current page
         */
        public void setPagination(final long numResults, final int resultsPerPage, final long currentPage)
        {
            mNumResults = numResults;
            mResultsPerPage = resultsPerPage;
            mCurrentPage = Math.min(currentPage, 1000);
        }
    }
}
