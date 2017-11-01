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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Map for assigning a number of {@link Validator}s to a common key.
 */
public abstract class ValidatorMap<K>
{
    protected Map<K, List<Validator>> mValidators = new HashMap<>();

    /**
     * Add validator to key.
     *
     * @param key data key
     * @param validator validator
     */
    public void addValidator(K key, Validator validator)
    {
        if (!mValidators.containsKey(key)) {
            mValidators.put(key, new ArrayList<>());
        }

        mValidators.get(key).add(validator);
    }

    /**
     * Remove validator from key.
     *
     * @param key data key
     * @param validator validator to remove
     */
    public void removeValidator(K key, Validator validator)
    {
        if (!mValidators.containsKey(key)) {
            return;
        }

        mValidators.get(key).remove(validator);
    }
}
