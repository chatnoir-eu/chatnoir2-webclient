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

import de.webis.chatnoir2.webclient.auth.ChatNoirAuthenticationFilter.AuthFilter;
import org.apache.shiro.authc.AuthenticationToken;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Basic pass-through authenticator that permits all requests.
 * This authenticator should be executed last in the chain.
 */
@SuppressWarnings("unused")
@AuthFilter
public class NullAuthenticationFilter extends ChatNoirAuthenticationFilter
{
    private static final String NAME = "default";
    private static final String PATH = "/**";
    private static final int ORDER   = Integer.MAX_VALUE;

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue)
    {
        return true;
    }

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception
    {
        return true;
    }

    @Override
    protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) throws Exception
    {
        return null;
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public String getPathPattern()
    {
        return PATH;
    }

    @Override
    public int getOrder()
    {
        return ORDER;
    }
}
