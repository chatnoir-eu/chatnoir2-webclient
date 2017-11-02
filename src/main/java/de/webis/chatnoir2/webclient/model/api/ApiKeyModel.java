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

import java.util.Arrays;
import java.util.Map;

/**
 * Model with data validations for ChatNoir API keys.
 */
public class ApiKeyModel extends ElasticsearchModel
{
    /**
     * Parent API key.
     */
    private String mParent = null;

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

        MapValidator userValidator = new MapValidator();
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

        addValidator("limits", new ApiLimitsValidator(
                ApiTokenRealm.getTypedPrincipalField(SecurityUtils.getSubject(), "limits")));

        ListValidator remoteHostsValidator = new ListValidator();
        remoteHostsValidator.addValidator(new IpAddressValidator());
        addValidator("remote_hosts", remoteHostsValidator);

        addValidator("expires", new DateValidator());
        addValidator("revoked", new BooleanValidator().strict(true));
    }

    /**
     * Set parent API key.
     *
     * @param parent parent API key as String
     */
    public void setParent(String parent)
    {
        mParent = parent;
    }

    /**
     * Get parent API key.
     */
    public String getParent()
    {
        return mParent;
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
            put("limits", new ApiTokenRealm.ApiLimits(
                    getDocumentId(),
                    (Long) limits.get("day"),
                    (Long) limits.get("week"),
                    (Long) limits.get("month")));
        } catch (ClassCastException e) {
            Configured.getInstance().getSysLogger().debug("Error loading model data", e);
            return false;
        }

        return success;
    }
}
