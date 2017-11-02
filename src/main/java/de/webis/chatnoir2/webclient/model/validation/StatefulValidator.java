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
 * Stateful validator base class.
 */
public abstract class StatefulValidator implements Validator
{
    /**
     * Last error message.
     */
    protected String mMessage = null;

    /**
     * Strict mode.
     */
    private boolean mStrict = false;

    @Override
    public String message()
    {
        return mMessage;
    }

    /**
     * Enable or disable strict validation mode.
     * Strict mode may enable additional validation constraints in inheriting validators.
     *
     * @param strict true to enable strict mode
     */
    public StatefulValidator strict(boolean strict)
    {
        mStrict = strict;
        return this;
    }

    /**
     * @return true if strict mode is enabled
     */
    public boolean isStrict()
    {
        return mStrict;
    }
}
