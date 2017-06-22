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
    public static final int SC_NOT_IMPLEMENTED       = HttpServletResponse.SC_NOT_IMPLEMENTED;
    public static final int SC_METHOD_NOT_ALLOWED    = HttpServletResponse.SC_METHOD_NOT_ALLOWED;
    public static final int SC_TOO_MANY_REQUESTS     = 429;

    public static final int SC_INTERNAL_SERVER_ERROR = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

    /**
     * Add an attribute with this key to the request to provide a customized error message.
     */
    public static String CUSTOM_ERROR_MSG_ATTR = ApiErrorModule.class.getName() + ".CUSTOM_MSG";

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response)  throws IOException, ServletException
    {
        handleError(request, response);
    }

    public void handleError(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
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

            case SC_METHOD_NOT_ALLOWED:
                errorObj = generateErrorResponse(request, errorCode, "Method not allowed");
                break;

            case SC_NOT_IMPLEMENTED:
                errorObj = generateErrorResponse(request, errorCode, "Not implemented");
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
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        handleError(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        handleError(request, response);
    }
}
