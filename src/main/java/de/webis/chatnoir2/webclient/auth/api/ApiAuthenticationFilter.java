/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2015-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.auth.api;

import de.webis.chatnoir2.webclient.api.ApiBootstrap;
import de.webis.chatnoir2.webclient.api.ApiModuleBase;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Authentication filter for ChatNoir 2 API requests.
 */
public class ApiAuthenticationFilter extends AuthenticatingFilter
{
    private static final String ERROR_INDICATOR_ATTRIBUTE = ApiAuthenticationFilter.class.getName() + ".error";

    @Override
    protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) throws Exception
    {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            ApiModuleBase apiModule = ApiBootstrap.bootstrapApiModule(httpRequest);
            return apiModule.getUserToken(httpRequest);
        } catch (ApiBootstrap.InvalidApiVersionException e) {
            ApiBootstrap.handleInvalidApiVersion((HttpServletResponse) response);
        } catch (Exception e) {
            ApiBootstrap.handleException(e, (HttpServletRequest) request, (HttpServletResponse) response);
        }

        request.setAttribute(ERROR_INDICATOR_ATTRIBUTE, true);
        return new ApiKeyAuthenticationToken("");
    }

    @Override
    public boolean onPreHandle(ServletRequest request, ServletResponse response, Object mappedValue) throws Exception {
        // turn off Java sessions
        request.setAttribute(DefaultSubjectContext.SESSION_CREATION_ENABLED, Boolean.FALSE);
        return super.onPreHandle(request, response, mappedValue);
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
}
