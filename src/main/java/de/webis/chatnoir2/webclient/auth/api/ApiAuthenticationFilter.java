/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2015-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.auth.api;

import de.webis.chatnoir2.webclient.api.ApiBootstrap;
import de.webis.chatnoir2.webclient.api.ApiModuleBase;
import de.webis.chatnoir2.webclient.auth.ChatNoirAuthenticationFilter;
import de.webis.chatnoir2.webclient.auth.ChatNoirAuthenticationFilter.AuthFilter;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;

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

    private static final String ERROR_INDICATOR_ATTRIBUTE = ApiAuthenticationFilter.class.getName() + ".error";

    /**
     * Retrieve API key / token from HTTP request if available.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @return API authentication token or null
     */
    public static AuthenticationToken retrieveToken(HttpServletRequest request, HttpServletResponse response) {
        try {
            ApiModuleBase apiModule = ApiBootstrap.bootstrapApiModule(request);
            return apiModule.getUserToken(request);
        } catch (Exception e) {
            ApiBootstrap.handleException(e, request, response);
        }

        request.setAttribute(ERROR_INDICATOR_ATTRIBUTE, true);
        return null;
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

    protected boolean onLoginSuccess(AuthenticationToken token, Subject subject,
                                     ServletRequest request, ServletResponse response) {
        return true;
    }

    protected boolean onLoginFailure(AuthenticationToken token, AuthenticationException e,
                                     ServletRequest request, ServletResponse response) {
        try {
            if (request.getAttribute(ERROR_INDICATOR_ATTRIBUTE) == null) {
                // don't print an authentication failure message if an error occurred
                ApiModuleBase errorModule = ApiBootstrap.getAuthenticationErrorModule((HttpServletRequest) request);
                errorModule.service(request, response);
            }
        } catch (ServletException | IOException ignored) {}

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
