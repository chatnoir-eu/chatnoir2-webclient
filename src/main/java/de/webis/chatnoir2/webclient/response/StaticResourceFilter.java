/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.response;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;

@WebFilter(urlPatterns = {
        StaticResourceFilter.ROUTE1,
        StaticResourceFilter.ROUTE2,
        StaticResourceFilter.ROUTE3,
        StaticResourceFilter.ROUTE4
    }
)

/**
 * Filter requests to serve static content.
 */
public class StaticResourceFilter implements Filter
{
    /**
     * Routes for static content.
     */
    public static final String
        ROUTE1 = "/static/*",
        ROUTE2 = "*.ico",
        ROUTE3 = "*.txt",
        ROUTE4 = "*.gif";

    /**
     * Request dispatcher.
     */
    private RequestDispatcher rd;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        rd = filterConfig.getServletContext().getNamedDispatcher("default");
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException
    {
        rd.forward(req, resp);
    }

    @Override
    public void destroy() {}
}
