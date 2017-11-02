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

/**
 * Stateful validator that allows optional values (null values).
 * By default, values are required.
 */
public abstract class OptionalValidator extends StatefulValidator
{

    private boolean mOptional = false;

    @Override
    public final boolean validate(Object obj)
    {
        if (null == obj && !isOptional()) {
            mMessage = "Valid value required.";
            return false;
        }

        return doValidate(obj);
    }

    /**
     * Validate <tt>obj</tt>;
     *
     * @param obj value object to validate
     * @return true if <tt>obj</tt> passed validation
     */
    protected abstract boolean doValidate(Object obj);

    /**
     * Make value optional. If optional mode is enabled, null values are allowed.
     *
     * @param optional true to enable optional mode
     */
    public OptionalValidator optional(boolean optional)
    {
        mOptional = optional;
        return this;
    }

    /**
     * @return true if value is optional
     */
    public boolean isOptional()
    {
        return mOptional;
    }
}
