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

    private static ApiTokenRealm mInstance = null;

    private final String KEY_INDEX;

    public static ApiTokenRealm getInstance()
    {
        synchronized (ApiTokenRealm.class) {
            if (null == mInstance) {
                mInstance = new ApiTokenRealm();
            }
        }

        return mInstance;
    }

    private ApiTokenRealm()
    {
        KEY_INDEX = Configured.getInstance().getConf().getString("auth.api.key_index", "chatnoir2_apikeys");
        createKeyIndex();

        setCachingEnabled(false);
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

    @Override
    @SuppressWarnings("unchecked")
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException
    {
        String apiKey = (String) token.getPrincipal();

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
                    (Long) l.get("day"),
                    (Long) l.get("week"),
                    (Long) l.get("month"));
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

        return new SimpleAuthenticationInfo(principalData, apiKey, getName());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals)
    {
        Map<String, Object> principalMap = (Map<String, Object>) principals.getPrimaryPrincipal();
        return new SimpleAuthorizationInfo((Set<String>) principalMap.get("roles"));
    }
}
