/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.auth;

import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.SimpleAccountRealm;
import org.apache.shiro.web.env.DefaultWebEnvironment;
import org.apache.shiro.web.filter.authc.AuthenticationFilter;
import org.apache.shiro.web.filter.mgt.DefaultFilterChainManager;
import org.apache.shiro.web.filter.mgt.FilterChainManager;
import org.apache.shiro.web.filter.mgt.FilterChainResolver;
import org.apache.shiro.web.filter.mgt.PathMatchingFilterChainResolver;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Default Shiro environment for ChatNoir 2.
 */
public class ChatNoirEnvironment extends DefaultWebEnvironment
{
    private static SecurityManager mSecurityManager = null;
    private static FilterChainResolver mFilterChainResolver = null;

    public ChatNoirEnvironment()
    {
        super();
        setFilterChainResolver(getFilterChainResolver());
        setSecurityManager(getSecurityManager());
    }

    @Override
    public SecurityManager getSecurityManager()
    {
        if (mSecurityManager == null) {
            SimpleAccountRealm realm = new SimpleAccountRealm();
            mSecurityManager = new DefaultWebSecurityManager(realm);
        }
        return mSecurityManager;
    }

    @Override
    public FilterChainResolver getFilterChainResolver()
    {
        if (null == mFilterChainResolver) {
            FilterChainManager manager = new DefaultFilterChainManager();
            manager.addFilter("default", new AuthenticationFilter()
            {
                @Override
                protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception
                {
                    return true;
                }
            });
            manager.createChain("/**", "default");

            PathMatchingFilterChainResolver resolver = new PathMatchingFilterChainResolver();
            resolver.setFilterChainManager(manager);
            mFilterChainResolver = resolver;
        }
        return mFilterChainResolver;
    }
}