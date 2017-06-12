/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2015-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.auth.api;

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
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.xcontent.XContentType;

import javax.annotation.CheckForNull;
import java.io.Serializable;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Shiro realm for API keys.
 */
public class ApiTokenRealm extends AuthorizingRealm implements Serializable
{
    private static final String PRINCIPALS_CACHE_NAME = ApiTokenRealm.class.getName() + "-0-principals";

    /**
     * DAO representing API call limits.
     */
    public class ApiLimits implements Serializable
    {
        private final String mApiKey;
        private final Long mDay;
        private final Long mWeek;
        private final Long mMonth;

        public ApiLimits(String apiKey, @Nullable  Long day, @Nullable Long week, @Nullable Long month)
        {
            mApiKey = apiKey;
            mDay    = day;
            mWeek   = week;
            mMonth  = month;
        }

        public String getApiKey()
        {
            return mApiKey;
        }

        @CheckForNull
        public Long getDailyLimit()
        {
            return mDay;
        }

        @CheckForNull
        public Long getWeeklyLimit()
        {
            return mWeek;
        }

        @CheckForNull
        public Long getMonthlyLimit()
        {
            return mMonth;
        }
    }

    private final String KEY_INDEX;

    public ApiTokenRealm()
    {
        KEY_INDEX = Configured.getInstance().getConf().getString("auth.api.key_index", "chatnoir2_apikeys");
        createKeyIndex();

        setAuthenticationTokenClass(ApiKeyAuthenticationToken.class);
    }

    private void createKeyIndex()
    {
        Configured config = Configured.getInstance();
        IndicesExistsRequest request = new IndicesExistsRequest(KEY_INDEX);
        try {
            IndicesExistsResponse response = config.getClient().admin().indices().exists(request).get();
            if (!response.isExists()) {
                config.getSysLogger().info(String.format("API key index '%s' does not exist, creating it.", KEY_INDEX));

                URL mappingFileURL = getClass().getClassLoader().getResource("apikey.mapping.json");
                assert mappingFileURL != null;
                Path mappingFile = Paths.get(mappingFileURL.toURI());
                final String mapping = Files.lines(mappingFile).reduce("", (a, b) -> a + b + "\n");
                config.getClient()
                        .admin()
                        .indices()
                        .prepareCreate(KEY_INDEX)
                        .setSource(mapping, XContentType.JSON)
                        .get();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        GetResponse response = Configured.getInstance().getClient().prepareGet(KEY_INDEX, "apikey", apiKey).get();
        if (!response.isExists()) {
            throw new AuthenticationException("No user found for key " + apiKey);
        }

        Map<String, Object> source = response.getSource();
        Map<String, Object> principalData = new HashMap<>();

        principalData.put("apikey", apiKey);

        // user data
        Map<String, String> userData = new HashMap<>();
        if (source.containsKey("user")) {
            Map<String, Object> userInfo = (Map) source.get("user");
            for (String k : userInfo.keySet()) {
                userData.put(k, (String) userInfo.get(k));
            }
        }
        principalData.put("userdata", userData);

        // api limits
        ApiLimits limits;
        if (null != source.get("limits")) {
            Map<String, Object> l = (Map) source.get("limits");
            limits = new ApiLimits(
                    apiKey,
                    l.get("day") != null ? ((Number) l.get("day")).longValue() : null,
                    l.get("week") != null ? ((Number) l.get("week")).longValue() : null,
                    l.get("month") != null ? ((Number) l.get("month")).longValue() : null);
        } else {
            limits = new ApiLimits(apiKey, null, null, null);
        }
        principalData.put("limits", limits);

        // user roles
        Set<String> roles = new HashSet<>();
        if (null != source.get("roles")) {
            roles.addAll((List) source.get("roles"));
        }
        principalData.put("roles", roles);

        getPrincipalsCache().put(apiKey, principalData);
    }

    /**
     * Get a specific field from a subject's primary principal.
     *
     * @param subject subject
     * @param field field to retrieve from principal
     * @param <T> return type
     * @return typed field value or null if it does not exist
     */
    @SuppressWarnings("unchecked")
    public static <T> T getTypedPrincipalField(Subject subject, String field)
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

            Map<String, Object> principals = cache.get(apiKey);
            if (null != principals) {
                return (T) principals.get(field);
            }
        }

        return null;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException
    {
        String apiKey = (String) token.getPrincipal();
        synchronized (apiKey.intern()) {
            refreshPrincipalsCache(apiKey);
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

            return new SimpleAuthorizationInfo((Set<String>) principalData.get("roles"));
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
