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

import de.webis.chatnoir2.webclient.model.validation.RecursiveMapValidator;

import java.util.HashMap;
import java.util.Map;

/**
 * Model with built-in data validation.
 */
public abstract class ValidatingModel<K, V> extends RecursiveMapValidator implements PersistentModel
{
    private Map<K, V> mData = new HashMap<>();

    /**
     * Get type-cast value for <tt>key</tt>.
     *
     * @param key field name
     * @return stored data or null if no such value exists
     * @throws ClassCastException if trying to cast to an invalid type
     */
    @SuppressWarnings("unchecked")
    public <T> T get(K key)
    {
        return (T) mData.get(key);
    }

    /**
     * Get all model data as a {@link Map}.
     *
     * @return stored data as a Map
     */
    public Map<K, V> getAll()
    {
        return mData;
    }

    /**
     * Add <tt>key</tt>> and map it to <tt>value</tt>.
     *
     * @param key field name
     * @param value field value
     */
    public void put(K key, V value)
    {
        mData.put(key, value);
    }

    /**
     * Copy all mappings from given map if they pass validation.
     * If a single value fails validation, none of the others will be committed either.
     *
     * @param map map of keys and values
     */
    public void putAll(Map<K, V> map)
    {
        mData.putAll(map);
    }

    /**
     * Check if model contains a certain key
     *
     * @param key key name
     */
    public boolean containsKey(K key)
    {
        return mData.containsKey(key);
    }

    /**
     * Remove key/value mapping.
     * This will only remove the data mapping, not its validators.
     *
     * @param key key name
     */
    public void remove(K key)
    {
        mData.remove(key);
    }

    /**
     * Validate model data.
     *
     * @return true if model passes validation
     */
    public boolean validate()
    {
        return validate(mData);
    }

    @Override
    public final boolean commit()
    {
        return validate() && doCommit();
    }

    /**
     * Commit data after validation
     */
    protected abstract boolean doCommit();
}
