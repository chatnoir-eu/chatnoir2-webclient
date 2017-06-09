/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.filters;

import de.webis.chatnoir2.webclient.api.ApiBootstrap;
import de.webis.chatnoir2.webclient.auth.api.ApiAuthenticationFilter;
import de.webis.chatnoir2.webclient.util.Configured;
import org.apache.shiro.util.AntPathMatcher;
import org.apache.shiro.web.servlet.ShiroFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Catch and handle any exceptions that might occur on the way.
 * This filter needs to be at the very top of the filter chain.
 */
@WebFilter(filterName="ErrorFilter", urlPatterns = ErrorFilter.ROUTE)
public class ErrorFilter extends ShiroFilter
{
    static final String ROUTE = "/*";

    private static final AntPathMatcher mPathmatcher = new AntPathMatcher();

    @Override
    public void doFilterInternal(ServletRequest request, ServletResponse response, FilterChain chain)
    {
        try {
            super.doFilterInternal(request, response, chain);
        } catch (Throwable e) {
            if (mPathmatcher.matches(ApiAuthenticationFilter.PATH, ((HttpServletRequest) request).getRequestURI())) {
                ApiBootstrap.handleException(e, (HttpServletRequest) request, (HttpServletResponse) response);
            } else {
                // TODO: properly handle web frontend errors
                Configured.getInstance().getSysLogger().error("Exception:", e);
            }
        }
    }
}
