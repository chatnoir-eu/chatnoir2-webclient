/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.auth;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO;

import javax.servlet.annotation.WebListener;
import java.io.Serializable;

/**
 * ChatNoir 2 session DAO.
 */
@WebListener
public class ChatNoirSessionDAO extends EnterpriseCacheSessionDAO
{
    @Override
    protected Serializable doCreate(Session session)
    {
        // use API token as session key if this is an API request
        Serializable sessionId = (String) session.getAttribute(ChatNoirWebSessionManager.USER_TOKEN_ATTR);

        // fall back to random session IDs if this is not an API request
        if (null == sessionId) {
            sessionId = generateSessionId(session);
        }

        assignSessionId(session, sessionId);
        return sessionId;
    }
}
