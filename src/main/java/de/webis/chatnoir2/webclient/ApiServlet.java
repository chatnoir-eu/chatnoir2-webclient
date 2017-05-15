/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2015-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient;

import de.webis.chatnoir2.webclient.api.ApiModuleV1;
import de.webis.chatnoir2.webclient.api.v1.ApiModuleBase;
import org.reflections.Reflections;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

/**
 * REST API Servlet for ChatNoir 2.
 *
 * @author Janek Bevendorff
 * @version 1
 */
@WebServlet(ApiServlet.ROUTE)
public class ApiServlet extends ChatNoirServlet
{
    enum ApiVersion
    {
        NONE,
        V1
    }

    /**
     * URL Routing for this servlet.
     */
    public static final String ROUTE = "/api/*";

    /**
     * Initialize servlet.
     */
    @Override
    public void init()
    {
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        String apiModulePattern = "_default";
        ApiVersion apiModuleVersion = ApiVersion.NONE;
        final String pathInfo = request.getPathInfo();
        if (null != pathInfo) {
            final String[] parts = pathInfo.split("/");
            if (2 <= parts.length) {
                switch (parts[1]) {
                    case "v1":
                        apiModuleVersion = ApiVersion.V1;
                        break;
                }
            }
            if (3 <= parts.length) {
                apiModulePattern = parts[2];
            }
        }

        if (ApiVersion.NONE == apiModuleVersion) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("ERROR: Invalid API version.");
            response.getWriter().flush();
            return;
        }

        final ApiModuleBase apiHandler = bootstrapApiModuleFromPattern(apiModulePattern, apiModuleVersion);
        if (null == apiHandler) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("ERROR: No API modules found. This shouldn't happen at all.");
            response.getWriter().flush();
            return;
        }

        if (apiHandler.validateRequest(request, response)) {
            if (request.getMethod().equals("GET")) {
                apiHandler.doGet(request, response);
            } else if (request.getMethod().equals("POST")) {
                apiHandler.doPost(request, response);
            } else {
                apiHandler.rejectMethod(response);
            }
        }
    }

    /**
     * Dynamically load {@link ApiModuleBase} child class to handle API request based on
     * the given URL pattern string and the classes' ApiModuleVX annotation values.
     *
     * @param pathPattern path pattern (e.g. '_default')
     * @return instance of {@link ApiModuleBase} child class or null if none found
     * @throws IllegalArgumentException if given invalid API version
     */
    private ApiModuleBase bootstrapApiModuleFromPattern(final String pathPattern, final ApiVersion apiVersion)
    {
        final Reflections reflections = new Reflections("de.webis.chatnoir2.webclient.api");

        if (apiVersion == ApiVersion.V1) {
            final Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(ApiModuleV1.class);
            for (final Class<?> apiModuleClass : annotated) {
                final ApiModuleV1 moduleAnnotation = apiModuleClass.getAnnotation(ApiModuleV1.class);
                if (Arrays.asList(moduleAnnotation.value()).contains(pathPattern)) {
                    try {
                        final Object tmpObj = apiModuleClass.getConstructor().newInstance();
                        if (tmpObj instanceof ApiModuleBase) {
                            return (ApiModuleBase) tmpObj;
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        throw new IllegalArgumentException("Invalid API version");
    }
}
