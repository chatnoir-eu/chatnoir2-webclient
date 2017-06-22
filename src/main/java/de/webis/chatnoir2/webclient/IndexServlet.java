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

        if (!getStrippedRequestURI(request).equals(ROUTE)) {
            forwardError(request, response, HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Renderer.render(getServletContext(), request, response, TEMPLATE_INDEX);
    }
}
