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

import de.webis.chatnoir2.webclient.response.Renderer;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * ChatNoir 2 Error page servlet.
 */
@WebServlet(ErrorServlet.ROUTE)
public class ErrorServlet extends ChatNoirServlet
{
    /**
     * URL Routing for this servlet.
     */
    public static final String ROUTE = "/error";

    /**
     * Default Mustache template.
     */
    private static final String TEMPLATE_INDEX = "/templates/chatnoir2-error.mustache";

    @Override
    protected void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        if (response.getStatus() == HttpServletResponse.SC_OK) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }

        Map<String, Object> templateVars = new HashMap<>();
        switch (response.getStatus()) {
            case HttpServletResponse.SC_NOT_FOUND:
                templateVars.put("notFound", true);
                break;
            case HttpServletResponse.SC_FORBIDDEN:
                templateVars.put("forbidden", true);
                break;
            case HttpServletResponse.SC_BAD_REQUEST:
                templateVars.put("badRequest", true);
                break;
            case HttpServletResponse.SC_INTERNAL_SERVER_ERROR:
                templateVars.put("internalServerError", true);
                break;
            default:
                templateVars.put("other", true);
                break;
        }
        String errorString = (String) request.getAttribute("javax.servlet.error.message");
        if (null != errorString) {
            templateVars.put("errorString", errorString);
        }
        templateVars.put("errorCode", response.getStatus());

        Renderer.render(getServletContext(), request, response, TEMPLATE_INDEX, templateVars);
    }
}
