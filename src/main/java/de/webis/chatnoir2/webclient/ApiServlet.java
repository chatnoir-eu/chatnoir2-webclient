/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2015 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient;

import de.webis.chatnoir2.webclient.api.ApiModule;
import de.webis.chatnoir2.webclient.api.ApiModuleBase;
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
        final String pathInfo = request.getPathInfo();
        if (null != pathInfo) {
            final String[] parts = pathInfo.split("/");
            if (2 <= parts.length) {
                apiModulePattern = parts[1];
            }
        }
        final ApiModuleBase apiHandler = bootstrapApiModuleFromPattern(apiModulePattern);
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
     * the given URL pattern string and the classes' {@link ApiModule} annotation values.
     *
     * @param pathPattern path pattern (e.g. '_default')
     * @return instance of {@link ApiModuleBase} child class or null if none found
     */
    private ApiModuleBase bootstrapApiModuleFromPattern(final String pathPattern)
    {
        final Reflections reflections = new Reflections("de.webis.chatnoir2.webclient.api");
        final Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(ApiModule.class);
        for (final Class<?> apiModuleClass : annotated) {
            final ApiModule moduleAnnotation = apiModuleClass.getAnnotation(ApiModule.class);
            if (Arrays.asList(moduleAnnotation.value()).contains(pathPattern)) {
                try {
                    final Object tmpObj = apiModuleClass.getConstructor().newInstance();
                    if (tmpObj instanceof ApiModuleBase) {
                        return (ApiModuleBase) tmpObj;
                    }
                } catch (Exception ignored) { }
            }
        }

        return null;
    }
}
