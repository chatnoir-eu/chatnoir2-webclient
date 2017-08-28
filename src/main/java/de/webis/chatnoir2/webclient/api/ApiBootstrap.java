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

import de.webis.chatnoir2.webclient.api.exceptions.*;
import de.webis.chatnoir2.webclient.api.v1.ApiModuleV1;
import de.webis.chatnoir2.webclient.util.AnnotationClassLoader;
import de.webis.chatnoir2.webclient.util.Configured;
import org.elasticsearch.common.Nullable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

/**
 * API module singleton bootstrap class.
 */
public class ApiBootstrap
{
    /**
     * API version enum.
     */
    public enum ApiVersion
    {
        NONE,
        V1
    }

    private static final HashMap<String, ApiModuleBase> mInstances = new HashMap<>();
    private static ApiModuleBase mErrorModule = null;

    /**
     * Dynamically create an {@link ApiModuleBase} instance to handle API request based on
     * the given URL pattern string. If the module can't be found, the error module
     * <code>ERROR_not_found</code> will be returned.
     * Created instances will be cached. Subsequent calls to this method with the same
     * parameters will return the same instance.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @return requested instance of {@link ApiModuleBase} or error module instance if not found
     * @throws InvalidApiVersionException if given invalid API version
     * @throws ApiModuleNotFoundException if the module could not be found
     */
    public static ApiModuleBase bootstrapApiModule(HttpServletRequest request, HttpServletResponse response)
            throws UserErrorException, ServletException, IOException
    {
        String apiModulePattern = "_default";
        String apiVersionPattern;
        ApiVersion apiModuleVersion = ApiVersion.NONE;
        String pathPattern = request.getPathInfo();

        if (null == pathPattern) {
            throw new ApiModuleNotFoundException("Empty path");
        }
        Path path = Paths.get(pathPattern);

        if (1 <= path.getNameCount()) {
            apiVersionPattern = path.getName(0).toString();
            switch (apiVersionPattern) {
                case "v1":
                    apiModuleVersion = ApiVersion.V1;
                    break;
            }
        }

        if (apiModuleVersion == ApiVersion.NONE) {
            throw new InvalidApiVersionException("Invalid API version");
        }

        if (2 <= path.getNameCount()) {
            apiModulePattern = path.getName(1).toString();
        }

        synchronized (mInstances) {
            if (mInstances.containsKey(apiModulePattern)) {
                ApiModuleBase instance = mInstances.get(apiModulePattern);
                instance.initApiRequest(request, response);
                return instance;
            }

            ApiModuleBase instance = AnnotationClassLoader.newInstance(
                    "de.webis.chatnoir2.webclient.api.v1",
                    apiModulePattern,
                    ApiModuleV1.class,
                    ApiModuleBase.class);

            if (null != instance) {
                mInstances.put(apiModulePattern, instance);
                return instance;
            }
        }

        throw new ApiModuleNotFoundException("No API endpoint found for path " + apiModulePattern);
    }

    /**
     * Return a configured API error module instance.
     *
     * @param request HTTP request
     */
    public synchronized static ApiModuleBase getErrorModule(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        if (null == mErrorModule) {
            mErrorModule = new ApiErrorModule();
        }
        mErrorModule.initApiRequest(request, response);
        return mErrorModule;
    }

    /**
     * Set the given HTTP status code and write an appropriate error response.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param statusCode response status code
     */
    public static void handleApiError(HttpServletRequest request, HttpServletResponse response, int statusCode)
            throws ServletException, IOException
    {
        response.setStatus(statusCode);
        ApiBootstrap.getErrorModule(request, response).service(request, response);
    }

    /**
     * Write an appropriate error response and set HTTP status code.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param statusCode response status code
     * @param errorMessage custom error message (null for default message)
     */
    public static void handleApiError(HttpServletRequest request, HttpServletResponse response,  int statusCode,
                                      @Nullable String errorMessage) throws ServletException, IOException
    {
        if (null != errorMessage) {
            request.setAttribute(ApiErrorModule.CUSTOM_ERROR_MSG_ATTR, errorMessage);
        }
        response.setStatus(statusCode);
        ApiBootstrap.getErrorModule(request, response).service(request, response);
    }

    /**
     * Handle uncaught exceptions.
     *
     * @param exception exception
     * @param request HTTP request
     * @param response HTTP response
     */
    public static void handleException(Throwable exception, HttpServletRequest request, HttpServletResponse response)
    {
        int statusCode;
        String message;

        if (exception instanceof ServletException && exception.getCause() != null) {
            exception = exception.getCause();
        }

        if (exception instanceof ApiModuleNotFoundException) {
            statusCode = ApiErrorModule.SC_NOT_FOUND;
            message = "API endpoint not found";
        } else if (exception instanceof RemoteAddressNotAllowedException) {
            statusCode = ApiErrorModule.SC_FORBIDDEN;
            message = "Remote IP not allowed";
        } else if (exception instanceof QuotaExceededException) {
            statusCode = ApiErrorModule.SC_TOO_MANY_REQUESTS;
            message = "Quota exceeded";
        } else if (exception instanceof MethodNotAllowedException) {
            statusCode = ApiErrorModule.SC_METHOD_NOT_ALLOWED;
            response.setHeader("Allow", String.join(",",
                    ((MethodNotAllowedException) exception).getAllowedMethods()));
            message = "Method not allowed";
        } else if (exception instanceof NotImplementedException) {
            statusCode = ApiErrorModule.SC_NOT_IMPLEMENTED;
            message = "Not implemented";
        } else if (exception instanceof UserErrorException) {
            statusCode = ApiErrorModule.SC_BAD_REQUEST;
            message = exception.getMessage();
        } else {
            statusCode = ApiErrorModule.SC_INTERNAL_SERVER_ERROR;
            message = "An internal server error occurred. Please try again later.";
            Configured.getInstance().getSysLogger().error("Internal server exception:", exception);
        }

        try {
            ApiBootstrap.handleApiError(request, response, statusCode, message);
        } catch (Throwable e) {
            Configured.getInstance().getSysLogger().error(
                    "While writing an error response, the following exception occurred:", e);
        }
    }
}
