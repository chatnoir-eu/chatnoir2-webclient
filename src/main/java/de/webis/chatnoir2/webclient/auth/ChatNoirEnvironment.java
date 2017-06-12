/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.auth;

import de.webis.chatnoir2.webclient.auth.api.ApiTokenRealm;
import de.webis.chatnoir2.webclient.resources.ConfigLoader;
import de.webis.chatnoir2.webclient.util.AnnotationClassLoader;
import de.webis.chatnoir2.webclient.util.Configured;
import org.apache.shiro.cache.ehcache.EhCacheManager;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.mgt.ExecutorServiceSessionValidationScheduler;
import org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO;
import org.apache.shiro.web.env.DefaultWebEnvironment;
import org.apache.shiro.web.filter.mgt.DefaultFilterChainManager;
import org.apache.shiro.web.filter.mgt.FilterChainManager;
import org.apache.shiro.web.filter.mgt.FilterChainResolver;
import org.apache.shiro.web.filter.mgt.PathMatchingFilterChainResolver;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Default Shiro environment for ChatNoir 2.
 */
public class ChatNoirEnvironment extends DefaultWebEnvironment
{
    private final String mSMMutex  = "";
    private final String mFCSMutex = "";

    private static DefaultSecurityManager mSecurityManager  = null;
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
        synchronized (mSMMutex) {
            if (mSecurityManager == null) {
                List<Realm> realms = new LinkedList<>();
                realms.add(new ApiTokenRealm());

                mSecurityManager = new DefaultWebSecurityManager(realms);

                // set session storage
                ChatNoirWebSessionManager sessionManager = new ChatNoirWebSessionManager();
                sessionManager.setSecurityManager(mSecurityManager);
                EnterpriseCacheSessionDAO sessionDAO = new ChatNoirSessionDAO();
                sessionManager.setSessionDAO(sessionDAO);

                // set default session timeout
                ConfigLoader.Config sessionConf = Configured.getInstance().getConf().get("auth");
                sessionManager.setGlobalSessionTimeout(sessionConf.getLong("default_session_timeout", 900000L));

                // configure periodic session validator
                ExecutorServiceSessionValidationScheduler validator = new ExecutorServiceSessionValidationScheduler();
                validator.setInterval(sessionConf.getLong("session_validation_interval", 600000L));
                validator.setSessionManager(sessionManager);
                validator.enableSessionValidation();
                sessionManager.setSessionValidationScheduler(validator);
                sessionManager.setSessionValidationSchedulerEnabled(true);

                mSecurityManager.setSessionManager(sessionManager);

                // use EHCache for caching and session persistence
                EhCacheManager cacheManager = new EhCacheManager();
                cacheManager.setCacheManagerConfigFile("classpath:ehcache.xml");
                mSecurityManager.setCacheManager(cacheManager);
            }
        }
        return mSecurityManager;
    }

    @Override
    public synchronized FilterChainResolver getFilterChainResolver()
    {
        synchronized (mFCSMutex) {
            if (null == mFilterChainResolver) {
                FilterChainManager manager = new DefaultFilterChainManager();
                List<ChatNoirAuthenticationFilter> filters = AnnotationClassLoader.newInstances(
                        "de.webis.chatnoir2.webclient.auth",
                        null,
                        ChatNoirAuthenticationFilter.AuthFilter.class,
                        ChatNoirAuthenticationFilter.class
                );

                // sort filters by their own order preferences
                filters.sort(Comparator.comparingInt(ChatNoirAuthenticationFilter::getOrder));

                // add authentication filters
                for (ChatNoirAuthenticationFilter f: filters) {
                    manager.addFilter(f.getName(), f);
                    manager.createChain(f.getPathPattern(), f.getName());
                }

                PathMatchingFilterChainResolver resolver = new PathMatchingFilterChainResolver();
                resolver.setFilterChainManager(manager);
                mFilterChainResolver = resolver;
            }
        }
        return mFilterChainResolver;
    }
}