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

import de.webis.chatnoir2.webclient.auth.api.ApiTokenRealm;
import de.webis.chatnoir2.webclient.resources.ConfigLoader;
import de.webis.chatnoir2.webclient.util.AnnotationClassLoader;
import de.webis.chatnoir2.webclient.util.CacheManager;
import de.webis.chatnoir2.webclient.util.Configured;
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

                // use ChatNoir cache manager for caching and session persistence
                mSecurityManager.setCacheManager(new CacheManager());
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