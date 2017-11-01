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

package de.webis.chatnoir2.webclient.model;

import de.webis.chatnoir2.webclient.model.validation.Validator;
import de.webis.chatnoir2.webclient.model.validation.ValidatorMap;

import java.util.HashMap;
import java.util.Map;

/**
 * Model with built-in data validation.
 */
public abstract class ValidatingModel<K, V> extends ValidatorMap<K> implements PersistentModel
{
    /**
     * Data validation exception.
     */
    public static class ValidationException extends Exception
    {
        public ValidationException(String message)
        {
            super(message);
        }
    }

    private Map<K, V> mData = new HashMap<>();

    /**
     * Get value for <tt>key</tt>.
     *
     * @param key field name
     * @return stored data or null if no such value exists
     */
    public V get(K key)
    {
        return mData.get(key);
    }

    /**
     * Add <tt>key</tt>> and map it to <tt>value</tt>.
     *
     * @param key field name
     * @param value field value
     * @throws ValidationException if <tt>value</tt> fails validation
     */
    public void put(K key, V value) throws ValidationException
    {
        if (mValidators.containsKey(key)) {
            for (Validator validator: mValidators.get(key)) {
                if (!validator.validate(value)) {
                    throw new ValidationException(validator.message());
                }
            }
        }

        mData.put(key, value);
    }

    /**
     * Copy all mappings from given map if they pass validation.
     * If a single value fails validation, none of the others will be committed either.
     *
     * @param map map of keys and values
     * @throws ValidationException if an entry in <tt>map</tt> fails validation
     */
    public void putAll(Map<K, V> map) throws ValidationException
    {
        for (K key: map.keySet()) {
            if (mValidators.containsKey(key)) {
                for (Validator validator : mValidators.get(key)) {
                    if (!validator.validate(map.get(key))) {
                        throw new ValidationException(validator.message());
                    }
                }
            }
        }

        mData.putAll(map);
    }

    /**
     * Remove key/value mapping.
     * This will only remove the data mapping, not its validators.
     *
     * @param key field name
     */
    public void remove(K key) throws ValidationException
    {
        mData.remove(key);
    }
}
