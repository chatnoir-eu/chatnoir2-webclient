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

package de.webis.chatnoir2.webclient.auth.api;

import de.webis.chatnoir2.webclient.model.api.ApiKeyModel;
import de.webis.chatnoir2.webclient.util.Configured;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Shiro realm for API keys.
 */
public class ApiTokenRealm extends AuthorizingRealm implements Serializable
{
    public static final String PRINCIPALS_CACHE_NAME = ApiTokenRealm.class.getName() + "-0-principals";

    public ApiTokenRealm()
    {
        setAuthenticationTokenClass(ApiKeyAuthenticationToken.class);
    }

    @Override
    public String getName()
    {
        return getClass().getName();
    }

    @Override
    public boolean supports(AuthenticationToken token)
    {
        return token != null && ApiKeyAuthenticationToken.class.isAssignableFrom(token.getClass());
    }

    /**
     * Helper method for refreshing principals cache from API token index.
     *
     * @param apiKey API token for which to refresh the principals cache
     */
    @SuppressWarnings("unchecked")
    private void refreshPrincipalsCache(String apiKey)
    {
        ApiKeyModel userModel = new ApiKeyModel();
        if (!userModel.loadById(apiKey)) {
            throw new AuthenticationException("Invalid API key");
        }
        if (userModel.isRevoked()) {
            throw new AuthenticationException("API key has been revoked");
        }
        LocalDateTime expiryDate = userModel.getExpiryDate();
        if (null != expiryDate && expiryDate.isBefore(LocalDateTime.now())) {
            throw new AuthenticationException("API key has expired");
        }

        Map<String, Object> principalData = new HashMap<>();
        principalData.put("model", userModel);

        getPrincipalsCache().put(apiKey, principalData);
    }

    /**
     * Get all fields from a subject's primary principal.
     *
     * @param subject subject
     * @return map of principal data
     */
    protected static Map<String, Object> getPrincipalFields(Subject subject)
    {
        RealmSecurityManager securityManager = ((RealmSecurityManager) SecurityUtils.getSecurityManager());

        String apiKey = (String) subject.getPrincipal();
        Cache<String, Map<String, Object>> cache;
        synchronized (apiKey.intern()) {
            cache = securityManager.getCacheManager().getCache(PRINCIPALS_CACHE_NAME);
            if (null == cache.get(apiKey)) {
                Collection<Realm> realms = securityManager.getRealms();
                for (Realm r : realms) {
                    if (r instanceof ApiTokenRealm) {
                        ((ApiTokenRealm) r).refreshPrincipalsCache(apiKey);
                        break;
                    }
                }
            }

            return cache.get(apiKey);
        }
    }

    /**
     * Get subject's user model.
     *
     * @param subject subject
     * @return {@link ApiKeyModel} for <tt>subject</tt>
     */
    public static ApiKeyModel getUserModel(Subject subject)
    {
        Map<String, Object> principals = getPrincipalFields(subject);
        if (null != principals) {
            return (ApiKeyModel) principals.get("model");
        }
        return null;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException
    {
        String apiKey = (String) token.getPrincipal();
        synchronized (apiKey.intern()) {
            try {
                refreshPrincipalsCache(apiKey);
            } catch (Throwable e) {
                if (!(e instanceof AuthenticationException)) {
                    Configured.getSysLogger().error("Error during authentication", e);
                } else {
                    throw e;
                }
            }
            return new SimpleAuthenticationInfo(apiKey, apiKey, getName());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals)
    {
        String apiKey = (String) principals.getPrimaryPrincipal();
        Map<String, Object> principalData;
        synchronized (apiKey.intern()) {
            principalData = getPrincipalsCache().get(apiKey);
            if (null == principalData) {
                refreshPrincipalsCache(apiKey);
                principalData = getPrincipalsCache().get(apiKey);
            }

            return new SimpleAuthorizationInfo(((ApiKeyModel) principalData.get("model")).getRoles());
        }
    }

    /**
     * Retrieve principals cache or create a new one of it does not exist.
     *
     * @return credentials cache
     */
    private Cache<String, Map<String, Object>> getPrincipalsCache()
    {
        return getCacheManager().getCache(PRINCIPALS_CACHE_NAME);
    }

    /**
     * Clear any cached principal data as well as any associated authentication and authorization data.
     *
     * @param principals principals of the account
     */
    public void clearCachedPrincipals(PrincipalCollection principals)
    {
        if (null == principals || principals.isEmpty()) {
            return;
        }

        String apiKey = (String) principals.getPrimaryPrincipal();
        synchronized (apiKey.intern()) {
            Cache<String, Map<String, Object>> credentialsCache = getPrincipalsCache();
            if (null != credentialsCache) {
                credentialsCache.remove(apiKey);
            }
        }

        clearCachedAuthenticationInfo(principals);
        clearCachedAuthorizationInfo(principals);
    }
}
