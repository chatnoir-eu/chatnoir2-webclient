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

import java.util.ArrayList;
import java.util.List;

/**
 * Stateful recursive list validator.
 */
public class RecursiveListValidator extends OptionalValidator
{
    private List<Validator> mValidators = new ArrayList<>();

    private int mMinElements = 0;

    @Override
    public boolean doValidate(Object obj)
    {
        List list;
        try {
            // noinspection unchecked
            list = (List) obj;
        } catch (ClassCastException ignored) {
            mMessage = "Not a list";
            return false;
        }

        if (list.size() < mMinElements) {
            mMessage = String.format("Minimum required number of elements is %d", mMinElements);
            return false;
        }

        for (Object value: list) {
            for (Validator validator: mValidators) {
                if (!validator.validate(value)) {
                    mMessage = validator.message();
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Set minimum number of elements the list must contain (default: 0)
     *
     * @param minElements minimum number of elements
     */
    public RecursiveListValidator minElements(int minElements)
    {
        mMinElements = minElements;
        return this;
    }

    /**
     * Add validator.
     *
     * @param validator validator
     */
    public void addValidator(Validator validator)
    {
        mValidators.add(validator);
    }

    /**
     * Remove validator.
     *
     * @param validator validator to remove
     */
    public void removeValidator( Validator validator)
    {
        mValidators.remove(validator);
    }
}
