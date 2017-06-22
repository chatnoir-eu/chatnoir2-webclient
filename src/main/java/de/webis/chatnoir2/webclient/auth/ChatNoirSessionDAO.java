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
