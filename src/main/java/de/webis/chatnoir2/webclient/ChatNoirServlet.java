/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient;

import de.webis.chatnoir2.webclient.util.Configured;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Base servlet class for ChatNoir servlets.
 * Provides some basic functionality which is needed everywhere.
 *
 * @author Janek Bevendorff
 */
public abstract class ChatNoirServlet extends HttpServlet
{
    /**
     * Write the given user query to the query log.
     *
     * @param searchProvider search provider for which to log the query
     * @param request HTTP request
     * @param queryString user query string
     * @param web true of query was sent via end-user web interface, false if query was sent via API
     */
    protected void writeQueryLog(Configured searchProvider, HttpServletRequest request, String queryString, boolean web)
    {
        try {
            // do not log requests from API keys with "nolog" role
            SecurityUtils.getSecurityManager().checkRole(SecurityUtils.getSubject().getPrincipals(), "nolog");
            return;
        } catch (AuthorizationException ignored) {}

        String ip = request.getHeader("X-Forwarded-For");
        if (null == ip) {
            ip = request.getRemoteHost();
        }
        String userAgent = request.getHeader("User-Agent");

        StringBuilder msg = new StringBuilder();
        if (null != ip) {
            msg.append("IP: ").append(ip).append(" ");
        }
        if (null != userAgent) {
            msg.append("USER-AGENT: ").append(userAgent).append(" ");
        }
        msg.append("QUERY: ").append(queryString);

        searchProvider.getLogger(web ? "Web" : "Api").info(msg);
    }

    /**
     * Forward to an error page displaying the given HTTP status code and a user-readable error message.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param errorCode HTTP error code
     */
    protected void forwardError(HttpServletRequest request, HttpServletResponse response, int errorCode) throws ServletException, IOException
    {
        response.setStatus(errorCode);
        getServletContext().getRequestDispatcher(ErrorServlet.ROUTE).forward(request, response);
    }

    /**
     * Check if a request was forwarded from a certain URL.
     *
     * @param request HTTP request
     * @param uri URI to check against
     * @return true if the request was forwarded from the given uri
     */
    protected boolean isForwardedForm(HttpServletRequest request, String uri)
    {
        return request.getAttribute("javax.servlet.forward.request_uri") != null &&
                request.getAttribute("javax.servlet.forward.request_uri").equals(IndexServlet.ROUTE);
    }
}
