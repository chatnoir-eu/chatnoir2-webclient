/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.auth;

import de.webis.chatnoir2.webclient.api.ApiBootstrap;
import de.webis.chatnoir2.webclient.api.exceptions.UserErrorException;
import de.webis.chatnoir2.webclient.auth.api.ApiAuthenticationFilter;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SessionContext;
import org.apache.shiro.util.AntPathMatcher;
import org.apache.shiro.web.servlet.ShiroHttpServletRequest;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.apache.shiro.web.util.WebUtils;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;

/**
 * ChatNoir 2 web session manager.
 */
@WebListener
public class ChatNoirWebSessionManager extends DefaultWebSessionManager
{
    public static final String USER_TOKEN_ATTR = ChatNoirWebSessionManager.class.getName() + ".CONTEXT";

    public ChatNoirWebSessionManager()
    {
        super();
        getSessionIdCookie().setName("SID");

        // TODO: make this dependent on whether this is a web frontend or API response
        setSessionIdCookieEnabled(false);
    }

    /**
     * Get user API token from request or null if no token was specified or this is no API request.
     *
     * @param request HTTP request
     * @return API token as string
     */
    protected Serializable getApiUserToken(HttpServletRequest request, HttpServletResponse response) throws UserErrorException
    {
        if (!new AntPathMatcher().matches(ApiAuthenticationFilter.PATH, request.getRequestURI())) {
            return null;
        }

        try {
            return (String) ApiAuthenticationFilter.retrieveToken(request, response).getPrincipal();
        } catch (ServletException | IOException e) {
            return null;
        }
    }

    @Override
    protected Session doCreateSession(SessionContext context)
    {
        Session s = newSessionInstance(context);

        // add API token to session if it exists, so we can use it to generate API sessions
        s.setAttribute(USER_TOKEN_ATTR, getApiUserToken(
                WebUtils.getHttpRequest(context),
                WebUtils.getHttpResponse(context)));
        create(s);
        s.removeAttribute(USER_TOKEN_ATTR);

        return s;
    }

    @Override
    protected Serializable getSessionId(ServletRequest request, ServletResponse response)
    {
        Serializable sessionId = super.getSessionId(request, response);
        if (null == sessionId) {
            sessionId = getApiUserToken((HttpServletRequest) request, (HttpServletResponse) response);
        }

        if (sessionId != null) {
            request.setAttribute(ShiroHttpServletRequest.REFERENCED_SESSION_ID, sessionId);
            request.setAttribute(ShiroHttpServletRequest.REFERENCED_SESSION_ID_IS_VALID, Boolean.TRUE);
        }

        return sessionId;
    }
}
