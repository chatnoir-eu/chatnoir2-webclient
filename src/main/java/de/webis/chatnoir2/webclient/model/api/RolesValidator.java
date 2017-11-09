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
import de.webis.chatnoir2.webclient.model.validation.StatefulValidator;
import org.apache.shiro.SecurityUtils;

import java.util.List;

/**
 * Validate role assignment to be a subset of allowed roles.
 * If the allowed rules include "admin", any role assignment will be valid.
 */
public class RolesValidator extends StatefulValidator
{
    private final List<String> mAllowedRoles;

    /**
     * Validate against current user's roles.
     */
    public RolesValidator()
    {
        mAllowedRoles = null;
    }

    /**
     * @param allowedRoles allowed user roles (null to validate against current user's roles)
     */
    public RolesValidator(List<String> allowedRoles) {
        mAllowedRoles = allowedRoles;
    }

    /**
     * @param referenceModel reference API key model to obtain allowed user roles
     */
    public RolesValidator(ApiKeyModel referenceModel) {
        this(referenceModel.getRoles());
    }

    @Override
    public boolean validate(Object obj)
    {
        List<String> allowedRoles = mAllowedRoles;
        if (null == allowedRoles) {
            allowedRoles = ApiTokenRealm.getUserModel(SecurityUtils.getSubject()).getRoles();
        }

        if (allowedRoles.contains("admin")) {
            return true;
        }

        List<String> list;
        try {
            //noinspection unchecked
            list = (List<String>) obj;
        } catch (ClassCastException ignored) {
            mMessage = "No permission to assign roles";
            return false;
        }

        for (String role: list) {
            if (!allowedRoles.contains(role)) {
                mMessage = String.format("No permission to assign role: %s", role);
                return false;
            }
        }

        return true;
    }
}
