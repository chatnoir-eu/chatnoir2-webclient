/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.auth;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SessionContext;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;

import javax.servlet.annotation.WebListener;

/**
 * ChatNoir 2 web session manager.
 */
@WebListener
public class ChatNoirWebSessionManager extends DefaultWebSessionManager
{
    public static final String CONTEXT_ATTR = ChatNoirWebSessionManager.class.getName() + ".CONTEXT";

    public ChatNoirWebSessionManager()
    {
        super();
        getSessionIdCookie().setName("SID");

        // TODO: make this dependent on whether this is a web frontend or API response
        setSessionIdCookieEnabled(false);
    }

    @Override
    protected Session doCreateSession(SessionContext context) {
        Session s = newSessionInstance(context);

        // add context to session so that we can generate IDs based on Servlet requests
        // TODO: fix context serialization issue
        s.setAttribute(CONTEXT_ATTR, context);
        create(s);
        s.removeAttribute(CONTEXT_ATTR);

        return s;
    }
}
