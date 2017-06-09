/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2015-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.auth.api;

import de.webis.chatnoir2.webclient.api.ApiBootstrap;
import de.webis.chatnoir2.webclient.api.ApiErrorModule;
import de.webis.chatnoir2.webclient.api.ApiModuleBase;
import de.webis.chatnoir2.webclient.auth.ChatNoirAuthenticationFilter;
import de.webis.chatnoir2.webclient.auth.ChatNoirAuthenticationFilter.AuthFilter;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Authentication filter for ChatNoir 2 API requests.
 */
@SuppressWarnings("unused")
@AuthFilter
public class ApiAuthenticationFilter extends ChatNoirAuthenticationFilter
{
    public static final String NAME = "api";
    public static final String PATH = "/api/**";
    public static final int ORDER   = 0;

    /**
     * Retrieve API key / token from HTTP request if available.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @return API authentication token or null
     */
    public static AuthenticationToken retrieveToken(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        ApiModuleBase apiModule = ApiBootstrap.bootstrapApiModule(request, response);
        return apiModule.getUserToken(request);
    }

    @Override
    public void doFilterInternal(ServletRequest request, ServletResponse response, FilterChain chain)
    {
        try {
            // catch any exceptions that might occur on the way
            super.doFilterInternal(request, response, chain);
        } catch (Throwable e) {
            ApiBootstrap.handleException(e, (HttpServletRequest) request, (HttpServletResponse) response);
        }
    }

    @Override
    protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) throws Exception
    {
        return retrieveToken((HttpServletRequest) request, (HttpServletResponse) response);
    }

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception
    {
        return executeLogin(request, response);
    }

    @Override
    protected boolean onLoginSuccess(AuthenticationToken token, Subject subject,
                                     ServletRequest request, ServletResponse response) {
        return true;
    }

    @Override
    protected boolean onLoginFailure(AuthenticationToken token, AuthenticationException e,
                                     ServletRequest request, ServletResponse response) {
        HttpServletRequest httpRequest   = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            ApiBootstrap.handleApiError(httpRequest, httpResponse,
                    ApiErrorModule.SC_UNAUTHORIZED, "Missing or invalid API key");
        } catch (Throwable exception) {
            ApiBootstrap.handleException(exception, httpRequest, httpResponse);
        }

        return false;
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public String getPathPattern()
    {
        return PATH;
    }

    @Override
    public int getOrder()
    {
        return ORDER;
    }
}
