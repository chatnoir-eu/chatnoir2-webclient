/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2015-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient;

import de.webis.chatnoir2.webclient.api.ApiBootstrap;
import de.webis.chatnoir2.webclient.api.ApiModuleBase;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * REST API Servlet for ChatNoir 2.
 *
 * @author Janek Bevendorff
 */
@WebServlet(ApiServlet.ROUTE)
public class ApiServlet extends ChatNoirServlet
{
    /**
     * URL Routing for this servlet.
     */
    public static final String ROUTE = "/api/*";

    @Override
    protected void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        ApiModuleBase apiHandler;
        try {
            apiHandler = ApiBootstrap.bootstrapApiModule(request);
        } catch (ApiBootstrap.InvalidApiVersionException e) {
            ApiBootstrap.handleInvalidApiVersion(response);
            return;
        }

        String requestMethod = request.getMethod();
        if (requestMethod.equals("GET") || requestMethod.equals("POST")) {
            apiHandler.service(request, response);
        } else {
            apiHandler.rejectMethod(request, response);
        }
    }
}
