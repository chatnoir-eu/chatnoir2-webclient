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

/**
 * Validator to check if given API limits are within permissible bounds
 */
public class ApiLimitsValidator extends StatefulValidator
{
    private final ApiTokenRealm.ApiLimits mLimits;

    /**
     * @param limits Maximum API limits to compare against
     */
    public ApiLimitsValidator(ApiTokenRealm.ApiLimits limits)
    {
        mLimits = limits;
    }

    @Override
    public boolean validate(Object obj)
    {
        if (!(obj instanceof ApiTokenRealm.ApiLimits)) {
            throw new RuntimeException("Object is not of type ApiLimits.");
        }

        ApiTokenRealm.ApiLimits limits = (ApiTokenRealm.ApiLimits) obj;
        if (mLimits.getDailyLimit() > 0 && limits.getDailyLimit() > mLimits.getDailyLimit()) {
            mMessage = "Daily limit out of bounds.";
            return false;
        }
        if (mLimits.getWeeklyLimit() > 0 && limits.getWeeklyLimit() > mLimits.getWeeklyLimit()) {
            mMessage = "Weekly limit out of bounds.";
            return false;
        }
        if (mLimits.getMonthlyLimit() > 0 && limits.getMonthlyLimit() > mLimits.getMonthlyLimit()) {
            mMessage = "Monthly limit out of bounds.";
            return false;
        }

        return true;
    }
}
