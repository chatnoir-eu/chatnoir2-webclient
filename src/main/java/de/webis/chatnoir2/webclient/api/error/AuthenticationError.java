/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2015-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.api.error;

import de.webis.chatnoir2.webclient.api.ApiModuleBase;
import org.elasticsearch.common.xcontent.XContentBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Authentication error module.
 */
public class AuthenticationError extends ApiModuleBase
{
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        final XContentBuilder errorObj = generateErrorResponse(request,
                HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid API key");
        writeResponse(response, errorObj, HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        doGet(request, response);
    }
}
