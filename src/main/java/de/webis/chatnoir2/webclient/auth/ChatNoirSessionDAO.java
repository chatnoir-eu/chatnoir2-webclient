/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.auth;

import de.webis.chatnoir2.webclient.api.ApiBootstrap;
import de.webis.chatnoir2.webclient.auth.api.ApiAuthenticationFilter;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO;
import org.apache.shiro.util.AntPathMatcher;
import org.apache.shiro.web.session.mgt.WebSessionContext;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;

/**
 * ChatNoir 2 session DAO.
 */
@WebListener
public class ChatNoirSessionDAO extends EnterpriseCacheSessionDAO
{
    /**
     * Get user API token from request or null if no token was specified or this is no API request.
     *
     * @param request HTTP request
     * @return API token as string
     */
    protected String getApiUserToken(HttpServletRequest request)
    {
        if (!new AntPathMatcher().matches(ApiAuthenticationFilter.PATH, request.getRequestURI())) {
            return null;
        }

        try {
            return (String) ApiBootstrap.bootstrapApiModule(request).getUserToken(request).getPrincipal();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected Serializable doCreate(Session session) {
        WebSessionContext context = (WebSessionContext) session.getAttribute(ChatNoirWebSessionManager.CONTEXT_ATTR);

        Serializable sessionId = null;
        // use API token as session key if this is an API request
        if (null != context) {
            sessionId = getApiUserToken((HttpServletRequest) context.getServletRequest());
        }

        // fall back to random session IDs if this is not an API request
        if (null == sessionId) {
            sessionId = generateSessionId(session);
        }

        assignSessionId(session, sessionId);
        return sessionId;
    }
}
