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

import de.webis.chatnoir2.webclient.ChatNoirServlet;
import de.webis.chatnoir2.webclient.api.exceptions.UserErrorException;
import de.webis.chatnoir2.webclient.auth.api.ApiAuthenticationFilter;
import de.webis.chatnoir2.webclient.auth.api.ApiTokenRealm;
import de.webis.chatnoir2.webclient.model.api.ApiKeyModel;
import de.webis.chatnoir2.webclient.resources.ConfigLoader;
import de.webis.chatnoir2.webclient.util.Configured;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SessionContext;
import org.apache.shiro.subject.Subject;
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
import java.util.Collection;

/**
 * ChatNoir 2 web session manager.
 */
@WebListener
public class ChatNoirWebSessionManager extends DefaultWebSessionManager
{
    private static final String ATTR_BASE = ChatNoirWebSessionManager.class.getName();

    /**
     * Temporary user token session attribute.
     */
    public static final String USER_TOKEN_ATTR = ATTR_BASE + ".CONTEXT";

    /**
     * API session attributes.
     */
    public static final String IS_API_SESSION_ATTR =             ATTR_BASE + ".IS_API_SESSION";
    private static final String API_SESSION_MONTH_WINDOW_START = ATTR_BASE + ".API_SESSION_MONTH_WINDOW_START";
    private static final String API_SESSION_WEEK_WINDOW_START =  ATTR_BASE + ".API_SESSION_WEEK_WINDOW_START";
    private static final String API_SESSION_DAY_WINDOW_START =   ATTR_BASE + ".API_SESSION_DAY_WINDOW_START";
    private static final String API_SESSION_MONTH_WINDOW_USAGE = ATTR_BASE + ".API_SESSION_MONTH_WINDOW_USAGE";
    private static final String API_SESSION_WEEK_WINDOW_USAGE =  ATTR_BASE + ".API_SESSION_WEEK_WINDOW_USAGE";
    private static final String API_SESSION_DAY_WINDOW_USAGE =   ATTR_BASE + ".API_SESSION_DAY_WINDOW_USAGE";

    private RealmSecurityManager mSecurityManager = null;

    /**
     * Path matcher for evaluating path prefixes.
     */
    private static final AntPathMatcher mPathMatcher = new AntPathMatcher();

    /**
     * API session life time: 30 days
     */
    private static final long API_SESSION_LIFETIME = 60L * 60 * 24 * 30 * 1000;

    public ChatNoirWebSessionManager()
    {
        super();
        getSessionIdCookie().setName("SID");

        // TODO: make this dependent on whether this is a web frontend or API response
        setSessionIdCookieEnabled(false);
    }

    public void setSecurityManager(RealmSecurityManager securityManager) {
        mSecurityManager = securityManager;
    }

    /**
     * Mark session as API session and initialize or reset month, week and day windows.
     *
     * @param session session object
     */
    private void initApiSession(Session session)
    {
        // mark as API session if not done so already
        session.setAttribute(IS_API_SESSION_ATTR, true);

        // set timeout to 30 days
        session.setTimeout(API_SESSION_LIFETIME);

        // reset windows
        long curTime = System.currentTimeMillis();
        session.setAttribute(API_SESSION_MONTH_WINDOW_START, curTime);
        session.setAttribute(API_SESSION_WEEK_WINDOW_START, curTime);
        session.setAttribute(API_SESSION_DAY_WINDOW_START, curTime);

        session.setAttribute(API_SESSION_MONTH_WINDOW_USAGE, 0L);
        session.setAttribute(API_SESSION_WEEK_WINDOW_USAGE, 0L);
        session.setAttribute(API_SESSION_DAY_WINDOW_USAGE, 0L);
    }

    /**
     * Check whether given session is a valid an initialized API session.
     *
     * @param session session to check
     * @return true if session is an API session
     */
    public boolean isApiSession(Session session)
    {
        Boolean isApiSession = (Boolean) session.getAttribute(IS_API_SESSION_ATTR);
        return isApiSession != null && isApiSession;
    }

    /**
     * Increment API quota counter for given API session.
     *
     * @param session API session to update
     * @throws IllegalArgumentException if session is not a properly initialize API session
     */
    public void incrementApiQuotaUsage(Session session) throws IllegalArgumentException
    {
        if (!isApiSession(session)) {
            throw new IllegalArgumentException("Session is not an API session");
        }

        safeAttributeIncrement(session, API_SESSION_DAY_WINDOW_USAGE);
        safeAttributeIncrement(session, API_SESSION_WEEK_WINDOW_USAGE);
        safeAttributeIncrement(session, API_SESSION_MONTH_WINDOW_USAGE);
    }

    /**
     * Safely increment an int or long session attribute with check for null and without overflows.
     *
     * @param attributeName session attribute
     */
    private void safeAttributeIncrement(Session session, String attributeName)
    {
        Long val = (Long) session.getAttribute(attributeName);
        if (null == val) {
            val = 0L;
        }
        long increment = val < Long.MAX_VALUE ? 1L : 0L;
        session.setAttribute(attributeName, val + increment);
    }

    /**
     * Validate and possibly reset quota usage windows for the given session.
     *
     * @param subject subject to validate API quota for
     * @return true if API quota not exceeded
     * @throws IllegalArgumentException if subject's session is not a properly initialized API session
     */
    public boolean validateApiSessionQuota(Subject subject) throws IllegalArgumentException
    {
        Session session = subject.getSession();
        if (!isApiSession(session)) {
            throw new IllegalArgumentException("Session is not an API session");
        }

        ApiKeyModel.ApiLimits limits = ApiTokenRealm.getUserModel(subject).getApiLimits();

        // validate quota for month, week and day
        return validateApiSessionWindow(session, 2, limits) &&
                validateApiSessionWindow(session, 1, limits) &&
                validateApiSessionWindow(session, 0, limits);
    }

    /**
     * Helper method for validating an API quota window.
     * If the window has expired, it will be reset.
     *
     * @param session API session to validate
     * @param windowDescriptor 0, 1 or 2 for month, week or day
     * @param limits API limits to validate against
     * @return true if quota window not exceeded
     */
    private boolean validateApiSessionWindow(Session session, int windowDescriptor, ApiKeyModel.ApiLimits limits)
    {
        String windowStartAttr = API_SESSION_DAY_WINDOW_START;
        String windowUsageAttr = API_SESSION_DAY_WINDOW_USAGE;
        long windowSizeMillis = 60L * 60 * 24 * 1000;
        if (0 == windowDescriptor) {
            windowStartAttr = API_SESSION_MONTH_WINDOW_START;
            windowUsageAttr = API_SESSION_DAY_WINDOW_USAGE;
            windowSizeMillis *= 30;
        } else if (1 == windowDescriptor) {
            windowStartAttr = API_SESSION_WEEK_WINDOW_START;
            windowUsageAttr = API_SESSION_DAY_WINDOW_USAGE;
            windowSizeMillis *= 7;
        }

        Long windowStart = (Long) session.getAttribute(windowStartAttr);
        Long windowUsage = (Long) session.getAttribute(windowUsageAttr);
        long currentTime = System.currentTimeMillis();

        // reset window if it's older than the window size
        if (currentTime - windowStart > windowSizeMillis) {
            session.setAttribute(windowStartAttr, currentTime);
            session.setAttribute(windowUsageAttr, 0L);
            windowUsage = 0L;
        }

        // check if quota exceeded
        long lim;
        if (0 == windowDescriptor) {
            lim = limits.getMonthlyLimit();
        } else if (1 == windowDescriptor) {
            lim = limits.getWeeklyLimit();
        } else {
            lim = limits.getDailyLimit();
        }
        return (lim <= 0) || (windowUsage < lim);
    }

    /**
     * Get user API token from request or null if no token was specified or this is no API request.
     *
     * @param request HTTP request
     * @return API token as string
     */
    protected Serializable getApiUserToken(HttpServletRequest request, HttpServletResponse response) throws UserErrorException
    {
        if (!mPathMatcher.matches(ApiAuthenticationFilter.PATH, ChatNoirServlet.getStrippedRequestURI(request))) {
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
        Session session = newSessionInstance(context);

        // add API token to session if it exists, so we can use it to generate API sessions
        session.setAttribute(USER_TOKEN_ATTR, getApiUserToken(
                WebUtils.getHttpRequest(context),
                WebUtils.getHttpResponse(context)));
        create(session);
        session.removeAttribute(USER_TOKEN_ATTR);

        return session;
    }

    @Override
    protected void onStart(Session session, SessionContext context) {
        super.onStart(session, context);

        // initialize session as API session if this is an API request
        if (mPathMatcher.matches(ApiAuthenticationFilter.PATH,
                ChatNoirServlet.getStrippedRequestURI(WebUtils.getHttpRequest(context)))) {
            initApiSession(session);
        }
    }

    @Override
    public void validateSessions()
    {
        try {
            super.validateSessions();
        } catch (Throwable e) {
            // catch and log all possible exceptions to make sure the session validation thread doesn't die
            Configured.getSysLogger().error("Exception thrown while validating sessions:", e);
        }
    }

    @Override
    protected Serializable getSessionId(ServletRequest request, ServletResponse response)
    {
        Serializable sessionId = super.getSessionId(request, response);
        if (null == sessionId && mPathMatcher.matches(ApiAuthenticationFilter.PATH,
                ChatNoirServlet.getStrippedRequestURI(WebUtils.toHttp(request)))) {
            sessionId = getApiUserToken(WebUtils.toHttp(request), WebUtils.toHttp(response));
        }

        if (sessionId != null) {
            request.setAttribute(ShiroHttpServletRequest.REFERENCED_SESSION_ID, sessionId);
            request.setAttribute(ShiroHttpServletRequest.REFERENCED_SESSION_ID_IS_VALID, Boolean.TRUE);
        }

        return sessionId;
    }
}
