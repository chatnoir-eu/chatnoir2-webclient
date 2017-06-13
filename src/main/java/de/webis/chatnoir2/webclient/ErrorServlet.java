/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
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
        templateVars.put("errorCode", response.getStatus());

        Renderer.render(getServletContext(), request, response, TEMPLATE_INDEX, templateVars);
    }
}
