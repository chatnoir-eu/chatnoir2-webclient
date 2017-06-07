/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2015-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

package de.webis.chatnoir2.webclient.auth.api;

import de.webis.chatnoir2.webclient.util.Configured;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.xcontent.XContentType;

import javax.annotation.CheckForNull;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Shiro realm for API keys.
 */
public class ApiKeyRealm extends AuthorizingRealm
{
    /**
     * DAO representing API call limits.
     */
    public class ApiLimits
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
            mMonth   = month;
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

    private static ApiKeyRealm mInstance = null;
    private final Configured mConfig;

    private final String KEY_INDEX;

    private final Map<String, ApiLimits> mApiKeyToLimits;
    private final Map<String, Set<String>> mApiKeyToRoles;

    public static ApiKeyRealm getInstance()
    {
        if (null == mInstance) {
            mInstance = new ApiKeyRealm();
        }

        return mInstance;
    }

    private ApiKeyRealm()
    {
        mConfig = new Configured();
        mApiKeyToLimits = new HashMap<>();
        mApiKeyToRoles = new HashMap<>();
        KEY_INDEX = mConfig.getConf().getString("auth.api.key_index", "chatnoir2_apikeys");
        createKeyIndex();

        setCachingEnabled(false);
    }

    private void createKeyIndex()
    {
        IndicesExistsRequest request = new IndicesExistsRequest(KEY_INDEX);
        try {
            IndicesExistsResponse response = mConfig.getClient().admin().indices().exists(request).get();
            if (!response.isExists()) {
                mConfig.getSysLogger().info(String.format("API key index '%s' does not exist, creating it.", KEY_INDEX));

                URL mappingFileURL = getClass().getClassLoader().getResource("apikey.mapping.json");
                assert mappingFileURL != null;
                Path mappingFile = Paths.get(mappingFileURL.toURI());
                final String mapping = Files.lines(mappingFile).reduce("", (a, b) -> a + b + "\n");
                mConfig.getClient()
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

    @Override
    @SuppressWarnings("unchecked")
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException
    {
        String apiKey = (String) token.getPrincipal();

        GetResponse response = mConfig.getClient().prepareGet(KEY_INDEX, "apikey", apiKey).get();
        if (!response.isExists()) {
            throw new AuthenticationException("No user found for key " + apiKey);
        }

        SimplePrincipalCollection coll = new SimplePrincipalCollection();
        coll.add(apiKey, getName());
        Map<String, Object> source = response.getSource();

        if (source.containsKey("user")) {
            Map<String, Object> userInfo = (Map) source.get("user");
            for (String k : userInfo.keySet()) {
                coll.add(new Tuple<>(k, (String) userInfo.get(k)), getName());
            }
        }

        if (null != source.get("limits")) {
            Map<String, Object> limits = (Map) source.get("limits");
            mApiKeyToLimits.put(apiKey, new ApiLimits(
                    apiKey,
                    (Long) limits.get("day"),
                    (Long) limits.get("week"),
                    (Long) limits.get("month")));
        } else {
            mApiKeyToLimits.put(apiKey, new ApiLimits(apiKey, null, null, null));
        }

        if (null != source.get("roles")) {
            mApiKeyToRoles.put(apiKey, new HashSet<>((List) source.get("roles")));
        } else {
            mApiKeyToRoles.put(apiKey, new HashSet<>());
        }

        return new SimpleAuthenticationInfo(coll, apiKey, getName());
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals)
    {
        System.out.println("doGetAuthorizationInfo " + principals);
        Set<String> roles = mApiKeyToRoles.get((String) principals.getPrimaryPrincipal());
        return new SimpleAuthorizationInfo(roles);
    }
}
