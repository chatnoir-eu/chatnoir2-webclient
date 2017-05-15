/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient;

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
 * @version 1
 */
public abstract class ChatNoirServlet extends HttpServlet
{
    /**
     * GET action for this servlet.
     *
     * @param request   The HTTP request
     * @param response  The HTTP response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        // set correct encoding
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
    }

    /**
     * POST action for this servlet.
     *
     * @param request   The HTTP request
     * @param response  The HTTP response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        // set correct encoding
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
    public static String getParameter(final String name, final HttpServletRequest request) {
        try {
            return new String(request.getParameter(name).getBytes(Charset.defaultCharset()), "UTF-8");
        } catch (UnsupportedEncodingException | NullPointerException ignored) {
            return null;
        }
    }
}
