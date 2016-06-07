/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import de.webis.chatnoir2.webclient.response.Renderer;
import de.webis.chatnoir2.webclient.search.DocumentRetriever;
import de.webis.chatnoir2.webclient.search.SearchProvider;
import de.webis.chatnoir2.webclient.search.SearchResultBuilder;

/**
 * Index Servlet for Chatnoir 2.
 *
 * @author Janek Bevendorff
 * @version 1
 */
@WebServlet(CacheServlet.ROUTE)
public class CacheServlet extends ChatNoirServlet
{
    /**
     * URL Routing for this servlet.
     */
    public static final String ROUTE = "/cache";

    /**
     * Default Mustache template.
     */
    private static final String TEMPLATE_INDEX = "/templates/chatnoir2-cache.mustache";

    /**
     * GET action for this servlet.
     *
     * @param request   The HTTP request
     * @param response  The HTTP response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        super.doGet(request, response);

        if (null == request.getParameter("docId")) {
            getServletContext().getRequestDispatcher(SearchServlet.ROUTE).forward(request, response);
            return;
        }

        final boolean rawMode       = (null != request.getParameter("raw"));
        final boolean plainTextMode = (null != request.getParameter("plain"));

        final DocumentRetriever retriever = new DocumentRetriever(getParameter("i", request));
        final HashMap<String, String> map = new HashMap<>();
        map.put("docId", getParameter("docId", request));

        try {
            retriever.doSearch(map);
            final ArrayList<SearchResultBuilder.SearchResult> results = retriever.getResults();
            if (0 == results.size()) {
                response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                getServletContext().getRequestDispatcher(SearchServlet.ROUTE).forward(request, response);
                return;
            }

            if (!rawMode) {
                final HashMap<String, String> templateVars = new HashMap<>();
                templateVars.put("doc-id", results.get(0).id());
                templateVars.put("trec-id", results.get(0).trecId());
                templateVars.put("title", results.get(0).title());
                templateVars.put("index", retriever.getEffectiveIndex());
                if (plainTextMode) {
                    templateVars.put("plainTextMode", "1");
                }
                //templateVars.put("cache-result", results.get(0).fullBody);
                Renderer.render(getServletContext(), request, response, TEMPLATE_INDEX, templateVars);
            } else {
                if (plainTextMode) {
                    response.setContentType("text/plain");
                    response.getWriter().print(results.get(0).fullBody());
                } else {
                    response.setContentType("text/html");
                    response.getWriter().print(results.get(0).rawHTML());
                }
            }
        } catch (SearchProvider.InvalidSearchFieldException e) {
            e.printStackTrace();
        } finally {
            retriever.finish();
        }
    }
}
