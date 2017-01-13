/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2015 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.api;

import de.webis.chatnoir2.webclient.ChatNoirServlet;
import de.webis.chatnoir2.webclient.resources.ApiKeyManager;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Base class for ChatNoir REST API modules.
 */
public abstract class ApiModuleBase extends ChatNoirServlet
{
    /**
     * Check if request is a valid request or write an error response if not.
     * Overriding methods should always call the parent method before running custom checks.
     * Servlets should not continue if this method returns false.
     *
     * @param request HTTP request
     * @return true if request is valid
     */
    public boolean validateRequest(final HttpServletRequest request, final HttpServletResponse response)
    {
        final String keyParameter = ChatNoirServlet.getParameter("apiKey", request);
        if (null != keyParameter && ApiKeyManager.getInstance().isApiKeyValid(keyParameter)) {
            return true;
        } else {
            final JSONObject errorResponse = generateErrorResponse(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid API key");
            try {
                writeResponse(response, errorResponse, HttpServletResponse.SC_UNAUTHORIZED);
            } catch (IOException ignored) { }
            return false;
        }
    }

    /**
     * Generate a JSON response object to indicate an error.
     *
     * @param errorCode API/HTTP error code
     * @param errorMessage error description
     * @return generated JSONObject
     */
    @SuppressWarnings("unchecked")
    public final JSONObject generateErrorResponse(final int errorCode, final String errorMessage)
    {
        final JSONObject responseObj = new JSONObject();
        final JSONObject errorObj = new JSONObject();
        errorObj.put("code", errorCode);
        errorObj.put("msg", errorMessage);
        responseObj.put("error", errorObj);
        return responseObj;
    }

    /**
     * Write an error response for rejecting the used HTTP request method.
     *
     * @param response HTTP response object
     */
    public final void rejectMethod(final HttpServletResponse response)
    {
        final JSONObject errorObj = generateErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Unsupported request method");
        try {
            writeResponse(response, errorObj, HttpServletResponse.SC_BAD_REQUEST);
        } catch (IOException ignored) { }
    }

    /**
     * Write API output to HTTP response with default status code 200.
     *
     * @param response HTTP response object
     * @param responseObject JSON answer
     * @throws IOException
     */
    public void writeResponse(final HttpServletResponse response, final JSONObject responseObject) throws IOException
    {
        writeResponse(response, responseObject, HttpServletResponse.SC_OK);
    }

    /**
     * Write API output to HTTP response with default status code 200.
     *
     * @param response HTTP response object
     * @param responseObject JSON answer
     * @param responseCode HTTP response status code
     * @throws IOException
     */
    public void writeResponse(final HttpServletResponse response, final JSONObject responseObject, final int responseCode) throws IOException
    {
        response.setStatus(responseCode);
        response.setContentType("application/json");
        response.getWriter().write(responseObject.toString());
        response.getWriter().flush();
    }

    /**
     * Handle GET request to API endpoint.
     * If GET is not supported for this request, {@link #rejectMethod(HttpServletResponse)} should be used to
     * generate and write an appropriate error response.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @throws IOException
     */
    @Override
    public abstract void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException;

    /**
     * Handle POST request to API endpoint.
     * If POST is not supported for this request, {@link #rejectMethod(HttpServletResponse)} should be used to
     * generate and write an appropriate error response.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @throws IOException
     */
    @Override
    public abstract void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException;
}