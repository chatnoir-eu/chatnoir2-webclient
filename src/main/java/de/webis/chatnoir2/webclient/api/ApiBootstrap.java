/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2015-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.api;

import de.webis.chatnoir2.webclient.api.error.AuthenticationError;
import de.webis.chatnoir2.webclient.api.error.NotFoundError;
import org.reflections.Reflections;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

/**
 * API module singleton bootstrap class.
 */
public class ApiBootstrap
{
    public static class InvalidApiVersionException extends Exception
    {
        public InvalidApiVersionException()
        {
            super();
        }

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
    public static ApiModuleBase bootstrapApiModule(HttpServletRequest request) throws InvalidApiVersionException
    {
        String apiModulePattern = "_default";
        String apiVersionPattern;
        ApiVersion apiModuleVersion = ApiVersion.NONE;
        String pathPattern = request.getPathInfo();
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

        if (mInstances.containsKey(pathPattern)) {
            ApiModuleBase instance = mInstances.get(pathPattern);
            instance.initApiRequest(request);
            return instance;
        }

        final Reflections reflections = new Reflections("de.webis.chatnoir2.webclient.api");

        if (apiModuleVersion == ApiVersion.V1) {
            final Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(ApiModuleV1.class);
            for (final Class<?> apiModuleClass : annotated) {
                final ApiModuleV1 moduleAnnotation = apiModuleClass.getAnnotation(ApiModuleV1.class);
                if (Arrays.asList(moduleAnnotation.value()).contains(apiModulePattern)) {
                    try {
                        final Object tmpObj = apiModuleClass.getConstructor().newInstance();
                        if (tmpObj instanceof ApiModuleBase) {
                            synchronized (mInstances) {
                                mInstances.put(pathPattern, (ApiModuleBase) tmpObj);
                            }
                            ((ApiModuleBase) tmpObj).initApiRequest(request);
                            return (ApiModuleBase) tmpObj;
                        }
                    } catch (Exception ignored) {}
                }
            }
        } else {
            throw new InvalidApiVersionException("Invalid API version");
        }

        return getNotFoundErrorModule(request);
    }

    /**
     * Get initialized 404 Not Found error module.
     *
     * @param request HTTP request
     */
    public synchronized static ApiModuleBase getNotFoundErrorModule(HttpServletRequest request)
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
    public synchronized static ApiModuleBase getAuthenticationErrorModule(HttpServletRequest request)
    {
        if (null == mAuthenticationModule) {
            mAuthenticationModule = new AuthenticationError();
        }
        mAuthenticationModule.initApiRequest(request);
        return mAuthenticationModule;
    }

    /**
     * Write an error message indicating that the requested API version is invalid.
     *
     * @param response HTTP response
     */
    public static void handleInvalidApiVersion(HttpServletResponse response) throws IOException
    {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.getWriter().println("ERROR: Invalid API version.");
        response.getWriter().flush();
    }
}
