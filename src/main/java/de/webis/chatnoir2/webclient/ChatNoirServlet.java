/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient;

import de.webis.chatnoir2.webclient.util.Configured;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * Base servlet class for ChatNoir servlets.
 * Provides some basic functionality which is needed everywhere.
 *
 * @author Janek Bevendorff
 */
public abstract class ChatNoirServlet extends HttpServlet
{
    /**
     * GET action for this servlet.
     *
     * @param request   The HTTP request
     * @param response  The HTTP response
     */
    @Override
    protected void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        setEncoding(request, response);
        super.service(request, response);
    }

    /**
     * Set correct request and response character encoding.
     *
     * @param request   The HTTP request
     * @param response  The HTTP response
     */
    protected void setEncoding(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
    }

    /**
     * Get a parameter from the given request object with correct UTF-8 encoding.
     *
     * @param name parameter name
     * @param request request object
     * @return UTF-8 encoded String, null if parameter does not exist
     */
    protected String getParameter(final String name, final HttpServletRequest request) {
        try {
            return new String(request.getParameter(name).getBytes(Charset.defaultCharset()), "UTF-8");
        } catch (UnsupportedEncodingException | NullPointerException ignored) {
            return null;
        }
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
}
