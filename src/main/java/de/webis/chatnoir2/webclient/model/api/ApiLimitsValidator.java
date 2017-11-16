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

/**
 * Validator to check if given API limits are within permissible bounds
 */
public class ApiLimitsValidator extends StatefulValidator
{
    private ApiKeyModel.ApiLimits mLimits;

    /**
     * Validate against current user's API limits.
     */
    public ApiLimitsValidator()
    {
        mLimits = null;
    }

    /**
     * @param limits Maximum API limits to compare against (null to validate against current user's limits)
     */
    public ApiLimitsValidator(ApiKeyModel.ApiLimits limits)
    {
        mLimits = limits;
    }

    /**
     * @param referenceModel reference model to obtain API limits from (null to validate against current user's limits)
     */
    public ApiLimitsValidator(ApiKeyModel referenceModel)
    {
        if (null != referenceModel) {
            mLimits = referenceModel.getApiLimits();
        } else {
            mLimits = null;
        }
    }

    /**
     * @param limits new reference limits
     */
    public void setLimits(ApiKeyModel.ApiLimits limits)
    {
        mLimits = limits;
    }

    @Override
    public boolean validate(Object obj)
    {
        ApiKeyModel.ApiLimits referenceLimits = mLimits;
        if (null == mLimits) {
            ApiKeyModel model = ApiTokenRealm.getUserModel(SecurityUtils.getSubject());
            assert model != null;
            referenceLimits = model.getApiLimits();
        }

        if (!(obj instanceof ApiKeyModel.ApiLimits)) {
            throw new RuntimeException("Object is not of type ApiLimits.");
        }

        ApiKeyModel.ApiLimits limits = (ApiKeyModel.ApiLimits) obj;
        if (limits.get("day") != null && referenceLimits.getDailyLimit() > 0 &&
                (limits.get("day") <= 0 || limits.get("day") > referenceLimits.getDailyLimit())) {
            mMessage = "Daily limit out of bounds.";
            return false;
        }
        if (limits.get("week") != null && referenceLimits.getWeeklyLimit() > 0 &&
                (limits.get("week") <= 0 || limits.get("week") > referenceLimits.getWeeklyLimit())) {
            mMessage = "Weekly limit out of bounds.";
            return false;
        }
        if (limits.get("month") != null && referenceLimits.getMonthlyLimit() > 0 &&
                (limits.get("month") <= 0 || limits.get("month") > referenceLimits.getMonthlyLimit())) {
            mMessage = "Monthly limit out of bounds.";
            return false;
        }

        return true;
    }
}
