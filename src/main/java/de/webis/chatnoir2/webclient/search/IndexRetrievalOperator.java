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

package de.webis.chatnoir2.webclient.search;

import de.webis.chatnoir2.webclient.resources.ConfigLoader;
import de.webis.chatnoir2.webclient.util.Configured;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Abstract base class for retrieval operations on Elasticsearch indices.
 */
public abstract class IndexRetrievalOperator extends Configured
{
    /**
     * List of allowed indices.
     */
    private ArrayList<String> mAllowedIndices;

    /**
     * List of default indices.
     */
    private ArrayList<String> mDefaultIndices;

    /**
     * Effective list of indices to search.
     */
    private ArrayList<String> mActiveIndices = null;

    /**
     * @param indices Array of index names to search (null means use default from config).
     *                Indices that are not present in the config will be ignored.
     */
    public IndexRetrievalOperator(String[] indices)
    {
        mAllowedIndices = new ArrayList<>(Arrays.asList(getConf().getStringArray("cluster.indices")));
        mDefaultIndices = new ArrayList<>(Arrays.asList(getConf().getStringArray("cluster.default_indices")));
        if (mDefaultIndices.isEmpty()) {
            mDefaultIndices.addAll(mAllowedIndices);
        }
        setActiveIndices(indices);
    }

    /**
     * Searches pre-defined default indices (from global config).
     */
    public IndexRetrievalOperator()
    {
        this(null);
    }

    /**
     * Select effective search indices from a given array of candidates.
     * If none of the candidates is an allowed index (from global settings),
     * the default indices will be chosen.
     *
     * @param candidateIndices candidate indices to choose from
     */
    public void setActiveIndices(String[] candidateIndices) {
        if (null == mActiveIndices) {
            mActiveIndices = new ArrayList<>();
        }

        mActiveIndices.clear();

        if (null == candidateIndices) {
            mActiveIndices.addAll(mDefaultIndices);
            return;
        }

        for (String index : candidateIndices) {
            if (isIndexAllowed(index)) {
                mActiveIndices.add(index.trim());
            }
        }

        if (mActiveIndices.isEmpty()) {
            mActiveIndices.addAll(mDefaultIndices);
        }
    }

    /**
     * Check whether a given index is an allowed index or an alias of an allowed index.
     *
     * @param indexName index name to check
     * @return whether index is allowed by global configuration
     */
    public boolean isIndexAllowed(String indexName) {
        if (null == indexName) {
            return false;
        }

        indexName = indexName.trim();

        if (mAllowedIndices.contains(indexName)) {
            return true;
        }

        ConfigLoader.Config[] aliases = getConf().get("cluster").getArray("index_aliases");
        for (ConfigLoader.Config c: aliases) {
            String index = c.getString("index");
            String alias = c.getString("alias");
            if ((index.equals(indexName) && mAllowedIndices.contains(alias)) ||
                    (alias.equals(indexName) && mAllowedIndices.contains(index))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get a List of all effectively active indices for search.
     *
     * @return List of index names
     */
    public String[] getEffectiveIndices()
    {
        return mActiveIndices.toArray(new String[mActiveIndices.size()]);
    }

    /**
     * Get a List of all allowed indices for search.
     *
     * @return List of index names
     */
    public String[] getAllowedIndices()
    {
        return mAllowedIndices.toArray(new String[mAllowedIndices.size()]);
    }
}
