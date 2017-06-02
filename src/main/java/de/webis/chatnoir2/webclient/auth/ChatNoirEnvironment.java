/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.auth;

import de.webis.chatnoir2.webclient.auth.api.ApiAuthenticationFilter;
import de.webis.chatnoir2.webclient.auth.api.ApiKeyRealm;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.web.env.DefaultWebEnvironment;
import org.apache.shiro.web.filter.mgt.DefaultFilterChainManager;
import org.apache.shiro.web.filter.mgt.FilterChainManager;
import org.apache.shiro.web.filter.mgt.FilterChainResolver;
import org.apache.shiro.web.filter.mgt.PathMatchingFilterChainResolver;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;

import java.util.LinkedList;
import java.util.List;

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
        List<Realm> realms = new LinkedList<>();
        realms.add(ApiKeyRealm.getInstance());

        if (mSecurityManager == null) {
            mSecurityManager = new DefaultWebSecurityManager(realms);
        }
        return mSecurityManager;
    }

    @Override
    public FilterChainResolver getFilterChainResolver()
    {
        if (null == mFilterChainResolver) {
            FilterChainManager manager = new DefaultFilterChainManager();
            manager.addFilter("default", new NullAuthenticationFilter());
            manager.addFilter("api", new ApiAuthenticationFilter());

            manager.createChain("/api/**", "api");
            manager.createChain("/**", "default");

            PathMatchingFilterChainResolver resolver = new PathMatchingFilterChainResolver();
            resolver.setFilterChainManager(manager);
            mFilterChainResolver = resolver;
        }
        return mFilterChainResolver;
    }
}