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

package de.webis.chatnoir2.webclient.auth.api;

import de.webis.chatnoir2.webclient.api.ApiBootstrap;
import de.webis.chatnoir2.webclient.api.ApiErrorModule;
import de.webis.chatnoir2.webclient.api.ApiModuleBase;
import de.webis.chatnoir2.webclient.api.exceptions.QuotaExceededException;
import de.webis.chatnoir2.webclient.api.exceptions.RemoteAddressNotAllowedException;
import de.webis.chatnoir2.webclient.api.exceptions.UserErrorException;
import de.webis.chatnoir2.webclient.auth.ChatNoirAuthenticationFilter;
import de.webis.chatnoir2.webclient.auth.ChatNoirAuthenticationFilter.AuthFilter;
import de.webis.chatnoir2.webclient.auth.ChatNoirWebSessionManager;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.subject.WebSubject;
import org.apache.shiro.web.util.WebUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;

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
    public static AuthenticationToken retrieveToken(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, UserErrorException
    {
        ApiModuleBase apiModule = ApiBootstrap.bootstrapApiModule(request, response);
        return apiModule.getUserToken(request);
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
                                     ServletRequest request, ServletResponse response)
    {
        return true;
    }

    @Override
    protected boolean onLoginFailure(AuthenticationToken token, AuthenticationException e,
                                     ServletRequest request, ServletResponse response)
    {
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

    /**
     * @throws QuotaExceededException if user authenticated, but exceeded their quota
     * @throws RemoteAddressNotAllowedException if user authenticated, but accesses service from forbidden remote address
     */
    @Override
    protected void executeChain(ServletRequest request, ServletResponse response, FilterChain chain) throws Exception
    {
        WebSubject subject = (WebSubject) SecurityUtils.getSubject();
        if (subject.isAuthenticated()) {
            // validate user quota
            DefaultWebSecurityManager securityManager = ((DefaultWebSecurityManager) SecurityUtils.getSecurityManager());
            ChatNoirWebSessionManager sessionManager  = (ChatNoirWebSessionManager) securityManager.getSessionManager();
            if (!sessionManager.validateApiSessionQuota(subject)) {
                throw new QuotaExceededException("API user quota exceeded");
            }

            // validate remote IP address
            Set<InetAddress> remoteHosts = ApiTokenRealm.getTypedPrincipalField(subject, "remote_hosts");
            if (null != remoteHosts && !remoteHosts.isEmpty()) {
                InetAddress ip = InetAddress.getByName(request.getRemoteHost());

                // trust X-Forwarded-For header only if forwarded from localhost
                if (InetAddress.getByName("127.0.0.1").equals(ip) || InetAddress.getByName("::1").equals(ip)) {
                    try {
                        String forwardedHost = WebUtils.toHttp(request).getHeader("X-Forwarded-For");
                        if (null != forwardedHost) {
                            ip = InetAddress.getByName(forwardedHost);
                        }
                    } catch (UnknownHostException ignored) {}
                }

                if (!remoteHosts.contains(ip)) {
                    throw new RemoteAddressNotAllowedException("Remote IP not allowed");
                }
            }

            sessionManager.incrementApiQuotaUsage(subject.getSession());
        }

        super.executeChain(request, response, chain);
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
