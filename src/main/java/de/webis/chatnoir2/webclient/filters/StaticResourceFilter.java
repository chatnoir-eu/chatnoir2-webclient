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

package de.webis.chatnoir2.webclient.filters;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;

/**
 * Filter requests to serve static content.
 */
@WebFilter(filterName = "StaticResourceFilter", urlPatterns = {
        StaticResourceFilter.ROUTE1,
        StaticResourceFilter.ROUTE2,
        StaticResourceFilter.ROUTE3,
        StaticResourceFilter.ROUTE4,
        StaticResourceFilter.ROUTE5
})
public class StaticResourceFilter implements Filter
{
    /**
     * Routes for static content.
     */
    static final String
        ROUTE1 = "/static/*",
        ROUTE2 = "*.ico",
        ROUTE3 = "*.txt",
        ROUTE4 = "*.png",
        ROUTE5 = "*.gif";

    /**
     * Request dispatcher.
     */
    private RequestDispatcher mRequestDispatcher;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        mRequestDispatcher = filterConfig.getServletContext().getNamedDispatcher("default");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        mRequestDispatcher.forward(request, response);
    }

    @Override
    public void destroy() {}
}
