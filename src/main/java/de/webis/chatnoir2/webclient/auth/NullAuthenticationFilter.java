/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2015-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
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
