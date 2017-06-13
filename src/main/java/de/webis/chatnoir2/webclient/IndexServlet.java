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

import de.webis.chatnoir2.webclient.response.Renderer;

/**
 * Index Servlet for ChatNoir 2.
 */
@WebServlet(IndexServlet.ROUTE)
public class IndexServlet extends ChatNoirServlet
{
    /**
     * URL Routing for this servlet.
     */
    public static final String ROUTE = "/";

    /**
     * Default Mustache template.
     */
    private static final String TEMPLATE_INDEX = "/templates/chatnoir2-index.mustache";

    /**
     * GET action for this servlet.
     *
     * @param request   The HTTP request
     * @param response  The HTTP response
     */
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        if (null != request.getParameter("q")) {
            getServletContext().getRequestDispatcher(SearchServlet.ROUTE).forward(request, response);
            return;
        }

        if (!request.getRequestURI().equals(ROUTE)) {
            forwardError(request, response, HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Renderer.render(getServletContext(), request, response, TEMPLATE_INDEX);
    }
}
