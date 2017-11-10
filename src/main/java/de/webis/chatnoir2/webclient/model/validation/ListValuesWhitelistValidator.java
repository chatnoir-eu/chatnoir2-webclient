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

import java.util.List;
import java.util.Set;

/**
 * Validate a list of values against a whitelist of allowed values.
 */
public class ListValuesWhitelistValidator extends StatefulValidator
{
    private final Set mAllowedValues;

    private Object mOffendingItem = null;

    /**
     * @param allowedValues list of allowed values (null means allow all)
     */
    public ListValuesWhitelistValidator(Set allowedValues)
    {
        mAllowedValues = allowedValues;
    }

    @Override
    public boolean validate(Object obj)
    {
        if (null == mAllowedValues) {
            return true;
        }

        if (!(obj instanceof List)) {
            mMessage = "Not a list";
            return false;
        }

        for (Object item: (List) obj) {
            if (!mAllowedValues.contains(item)) {
                mMessage = String.format("List value %s not allowed", item);
                mOffendingItem = item;
                return false;
            }
        }
        return true;
    }

    /**
     * @return get last offending item (or null if {@link #validate(Object)} passes)
     */
    public Object getOffendingItem()
    {
        return mOffendingItem;
    }
}
