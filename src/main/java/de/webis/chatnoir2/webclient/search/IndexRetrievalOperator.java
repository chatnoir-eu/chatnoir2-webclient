/*
 * ChatNoir 2 Web frontend.
 *
 * Copyright (C) 2014-2017 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
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
}
