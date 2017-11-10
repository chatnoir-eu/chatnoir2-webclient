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

package de.webis.chatnoir2.webclient.model.validation;

import org.apache.commons.lang.StringUtils;

/**
 * Validator to check if object is a Long or Integer or numeric (integer) String.
 * Numeric Strings require strict mode to be off to pass validation.
 */
public class IntegerValidator extends OptionalValidator
{
    @Override
    protected boolean doValidate(Object obj)
    {
        if (obj instanceof Long || obj instanceof Integer) {
            return true;
        }

        if (!isStrict() && obj instanceof String) {
            boolean isValid = StringUtils.isNumeric((String) obj);
            if (!isValid) {
                mMessage = "String is not a number.";
                return false;
            }
            return true;
        }

        mMessage = "Not a number";
        return false;
    }
}
