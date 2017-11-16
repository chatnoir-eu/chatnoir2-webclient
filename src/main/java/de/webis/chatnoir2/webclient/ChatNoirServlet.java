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
     * Get request URI without the context path.
     *
     * @param request HTTP request
     * @return request URI with context path stripped
     */
    public static String getStrippedRequestURI(HttpServletRequest request)
    {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

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
    public boolean isForwardedForm(HttpServletRequest request, String uri)
    {
        String forwardUri = (String) request.getAttribute("javax.servlet.forward.request_uri");
        if (null == forwardUri) {
            return false;
        }
        forwardUri = forwardUri.substring(request.getContextPath().length());
        return  forwardUri.equals(uri);
    }

    /**
     * Redirect to a path with a "301 Moved Permanently" status, while taking into account context paths.
     *
     * @param request HTTP request
     * @param response HTTP response
     */
    protected void redirect(HttpServletRequest request, HttpServletResponse response, String path) throws IOException {
        response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        response.setHeader("Location", request.getContextPath() + path);
    }

    /**
     * Redirect to a path with a "302 Found" status, while taking into account context paths.
     *
     * @param request HTTP request
     * @param response HTTP response
     */
    protected void redirectTemporary(HttpServletRequest request, HttpServletResponse response, String path) throws IOException {
        response.sendRedirect(request.getContextPath() + path);
    }
}
