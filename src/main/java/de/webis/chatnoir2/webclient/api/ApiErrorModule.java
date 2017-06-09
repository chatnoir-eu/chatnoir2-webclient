/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2015-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.api;

import org.elasticsearch.common.xcontent.XContentBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Not Found error module.
 */
public class ApiErrorModule extends ApiModuleBase
{
    public static final int SC_NOT_FOUND             = HttpServletResponse.SC_NOT_FOUND;
    public static final int SC_FORBIDDEN             = HttpServletResponse.SC_FORBIDDEN;
    public static final int SC_UNAUTHORIZED          = HttpServletResponse.SC_UNAUTHORIZED;
    public static final int SC_BAD_REQUEST           = HttpServletResponse.SC_BAD_REQUEST;
    public static final int SC_TOO_MANY_REQUESTS     = 429;

    public static final int SC_INTERNAL_SERVER_ERROR = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

    /**
     * Add an attribute with this key to the request to provide a customized error message.
     */
    public static String CUSTOM_ERROR_MSG_ATTR = ApiErrorModule.class.getName() + ".CUSTOM_MSG";

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response)  throws IOException, ServletException
    {
        super.service(request, response);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        int errorCode = response.getStatus();
        final XContentBuilder errorObj;

        String customMsg = (String) request.getAttribute(CUSTOM_ERROR_MSG_ATTR);
        if (null != customMsg) {
            errorObj = generateErrorResponse(request, errorCode, customMsg);
            writeResponse(response, errorObj, errorCode);
            return;
        }

        switch (errorCode) {
            case SC_BAD_REQUEST:
                errorObj = generateErrorResponse(request, errorCode, "Bad request");
                break;

            case SC_NOT_FOUND:
                errorObj = generateErrorResponse(request, errorCode, "API endpoint not found");
                break;

            case SC_FORBIDDEN:
                errorObj = generateErrorResponse(request, errorCode, "Forbidden");
                break;

            case SC_UNAUTHORIZED:
                errorObj = generateErrorResponse(request, errorCode, "Unauthorized");
                break;

            case SC_TOO_MANY_REQUESTS:
                errorObj = generateErrorResponse(request, errorCode, "Quota exceeded");
                break;

            case SC_INTERNAL_SERVER_ERROR:
                errorObj = generateErrorResponse(request, errorCode, "Internal server error.");
                break;

            default:
                errorObj = generateErrorResponse(request, errorCode, "");
        }
        writeResponse(response, errorObj, errorCode);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        doGet(request, response);
    }
}
