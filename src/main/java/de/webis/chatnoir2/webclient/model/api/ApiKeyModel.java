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

import de.webis.chatnoir2.webclient.auth.api.ApiTokenRealm;
import de.webis.chatnoir2.webclient.model.ElasticsearchModel;
import de.webis.chatnoir2.webclient.model.validation.*;
import de.webis.chatnoir2.webclient.util.Configured;
import org.apache.shiro.SecurityUtils;
import org.elasticsearch.common.Nullable;

import java.io.Serializable;
import java.util.*;

/**
 * Model with data validations for ChatNoir API keys.
 */
public class ApiKeyModel extends ElasticsearchModel
{
    /**
     * Parent API key.
     */
    private ApiKeyModel mParent = null;

    public ApiKeyModel()
    {
        super(Configured.getInstance().getConf().getString("auth.api.key_index"),
                "apikey",
                "apikey.mapping.json");

        allowedKeys(Arrays.asList(
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
        userValidator.addValidator("last_name",  new StringNotEmptyValidator());
        userValidator.addValidator("email",      new EmailAddressValidator());
        userValidator.addValidator("address",    new StringNotEmptyValidator().optional(true));
        userValidator.addValidator("zip_code",   new StringNotEmptyValidator().optional(true));
        userValidator.addValidator("country",    new StringNotEmptyValidator().optional(true));
        addValidator("user", userValidator);

        addValidator("limits", new ApiLimitsValidator(mParent));

        RecursiveListValidator remoteHostsValidator = new RecursiveListValidator();
        remoteHostsValidator.addValidator(new IpAddressValidator());
        addValidator("remote_hosts", remoteHostsValidator);

        addValidator("roles", new RolesValidator(mParent));
        addValidator("expires", new DateValidator());
        addValidator("revoked", new BooleanValidator().strict(true));
    }

    /**
     * @param id user ID to load the model for
     * @throws RuntimeException if model could not be loaded for <tt>id</tt>
     */
    public ApiKeyModel(String id) throws RuntimeException
    {
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
    public void setParent(ApiKeyModel parent)
    {
        mParent = parent;
    }

    /**
     * Set parent API key by String id.
     *
     * @param parentId parent API key as String
     */
    public void setParentById(String parentId)
    {
        mParent = new ApiKeyModel(parentId);
    }

    /**
     * @return parent API key model
     */
    public ApiKeyModel getParent()
    {
        return mParent;
    }

    /**
     * @return user's API limits
     */
    public ApiLimits getApiLimits()
    {
        return (ApiLimits) get("limits");
    }

    /**
     * @return user's roles
     */
    public List<String> getRoles()
    {
        Object roles = get("roles");
        if (null == roles) {
            return new ArrayList<>();
        }
        // noinspection unchecked
        return (List<String>) roles;
    }

    @Override
    protected boolean doCommit()
    {
        put("parent", getParent());
        boolean success = super.doCommit();
        remove("parent");
        return success;
    }

    @Override
    public boolean loadById(String documentId)
    {
        boolean success = super.loadById(documentId);

        if (containsKey("parent")) {
            setParent(get("parent"));
            remove("parent");
        }

        try {
            Map<String, Object> limits = get("limits");
            put("limits", new ApiLimits(
                    (Long) limits.get("day"),
                    (Long) limits.get("week"),
                    (Long) limits.get("month")));
        } catch (ClassCastException e) {
            Configured.getInstance().getSysLogger().debug("Error loading model data", e);
            return false;
        }

        return success;
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
            if (null != mParent) {
                return ((ApiLimits) mParent.get("limits")).getLimit(field);
            }
            if (null == get(field)) {
                return Configured.getInstance().getConf().getLong("auth.api.default_quota_limits." + field);
            }
            return get(field);
        }
    }
}
