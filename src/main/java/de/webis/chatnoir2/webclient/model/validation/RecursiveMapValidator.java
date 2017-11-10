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
 * Recursively validate map entries.
 */
public class RecursiveMapValidator extends OptionalValidator
{
    /**
     * Last error message.
     */
    protected String mMessage = null;

    /**
     * Validators to be applied at top level.
     */
    protected List<Validator> mRootValidators = new ArrayList<>();

    /**
     * Validators to be applied to specific fields.
     */
    protected final Map<Object, List<Validator>> mValidators = new HashMap<>();

    /**
     * Allowed keys.
     */
    private final List<Object> mAllowedKeys = new ArrayList<>();

    @Override
    protected boolean doValidate(Object obj)
    {
        Map map;
        try {
            // noinspection unchecked
            map = (Map) obj;
        } catch (ClassCastException ignored) {
            mMessage = "Not a map";
            return false;
        }

        if (isStrict() && mAllowedKeys.isEmpty()) {
            mMessage = "List must be empty";
            return false;
        }

        for (Object key: map.keySet()) {
            if (!mAllowedKeys.contains(key)) {
                mMessage = String.format("Illegal key: '%s'", key.toString());
                return false;
            }
        }

        for (Object key: mValidators.keySet()) {
            List<Validator> validators = mValidators.get(key);
            for (Validator validator: validators) {
                if (!validator.validate(map.get(key))) {
                    mMessage = String.format("Error validating '%s': %s", key.toString(), validator.message());
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Add validator to the root level of the validated data structure.
     *
     * @param validator validator
     */
    public void addValidator(Validator validator)
    {
        mRootValidators.add(validator);
    }

    /**
     * Remove validator from the root level of the validated data structure.
     *
     * @param validator validator to remove
     */
    public void removeValidator(Validator validator)
    {
        mRootValidators.remove(validator);
    }

    /**
     * Add validator to a specific key of the validated data structure.
     *
     * @param key data key
     * @param validator validator
     */
    public void addValidator(Object key, Validator validator)
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
    public void removeValidator(Object key, Validator validator)
    {
        if (!mValidators.containsKey(key)) {
            return;
        }

        mValidators.get(key).remove(validator);
    }

    @Override
    public String message()
    {
        return mMessage;
    }

    /**
     * Set whitelist of allowed keys inside the map.
     * If the list is empty and strict mode is on, the validated list must be empty.
     * Otherwise an empty list means that all keys are allowed.
     *
     * @param keys allowed keys
     */
    public RecursiveMapValidator allowedKeys(List<Object> keys)
    {
        mAllowedKeys.clear();
        mAllowedKeys.addAll(keys);
        return this;
    }
}
