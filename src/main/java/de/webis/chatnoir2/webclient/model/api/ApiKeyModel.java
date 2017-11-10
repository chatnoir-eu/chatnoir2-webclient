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

package de.webis.chatnoir2.webclient.model.api;

import de.webis.chatnoir2.webclient.model.ElasticsearchModel;
import de.webis.chatnoir2.webclient.model.validation.*;
import de.webis.chatnoir2.webclient.util.Configured;
import org.elasticsearch.common.Nullable;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Model with data validations for ChatNoir API keys.
 */
public class ApiKeyModel extends ElasticsearchModel {
    /**
     * Parent API key.
     */
    private ApiKeyModel mParent = null;

    private final ApiLimitsValidator mApiLimitsValidator;
    private final RolesValidator mRolesValidator;

    public ApiKeyModel() {
        super(Configured.getInstance().getConf().getString("auth.api.key_index"),
                "apikey",
                "apikey.mapping.json");

        allowedKeys(Arrays.asList(
                "parent",
                "user",
                "limits",
                "remote_hosts",
                "roles",
                "expires",
                "revoked"));

        RecursiveMapValidator userValidator = new RecursiveMapValidator();
        userValidator.allowedKeys(Arrays.asList(
                "first_name",
                "last_name",
                "email",
                "address",
                "zip_code",
                "country",
                "email"));
        userValidator.addValidator("first_name", new StringNotEmptyValidator());
        userValidator.addValidator("last_name", new StringNotEmptyValidator());
        userValidator.addValidator("email", new EmailAddressValidator());
        userValidator.addValidator("address", new StringNotEmptyValidator().optional(true));
        userValidator.addValidator("zip_code", new StringNotEmptyValidator().optional(true));
        userValidator.addValidator("country", new StringNotEmptyValidator().optional(true));
        addValidator("user", userValidator.strict(true));

        mApiLimitsValidator = new ApiLimitsValidator(mParent);
        addValidator("limits", mApiLimitsValidator);

        RecursiveListValidator remoteHostsValidator = new RecursiveListValidator();
        remoteHostsValidator.addValidator(new IpAddressValidator());
        addValidator("remote_hosts", remoteHostsValidator.optional(true));

        mRolesValidator = new RolesValidator(mParent);
        addValidator("roles", mRolesValidator);
        addValidator("expires", new DateValidator().optional(true));
        addValidator("revoked", new BooleanValidator().optional(true));

        // initialize default fields
        setParent(null);
        Map<String, Object> user = new HashMap<>();
        user.put("first_name", "");
        user.put("last_name", "");
        user.put("address", null);
        user.put("zip_code", null);
        user.put("country", null);
        user.put("email", null);
        put("user", user);
        put("limits", new ApiLimits(null, null, null));
        put("remote_hosts", new HashSet<InetAddress>());
        put("roles", new HashSet<String>());
        put("expires", null);
        put("revoked", false);
    }

    /**
     * @param id user ID to load the model for
     * @throws RuntimeException if model could not be loaded for <tt>id</tt>
     */
    public ApiKeyModel(String id) throws RuntimeException {
        this();
        if (!loadById(id)) {
            throw new RuntimeException(String.format("Could not load user model for id: %s", id));
        }
    }

    /**
     * Set parent API key.
     *
     * @param parent parent API key model
     */
    public void setParent(ApiKeyModel parent) {
        mParent = parent;
        putNoUpdate("parent", parent);

        if (null == parent) {
            mApiLimitsValidator.setLimits(null);
            mRolesValidator.setAllowedRoles(null);
        } else {
            mApiLimitsValidator.setLimits(parent.getApiLimits());
            mRolesValidator.setAllowedRoles(parent.getRoles());
        }
    }

    /**
     * Set parent API key by String id.
     *
     * @param parentId parent API key as String
     */
    public void setParentById(String parentId) {
        mParent = new ApiKeyModel(parentId);
    }

    /**
     * @return parent API key model
     */
    public ApiKeyModel getParent() {
        return mParent;
    }

    /**
     * @return user's API limits
     */
    public ApiLimits getApiLimits() {
        return (ApiLimits) get("limits");
    }

    /**
     * @return user's API limits
     */
    public Set<InetAddress> getRemoteHosts() {
        // noinspection unchecked
        return (Set<InetAddress>) get("remote_hosts");
    }

    /**
     * @return user's roles
     */
    public Set<String> getRoles() {
        Object roles = get("roles");
        Set<String> rolesSet = new HashSet<>();
        if (null != roles) {
            // noinspection unchecked
            rolesSet.addAll((List<String>) roles);
        }
        return rolesSet;
    }

    @Override
    public boolean loadById(String documentId) {
        return super.loadById(documentId) && updateDataStructures(null);
    }

    @Override
    public void put(String key, Object value)
    {
        putNoUpdate(key, value);
        updateDataStructures(key);
    }

    /**
     * Put value without updating data structures.
     */
    private void putNoUpdate(String key, Object value)
    {
        super.put(key, value);
    }

    @Override
    public void putAll(Map<String, Object> map)
    {
        super.putAll(map);
        updateDataStructures(null);
    }

    /**
     * Update data structures after map data changes.
     * @param field field name to update, null to update all
     *
     * @return true on success
     */
    private boolean updateDataStructures(@Nullable String field)
    {
        if (null == field || field.equals("parent")) {
            Object parent = get("parent");
            if (parent instanceof ApiKeyModel) {
                setParent((ApiKeyModel) parent);
            } else if (parent instanceof String) {
                setParentById((String) parent);
            } else {
                setParent(null);
            }
        }

        if (null == field || field.equals("limits")) {
            Map<String, Object> limits = get("limits");
            try {
                putNoUpdate("limits", new ApiLimits(
                        null != limits.get("day") ? ((Integer) limits.get("day")).longValue() : null,
                        null != limits.get("week") ? ((Integer) limits.get("week")).longValue() : null,
                        null != limits.get("month") ? ((Integer) limits.get("month")).longValue() : null));
            } catch (ClassCastException ignored) {
                try {
                    // returned type may already be Long, so try again
                    putNoUpdate("limits", new ApiLimits(
                            (Long) limits.get("day"),
                            (Long) limits.get("week"),
                            (Long) limits.get("month")));
                } catch (ClassCastException e) {
                    Configured.getInstance().getSysLogger().debug("Error loading model data", e);
                    return false;
                }
            }
        }

        if (null == field || field.equals("remote_hosts")) {
            Set<String> remoteHosts = new HashSet<>();
            if (null != get("remote_hosts")) {
                remoteHosts.addAll(get("remote_hosts"));
                Set<InetAddress> addressSet = new HashSet<>();
                for (Object ip : remoteHosts) {
                    try {
                        if (ip instanceof String) {
                            addressSet.add(InetAddress.getByName((String) ip));
                        } else {
                            addressSet.add((InetAddress) ip);
                        };
                    } catch (UnknownHostException ignored) {}
                }
                putNoUpdate("remote_hosts", addressSet);
            }
        }

        return true;
    }

    /**
     * API limits data object.
     */
    public class ApiLimits extends HashMap<String, Long> implements Serializable
    {
        /**
         * @param day daily limit (null for default)
         * @param week weekly limit (null for default)
         * @param month monthly limit (null for default)
         */
        public ApiLimits(@Nullable Long day, @Nullable Long week, @Nullable Long month)
        {
            put("day", day);
            put("week", week);
            put("month", month);
        }

        public long getDailyLimit()
        {
            return getLimit("day");
        }

        public long getWeeklyLimit()
        {
            return getLimit("week");
        }

        public long getMonthlyLimit()
        {
            return getLimit("month");
        }

        private long getLimit(String field)
        {
            if (get(field) == null && null != mParent) {
                return ((ApiLimits) mParent.get("limits")).getLimit(field);
            }
            if (null == get(field)) {
                return Configured.getInstance().getConf().getLong("auth.api.default_quota_limits." + field);
            }
            return get(field);
        }
    }
}
