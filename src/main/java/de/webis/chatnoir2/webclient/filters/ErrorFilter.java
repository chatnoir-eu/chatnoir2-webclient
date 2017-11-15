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

import de.webis.chatnoir2.webclient.ChatNoirServlet;
import de.webis.chatnoir2.webclient.ErrorServlet;
import de.webis.chatnoir2.webclient.api.ApiBootstrap;
import de.webis.chatnoir2.webclient.api.exceptions.UserErrorException;
import de.webis.chatnoir2.webclient.auth.api.ApiAuthenticationFilter;
import de.webis.chatnoir2.webclient.util.Configured;
import org.apache.shiro.util.AntPathMatcher;
import org.apache.shiro.web.servlet.ShiroFilter;
import org.apache.shiro.web.util.WebUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletResponse;

/**
 * Catch and handle any exceptions that might occur on the way.
 * This filter needs to be at the very top of the filter chain.
 */
@WebFilter(filterName="ErrorFilter", urlPatterns = ErrorFilter.ROUTE)
public class ErrorFilter extends ShiroFilter
{
    static final String ROUTE = "/*";

    private static final AntPathMatcher mPathmatcher = new AntPathMatcher();

    @Override
    public void doFilterInternal(ServletRequest request, ServletResponse response, FilterChain chain)
    {
        try {
            super.doFilterInternal(request, response, chain);
        } catch (Throwable e) {
            handleException(e, request, response);
        }
    }

    /**
     * Helper method for handling any exceptions.
     * This method itself will never throw any exception. If an exception is thrown during its execution, for whatever
     * reason, it will silently fail.
     *
     * @param exception thrown exception
     * @param request HTTP request
     * @param response HTTP response
     */
    private void handleException(Throwable exception, ServletRequest request, ServletResponse response)
    {
        try {
            try {
                HttpServletResponse httpResponse = WebUtils.toHttp(response);

                // handle API errors separately
                if (mPathmatcher.matches(ApiAuthenticationFilter.PATH,
                        ChatNoirServlet.getStrippedRequestURI(WebUtils.toHttp(request)))) {
                    ApiBootstrap.handleException(exception, WebUtils.toHttp(request), WebUtils.toHttp(response));
                    return;
                }

                // throw away top-most wrapper exception if it's a servlet exception
                if (exception instanceof ServletException && exception.getCause() != null) {
                    exception = exception.getCause();
                }

                // display error page
                httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                if (exception instanceof UserErrorException) {
                    httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }

                Configured.getSysLogger().error(
                        "Internal server exception:", exception);
                getServletContext().getRequestDispatcher(ErrorServlet.ROUTE).forward(request, response);

            } catch (Throwable followUpException) {
                Configured.getSysLogger().error(
                        "Follow-up exception while handling exception:", followUpException);
            }
        } catch (Throwable dead) {
            // whatever is happening here must be really bad, so try one last thing...
            dead.printStackTrace();
        }
    }
}
