/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2015-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.api;

import de.webis.chatnoir2.webclient.api.error.AuthenticationError;
import de.webis.chatnoir2.webclient.api.error.NotFoundError;
import de.webis.chatnoir2.webclient.api.v1.ApiModuleV1;
import de.webis.chatnoir2.webclient.util.AnnotationClassLoader;
import de.webis.chatnoir2.webclient.util.Configured;
import org.elasticsearch.common.xcontent.XContentBuilder;

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
    public static class InvalidApiVersionException extends ApiModuleBase.UserErrorException
    {
        public InvalidApiVersionException(String message)
        {
            super(message);
        }
    }

    public enum ApiVersion
    {
        NONE,
        V1
    }

    private static final HashMap<String, ApiModuleBase> mInstances = new HashMap<>();
    private static ApiModuleBase mNotFoundModule = null;
    private static ApiModuleBase mAuthenticationModule = null;

    /**
     * Dynamically create an {@link ApiModuleBase} instance to handle API request based on
     * the given URL pattern string. If the module can't be found, the error module
     * <code>ERROR_not_found</code> will be returned.
     * Created instances will be cached. Subsequent calls to this method with the same
     * parameters will return the same instance.
     *
     * @param request HTTP request
     * @return requested instance of {@link ApiModuleBase} or error module instance if not found
     * @throws InvalidApiVersionException if given invalid API version
     */
    public static ApiModuleBase bootstrapApiModule(HttpServletRequest request) throws ServletException
    {
        String apiModulePattern = "_default";
        String apiVersionPattern;
        ApiVersion apiModuleVersion = ApiVersion.NONE;
        String pathPattern = request.getPathInfo();

        if (null == pathPattern) {
            return getNotFoundErrorModule(request);
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
        if (2 <= path.getNameCount()) {
            apiModulePattern = path.getName(1).toString();
        }

        synchronized (mInstances) {
            if (mInstances.containsKey(apiModulePattern)) {
                ApiModuleBase instance = mInstances.get(apiModulePattern);
                instance.initApiRequest(request);
                return instance;
            }

            ApiModuleBase instance;
            if (apiModuleVersion == ApiVersion.V1) {
                instance = AnnotationClassLoader.newInstance(
                        "de.webis.chatnoir2.webclient.api.v1",
                        apiModulePattern,
                        ApiModuleV1.class,
                        ApiModuleBase.class);
            } else {
                throw new InvalidApiVersionException("Invalid API version");
            }

            if (null != instance) {
                mInstances.put(apiModulePattern, instance);
                return instance;
            }
        }

        return getNotFoundErrorModule(request);
    }

    /**
     * Get initialized 404 Not Found error module.
     *
     * @param request HTTP request
     */
    public synchronized static ApiModuleBase getNotFoundErrorModule(HttpServletRequest request) throws ServletException
    {
        if (null == mNotFoundModule) {
            mNotFoundModule = new NotFoundError();
        }
        mNotFoundModule.initApiRequest(request);
        return mNotFoundModule;
    }

    /**
     * Get initialized 401 Authentication error module.
     *
     * @param request HTTP request
     */
    public synchronized static ApiModuleBase getAuthenticationErrorModule(HttpServletRequest request) throws ServletException
    {
        if (null == mAuthenticationModule) {
            mAuthenticationModule = new AuthenticationError();
        }
        mAuthenticationModule.initApiRequest(request);
        return mAuthenticationModule;
    }

    /**
     * Handle uncaught exceptions.
     *
     * @param exception exception
     * @param request HTTP request
     * @param response HTTP response
     */
    public static void handleException(Exception exception, HttpServletRequest request, HttpServletResponse response)
    {
        String message;
        int responseCode;
        if (exception instanceof ApiModuleBase.UserErrorException) {
            message = exception.getMessage();
            responseCode = HttpServletResponse.SC_BAD_REQUEST;
        } else {
            message = "An internal server error occurred. Please try again later.";
            responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            Configured.getInstance().getSysLogger().error("Internal server exception:", exception);
        }

        ApiModuleBase moduleBase = new ApiModuleBase()
        {
            @Override
            public void doGet(HttpServletRequest request, HttpServletResponse response)
                    throws IOException, ServletException {}

            @Override
            public void doPost(HttpServletRequest request, HttpServletResponse response)
                    throws IOException, ServletException {}
        };
        try {
            moduleBase.initApiRequest(request);
        } catch (Exception ignored) {}
        final XContentBuilder errorObj = moduleBase.generateErrorResponse(request, responseCode, message);
        try {
            moduleBase.writeResponse(response, errorObj, responseCode);
        } catch (IOException e) {
            Configured.getInstance().getSysLogger().error(
                    "While writing an error response, the following exception occurred:", exception);
        }
    }
}
